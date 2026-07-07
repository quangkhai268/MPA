import { Component, OnInit, signal, inject } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';

import { MpaService } from '../../core/services/mpa.service';
import { ThePhatHanhItem, TheSummary } from '../../core/models/mpa.model';
import { PageResponse } from '../../core/models/user.model';

@Component({
  selector: 'app-quan-ly-the',
  standalone: true,
  imports: [CommonModule, FormsModule, MatIconModule, MatProgressSpinnerModule, MatTooltipModule, DatePipe],
  templateUrl: './quan-ly-the.component.html',
  styleUrl: './quan-ly-the.component.scss'
})
export class QuanLyTheComponent implements OnInit {
  private mpaService = inject(MpaService);
  private router = inject(Router);

  // ── Tab ───────────────────────────────────────────────────────────────
  activeTab = 0;

  // ── Filters ───────────────────────────────────────────────────────────
  searchText     = '';
  trangThai      = '';
  hinhThuc       = '';
  productCode    = '';
  loaiTheTinDung = '';
  chuaKichHoat   = false;
  chuaPsgd       = false;
  chuaDatPtn     = false;
  soNgayMin      = 7;

  // ── Chưa kích hoạt: tùy chọn ngưỡng số ngày ────────────────────────────
  soNgayMinOptions = [
    { value: 0,  label: 'Tất cả thẻ chưa kích hoạt' },
    { value: 7,  label: '> 7 ngày' },
    { value: 14, label: '> 14 ngày' },
    { value: 30, label: '> 30 ngày' },
    { value: 60, label: '> 60 ngày' },
    { value: 90, label: '> 90 ngày' },
  ];

  // ── Dropdown options ─────────────────────────────────────────────────
  trangThaiOptions  = signal<string[]>([]);
  hinhThucOptions   = signal<string[]>([]);
  productOptions    = signal<string[]>([]);

  // ── Summary (KPI cards) ──────────────────────────────────────────────
  summary = signal<TheSummary | null>(null);
  summaryLoading = signal(false);

  // ── Table data ────────────────────────────────────────────────────────
  loading   = signal(false);
  pageData  = signal<PageResponse<ThePhatHanhItem> | null>(null);
  currentPage = 0;
  readonly pageSize = 20;

  ngOnInit(): void {
    this.loadSummary();
    this.loadDropdowns();
    this.loadPage(0);
  }

  // ── Tabs ──────────────────────────────────────────────────────────────

  switchTab(tab: number): void {
    this.activeTab = tab;
    this.chuaKichHoat = tab === 1;
    this.chuaPsgd     = tab === 2;
    this.chuaDatPtn   = tab === 3;
    this.loadPage(0);
  }

  // ── Load ─────────────────────────────────────────────────────────────

  private loadSummary(): void {
    this.summaryLoading.set(true);
    this.mpaService.getTheSummary().subscribe({
      next: res => {
        if (res.success) this.summary.set(res.data);
        this.summaryLoading.set(false);
      },
      error: () => this.summaryLoading.set(false)
    });
  }

  private loadDropdowns(): void {
    this.mpaService.getTheTrangThaiOptions().subscribe(res => {
      if (res.success) this.trangThaiOptions.set(res.data);
    });
    this.mpaService.getTheHinhThucOptions().subscribe(res => {
      if (res.success) this.hinhThucOptions.set(res.data);
    });
    this.mpaService.getTheProductOptions().subscribe(res => {
      if (res.success) this.productOptions.set(res.data);
    });
  }

  loadPage(page: number): void {
    this.currentPage = page;
    this.loading.set(true);
    this.mpaService.getTheList(
      this.searchText.trim(),
      this.trangThai, this.hinhThuc, this.productCode,
      this.loaiTheTinDung,
      this.chuaKichHoat, this.soNgayMin, this.chuaPsgd, this.chuaDatPtn,
      page, this.pageSize
    ).subscribe({
      next: res => {
        if (res.success) this.pageData.set(res.data);
        this.loading.set(false);
      },
      error: () => this.loading.set(false)
    });
  }

  search(): void { this.loadPage(0); }

  goToDetail(id: number): void {
    this.router.navigate(['/quan-ly-the', id]);
  }

  // ── Pagination helpers ────────────────────────────────────────────────

  get totalPages(): number    { return this.pageData()?.totalPages ?? 0; }
  get totalElements(): number { return this.pageData()?.totalElements ?? 0; }
  get items(): ThePhatHanhItem[] { return this.pageData()?.content ?? []; }
  stt(i: number): number { return this.currentPage * this.pageSize + i + 1; }

  pageNumbers(): number[] {
    const total = this.totalPages, cur = this.currentPage;
    const range: number[] = [];
    for (let i = Math.max(0, cur - 2); i <= Math.min(total - 1, cur + 2); i++) range.push(i);
    return range;
  }

  // ── Display helpers ───────────────────────────────────────────────────

  formatTy(value: number | null | undefined): string {
    if (value == null) return '0';
    const ty = value / 1000;
    return ty.toLocaleString('vi-VN', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
  }

  formatTrieu(value: number | null | undefined): string {
    if (value == null) return '0';
    return value.toLocaleString('vi-VN', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
  }

  formatSoTienVnd(value: number | null | undefined): string {
    if (value == null) return '—';
    if (value >= 1_000_000_000) {
      return this.trimNum(value / 1_000_000_000) + ' tỷ';
    }
    if (value >= 10_000_000) {
      return this.trimNum(value / 1_000_000) + ' triệu';
    }
    return Math.round(value).toLocaleString('vi-VN');
  }

  private trimNum(v: number): string {
    if (Number.isInteger(v)) return v.toLocaleString('vi-VN');
    return parseFloat(v.toFixed(3)).toLocaleString('vi-VN', { maximumFractionDigits: 3 });
  }

  getDisplayTrangThai(tt: string | null): string {
    return tt?.trim() || '—';
  }

  formatSdt(sdt: string | null): string {
    if (!sdt) return '';
    return sdt.trim().replace(/^\/+|\/+$/g, '');
  }

  trangThaiBadgeClass(tt: string | null): string {
    if (!tt) return 'badge-gray';
    const norm = tt.trim().toLowerCase();
    if (norm === 'card ok')                          return 'badge-green';
    if (norm.includes('fraud') || norm.includes('lost') || norm.includes('suspend')
        || norm.includes('inactive'))                return 'badge-red';
    if (norm.includes('closed') || norm.includes('not used')) return 'badge-gray';
    if (norm.includes('pending') || norm.includes('wait'))    return 'badge-yellow';
    return 'badge-gray';
  }

  networkBadgeClass(network: string | null): string {
    if (!network) return '';
    switch (network.toUpperCase()) {
      case 'VISA': return 'net-visa';
      case 'JCB':  return 'net-jcb';
      case 'MASTERCARD': return 'net-mc';
      default: return 'net-other';
    }
  }

  pctColor(pct: number): string {
    if (pct >= 100) return 'pct-green';
    if (pct >= 50)  return 'pct-yellow';
    return 'pct-red';
  }

  clamp100(v: number): number { return Math.min(v, 100); }

  exportExcel(): void {
    alert('Chức năng Xuất Excel đang được phát triển.');
  }
}
