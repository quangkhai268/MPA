import { Component, OnInit, signal, inject } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';

import { MpaService } from '../../core/services/mpa.service';
import { AuthService } from '../../core/services/auth.service';
import { SystemSettingService } from '../../core/services/system-setting.service';
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
  private route = inject(ActivatedRoute);
  private systemSettingService = inject(SystemSettingService);
  auth = inject(AuthService);

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
  soNgayMinLaMacDinhHeThong = false;

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

  // ── Giữ vị trí cuộn khi back từ trang chi tiết thẻ ─────────────────────
  private readonly scrollKey = 'qlt-scroll-pos';

  ngOnInit(): void {
    const qp = this.route.snapshot.queryParamMap;
    const hasFilters = qp.keys.length > 0;
    if (hasFilters) {
      this.activeTab       = Number(qp.get('tab') ?? 0);
      this.searchText      = qp.get('q') ?? '';
      this.trangThai       = qp.get('trangThai') ?? '';
      this.productCode     = qp.get('productCode') ?? '';
      this.loaiTheTinDung  = qp.get('loaiTheTinDung') ?? '';
      this.chuaKichHoat    = qp.get('chuaKichHoat') === '1';
      this.chuaPsgd        = qp.get('chuaPsgd') === '1';
      this.chuaDatPtn      = qp.get('chuaDatPtn') === '1';
      this.currentPage     = Number(qp.get('page') ?? 0);
      const soNgay = qp.get('soNgayMin');
      if (soNgay != null) {
        this.soNgayMin = Number(soNgay);
        this.soNgayMinLaMacDinhHeThong = false;
      }
    }

    const savedScroll = sessionStorage.getItem(this.scrollKey);
    const restoreScrollY = savedScroll != null ? Number(savedScroll) : null;

    this.systemSettingService.getAll().subscribe({
      next: res => {
        if (res.success && !hasFilters) {
          const setting = res.data.find(s => s.settingKey === 'CHUA_KICH_HOAT_SO_NGAY');
          const value = setting?.settingValue ? parseInt(setting.settingValue, 10) : NaN;
          if (!isNaN(value)) {
            this.soNgayMin = value;
            this.soNgayMinLaMacDinhHeThong = true;
          }
        }
        this.loadSummary();
        this.loadDropdowns();
        this.loadPage(this.currentPage, restoreScrollY);
      },
      error: () => {
        this.loadSummary();
        this.loadDropdowns();
        this.loadPage(this.currentPage, restoreScrollY);
      }
    });
  }

  // Vùng nội dung chính (main.main-content) mới thực sự cuộn, không phải window
  private getScrollContainer(): Element | null {
    return document.querySelector('main.main-content');
  }

  private restoreScroll(y: number): void {
    sessionStorage.removeItem(this.scrollKey);
    requestAnimationFrame(() => requestAnimationFrame(() => {
      const el = this.getScrollContainer();
      if (el) el.scrollTop = y; else window.scrollTo({ top: y });
    }));
  }

  // ── Giữ nguyên filter trên URL để back từ trang chi tiết không mất ─────
  private syncQueryParams(): void {
    const params: Record<string, string | null> = {
      tab:            this.activeTab ? String(this.activeTab) : null,
      q:              this.searchText.trim() || null,
      trangThai:      this.trangThai || null,
      productCode:    this.productCode || null,
      loaiTheTinDung: this.loaiTheTinDung || null,
      chuaKichHoat:   this.chuaKichHoat ? '1' : null,
      chuaPsgd:       this.chuaPsgd ? '1' : null,
      chuaDatPtn:     this.chuaDatPtn ? '1' : null,
      soNgayMin:      this.soNgayMinLaMacDinhHeThong ? null : String(this.soNgayMin),
      page:           this.currentPage ? String(this.currentPage) : null,
    };
    this.router.navigate([], { relativeTo: this.route, queryParams: params, replaceUrl: true });
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

  loadPage(page: number, restoreScrollY: number | null = null): void {
    this.currentPage = page;
    this.syncQueryParams();
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
        if (restoreScrollY != null) this.restoreScroll(restoreScrollY);
      },
      error: () => this.loading.set(false)
    });
  }

  search(): void { this.loadPage(0); }

  onSoNgayMinChange(): void {
    this.soNgayMinLaMacDinhHeThong = false;
    this.search();
  }

  goToDetail(id: number): void {
    const el = this.getScrollContainer();
    sessionStorage.setItem(this.scrollKey, String(el ? el.scrollTop : window.scrollY));
    this.router.navigate(['/quan-ly-the', id], { queryParams: { returnUrl: this.router.url } });
  }

  goToCaiDat(): void {
    this.router.navigate(['/quan-ly-the/cai-dat']);
  }

  goToChienDich(): void {
    this.router.navigate(['/quan-ly-the/chien-dich']);
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
