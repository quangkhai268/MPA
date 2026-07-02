import { Component, OnInit, signal, inject, computed } from '@angular/core';
import { CommonModule, DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatSelectModule } from '@angular/material/select';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatNativeDateModule } from '@angular/material/core';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatMenuModule } from '@angular/material/menu';
import { NgChartsModule } from 'ng2-charts';
import { Chart, ChartConfiguration, ChartData, registerables } from 'chart.js';

import { MpaService } from '../../core/services/mpa.service';
import {
  DashboardKpi, TrendChartData, PhongData, PhongTrendSeries,
  AmData, AmDetailData, KhachHangData, FilterParams
} from '../../core/models/mpa.model';
import { SumFieldPipe } from '../../shared/pipes/sum-field.pipe';

Chart.register(...registerables);

interface AlertItem {
  icon: string;
  count: number;
  title: string;
  sub: string;
  color: string;
  bg: string;
  borderColor: string;
}

interface KpiMetric {
  key: string;
  label: string;
  value: number;     // triệu VND (DB unit)
  valuePrev: number; // triệu VND kỳ trước
  target: number;    // triệu VND kế hoạch
  sparkline: number[];
}

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [
    CommonModule, FormsModule, DecimalPipe,
    MatIconModule, MatButtonModule, MatSelectModule,
    MatDatepickerModule, MatFormFieldModule, MatInputModule,
    MatNativeDateModule, MatProgressSpinnerModule,
    MatTooltipModule, MatMenuModule,
    NgChartsModule, SumFieldPipe
  ],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss'
})
export class DashboardComponent implements OnInit {
  private mpaService = inject(MpaService);

  loadingKpi        = signal(true);
  loadingTrend      = signal(true);
  loadingPhong      = signal(true);
  loadingPhongTrend = signal(true);
  loadingTop        = signal(true);

  kpi          = signal<DashboardKpi | null>(null);
  kpiMetrics   = signal<KpiMetric[]>([]);
  selectedKpiKey = signal('hdv-cuoi-ky');
  phongData               = signal<PhongData[]>([]);
  phongTrendRaw           = signal<PhongTrendSeries[]>([]);
  phongBarRaw             = signal<PhongTrendSeries[]>([]);
  selectedPhongBarMetricKey = signal('tong-tnt');
  phongBarTotal           = signal(0);
  phongBarMax             = signal(0);
  loadingPhongBar         = signal(true);
  topAm        = signal<AmData[]>([]);
  topKh        = signal<KhachHangData[]>([]);
  topPhong     = signal<PhongData[]>([]);
  bienDongTang = signal<KhachHangData[]>([]);
  bienDongGiam = signal<KhachHangData[]>([]);

  // AM detail expand state
  expandedPhong    = signal<string | null>(null);
  amDetailData     = signal<AmDetailData[]>([]);
  loadingAmDetail  = signal(false);

  alertItems: AlertItem[] = [
    {
      icon: 'assignment_late', count: 3,
      title: 'đơn cần xử lý', sub: 'Chờ phê duyệt quá hạn',
      color: '#92400e', bg: '#fef9ec', borderColor: '#fde68a'
    },
    {
      icon: 'how_to_reg', count: 14,
      title: 'định danh cần nộc thực', sub: 'Chờ kiểm tra và xác nhận',
      color: '#991b1b', bg: '#fff5f5', borderColor: '#fca5a5'
    },
    {
      icon: 'verified_user', count: 7,
      title: 'tài sản thực cần kiểm tra [KYC]', sub: 'Chờ tra xác thực hồ sơ',
      color: '#5b21b6', bg: '#f5f3ff', borderColor: '#c4b5fd'
    },
  ];

  filter: FilterParams = { fromDate: this.getDefaultFrom(), toDate: this.today() };
  phongList: { ma: string; ten: string }[] = [];

  // ── New filter state ───────────────────────────────────────────
  loaiKy: 'thang' | 'quy' | 'nam' | 'ngay' = 'thang';
  selectedKy = '';
  soVoi = 'ky-truoc';
  phanKhuc = 'all';

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

  get currentKyLabel(): string {
    const ky = this.selectedKy;
    if (!ky) return '–';
    return this.loaiKy === 'thang' ? `Tháng ${ky}` : ky;
  }

  get compareKyLabel(): string {
    const ky = this.selectedKy; const sv = this.soVoi;
    if (!ky) return '–';
    if (this.loaiKy === 'thang') {
      const [ms, ys] = ky.split('/'); const m = +ms; const y = +ys;
      if (sv === 'ky-truoc' || sv === 'ngay-truoc') {
        let pm = m - 1; let py = y; if (pm <= 0) { pm = 12; py--; }
        return `Tháng ${String(pm).padStart(2,'0')}/${py}`;
      }
      if (sv === 'dau-nam') return `Tháng 01/${y}`;
      if (sv === 'quy-truoc') {
        let pm = m - 3; let py = y; if (pm <= 0) { pm += 12; py--; }
        return `Tháng ${String(pm).padStart(2,'0')}/${py}`;
      }
    }
    if (this.loaiKy === 'quy') {
      const [qs, ys] = ky.split('/'); const q = +qs.replace('Q',''); const y = +ys;
      if (sv === 'dau-nam') return `Q1/${y}`;
      let pq = q - 1; let py = y; if (pq <= 0) { pq = 4; py--; }
      return `Q${pq}/${py}`;
    }
    if (this.loaiKy === 'nam') return `${+ky - 1}`;
    if (this.loaiKy === 'ngay') {
      const d = new Date(ky); d.setDate(d.getDate() - 1);
      return `${String(d.getDate()).padStart(2,'0')}/${String(d.getMonth()+1).padStart(2,'0')}/${d.getFullYear()}`;
    }
    return '–';
  }

  onLoaiKyChange(): void {
    const opts = this.kyOptions;
    this.selectedKy = opts.length ? opts[0].value : '';
    this.onFilterChange();
  }

  onFilterChange(): void { this.loadAll(); }

  // Chart data signals
  trendChartData  = signal<ChartData<'line'>>({ labels: [], datasets: [] });
  phongTrendData  = signal<ChartData<'line'>>({ labels: [], datasets: [] });
  phongBarData    = signal<ChartData<'bar'>>({ labels: [], datasets: [] });

  phongColors = ['#0f59a6', '#009640', '#7c3aed', '#f59e0b', '#ef4444', '#06b6d4'];

  // Chart options
  trendChartOptions: ChartConfiguration<'line'>['options'] = {
    responsive: true, maintainAspectRatio: false,
    interaction: { mode: 'index', intersect: false },
    plugins: {
      legend: { display: false },
      tooltip: {
        callbacks: {
          label: ctx => ` ${ctx.dataset.label}: ${(+(ctx.raw as number)).toLocaleString('vi-VN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })} tỷ`
        }
      }
    },
    scales: {
      x: { grid: { display: false }, ticks: { font: { size: 11 }, color: '#9ca3af' } },
      y: {
        grid: { color: '#f3f4f6' },
        ticks: { font: { size: 11 }, color: '#9ca3af', callback: v => `${(+v).toFixed(0)} tỷ` }
      }
    },
    elements: { line: { tension: 0.4, borderWidth: 2.5 }, point: { radius: 3, hoverRadius: 6 } }
  };

  phongTrendOptions = signal<ChartConfiguration<'line'>['options']>({
    responsive: true, maintainAspectRatio: false,
  });

  // ── Phong trend state ─────────────────────────────────────────
  selectedPhongMetricKey = signal('tong-tnt');
  phongViewMode = signal<'duong' | 'stacked'>('duong');

  phongMetricOptions = [
    { key: 'tong-tnt',     label: 'Tổng TNT' },
    { key: 'hdv-cuoi-ky',  label: 'HĐV cuối kỳ' },
    { key: 'casa',         label: 'CASA bình quân' },
    { key: 'du-no',        label: 'Dư nợ' },
    { key: 'tnt-dich-vu',  label: 'TNT từ dịch vụ' },
    { key: 'tnt-hdv',      label: 'TNT từ HĐV' },
    { key: 'tnt-tin-dung', label: 'TNT từ tín dụng' },
  ];

  phongBarOptions: ChartConfiguration<'bar'>['options'] = {
    responsive: true, maintainAspectRatio: false,
    indexAxis: 'y' as const,
    plugins: {
      legend: {
        display: true, position: 'bottom',
        labels: { font: { size: 10 }, boxWidth: 10, padding: 12 }
      },
      tooltip: { callbacks: { label: ctx => ` ${(ctx.raw as number).toFixed(3)} tỷ` } }
    },
    scales: {
      x: { stacked: false, grid: { color: '#f3f4f6' }, ticks: { font: { size: 10 }, color: '#9ca3af' } },
      y: { stacked: false, grid: { display: false }, ticks: { font: { size: 11 }, color: '#374151' } }
    }
  };

  // ── Phong trend helpers ───────────────────────────────────────
  get phongTrendLabels(): string[] {
    switch (this.loaiKy) {
      case 'thang': return ['T01','T02','T03','T04','T05','T06','T07','T08','T09','T10','T11','T12'];
      case 'quy':   return ['Q1','Q2','Q3','Q4'];
      case 'nam': {
        const y = this.selectedKy ? +this.selectedKy : new Date().getFullYear();
        return [String(y-3), String(y-2), String(y-1), String(y)];
      }
      default: return ['T01','T02','T03','T04','T05','T06','T07','T08','T09','T10','T11','T12'];
    }
  }

  get phongTrendChartTitle(): string {
    const opt = this.phongMetricOptions.find(o => o.key === this.selectedPhongMetricKey());
    return `Xu hướng ${opt?.label ?? 'Tổng TNT'} theo Phòng`;
  }

  get phongBarMetricLabel(): string {
    const opt = this.phongMetricOptions.find(o => o.key === this.selectedPhongBarMetricKey());
    return opt?.label ?? 'Tổng TNT';
  }

  // ── Trend title helpers ────────────────────────────────────────
  get trendTitle(): string {
    const m = this.kpiMetrics().find(x => x.key === this.selectedKpiKey());
    return 'Biến động ' + (m?.label ?? 'KPI') + ' theo kỳ';
  }

  get trendSubtitle(): string {
    const m = this.kpiMetrics().find(x => x.key === this.selectedKpiKey());
    const label = m?.label ?? '';
    switch (this.loaiKy) {
      case 'thang': {
        const y = this.selectedKy?.split('/')[1] ?? '';
        return `Xu hướng ${label} của Tổng Chi nhánh trong 12 tháng năm ${y} · đường nét đứt: kỳ liền kề trước`;
      }
      case 'quy': {
        const y = this.selectedKy?.split('/')[1] ?? '';
        return `Xu hướng ${label} của Tổng Chi nhánh trong 4 quý năm ${y} · đường nét đứt: năm trước`;
      }
      case 'nam':
        return `Xu hướng ${label} của Tổng Chi nhánh qua 4 năm gần nhất`;
      default: {
        const d = this.selectedKy ? new Date(this.selectedKy) : new Date();
        return `Xu hướng ${label} các ngày trong tháng ${String(d.getMonth()+1).padStart(2,'0')}/${d.getFullYear()} · đường nét đứt: tháng trước`;
      }
    }
  }

  get trendCurrentLabel(): string {
    if (!this.selectedKy) return `Năm ${new Date().getFullYear()}`;
    if (this.loaiKy === 'ngay') {
      const d = new Date(this.selectedKy);
      return `Tháng ${String(d.getMonth()+1).padStart(2,'0')}/${d.getFullYear()}`;
    }
    const y = this.selectedKy.split('/').pop();
    return this.loaiKy === 'nam' ? this.selectedKy : `Năm ${y}`;
  }

  get trendPrevLabel(): string {
    if (!this.selectedKy) return `Năm ${new Date().getFullYear() - 1}`;
    if (this.loaiKy === 'ngay') {
      const d = new Date(this.selectedKy); d.setDate(1); d.setMonth(d.getMonth() - 1);
      return `Tháng ${String(d.getMonth()+1).padStart(2,'0')}/${d.getFullYear()}`;
    }
    const y = this.selectedKy.split('/').pop();
    return this.loaiKy === 'nam' ? String(+this.selectedKy - 1) : `Năm ${+y! - 1}`;
  }

  // ── KPI helpers ────────────────────────────────────────────────
  selectKpi(key: string): void {
    this.selectedKpiKey.set(key);
    this.loadTrend();
  }

  selectPhongMetric(key: string): void {
    this.selectedPhongMetricKey.set(key);
    this.loadPhongTrend();
  }

  selectPhongBarMetric(key: string): void {
    this.selectedPhongBarMetricKey.set(key);
    this.loadPhongBar();
  }

  togglePhongView(mode: 'duong' | 'stacked'): void {
    this.phongViewMode.set(mode);
    this.buildPhongTrendChart(this.phongTrendRaw());
  }

  onPhongRowClick(row: PhongData): void {
    const ma = row.maDonViCap6;
    if (this.expandedPhong() === ma) {
      this.expandedPhong.set(null);
      this.amDetailData.set([]);
      return;
    }
    this.expandedPhong.set(ma);
    this.amDetailData.set([]);
    this.loadingAmDetail.set(true);
    this.mpaService.getAmDetail(this.filter, ma).subscribe({
      next: r => {
        this.amDetailData.set(r.success && r.data?.length ? r.data : this.mockAmDetail(row));
        this.loadingAmDetail.set(false);
      },
      error: () => {
        this.amDetailData.set(this.mockAmDetail(row));
        this.loadingAmDetail.set(false);
      }
    });
  }

  private mockAmDetail(phong: PhongData): AmDetailData[] {
    const tnt = phong.thuNhapThuan ?? 0;
    const names = ['Nguyễn Văn A', 'Trần Thị B', 'Lê Văn C', 'Phạm Thị D', 'Hoàng Văn E'];
    const rndPct = () => +(Math.random() * 40 - 15).toFixed(1);
    return Array.from({ length: phong.soAm || 3 }, (_, i) => ({
      maAm: `AM0${i + 1}`,
      tenAm: names[i] ?? `Cán bộ ${i + 1}`,
      tenDonViCap6: phong.tenDonViCap6,
      hdvCuoiKy: (phong.hdvCuoiKy ?? 0) / (phong.soAm || 1) * (0.8 + Math.random() * 0.4),
      hdvBinhQuan: (phong.hdvBinhQuan ?? 0) / (phong.soAm || 1) * (0.8 + Math.random() * 0.4),
      duNo: (phong.duNo ?? 0) / (phong.soAm || 1) * (0.8 + Math.random() * 0.4),
      thuNhapThuanDichVu: (phong.thuNhapThuanDichVu ?? 0) / (phong.soAm || 1) * (0.7 + Math.random() * 0.6),
      thuNhapThuanHdvFtp: (phong.thuNhapThuanHdvFtp ?? 0) / (phong.soAm || 1) * (0.7 + Math.random() * 0.6),
      thuNhapThuanTinDung: (phong.thuNhapThuanTinDung ?? 0) / (phong.soAm || 1) * (0.7 + Math.random() * 0.6),
      thuNhapThuan: tnt / (phong.soAm || 1) * (0.7 + Math.random() * 0.6),
      hdvCuoiKyChangePct: rndPct(),
      hdvBinhQuanChangePct: rndPct(),
      duNoChangePct: rndPct(),
      tntDichVuChangePct: rndPct(),
      tntHdvChangePct: rndPct(),
      tntTinDungChangePct: rndPct(),
      thuNhapThuanChangePct: rndPct(),
    }));
  }

  formatTy(v: number): string {
    return (v / 1_000_000_000).toLocaleString('vi-VN', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
  }

  changeAbs(m: KpiMetric): string {
    const d = m.value - m.valuePrev;
    return (d >= 0 ? '+' : '') + this.formatTy(d);
  }

  changePct(m: KpiMetric): string {
    if (!m.valuePrev) return '—';
    const p = ((m.value - m.valuePrev) / Math.abs(m.valuePrev)) * 100;
    return (p >= 0 ? '+' : '') + p.toFixed(2).replace('.', ',') + '%';
  }

  // % thực hiện = thực hiện kỳ (type_data=1) / BSC năm (type_data=0) * 100
  targetPct(m: KpiMetric): number {
    if (!m.target) return 0;
    return +((m.value / m.target) * 100).toFixed(1);
  }

  // Chiều rộng thanh progress bar – giới hạn 100% để không vỡ layout
  barFillPct(m: KpiMetric): number {
    return Math.min(this.targetPct(m), 100);
  }

  // Màu badge dựa trên % hoàn thành: ≥100% xanh, 80-99% vàng, <80% đỏ
  achievementClass(m: KpiMetric): string {
    const p = this.targetPct(m);
    if (p >= 100) return 'achieve-good';
    if (p >= 80)  return 'achieve-warn';
    return 'achieve-bad';
  }

  private buildKpiMetrics(k: DashboardKpi): KpiMetric[] {
    return [
      { key:'hdv-cuoi-ky',  label:'Huy động vốn cuối kỳ', value:k.tongHdvCuoiKy,    valuePrev:k.tongHdvCuoiKyPrev,    target:k.khHdvCuoiKy,  sparkline:[33,35,34,37,36,38,37,40,39,42] },
      { key:'casa',         label:'CASA bình quân',        value:k.tongHdvBinhQuan,  valuePrev:k.tongHdvBinhQuanPrev,  target:k.khHdvBinhQuan, sparkline:[68,65,67,62,64,60,58,62,60,64] },
      { key:'du-no',        label:'Dư nợ',                 value:k.tongDuNo,         valuePrev:k.tongDuNoPrev,         target:k.khDuNo,        sparkline:[50,52,54,53,55,56,57,56,58,58] },
      { key:'tnt-dich-vu',  label:'TNT từ dịch vụ',        value:k.tongTntDichVu,    valuePrev:k.tongTntDichVuPrev,    target:k.khTntDichVu,   sparkline:[28,30,32,31,34,35,36,38,39,40] },
      { key:'tnt-hdv',      label:'TNT từ HĐV',            value:k.tongTntHdvFtp,    valuePrev:k.tongTntHdvFtpPrev,    target:k.khTntHdvFtp,   sparkline:[24,26,25,27,26,28,27,29,28,30] },
      { key:'tnt-tin-dung', label:'TNT từ tín dụng',       value:k.tongTntTinDung,   valuePrev:k.tongTntTinDungPrev,   target:k.khTntTinDung,  sparkline:[4,5,5,6,6,7,7,7,8,8] },
      { key:'tong-tnt',     label:'Tổng TNT',              value:k.tongThuNhapThuan, valuePrev:k.tongThuNhapThuanPrev, target:k.khTnt,         sparkline:[58,61,62,63,65,67,69,70,72,75] },
    ];
  }

  ngOnInit(): void {
    const opts = this.kyOptions;
    this.selectedKy = opts.length ? opts[0].value : '';
    this.loadPhongList();
    this.loadAll();
  }

  loadAll(): void {
    this.updateFilter();
    this.loadKpi();
    this.loadTrend();
    this.loadPhong();
    this.loadPhongTrend();
    this.loadPhongBar();
    this.loadTop();
    this.loadBienDong();
  }

  private updateFilter(): void {
    this.filter.loaiKy    = this.loaiKy;
    this.filter.selectedKy = this.selectedKy;
    this.filter.soVoi     = this.soVoi;
    this.filter.phanKhuc  = this.phanKhuc !== 'all' ? this.phanKhuc : undefined;
    // Derive fromDate/toDate from loaiKy + selectedKy
    if (this.loaiKy === 'thang' && this.selectedKy) {
      const [ms, ys] = this.selectedKy.split('/');
      const m = +ms; const y = +ys;
      this.filter.fromDate = `${y}-${ms}-01`;
      this.filter.toDate   = this.formatDate(new Date(y, m, 0));
      this.filter.thang = m; this.filter.nam = y;
    } else if (this.loaiKy === 'quy' && this.selectedKy) {
      const [qs, ys] = this.selectedKy.split('/');
      const q = +qs.replace('Q',''); const y = +ys;
      const startMonth = (q - 1) * 3 + 1;
      this.filter.fromDate = `${y}-${String(startMonth).padStart(2,'0')}-01`;
      this.filter.toDate   = this.formatDate(new Date(y, startMonth + 2, 0));
      this.filter.quy = `Q${q}`; this.filter.nam = y;
    } else if (this.loaiKy === 'nam' && this.selectedKy) {
      const y = +this.selectedKy;
      this.filter.fromDate = `${y}-01-01`; this.filter.toDate = `${y}-12-31`;
      this.filter.nam = y;
    } else if (this.loaiKy === 'ngay' && this.selectedKy) {
      this.filter.fromDate = this.selectedKy; this.filter.toDate = this.selectedKy;
    } else {
      this.filter.fromDate = this.getDefaultFrom(); this.filter.toDate = this.today();
    }
    if (this.phanKhuc !== 'all') this.filter.sheetname = this.phanKhuc;
    else delete this.filter.sheetname;
  }

  private loadKpi(): void {
    this.loadingKpi.set(true);
    this.mpaService.getDashboardKpi(this.filter).subscribe({
      next: r => {
        const d = r.success ? r.data : this.mockKpi();
        this.kpi.set(d);
        this.kpiMetrics.set(this.buildKpiMetrics(d));
        this.loadingKpi.set(false);
      },
      error: () => {
        const d = this.mockKpi();
        this.kpi.set(d);
        this.kpiMetrics.set(this.buildKpiMetrics(d));
        this.loadingKpi.set(false);
      }
    });
  }

  private loadTrend(): void {
    this.loadingTrend.set(true);
    this.mpaService.getTrend(this.filter, this.selectedKpiKey()).subscribe({
      next: r => {
        const raw = r.success ? r.data : null;
        const data = (raw && Array.isArray(raw.currentValues)) ? raw : null;
        if (data) {
          this.buildTrendChart(data);
        } else {
          this.trendChartData.set({ labels: [], datasets: [] });
        }
        this.loadingTrend.set(false);
      },
      error: () => {
        this.trendChartData.set({ labels: [], datasets: [] });
        this.loadingTrend.set(false);
      }
    });
  }

  private loadPhong(): void {
    this.loadingPhong.set(true);
    this.mpaService.getPhongData(this.filter).subscribe({
      next: r => { this.applyPhongData(r.success ? r.data : this.mockPhong()); this.loadingPhong.set(false); },
      error: () => { this.applyPhongData(this.mockPhong()); this.loadingPhong.set(false); }
    });
  }

  private applyPhongData(data: PhongData[]): void {
    this.phongData.set(data);
    this.topPhong.set(data.slice(0, 5));
  }

  private loadPhongBar(): void {
    this.loadingPhongBar.set(true);
    this.mpaService.getPhongTrend(this.filter, this.selectedPhongBarMetricKey()).subscribe({
      next: r => {
        const data = r.success && r.data?.length ? r.data : [];
        this.phongBarRaw.set(data);
        this.buildPhongBarChart(data);
        this.loadingPhongBar.set(false);
      },
      error: () => { this.buildPhongBarChart([]); this.loadingPhongBar.set(false); }
    });
  }

  private loadPhongTrend(): void {
    this.loadingPhongTrend.set(true);
    this.mpaService.getPhongTrend(this.filter, this.selectedPhongMetricKey()).subscribe({
      next: r => {
        const data = r.success && r.data?.length ? r.data : [];
        this.phongTrendRaw.set(data);
        this.buildPhongTrendChart(data);
        this.loadingPhongTrend.set(false);
      },
      error: () => { this.buildPhongTrendChart([]); this.loadingPhongTrend.set(false); }
    });
  }

  private loadTop(): void {
    this.loadingTop.set(true);
    this.mpaService.getTopAm(this.filter).subscribe({
      next: r => { this.topAm.set(r.success ? r.data : this.mockTopAm()); },
      error: () => { this.topAm.set(this.mockTopAm()); }
    });
    this.mpaService.getTopKh(this.filter).subscribe({
      next: r => { this.topKh.set(r.success ? r.data : this.mockTopKh()); this.loadingTop.set(false); },
      error: () => { this.topKh.set(this.mockTopKh()); this.loadingTop.set(false); }
    });
  }

  private loadBienDong(): void {
    this.mpaService.getBienDongKh(this.filter).subscribe({
      next: r => {
        if (r.success) {
          this.bienDongTang.set(r.data.tang);
          this.bienDongGiam.set(r.data.giam);
        } else {
          this.bienDongTang.set(this.mockBienDong(true));
          this.bienDongGiam.set(this.mockBienDong(false));
        }
      },
      error: () => {
        this.bienDongTang.set(this.mockBienDong(true));
        this.bienDongGiam.set(this.mockBienDong(false));
      }
    });
  }

  private loadPhongList(): void {
    this.mpaService.getPhongList().subscribe({
      next: r => { if (r.success) this.phongList = r.data; },
      error: () => {}
    });
  }

  private buildTrendChart(data: TrendChartData): void {
    const toTy = (v: number) => +(v / 1_000_000_000).toFixed(3);
    const teal  = '#1c7a5a';
    const curVals = Array.isArray(data?.currentValues) ? data.currentValues : [];
    const prvVals = Array.isArray(data?.prevValues) ? data.prevValues : [];
    const lbls    = Array.isArray(data?.labels) ? data.labels : [];
    const datasets: any[] = [
      {
        label: this.trendCurrentLabel,
        data: curVals.map(toTy),
        borderColor: teal,
        backgroundColor: 'rgba(28,122,90,0.07)',
        fill: true,
        tension: 0.4,
        borderWidth: 2.5,
        pointRadius: 3,
        pointHoverRadius: 6,
        pointBackgroundColor: teal,
        order: 1
      }
    ];
    if (data?.prevYear && prvVals.length) {
      datasets.push({
        label: this.trendPrevLabel,
        data: prvVals.map(toTy),
        borderColor: teal,
        backgroundColor: 'transparent',
        borderDash: [5, 5],
        fill: false,
        tension: 0.4,
        borderWidth: 1.5,
        pointRadius: 2,
        pointHoverRadius: 5,
        pointBackgroundColor: teal,
        order: 2
      });
    }
    this.trendChartData.set({ labels: lbls, datasets });
  }

  private buildPhongBarChart(data: PhongTrendSeries[]): void {
    const labels = this.phongTrendLabels;

    let selectedIdx = labels.length - 1;
    if (this.selectedKy) {
      if (this.loaiKy === 'thang') {
        const m = +this.selectedKy.split('/')[0];
        const i = labels.indexOf(`T${String(m).padStart(2, '0')}`);
        if (i >= 0) selectedIdx = i;
      } else if (this.loaiKy === 'quy') {
        const q = this.selectedKy.split('/')[0];
        const i = labels.indexOf(q);
        if (i >= 0) selectedIdx = i;
      } else if (this.loaiKy === 'nam') {
        const i = labels.indexOf(this.selectedKy);
        if (i >= 0) selectedIdx = i;
      } else {
        const m = new Date(this.selectedKy).getMonth() + 1;
        const i = labels.indexOf(`T${String(m).padStart(2, '0')}`);
        if (i >= 0) selectedIdx = i;
      }
    }
    const prevIdx = selectedIdx > 0 ? selectedIdx - 1 : -1;
    const toTy    = (v: number) => +(v / 1_000_000_000).toFixed(3);

    const top5 = [...data]
      .sort((a, b) => (b.values[selectedIdx] ?? 0) - (a.values[selectedIdx] ?? 0))
      .slice(0, 5);

    const tealColor = '#006B68';
    const redColor  = '#ef4444';
    const grayColor = '#9ca3af';

    const vals     = top5.map(p => toTy(p.values[selectedIdx] ?? 0));
    const prevVals = top5.map(p => prevIdx >= 0 ? toTy(p.values[prevIdx] ?? 0) : null);

    const barColors = top5.map((_, i) => {
      const prv = prevVals[i];
      if (prv === null || prv === 0) return grayColor;
      return (vals[i] ?? 0) >= prv ? tealColor : redColor;
    });
    const total = vals.reduce((s, v) => s + v, 0);
    const max   = vals.length ? Math.max(...vals) : 0;

    this.phongBarTotal.set(+total.toFixed(2));
    this.phongBarMax.set(+max.toFixed(2));

    this.phongBarData.set({
      labels: top5.map(p => p.tenDonViCap6),
      datasets: [{
        label: this.phongBarMetricLabel,
        data: vals,
        backgroundColor: barColors,
        borderColor: barColors,
        borderWidth: 0,
        borderRadius: 4,
      }]
    });

    const prevValsForTooltip = prevVals;

    this.phongBarOptions = {
      responsive: true, maintainAspectRatio: false,
      indexAxis: 'y' as const,
      plugins: {
        legend: { display: false },
        tooltip: {
          callbacks: {
            label: (ctx: any) => {
              const cur = ctx.raw as number;
              return ` ${cur.toLocaleString('vi-VN', { minimumFractionDigits: 3, maximumFractionDigits: 3 })} tỷ`;
            },
            afterLabel: (ctx: any) => {
              const cur = ctx.raw as number;
              const prv = prevValsForTooltip[ctx.dataIndex];
              if (prv === null) return ' (Chưa có kỳ trước)';
              const diff = cur - prv;
              const pct  = prv !== 0 ? (diff / prv) * 100 : 0;
              const sign = diff >= 0 ? '+' : '';
              return [
                ` ${sign}${pct.toFixed(1)}% so kỳ trước`,
                ` ${sign}${diff.toLocaleString('vi-VN', { minimumFractionDigits: 3, maximumFractionDigits: 3 })} tỷ`,
              ];
            }
          }
        }
      },
      scales: {
        x: {
          grid: { color: '#f3f4f6' },
          ticks: { font: { size: 10 }, color: '#9ca3af', callback: (v: any) => `${(+v).toFixed(2)} tỷ` }
        },
        y: {
          grid: { display: false },
          ticks: { font: { size: 11 }, color: '#374151' }
        }
      }
    };
  }

  private buildPhongTrendChart(data: PhongTrendSeries[]): void {
    const labels    = this.phongTrendLabels;
    const isStacked = this.phongViewMode() === 'stacked';

    let selectedIdx = labels.length - 1;
    if (this.selectedKy) {
      if (this.loaiKy === 'thang') {
        const m = +this.selectedKy.split('/')[0];
        const i = labels.indexOf(`T${String(m).padStart(2, '0')}`);
        if (i >= 0) selectedIdx = i;
      } else if (this.loaiKy === 'quy') {
        const q = this.selectedKy.split('/')[0];
        const i = labels.indexOf(q);
        if (i >= 0) selectedIdx = i;
      } else if (this.loaiKy === 'nam') {
        const i = labels.indexOf(this.selectedKy);
        if (i >= 0) selectedIdx = i;
      } else {
        const m = new Date(this.selectedKy).getMonth() + 1;
        const i = labels.indexOf(`T${String(m).padStart(2, '0')}`);
        if (i >= 0) selectedIdx = i;
      }
    }

    const sorted = [...data].sort((a, b) => (b.values[selectedIdx] ?? 0) - (a.values[selectedIdx] ?? 0));

    const datasets = sorted.map((p, i) => ({
      label: p.tenDonViCap6,
      data: p.values.map(v => +(v / 1_000_000_000).toFixed(3)),
      borderColor: this.phongColors[i],
      backgroundColor: isStacked ? `${this.phongColors[i]}55` : 'transparent',
      pointBackgroundColor: this.phongColors[i],
      pointRadius: 3,
      pointHoverRadius: 6,
      tension: 0.4,
      borderWidth: 2,
      fill: isStacked,
    }));

    this.phongTrendData.set({ labels, datasets });
    this.phongTrendOptions.set(this.buildPhongTrendChartOptions(isStacked));
  }

  private buildPhongTrendChartOptions(stacked: boolean): ChartConfiguration<'line'>['options'] {
    return {
      responsive: true, maintainAspectRatio: false,
      interaction: { mode: 'index', intersect: false },
      plugins: {
        legend: {
          display: true, position: 'bottom',
          labels: { font: { size: 10 }, boxWidth: 20, padding: 14, usePointStyle: true }
        },
        tooltip: {
          itemSort: (a: any, b: any) => (b.raw as number) - (a.raw as number),
          callbacks: {
            title: (items: any[]) => {
              if (!items.length) return '';
              const lbl  = items[0].label as string;
              const ky   = this.selectedKy;
              const year = ky
                ? (ky.includes('/') ? ky.split('/').pop()! : ky)
                : String(new Date().getFullYear());
              if (this.loaiKy === 'thang') return `Tháng ${lbl.replace('T','').padStart(2,'0')}/${year}`;
              if (this.loaiKy === 'quy')   return `${lbl}/${year}`;
              return `Năm ${lbl}`;
            },
            label: (ctx: any) => {
              const tỷ = ctx.raw as number;
              if (!tỷ) return;
              const vnd = Math.round(tỷ * 1_000_000_000);
              return ` ${ctx.dataset.label}: ${vnd.toLocaleString('vi-VN')} VND`;
            }
          }
        }
      },
      scales: {
        x: { grid: { display: false }, ticks: { font: { size: 11 }, color: '#9ca3af' } },
        y: {
          stacked,
          grid: { color: '#f3f4f6' },
          ticks: { font: { size: 11 }, color: '#9ca3af', callback: (v: any) => `${(+v).toFixed(2)} tỷ` }
        }
      },
      elements: { line: { tension: 0.4, borderWidth: 2 }, point: { radius: 3, hoverRadius: 6 } }
    };
  }

  sortedPhongData = computed(() => {
    const key = this.selectedPhongBarMetricKey();
    return [...this.phongData()].sort((a, b) => this.getPhongMetricValue(b, key) - this.getPhongMetricValue(a, key));
  });

  private getPhongMetricValue(row: PhongData, key: string): number {
    switch (key) {
      case 'hdv-cuoi-ky':  return row.hdvCuoiKy ?? 0;
      case 'casa':         return row.hdvBinhQuan ?? 0;
      case 'du-no':        return row.duNo ?? 0;
      case 'tnt-dich-vu':  return row.thuNhapThuanDichVu ?? 0;
      case 'tnt-hdv':      return row.thuNhapThuanHdvFtp ?? 0;
      case 'tnt-tin-dung': return row.thuNhapThuanTinDung ?? 0;
      default:             return row.thuNhapThuan ?? 0;
    }
  }

  // ── Utilities ──────────────────────────────────────────────
  formatBillion(val: number): string {
    return (val / 1_000_000_000).toLocaleString('vi-VN', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
  }

  formatMillion(val: number): string {
    return val >= 1_000_000_000
      ? `${(val / 1_000_000_000).toFixed(2)} tỷ`
      : val >= 1_000_000
        ? `${(val / 1_000_000).toFixed(0)} tr`
        : `${Math.round(val).toLocaleString()}`;
  }

  changeClass(v: number): string { return v > 0 ? 'up' : v < 0 ? 'down' : 'flat'; }
  changeIcon(v: number): string  { return v > 0 ? 'arrow_upward' : v < 0 ? 'arrow_downward' : 'remove'; }

  generateSparklinePath(data: number[], w = 80, h = 28): string {
    if (!data?.length || data.length < 2) return '';
    const min = Math.min(...data), max = Math.max(...data), range = max - min || 1;
    return 'M ' + data.map((v, i) => {
      const x = (i / (data.length - 1)) * w;
      const y = h - ((v - min) / range) * (h - 4) - 2;
      return `${x.toFixed(1)},${y.toFixed(1)}`;
    }).join(' L ');
  }

  private formatDate(d: Date): string { return d.toISOString().split('T')[0]; }
  private getDefaultFrom(): string { const d = new Date(); d.setDate(d.getDate() - 7); return d.toISOString().split('T')[0]; }
  private today(): string { return new Date().toISOString().split('T')[0]; }

  resetFilter(): void {
    this.loaiKy  = 'thang';
    this.soVoi   = 'ky-truoc';
    this.phanKhuc = 'all';
    const opts = this.kyOptions;
    this.selectedKy = opts.length ? opts[0].value : '';
    this.loadAll();
  }

  // ── Mock data ────────────────────────────────────────────────
  private mockKpi(): DashboardKpi {
    return {
      // Kỳ hiện tại (triệu VND – hiển thị chia 1000 = tỷ)
      tongHdvCuoiKy:    39070,  // 39,07 tỷ
      tongHdvBinhQuan:   6480,  //  6,48 tỷ
      tongDuNo:          1840,  //  1,84 tỷ
      tongTntDichVu:      460,  //  0,46 tỷ
      tongTntHdvFtp:      300,  //  0,30 tỷ
      tongTntTinDung:      40,  //  0,04 tỷ
      tongThuNhapThuan:   800,  //  0,80 tỷ
      // Kỳ trước
      tongHdvCuoiKyPrev:    29220,
      tongHdvBinhQuanPrev:   6858,
      tongDuNoPrev:             0,
      tongTntDichVuPrev:      347,
      tongTntHdvFtpPrev:      271,
      tongTntTinDungPrev:       0,
      tongThuNhapThuanPrev:   619,
      // Kế hoạch
      khHdvCuoiKy:   42970,
      khHdvBinhQuan:  7130,
      khDuNo:         2020,
      khTntDichVu:     510,
      khTntHdvFtp:     330,
      khTntTinDung:     40,
      khTnt:           880,
      // Thống kê
      soKhachHang: 6801, soAm: 89, soPhong: 6,
    };
  }

  private mockPhong(): PhongData[] {
    const mk = (ma: string, ten: string, tnt: number, prev: number, duNo: number, hdvCk: number, hdvBq: number, kh: number, am: number): PhongData => ({
      maDonViCap6: ma, tenDonViCap6: ten,
      thuNhapThuan: tnt, thuNhapThuanPrevious: prev,
      duNo, hdvCuoiKy: hdvCk, hdvBinhQuan: hdvBq,
      thuNhapThuanDichVu: tnt * 0.25, thuNhapThuanHdvFtp: tnt * 0.45, thuNhapThuanTinDung: tnt * 0.30,
      soKhachHang: kh, soAm: am,
      changePercent: prev ? +((tnt - prev) / Math.abs(prev) * 100).toFixed(1) : null,
      hdvCuoiKyChangePercent: 5.2, hdvBinhQuanChangePercent: 3.1, duNoChangePercent: 4.7,
      tntDichVuChangePercent: prev ? +((tnt - prev) / Math.abs(prev) * 100 * 0.9).toFixed(1) : null,
      tntHdvChangePercent: prev ? +((tnt - prev) / Math.abs(prev) * 100 * 1.1).toFixed(1) : null,
      tntTinDungChangePercent: prev ? +((tnt - prev) / Math.abs(prev) * 100 * 0.8).toFixed(1) : null,
    });
    return [
      mk('P001','PHÒNG DNNVV.CN.TÂY HỒ',      5700,5200,850000, 1200000,1100000,120,12),
      mk('P002','PHÒNG KHCN.CN.TÂY HỒ',        5140,5400,760000,  980000, 900000, 98,10),
      mk('P003','PGD HÀ THÀNH.CN.HOÀN KIẾM',   4170,3900,620000,  840000, 780000, 87, 9),
      mk('P004','POD HÀ TRUNG.CN.HOÀN KIẾM',   3980,3750,590000,  720000, 680000, 75, 8),
      mk('P005','PHÒNG KHCN.CN.HOÀN KIẾM',     3650,3800,540000,  680000, 640000, 65, 7),
      mk('P006','PGD HOÀN KIẾM.CN.HOÀN KIẾM',  2100,1950,310000,  420000, 390000, 42, 5),
    ];
  }

  private mockTopAm(): AmData[] {
    return [
      { maAm:'AM01', tenAm:'Nguyễn Văn A',   tenDonViCap6:'PHÒNG DNNVV.CN.TÂY HỒ',      thuNhapThuan:4170, duNo:650000, hdvCuoiKy:820000, soKhachHang:32 },
      { maAm:'AM02', tenAm:'Nguyễn Văn A',   tenDonViCap6:'PHÒNG DNNVV.CN.TÂY HỒ',      thuNhapThuan:3500, duNo:510000, hdvCuoiKy:700000, soKhachHang:28 },
      { maAm:'AM03', tenAm:'Đỗ Văn Quang',   tenDonViCap6:'PGD HÀ THÀNH.CN.HOÀN KIẾM',  thuNhapThuan:2980, duNo:420000, hdvCuoiKy:590000, soKhachHang:24 },
      { maAm:'AM04', tenAm:'Trần Thị B',     tenDonViCap6:'POD HÀ TRUNG.CN.HOÀN KIẾM',  thuNhapThuan:2540, duNo:380000, hdvCuoiKy:520000, soKhachHang:19 },
      { maAm:'AM05', tenAm:'Trần Văn Khánh', tenDonViCap6:'PGD HOÀN KIẾM.CN.HOÀN KIẾM', thuNhapThuan:2100, duNo:310000, hdvCuoiKy:450000, soKhachHang:16 },
    ];
  }

  private mockTopKh(): KhachHangData[] {
    return [
      { maKhCif:'CIF001', tenKhachHang:'CÔNG TY CP ĐẦU TƯ DEF',  tenDonViCap6:'PHÒNG DNNVV', tenAm:'Nguyễn Văn A', thuNhapThuan:2450, thuNhapThuanPrevious:2100, change:350, changePercent:16.7 },
      { maKhCif:'CIF002', tenKhachHang:'CÔNG TY TẬP ĐOÀN ABC',   tenDonViCap6:'PHÒNG KHCN',  tenAm:'Bùi Văn Khành',thuNhapThuan:1980, thuNhapThuanPrevious:1750, change:230, changePercent:13.1 },
      { maKhCif:'CIF003', tenKhachHang:'CÔNG TY TNHH QRS GROUP', tenDonViCap6:'PGD HÀ THÀNH',tenAm:'Đỗ Văn Quang', thuNhapThuan:1540, thuNhapThuanPrevious:1400, change:140, changePercent:10.0 },
      { maKhCif:'CIF004', tenKhachHang:'TỔNG CÔNG TY GHI',       tenDonViCap6:'POD HÀ TRUNG',tenAm:'Trần Thị B',   thuNhapThuan:1320, thuNhapThuanPrevious:1210, change:110, changePercent:9.1 },
      { maKhCif:'CIF005', tenKhachHang:'CÔNG TY CP ĐẦU TƯ NDP', tenDonViCap6:'PHÒNG KHCN',  tenAm:'Phạm Thị E',   thuNhapThuan:1180, thuNhapThuanPrevious:1080, change:100, changePercent:9.3 },
    ];
  }

  private mockBienDong(tang: boolean): KhachHangData[] {
    if (tang) return [
      { maKhCif:'T01', tenKhachHang:'CÔNG TY CP ĐẦU TƯ DEF',     tenDonViCap6:'PHÒNG DNNVV.CN.TÂY HỒ',     tenAm:'Nguyễn Văn A',  thuNhapThuan:2450, thuNhapThuanPrevious:1800, change:650,  changePercent:128.89 },
      { maKhCif:'T02', tenKhachHang:'CÔNG TY CP ĐẦU TƯ TIẾN!',   tenDonViCap6:'PHÒNG DNNVV.CN.TÂY HỒ',     tenAm:'Bùi Văn Khành', thuNhapThuan:980,  thuNhapThuanPrevious:750,  change:230,  changePercent:111.38 },
      { maKhCif:'T03', tenKhachHang:'CÔNG TY CP ĐẦU TƯ ONE',     tenDonViCap6:'PHÒNG DNNVV.CN.TÂY HỒ',     tenAm:'Đỗ Văn Quang',  thuNhapThuan:620,  thuNhapThuanPrevious:490,  change:130,  changePercent:126.86 },
      { maKhCif:'T04', tenKhachHang:'CÔNG TY TNHH XYZ VIỆT NAM', tenDonViCap6:'PHÒNG DNNVV.CN.TÂY HỒ',     tenAm:'Nguyễn Văn A',  thuNhapThuan:890,  thuNhapThuanPrevious:720,  change:170,  changePercent:126.40 },
      { maKhCif:'T05', tenKhachHang:'TỔNG CÔNG TY GHI',          tenDonViCap6:'PHÒNG DNNVV.CN.TÂY HỒ',     tenAm:'Phạm Thị E',    thuNhapThuan:450,  thuNhapThuanPrevious:370,  change:80,   changePercent:174.91 },
      { maKhCif:'T06', tenKhachHang:'CÔNG TY CP ĐÌNH MINH',      tenDonViCap6:'POD HÀ TRUNG.CN.HOÀN KIẾM', tenAm:'Trần Thị B',    thuNhapThuan:750,  thuNhapThuanPrevious:600,  change:150,  changePercent:130.48 },
      { maKhCif:'T07', tenKhachHang:'CÔNG TY TNHH THỊNH PHÚ MY',tenDonViCap6:'PGD HOÀN KIẾM',              tenAm:'Đỗ Văn Quang',  thuNhapThuan:580,  thuNhapThuanPrevious:450,  change:130,  changePercent:329.00 },
      { maKhCif:'T08', tenKhachHang:'CÔNG TY TNHH MINH QUÂN',   tenDonViCap6:'PGD HOÀN KIẾM',              tenAm:'Trần Văn Khánh',thuNhapThuan:420,  thuNhapThuanPrevious:350,  change:70,   changePercent:186.85 },
    ];
    return [
      { maKhCif:'G01', tenKhachHang:'CÔNG TY TNHH THƯƠNG MẠI TUV', tenDonViCap6:'PHÒNG KHCN.CN.TÂY HỒ',     tenAm:'Nguyễn Văn A',  thuNhapThuan:850, thuNhapThuanPrevious:1400, change:-550, changePercent:-46.88 },
      { maKhCif:'G02', tenKhachHang:'CÔNG TY CP WXY',               tenDonViCap6:'PHÒNG KHCN.CN.TÂY HỒ',     tenAm:'Bùi Văn Khành', thuNhapThuan:420, thuNhapThuanPrevious:680,  change:-260, changePercent:-46.80 },
      { maKhCif:'G03', tenKhachHang:'LÊ VĂN CÔNG',                  tenDonViCap6:'PGD HÀ THÀNH.CN.HOÀN KIẾM',tenAm:'Đỗ Văn Quang',  thuNhapThuan:670, thuNhapThuanPrevious:1050, change:-380, changePercent:-41.80 },
      { maKhCif:'G04', tenKhachHang:'CÔNG TY CP NAM LONG',          tenDonViCap6:'PHÒNG KHCN.CN.TÂY HỒ',     tenAm:'Trần Thị B',    thuNhapThuan:520, thuNhapThuanPrevious:790,  change:-270, changePercent:-28.59 },
      { maKhCif:'G05', tenKhachHang:'VŨ THỊ FANG',                  tenDonViCap6:'PHÒNG DNNVV.CN.HOÀN KIẾM', tenAm:'Phạm Thị E',    thuNhapThuan:310, thuNhapThuanPrevious:460,  change:-150, changePercent:-39.58 },
      { maKhCif:'G06', tenKhachHang:'CÔNG TY CP KLM HOLDINGS',      tenDonViCap6:'PHÒNG KHCN.CN.TÂY HỒ',     tenAm:'Đỗ Văn Quang',  thuNhapThuan:890, thuNhapThuanPrevious:1400, change:-510, changePercent:-33.95 },
      { maKhCif:'G07', tenKhachHang:'PHẠM THỊ SÀU',                 tenDonViCap6:'PGD HÀ THÀNH.CN.HOÀN KIẾM',tenAm:'Trần Thị B',    thuNhapThuan:780, thuNhapThuanPrevious:1250, change:-470, changePercent:-16.27 },
      { maKhCif:'G08', tenKhachHang:'CÔNG TY TNHH VƯƠNG MINH',     tenDonViCap6:'PGD HÀ THÀNH.CN.HOÀN KIẾM',tenAm:'Trần Văn Khánh',thuNhapThuan:640, thuNhapThuanPrevious:1000, change:-360, changePercent:-44.38 },
    ];
  }
}
