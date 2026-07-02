import { Component, OnInit, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';

import { MpaService } from '../../core/services/mpa.service';
import { ThongTinAmItem, ThongTinAmSaveRequest, CanBoGroup, UnitOption } from '../../core/models/mpa.model';
import { PageResponse } from '../../core/models/user.model';

type ViewMode = 'ma-am' | 'can-bo';

@Component({
  selector: 'app-quan-ly-am',
  standalone: true,
  imports: [CommonModule, FormsModule, MatIconModule, MatButtonModule, MatProgressSpinnerModule, MatTooltipModule],
  templateUrl: './quan-ly-am.component.html',
  styleUrl: './quan-ly-am.component.scss'
})
export class QuanLyAmComponent implements OnInit {
  private mpaService = inject(MpaService);

  // ── View state ────────────────────────────────────────────────────────
  viewMode: ViewMode = 'ma-am';
  searchText = '';
  loading = signal(false);

  // ── Theo mã AM (paginated) ────────────────────────────────────────────
  pageData = signal<PageResponse<ThongTinAmItem> | null>(null);
  currentPage = 0;
  readonly pageSize = 20;

  // ── Theo cán bộ (all, client-side grouping) ──────────────────────────
  allItems = signal<ThongTinAmItem[]>([]);
  canBoGroups = signal<CanBoGroup[]>([]);
  expandedCanBo = new Set<string>();

  // ── Dialog ────────────────────────────────────────────────────────────
  showDialog = signal(false);
  saving = signal(false);
  editId: number | null = null;
  phongOptions = signal<UnitOption[]>([]);

  dlg: ThongTinAmSaveRequest = this.emptyDlg();

  readonly loaiAmOptions = ['Tín dụng', 'CASA', 'Thẻ', 'Tổng hợp'];

  ngOnInit(): void {
    this.load();
    this.loadPhongOptions();
  }

  // ── Load data ─────────────────────────────────────────────────────────

  load(): void {
    if (this.viewMode === 'ma-am') {
      this.loadPage(0);
    } else {
      this.loadAll();
    }
  }

  loadPage(page: number): void {
    this.currentPage = page;
    this.loading.set(true);
    this.mpaService.getQuanLyAmList(this.searchText.trim(), page, this.pageSize).subscribe({
      next: res => {
        if (res.success) this.pageData.set(res.data);
        this.loading.set(false);
      },
      error: () => this.loading.set(false)
    });
  }

  loadAll(): void {
    this.loading.set(true);
    this.mpaService.getQuanLyAmAll(this.searchText.trim()).subscribe({
      next: res => {
        if (res.success) {
          this.allItems.set(res.data);
          this.buildGroups(res.data);
        }
        this.loading.set(false);
      },
      error: () => this.loading.set(false)
    });
  }

  buildGroups(items: ThongTinAmItem[]): void {
    const map = new Map<string, CanBoGroup>();
    for (const item of items) {
      const key = item.tenAm ?? '(Chưa có tên)';
      if (!map.has(key)) {
        map.set(key, {
          tenAm: key,
          maDonViCap6: item.maDonViCap6,
          tenPhong: item.tenPhong,
          items: [],
          trangThai: item.trangThai
        });
      }
      map.get(key)!.items.push(item);
    }
    this.canBoGroups.set(Array.from(map.values()));
  }

  onSearch(): void {
    this.load();
  }

  onViewModeChange(mode: ViewMode): void {
    this.viewMode = mode;
    this.load();
  }

  // ── Pagination ────────────────────────────────────────────────────────

  get totalPages(): number {
    return this.pageData()?.totalPages ?? 0;
  }

  get pageItems(): ThongTinAmItem[] {
    return this.pageData()?.content ?? [];
  }

  pageNumbers(): number[] {
    return Array.from({ length: this.totalPages }, (_, i) => i);
  }

  stt(index: number): number {
    return this.currentPage * this.pageSize + index + 1;
  }

  // ── Cán bộ expand/collapse ────────────────────────────────────────────

  toggleExpand(tenAm: string): void {
    if (this.expandedCanBo.has(tenAm)) {
      this.expandedCanBo.delete(tenAm);
    } else {
      this.expandedCanBo.add(tenAm);
    }
  }

  isExpanded(tenAm: string): boolean {
    return this.expandedCanBo.has(tenAm);
  }

  // ── Dialog ────────────────────────────────────────────────────────────

  openAdd(): void {
    this.editId = null;
    this.dlg = this.emptyDlg();
    this.showDialog.set(true);
  }

  openEdit(item: ThongTinAmItem): void {
    this.editId = item.id;
    this.dlg = {
      maCn: item.maCn,
      maDonViCap6: item.maDonViCap6,
      maAm: item.maAm,
      tenAm: item.tenAm,
      soDienThoai: item.soDienThoai,
      ngayBatDau: item.ngayBatDau,
      email: item.email,
      chucVu: item.chucVu,
      loaiAm: item.loaiAm,
      trangThai: item.trangThai
    };
    this.showDialog.set(true);
  }

  closeDialog(): void {
    this.showDialog.set(false);
  }

  saveDialog(): void {
    if (!this.dlg.maAm?.trim() || !this.dlg.tenAm?.trim()) return;
    this.saving.set(true);
    const obs$ = this.editId
      ? this.mpaService.updateQuanLyAm(this.editId, this.dlg)
      : this.mpaService.createQuanLyAm(this.dlg);

    obs$.subscribe({
      next: res => {
        this.saving.set(false);
        if (res.success) {
          this.showDialog.set(false);
          this.load();
        }
      },
      error: () => this.saving.set(false)
    });
  }

  delete(id: number): void {
    if (!confirm('Xác nhận xóa mã AM này?')) return;
    this.mpaService.deleteQuanLyAm(id).subscribe(res => {
      if (res.success) this.load();
    });
  }

  onPhongChange(): void {
    const phong = this.phongOptions().find(p => p.ma === this.dlg.maDonViCap6);
    this.dlg.maCn = phong ? null : null;
  }

  // ── Helpers ───────────────────────────────────────────────────────────

  private loadPhongOptions(): void {
    this.mpaService.getPhongListForCt().subscribe(res => {
      if (res.success) this.phongOptions.set(res.data);
    });
  }

  private emptyDlg(): ThongTinAmSaveRequest {
    return {
      maCn: null, maDonViCap6: null,
      maAm: '', tenAm: '',
      soDienThoai: null, ngayBatDau: null,
      email: null, chucVu: null, loaiAm: null,
      trangThai: 1
    };
  }

  loaiBadgeClass(loai: string | null): string {
    const l = (loai ?? '').toLowerCase();
    if (l.includes('tín dụng') || l === 'credit') return 'loai-td';
    if (l.includes('casa'))                        return 'loai-casa';
    if (l.includes('thẻ') || l === 'card')         return 'loai-the';
    return 'loai-other';
  }

  loaiLabel(loai: string | null): string {
    return loai || '—';
  }

  get canSave(): boolean {
    return !!this.dlg.maAm?.trim() && !!this.dlg.tenAm?.trim() && !this.saving();
  }

  importExcel(): void {
    alert('Chức năng Import Excel đang được phát triển.');
  }
}
