import { Component, OnInit, signal, inject } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';

import { MpaService } from '../../core/services/mpa.service';
import { ThongTinKhachHangItem, ThongTinKhachHangSaveRequest, UnitOption } from '../../core/models/mpa.model';
import { PageResponse } from '../../core/models/user.model';

@Component({
  selector: 'app-khach-hang',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule, MatIconModule, MatProgressSpinnerModule, MatTooltipModule, DatePipe],
  templateUrl: './khach-hang.component.html',
  styleUrl: './khach-hang.component.scss'
})
export class KhachHangComponent implements OnInit {
  private mpaService = inject(MpaService);
  private router = inject(Router);

  // ── Filter ────────────────────────────────────────────────────────────
  searchText     = '';
  selectedType: number | null = null;

  // ── Data ─────────────────────────────────────────────────────────────
  loading  = signal(false);
  pageData = signal<PageResponse<ThongTinKhachHangItem> | null>(null);
  currentPage = 0;
  readonly pageSize = 20;

  // ── Dropdown options ─────────────────────────────────────────────────
  phanKhucTypes = signal<{ value: number; label: string }[]>([]);
  phongOptions  = signal<UnitOption[]>([]);
  amOptions     = signal<{ ma: string; ten: string }[]>([]);

  readonly PHAN_KHUC_LABELS: Record<number, string> = {
    1: 'Khối Bán Buôn',
    2: 'Khối Bán Lẻ',
    3: 'Khối FDI',
    4: 'Khách hàng đặc biệt'
  };

  // ── Dialog ────────────────────────────────────────────────────────────
  showDialog = signal(false);
  saving     = signal(false);
  editId: number | null = null;
  dlg: ThongTinKhachHangSaveRequest = this.emptyDlg();

  ngOnInit(): void {
    this.loadPage(0);
    this.loadDropdowns();
  }

  // ── Load ─────────────────────────────────────────────────────────────

  loadPage(page: number): void {
    this.currentPage = page;
    this.loading.set(true);
    this.mpaService.getKhachHangList(this.searchText.trim(), this.selectedType, page, this.pageSize).subscribe({
      next: res => {
        if (res.success) this.pageData.set(res.data);
        this.loading.set(false);
      },
      error: () => this.loading.set(false)
    });
  }

  search(): void { this.loadPage(0); }

  onTypeChange(): void { this.loadPage(0); }

  private loadDropdowns(): void {
    this.mpaService.getKhachHangTypes().subscribe(res => {
      if (res.success) {
        this.phanKhucTypes.set(
          res.data.map(v => ({ value: v, label: this.PHAN_KHUC_LABELS[v] ?? 'Loại ' + v }))
        );
      }
    });
    this.mpaService.getPhongListForCt().subscribe(res => {
      if (res.success) this.phongOptions.set(res.data);
    });
    this.mpaService.getAmDropdown().subscribe(res => {
      if (res.success) this.amOptions.set(res.data);
    });
  }

  // ── Pagination ────────────────────────────────────────────────────────

  get totalPages(): number { return this.pageData()?.totalPages ?? 0; }
  get totalElements(): number { return this.pageData()?.totalElements ?? 0; }
  get items(): ThongTinKhachHangItem[] { return this.pageData()?.content ?? []; }

  pageNumbers(): number[] {
    const total = this.totalPages;
    const cur   = this.currentPage;
    const range: number[] = [];
    const delta = 2;
    for (let i = Math.max(0, cur - delta); i <= Math.min(total - 1, cur + delta); i++) {
      range.push(i);
    }
    return range;
  }

  stt(index: number): number { return this.currentPage * this.pageSize + index + 1; }

  // ── Dialog ────────────────────────────────────────────────────────────

  openAdd(): void {
    this.editId = null;
    this.dlg = this.emptyDlg();
    this.showDialog.set(true);
  }

  openEdit(item: ThongTinKhachHangItem): void {
    this.editId = item.id;
    this.dlg = {
      maCn:          item.maCn,
      maDonViCap6:   item.maDonViCap6,
      maAm:          item.maAm,
      maKhCif:       item.maKhCif,
      tenKhachHang:  item.tenKhachHang,
      soDienThoai:   item.soDienThoai,
      ngayBatDau:    item.ngayBatDau,
      email:         item.email,
      typeKhachHang: item.typeKhachHang,
      trangThai:     item.trangThai
    };
    this.showDialog.set(true);
  }

  closeDialog(): void { this.showDialog.set(false); }

  saveDialog(): void {
    if (!this.dlg.maKhCif?.trim() || !this.dlg.tenKhachHang?.trim()) return;
    this.saving.set(true);
    const obs$ = this.editId
      ? this.mpaService.updateKhachHang(this.editId, this.dlg)
      : this.mpaService.createKhachHang(this.dlg);

    obs$.subscribe({
      next: res => {
        this.saving.set(false);
        if (res.success) { this.showDialog.set(false); this.loadPage(this.currentPage); }
      },
      error: () => this.saving.set(false)
    });
  }

  delete(id: number): void {
    if (!confirm('Xác nhận xóa khách hàng này?')) return;
    this.mpaService.deleteKhachHang(id).subscribe(res => {
      if (res.success) this.loadPage(this.currentPage);
    });
  }

  // ── Helpers ───────────────────────────────────────────────────────────

  onAmChange(): void {
    const am = this.amOptions().find(a => a.ma === this.dlg.maAm);
    if (am) {
      const phong = this.phongOptions().find(p => p.ma === this.dlg.maDonViCap6);
      if (!phong) this.dlg.maDonViCap6 = null;
    }
  }

  get canSave(): boolean {
    return !!this.dlg.maKhCif?.trim() && !!this.dlg.tenKhachHang?.trim() && !this.saving();
  }

  phanKhucLabel(type: number | null): string {
    if (type == null) return '—';
    return this.PHAN_KHUC_LABELS[type] ?? 'Loại ' + type;
  }

  phanKhucClass(type: number | null): string {
    switch (type) {
      case 1: return 'pk-ban-buon';
      case 2: return 'pk-ban-le';
      case 3: return 'pk-fdi';
      case 4: return 'pk-dac-biet';
      default: return 'pk-other';
    }
  }

  exportExcel(): void {
    alert('Chức năng Xuất Excel đang được phát triển.');
  }

  goToDetail(cif: string): void {
    this.router.navigate(['/khach-hang', cif]);
  }

  private emptyDlg(): ThongTinKhachHangSaveRequest {
    return {
      maCn: null, maDonViCap6: null, maAm: null,
      maKhCif: '', tenKhachHang: '',
      soDienThoai: null, ngayBatDau: null, email: null,
      typeKhachHang: null, trangThai: 1
    };
  }
}
