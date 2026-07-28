import { Component, OnInit, signal, inject } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { NgChartsModule } from 'ng2-charts';
import { Chart, ChartData, ChartConfiguration, ChartDataset, registerables } from 'chart.js';

import { MpaService } from '../../../core/services/mpa.service';
import { AmChiTietResponse, XuHuongResponse, ThePhatHanhItem } from '../../../core/models/mpa.model';
import { PageResponse } from '../../../core/models/user.model';

Chart.register(...registerables);

type LoaiKy = 'thang' | 'quy' | 'nam';

interface MetricCard {
  key: string;
  label: string;
  th: number;
  kh: number;
  pct: number | null;
  delta: number | null;
}

@Component({
  selector: 'app-am-detail',
  standalone: true,
  imports: [
    CommonModule, DatePipe, FormsModule, RouterModule,
    MatIconModule, MatProgressSpinnerModule, MatTooltipModule, NgChartsModule
  ],
  templateUrl: './am-detail.component.html',
  styleUrl: './am-detail.component.scss'
})
export class AmDetailComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private mpaService = inject(MpaService);

  // ── Scope (1 trong 2 — quyết định bởi route nào khớp) ────────────────
  scopeMaAm: string | null = null;
  scopeTenAm: string | null = null;

  // ── Filter state ──────────────────────────────────────────────────
  loaiKy: LoaiKy = 'thang';
  selectedNam   = new Date().getFullYear();
  selectedThang = new Date().getMonth() + 1;
  selectedQuy   = 'Q' + Math.ceil((new Date().getMonth() + 1) / 3);

  readonly namOptions   = Array.from({ length: 6 }, (_, i) => new Date().getFullYear() - i);
  readonly thangOptions = Array.from({ length: 12 }, (_, i) => i + 1);
  readonly quyOptions   = ['Q1', 'Q2', 'Q3', 'Q4'];

  // ── Chi tiết ──────────────────────────────────────────────────────
  loading = signal(false);
  data = signal<AmChiTietResponse | null>(null);
  private lastAmKey = '';

  // ── Xu hướng ──────────────────────────────────────────────────────
  xuHuongLoading = signal(false);
  xuHuongCharts = signal<{ metricKey: string; label: string; data: ChartData<'bar'> }[]>([]);

  readonly xuHuongChartOptions: ChartConfiguration<'bar'>['options'] = {
    responsive: true, maintainAspectRatio: false,
    interaction: { mode: 'index', intersect: false },
    plugins: {
      legend: { display: true, position: 'top', labels: { font: { size: 11 }, boxWidth: 10, padding: 12 } },
      tooltip: { callbacks: { label: ctx => ` ${ctx.dataset.label}: ${this.formatTy(ctx.raw as number)}` } }
    },
    scales: {
      x: { grid: { display: false }, ticks: { font: { size: 11 }, color: '#9ca3af' } },
      y: { grid: { color: '#f3f4f6' }, ticks: { font: { size: 11 }, color: '#9ca3af', maxTicksLimit: 6, callback: v => this.formatTy(+v) } }
    }
  };

  // ── Quản lý thẻ ───────────────────────────────────────────────────
  cardsLoading = signal(false);
  cardsPage = signal<PageResponse<ThePhatHanhItem> | null>(null);
  currentCardPage = 0;
  readonly cardPageSize = 10;

  ngOnInit(): void {
    this.scopeMaAm = this.route.snapshot.paramMap.get('maAm');
    this.scopeTenAm = this.route.snapshot.paramMap.get('tenAm');
    this.load();
  }

  // ── Derived ───────────────────────────────────────────────────────
  get selectedKy(): string {
    if (this.loaiKy === 'nam') return String(this.selectedNam);
    if (this.loaiKy === 'quy') return `${this.selectedQuy}/${this.selectedNam}`;
    return `${String(this.selectedThang).padStart(2, '0')}/${this.selectedNam}`;
  }

  get periodLabel(): string {
    if (this.loaiKy === 'nam') return `Năm ${this.selectedNam}`;
    if (this.loaiKy === 'quy') return `${this.selectedQuy}/${this.selectedNam}`;
    return `Tháng ${String(this.selectedThang).padStart(2, '0')}/${this.selectedNam}`;
  }

  get cardTotalPages(): number {
    return this.cardsPage()?.totalPages ?? 0;
  }

  get cardItems(): ThePhatHanhItem[] {
    return this.cardsPage()?.content ?? [];
  }

  // ── Load ──────────────────────────────────────────────────────────
  onFilterChange(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.mpaService.getAmChiTiet(this.loaiKy, this.selectedKy, this.scopeMaAm ?? undefined, this.scopeTenAm ?? undefined).subscribe({
      next: res => {
        if (res.success) {
          this.data.set(res.data);
          this.maybeReloadCards(res.data);
        }
        this.loading.set(false);
      },
      error: () => this.loading.set(false)
    });
    this.loadXuHuong();
  }

  private loadXuHuong(): void {
    this.xuHuongLoading.set(true);
    this.mpaService.getAmChiTietXuHuong(this.loaiKy, this.selectedNam, this.scopeMaAm ?? undefined, this.scopeTenAm ?? undefined).subscribe({
      next: res => {
        if (res.success) this.buildXuHuongCharts(res.data);
        this.xuHuongLoading.set(false);
      },
      error: () => this.xuHuongLoading.set(false)
    });
  }

  private buildXuHuongCharts(data: XuHuongResponse): void {
    this.xuHuongCharts.set(data.metrics.map(m => ({
      metricKey: m.metricKey,
      label: m.metricLabel,
      data: {
        labels: data.periods,
        datasets: [
          { type: 'bar', label: 'Kế hoạch', data: m.keHoachValues,
            backgroundColor: '#F59E0B', borderRadius: 4, order: 2 },
          { type: 'line', label: 'Thực hiện', data: m.thucHienValues,
            borderColor: '#0f59a6', backgroundColor: 'transparent',
            tension: 0.35, borderWidth: 2.5, pointRadius: 3, order: 1 }
        ] as ChartDataset<'bar' | 'line'>[]
      } as ChartData<'bar'>
    })));
  }

  // ── Quản lý thẻ ───────────────────────────────────────────────────

  private maybeReloadCards(d: AmChiTietResponse): void {
    const key = (d.maAmList ?? []).join(',');
    if (key === this.lastAmKey) return;
    this.lastAmKey = key;
    this.loadCards(0);
  }

  loadCards(page: number): void {
    const codes = this.data()?.maAmList;
    if (!codes || !codes.length) return;
    this.currentCardPage = page;
    this.cardsLoading.set(true);
    this.mpaService.getTheList('', '', '', '', '', '', '', false, 0, false, false, false, page, this.cardPageSize, codes).subscribe({
      next: res => {
        if (res.success) this.cardsPage.set(res.data);
        this.cardsLoading.set(false);
      },
      error: () => this.cardsLoading.set(false)
    });
  }

  // ── Điều hướng ────────────────────────────────────────────────────

  goBack(): void {
    const returnUrl = this.route.snapshot.queryParamMap.get('returnUrl');
    if (returnUrl) {
      this.router.navigateByUrl(returnUrl);
    } else {
      this.router.navigate(['/quan-ly-am']);
    }
  }

  goToCard(id: number): void {
    this.router.navigate(['/quan-ly-the', id], { queryParams: { returnUrl: this.router.url } });
  }

  goToAmDetail(maAm: string): void {
    this.router.navigate(['/quan-ly-am', maAm]);
  }

  // ── Cards KPI ─────────────────────────────────────────────────────

  get metricCards(): MetricCard[] {
    const d = this.data();
    if (!d) return [];
    return [
      { key: 'hdv-cuoi-ky', label: 'HĐV cuối kỳ',     th: d.hdvCuoiKyTh,    kh: d.hdvCuoiKyKh,    pct: d.hdvCuoiKyPct,    delta: d.hdvCuoiKyDelta },
      { key: 'casa-bq',     label: 'CASA bình quân',   th: d.casaBinhQuanTh, kh: d.casaBinhQuanKh, pct: d.casaBinhQuanPct, delta: d.casaBinhQuanDelta },
      { key: 'tong-tnt',    label: 'Tổng TNT',         th: d.tntTh,          kh: d.tntKh,          pct: d.tntPct,          delta: d.tntDelta },
      { key: 'du-no',       label: 'Dư nợ tín dụng',   th: d.duNoTh,         kh: d.duNoKh,         pct: d.duNoPct,         delta: d.duNoDelta },
      { key: 'tnt-dv',      label: 'TNT từ dịch vụ',   th: d.tntDichVuTh,    kh: d.tntDichVuKh,    pct: d.tntDichVuPct,    delta: d.tntDichVuDelta },
      { key: 'tnt-hdv',     label: 'TNT từ HĐV',       th: d.tntHdvTh,       kh: d.tntHdvKh,       pct: d.tntHdvPct,       delta: d.tntHdvDelta },
      { key: 'tnt-td',      label: 'TNT từ tín dụng',  th: d.tntTinDungTh,   kh: d.tntTinDungKh,   pct: d.tntTinDungPct,   delta: d.tntTinDungDelta },
    ];
  }

  // ── Format helpers (giống hệt giao-chi-tieu.component.ts để nhất quán số liệu) ──

  formatTy(val: number): string {
    if (val === null || val === undefined) return '—';
    const ty = val / 1_000_000_000;
    const rounded = Math.round(ty);
    if (rounded === 0 && ty !== 0) {
      return ty.toFixed(2).replace('.', ',') + ' tỷ';
    }
    return rounded.toLocaleString('vi-VN') + ' tỷ';
  }

  cellClass(pct: number | null): string {
    if (pct === null) return 'cell-chua';
    if (pct >= 100) return 'cell-dat';
    if (pct >= 80)  return 'cell-canh';
    return 'cell-rui';
  }

  pctText(pct: number | null): string {
    if (pct === null) return '—';
    return Math.round(pct) + '%';
  }

  deltaSign(d: number | null): string {
    if (d === null) return '';
    return (d > 0 ? '+' : '') + d.toFixed(1).replace('.', ',') + '%';
  }

  trangThaiLabel(t: number | null | undefined): string {
    return t === 1 ? 'Hoạt động' : 'Không hoạt động';
  }

  trangThaiTheBadgeClass(tt: string | null): string {
    if (!tt) return 'badge-gray';
    const norm = tt.trim().toLowerCase();
    if (norm === 'card ok') return 'badge-green';
    if (norm.includes('fraud') || norm.includes('lost') || norm.includes('suspend') || norm.includes('inactive')) return 'badge-red';
    if (norm.includes('closed') || norm.includes('not used')) return 'badge-gray';
    if (norm.includes('pending') || norm.includes('wait')) return 'badge-yellow';
    return 'badge-gray';
  }
}
