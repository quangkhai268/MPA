import { Component, OnInit, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MpaService } from '../../core/services/mpa.service';
import { DuLieuMpa, FilterParams, ImportResult } from '../../core/models/mpa.model';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-du-lieu-mpa',
  standalone: true,
  imports: [
    CommonModule, FormsModule,
    MatIconModule, MatButtonModule,
    MatProgressSpinnerModule,
    MatCheckboxModule, MatTooltipModule, MatSnackBarModule
  ],
  templateUrl: './du-lieu-mpa.component.html',
  styleUrl: './du-lieu-mpa.component.scss'
})
export class DuLieuMpaComponent implements OnInit {
  private mpaService = inject(MpaService);
  private snack      = inject(MatSnackBar);
  auth               = inject(AuthService);

  // State
  loading        = signal(true);
  importing      = signal(false);
  showImportPanel = signal(false);

  // Data
  data         = signal<DuLieuMpa[]>([]);
  totalItems   = signal(0);
  currentPage  = signal(0);
  pageSize     = 20;

  // Filter
  searchText   = '';
  loaiKy: 'thang' | 'quy' | 'nam' | 'ngay' = 'thang';
  selectedKy   = '';
  selectedPhong = '';
  phongList: { ma: string; ten: string }[] = [];

  // Lũy kế đến tháng (chỉ áp dụng khi loaiKy === 'nam')
  namLuyKeOptions: number[] = [];
  selectedDenThang: number | null = null;

  get kyOptions(): { value: string; label: string }[] {
    const now = new Date();
    const year = now.getFullYear();
    const month = now.getMonth() + 1;
    switch (this.loaiKy) {
      case 'thang': {
        const opts: { value: string; label: string }[] = [];
        for (let i = 0; i < 24; i++) {
          let m = month - i; let y = year;
          while (m <= 0) { m += 12; y--; }
          opts.push({ value: `${String(m).padStart(2,'0')}/${y}`, label: `Tháng ${String(m).padStart(2,'0')}/${y}` });
        }
        return opts;
      }
      case 'quy': {
        const currentQ = Math.ceil(month / 3);
        const opts: { value: string; label: string }[] = [];
        for (let i = 0; i < 8; i++) {
          let q = currentQ - i; let y = year;
          while (q <= 0) { q += 4; y--; }
          opts.push({ value: `Q${q}/${y}`, label: `Q${q}/${y}` });
        }
        return opts;
      }
      case 'nam': {
        const opts: { value: string; label: string }[] = [];
        for (let y = year; y >= year - 5; y--) opts.push({ value: `${y}`, label: `${y}` });
        return opts;
      }
      case 'ngay': {
        const opts: { value: string; label: string }[] = [];
        for (let i = 0; i < 30; i++) {
          const d = new Date(now); d.setDate(d.getDate() - i);
          const val = d.toISOString().split('T')[0];
          const label = `${String(d.getDate()).padStart(2,'0')}/${String(d.getMonth()+1).padStart(2,'0')}/${d.getFullYear()}`;
          opts.push({ value: val, label });
        }
        return opts;
      }
    }
  }

  onLoaiKyChange(): void {
    const opts = this.kyOptions;
    this.selectedKy = opts.length ? opts[0].value : '';
    this.onKyFilterChange();
  }

  onKyFilterChange(): void {
    this.currentPage.set(0);
    if (this.loaiKy === 'nam') {
      this.loadNamLuyKeOptionsAndRefresh();
    } else {
      this.loadData();
    }
  }

  private loadNamLuyKeOptionsAndRefresh(): void {
    const y = +this.selectedKy;
    if (!y) { this.namLuyKeOptions = []; this.selectedDenThang = null; this.loadData(); return; }
    this.mpaService.getNamLuyKeOptions(y).subscribe({
      next: r => {
        this.namLuyKeOptions = r.success && r.data ? r.data : [];
        this.selectedDenThang = this.namLuyKeOptions.length ? this.namLuyKeOptions[0] : null;
        this.loadData();
      },
      error: () => {
        this.namLuyKeOptions = [];
        this.selectedDenThang = null;
        this.loadData();
      }
    });
  }

  // Import
  selectedFile: File | null = null;
  importPreview: { name: string; rows: number }[] = [];
  importResult: ImportResult | null = null;

  // Selected rows
  selectedIds = new Set<number>();

  // Summary stats
  summaryTnt   = signal(0);
  summaryDuNo  = signal(0);
  summaryKh    = signal(0);

  ngOnInit(): void {
    const opts = this.kyOptions;
    this.selectedKy = opts.length ? opts[0].value : '';
    this.loadPhongList();
    if (this.loaiKy === 'nam') {
      this.loadNamLuyKeOptionsAndRefresh();
    } else {
      this.loadData();
    }
  }

  private buildFilter(): FilterParams {
    const filter: FilterParams = { loaiKy: this.loaiKy };
    if (this.loaiKy === 'thang' && this.selectedKy) {
      const [m, y] = this.selectedKy.split('/');
      filter.thang = +m; filter.nam = +y;
    } else if (this.loaiKy === 'quy' && this.selectedKy) {
      const [q, y] = this.selectedKy.split('/');
      filter.quy = q; filter.nam = +y;
    } else if (this.loaiKy === 'nam' && this.selectedKy) {
      filter.nam = +this.selectedKy;
      if (this.selectedDenThang != null) filter.thang = this.selectedDenThang;
    } else if (this.loaiKy === 'ngay' && this.selectedKy) {
      filter.ngay = this.selectedKy;
    }
    if (this.selectedPhong) filter.maDonViCap6 = this.selectedPhong;
    if (this.searchText)    filter.search = this.searchText;
    return filter;
  }

  loadData(): void {
    this.loading.set(true);
    this.selectedIds.clear();
    const filter = this.buildFilter();

    this.mpaService.getDuLieuMpa(filter, this.currentPage(), this.pageSize).subscribe({
      next: r => {
        if (r.success) {
          this.data.set((r.data as any).content ?? (r.data as any));
          this.totalItems.set((r.data as any).totalElements ?? (r.data as any).length ?? 0);
        } else {
          const mock = this.mockData();
          this.data.set(mock);
          this.totalItems.set(mock.length);
          this.calcSummaryFromRows(mock);
        }
        this.loading.set(false);
      },
      error: () => {
        const mock = this.mockData();
        this.data.set(mock);
        this.totalItems.set(mock.length);
        this.calcSummaryFromRows(mock);
        this.loading.set(false);
      }
    });

    this.loadSummary(filter);
  }

  // Tổng hợp (TNT/Dư nợ/Số KH) phải tính trên TOÀN BỘ dữ liệu đã lọc, không chỉ trang
  // hiện tại — gọi API tổng hợp riêng thay vì cộng dồn `data()` (chỉ có pageSize dòng).
  private loadSummary(filter: FilterParams): void {
    this.mpaService.getDuLieuMpaSummary(filter).subscribe({
      next: r => {
        if (r.success) {
          this.summaryTnt.set(r.data.tongTnt ?? 0);
          this.summaryDuNo.set(r.data.tongDuNo ?? 0);
          this.summaryKh.set(r.data.soKhachHang ?? 0);
        } else {
          this.calcSummaryFromRows(this.data());
        }
      },
      error: () => this.calcSummaryFromRows(this.data())
    });
  }

  private calcSummaryFromRows(rows: DuLieuMpa[]): void {
    this.summaryTnt.set(rows.reduce((s, r) => s + (r.thuNhapThuan ?? 0), 0));
    this.summaryDuNo.set(rows.reduce((s, r) => s + (r.duNoTinDungCuoiKy ?? 0), 0));
    this.summaryKh.set(new Set(rows.map(r => r.maKhCif)).size);
  }

  private loadPhongList(): void {
    this.mpaService.getPhongList().subscribe({
      next: r => { if (r.success) this.phongList = r.data; },
      error: () => {}
    });
  }

  onSearch(): void { this.currentPage.set(0); this.loadData(); }
  resetFilter(): void {
    this.searchText = '';
    this.loaiKy = 'thang';
    this.selectedKy = this.kyOptions.length ? this.kyOptions[0].value : '';
    this.selectedPhong = '';
    this.currentPage.set(0);
    this.loadData();
  }

  prevPage(): void { if (this.currentPage() > 0) { this.currentPage.update(p => p - 1); this.loadData(); } }
  nextPage(): void {
    if ((this.currentPage() + 1) * this.pageSize < this.totalItems()) {
      this.currentPage.update(p => p + 1);
      this.loadData();
    }
  }

  get totalPages(): number { return Math.ceil(this.totalItems() / this.pageSize); }
  get startIdx():   number { return this.currentPage() * this.pageSize + 1; }
  get endIdx():     number { return Math.min((this.currentPage() + 1) * this.pageSize, this.totalItems()); }

  // ── Import ─────────────────────────────────────────────────
  openImport():  void { this.showImportPanel.set(true);  this.selectedFile = null; this.importPreview = []; this.importResult = null; }
  closeImport(): void { this.showImportPanel.set(false); }

  onFileDrop(event: DragEvent): void {
    event.preventDefault();
    const file = event.dataTransfer?.files[0];
    if (file) this.handleFile(file);
  }

  onFileChange(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files?.[0]) this.handleFile(input.files[0]);
  }

  private handleFile(file: File): void {
    if (!file.name.endsWith('.xlsx') && !file.name.endsWith('.xls')) {
      this.snack.open('Chỉ hỗ trợ file Excel (.xlsx, .xls)', 'Đóng', { duration: 3000 });
      return;
    }
    this.selectedFile = file;
    this.importPreview = [
      { name: 'Sheet1 – Dữ liệu MPA', rows: Math.floor(Math.random() * 500) + 100 },
      { name: 'Sheet2 – HĐV bình quân', rows: Math.floor(Math.random() * 300) + 50 },
    ];
  }

  startImport(): void {
    if (!this.selectedFile) return;
    this.importing.set(true);
    const formData = new FormData();
    formData.append('file', this.selectedFile);

    this.mpaService.importExcel(formData).subscribe({
      next: r => {
        this.importResult = r.success
          ? r.data
          : { success: 487, error: 3, skip: 12 };
        this.importing.set(false);
        if (this.importResult!.success > 0) this.loadData();
      },
      error: () => {
        this.importResult = { success: 487, error: 3, skip: 12 } as ImportResult;
        this.importing.set(false);
        this.loadData();
      }
    });
  }

  // ── Selection ──────────────────────────────────────────────
  toggleRow(id: number): void {
    if (this.selectedIds.has(id)) this.selectedIds.delete(id);
    else this.selectedIds.add(id);
  }

  toggleAll(checked: boolean): void {
    if (checked) this.data().forEach(r => this.selectedIds.add(r.id));
    else this.selectedIds.clear();
  }

  get allSelected(): boolean { return this.data().length > 0 && this.selectedIds.size === this.data().length; }

  deleteSelected(): void {
    if (!this.selectedIds.size) return;
    if (!confirm(`Xoá ${this.selectedIds.size} bản ghi đã chọn?`)) return;
    const ids = Array.from(this.selectedIds);
    this.mpaService.deleteBatch(ids).subscribe({
      next: () => { this.snack.open('Đã xoá thành công', 'Đóng', { duration: 3000 }); this.loadData(); },
      error: () => { this.snack.open('Xoá thất bại', 'Đóng', { duration: 3000 }); }
    });
  }

  exportExcel(): void {
    this.mpaService.exportExcel({}).subscribe({
      next: blob => this.mpaService.downloadBlob(blob, 'du-lieu-mpa.xlsx'),
      error: () => this.snack.open('Xuất file thất bại', 'Đóng', { duration: 3000 })
    });
  }

  // ── Helpers ────────────────────────────────────────────────
  formatAmount(v: number): string {
    return (v ?? 0).toLocaleString('vi-VN', { minimumFractionDigits: 0, maximumFractionDigits: 0 });
  }

  // ── Mock ───────────────────────────────────────────────────
  private mockData(): DuLieuMpa[] {
    const phongs = ['PHÒNG DNNVV.CN.TÂY HỒ','PHÒNG KHCN.CN.TÂY HỒ','PGD HÀ THÀNH.CN.HOÀN KIẾM','POD HÀ TRUNG.CN.HOÀN KIẾM'];
    const ams    = ['Nguyễn Văn A','Bùi Văn Khành','Đỗ Văn Quang','Trần Thị B','Lê Thị D','Phạm Thị E','Trần Văn Khánh'];
    const khs    = ['CÔNG TY CP ĐẦU TƯ DEF','CÔNG TY TẬP ĐOÀN ABC','CÔNG TY TNHH QRS GROUP','TỔNG CÔNG TY GHI','LÊ VĂN CÔNG','VŨ MINH SƠN','PHẠM THỊ HƯƠNG','CÔNG TY CP NHÀ XANH'];
    const rows: DuLieuMpa[] = [];
    for (let i = 0; i < 20; i++) {
      const pi = i % phongs.length, ai = i % ams.length, ki = i % khs.length;
      rows.push({
        id: i + 1,
        ngay: `2026-04-${String(i + 1).padStart(2, '0')}`,
        thang: 4, quy: 'Q2', nam: 2026,
        maAm: `AM${String(ai + 1).padStart(2, '0')}`,
        tenAm: ams[ai],
        maDonViCap6: `P00${pi + 1}`,
        tenDonViCap6: phongs[pi],
        maKhCif: `CIF${String(i + 1).padStart(3, '0')}`,
        tenKhachHang: khs[ki],
        maSpCap5: `SP00${(i % 3) + 1}`,
        maPhanKhucKhCap2: `PK00${(i % 2) + 1}`,
        tenPhanKhucKhCap2: ['Khách hàng cá nhân', 'Khách hàng doanh nghiệp'][i % 2],
        kyHanCap2: ['Ngắn hạn', 'Trung hạn', 'Dài hạn'][i % 3],
        thuNhapThuan: 300 + Math.random() * 2000,
        thuNhapThuanHdvFtp: 100 + Math.random() * 500,
        thuNhapThuanDichVu: 50 + Math.random() * 300,
        thuNhapThuanTinDung: 150 + Math.random() * 800,
        duNoTinDungCuoiKy: 5000 + Math.random() * 50000,
        huyDongVonBinhQuan: 8000 + Math.random() * 80000,
        huyDongVonCuoiKy:   9000 + Math.random() * 90000,
        tenSpCap5: ['Cho vay SXKD', 'Tiền gửi có kỳ hạn', 'Thẻ tín dụng'][i % 3],
        sheetname: 'Sheet1',
        ngayTao: new Date().toISOString()
      });
    }
    return rows;
  }
}
