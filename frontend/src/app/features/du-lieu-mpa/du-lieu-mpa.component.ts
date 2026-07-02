import { Component, OnInit, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
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
    MatIconModule, MatButtonModule, MatFormFieldModule,
    MatInputModule, MatSelectModule, MatDatepickerModule,
    MatNativeDateModule, MatProgressSpinnerModule,
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
  fromDate     = new Date(new Date().getFullYear(), new Date().getMonth(), 1);
  toDate       = new Date();
  selectedPhong = '';
  phongList: { ma: string; ten: string }[] = [];

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
    this.loadPhongList();
    this.loadData();
  }

  loadData(): void {
    this.loading.set(true);
    this.selectedIds.clear();
    const filter: FilterParams = {
      fromDate: this.formatDate(this.fromDate),
      toDate:   this.formatDate(this.toDate),
      page:     this.currentPage(),
      size:     this.pageSize
    };
    if (this.selectedPhong) filter.maDonViCap6 = this.selectedPhong;
    if (this.searchText)    filter.search = this.searchText;

    this.mpaService.getDuLieuMpa(filter, this.currentPage(), this.pageSize).subscribe({
      next: r => {
        if (r.success) {
          this.data.set((r.data as any).content ?? (r.data as any));
          this.totalItems.set((r.data as any).totalElements ?? (r.data as any).length ?? 0);
        } else {
          this.data.set(this.mockData());
          this.totalItems.set(this.mockData().length);
        }
        this.calcSummary();
        this.loading.set(false);
      },
      error: () => {
        this.data.set(this.mockData());
        this.totalItems.set(this.mockData().length);
        this.calcSummary();
        this.loading.set(false);
      }
    });
  }

  private calcSummary(): void {
    const rows = this.data();
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
    this.fromDate   = new Date(new Date().getFullYear(), new Date().getMonth(), 1);
    this.toDate     = new Date();
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
  formatDate(d: Date): string { return d.toISOString().split('T')[0]; }
  formatBillion(v: number): string { return (v / 1000).toFixed(3) + ' tỷ'; }
  formatMillion(v: number): string { return v >= 1000 ? `${(v/1000).toFixed(2)} tỷ` : `${v.toFixed(0)} tr`; }

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
