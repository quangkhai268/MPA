import { Component, OnInit, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { NgChartsModule } from 'ng2-charts';
import { Chart, ChartData, ChartConfiguration, registerables } from 'chart.js';
import { MpaService } from '../../core/services/mpa.service';
import { AuthService } from '../../core/services/auth.service';
import { FilterParams, PhongData, AmData, KhachHangData } from '../../core/models/mpa.model';

Chart.register(...registerables);

interface ReportType {
  id: string;
  label: string;
  icon: string;
  desc: string;
  color: string;
}

interface ReportRow {
  name: string;
  value: number;
  prev: number;
  change: number;
  changePct: number;
  unit: string;
}

@Component({
  selector: 'app-bao-cao',
  standalone: true,
  imports: [
    CommonModule, FormsModule,
    MatIconModule, MatButtonModule,
    MatProgressSpinnerModule, MatTooltipModule,
    NgChartsModule
  ],
  templateUrl: './bao-cao.component.html',
  styleUrl: './bao-cao.component.scss'
})
export class BaoCaoComponent implements OnInit {
  private mpaService = inject(MpaService);
  auth               = inject(AuthService);

  loading      = signal(false);
  activeReport = signal('tnt-phong');

  // ── Filter (Loại kỳ / Kỳ / So với) — cùng pattern với dashboard ────────
  loaiKy: 'thang' | 'quy' | 'nam' | 'ngay' = 'thang';
  selectedKy = '';
  soVoi = 'ky-truoc';
  namLuyKeOptions: number[] = [];
  selectedDenThang: number | null = null;
  private filter: FilterParams = {};

  reportTypes: ReportType[] = [
    { id: 'tnt-phong',  label: 'TNT theo Phòng',       icon: 'business',        desc: 'Báo cáo TNT phân tích theo từng phòng/đơn vị',     color: '#0f59a6' },
    { id: 'tnt-am',     label: 'TNT theo cán bộ AM',   icon: 'person',          desc: 'Xếp hạng hiệu suất từng cán bộ AM',                 color: '#009640' },
    { id: 'tnt-kh',     label: 'TNT theo Khách hàng',  icon: 'groups',          desc: 'Báo cáo đóng góp TNT từng khách hàng',             color: '#7c3aed' },
    { id: 'hdv',        label: 'Huy động vốn',          icon: 'savings',         desc: 'Phân tích HĐV theo kỳ và phòng ban',               color: '#f59e0b' },
    { id: 'duno',       label: 'Dư nợ tín dụng',       icon: 'trending_up',     desc: 'Biến động dư nợ tín dụng trong kỳ',                color: '#ef4444' },
    { id: 'xu-huong',   label: 'Xu hướng theo tháng',  icon: 'timeline',        desc: 'So sánh xu hướng TNT qua các tháng',               color: '#06b6d4' },
  ];

  // Chart data
  barChartData  = signal<ChartData<'bar'>>({ labels: [], datasets: [] });
  lineChartData = signal<ChartData<'line'>>({ labels: [], datasets: [] });
  pieChartData  = signal<ChartData<'doughnut'>>({ labels: [], datasets: [] });

  barChartOptions: ChartConfiguration<'bar'>['options'] = {
    responsive: true, maintainAspectRatio: false,
    plugins: {
      legend: { display: false },
      tooltip: { callbacks: { label: ctx => ` ${this.formatTy(ctx.raw as number)} tỷ` } }
    },
    scales: {
      x: { grid: { display: false }, ticks: { font: { size: 11 }, color: '#9ca3af' } },
      y: { grid: { color: '#f3f4f6' }, ticks: { font: { size: 11 }, color: '#9ca3af', callback: v => `${this.formatTyAxis(v as number)} tỷ` } }
    },
    elements: { bar: { borderRadius: 6 } }
  };

  lineChartOptions: ChartConfiguration<'line'>['options'] = {
    responsive: true, maintainAspectRatio: false,
    interaction: { mode: 'index', intersect: false },
    plugins: {
      legend: { display: true, position: 'top', labels: { font: { size: 11 }, boxWidth: 10, padding: 14 } },
      tooltip: { callbacks: { label: ctx => ` ${ctx.dataset.label}: ${this.formatTy(ctx.raw as number)} tỷ` } }
    },
    scales: {
      x: { grid: { display: false }, ticks: { font: { size: 11 }, color: '#9ca3af' } },
      y: { grid: { color: '#f3f4f6' }, ticks: { font: { size: 11 }, color: '#9ca3af', callback: v => `${this.formatTyAxis(v as number)} tỷ` } }
    },
    elements: { line: { tension: 0.4, borderWidth: 2 }, point: { radius: 3, hoverRadius: 6 } }
  };

  doughnutOptions: ChartConfiguration<'doughnut'>['options'] = {
    responsive: true, maintainAspectRatio: false,
    plugins: {
      legend: { display: true, position: 'right', labels: { font: { size: 11 }, padding: 14 } },
      tooltip: { callbacks: { label: ctx => ` ${ctx.label}: ${this.formatTy(ctx.raw as number)} tỷ` } }
    },
    cutout: '60%'
  };

  /** Chia cho 1 tỷ (1e9) — dữ liệu BSC ở đây là số nguyên VND, không phải triệu như du_lieu_mpa. */
  private formatTy(v: number): string {
    return (v / 1_000_000_000).toFixed(2);
  }

  /** Format trục Y: số nguyên, dấu chấm phân cách hàng nghìn (VD 12000t → "12.000"). */
  private formatTyAxis(v: number): string {
    return Math.round(v / 1_000_000_000).toLocaleString('vi-VN');
  }

  tableRows = signal<ReportRow[]>([]);

  readonly reportColors = ['#0f59a6', '#009640', '#7c3aed', '#f59e0b', '#ef4444', '#06b6d4', '#0891b2', '#d946ef', '#65a30d', '#f43f5e'];

  // ── Loại kỳ / Kỳ (copy pattern từ dashboard.component.ts) ──────────────
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

  ngOnInit(): void {
    this.selectedKy = this.kyOptions.length ? this.kyOptions[0].value : '';
    this.loadReport();
  }

  selectReport(id: string): void {
    this.activeReport.set(id);
    this.loadReport();
  }

  onLoaiKyChange(): void {
    this.selectedKy = this.kyOptions.length ? this.kyOptions[0].value : '';
    this.onFilterChange();
  }

  onFilterChange(): void {
    if (this.loaiKy === 'nam') {
      this.loadNamLuyKeOptionsAndRefresh();
    } else {
      this.loadReport();
    }
  }

  private loadNamLuyKeOptionsAndRefresh(): void {
    const y = +this.selectedKy;
    if (!y) { this.namLuyKeOptions = []; this.selectedDenThang = null; this.loadReport(); return; }
    this.mpaService.getNamLuyKeOptions(y).subscribe({
      next: r => {
        this.namLuyKeOptions = r.success && r.data ? r.data : [];
        this.selectedDenThang = this.namLuyKeOptions.length ? this.namLuyKeOptions[0] : null;
        this.loadReport();
      },
      error: () => {
        this.namLuyKeOptions = [];
        this.selectedDenThang = null;
        this.loadReport();
      }
    });
  }

  private updateFilter(): void {
    this.filter.loaiKy = this.loaiKy;
    this.filter.selectedKy = this.selectedKy;
    this.filter.soVoi = this.soVoi;
    this.filter.denThangLuyKe = undefined;
    this.filter.thang = undefined;
    this.filter.quy = undefined;
    this.filter.nam = undefined;
    this.filter.ngay = undefined;
    if (this.loaiKy === 'thang' && this.selectedKy) {
      const [m, y] = this.selectedKy.split('/');
      this.filter.thang = +m; this.filter.nam = +y;
    } else if (this.loaiKy === 'quy' && this.selectedKy) {
      const [q, y] = this.selectedKy.split('/');
      this.filter.quy = q; this.filter.nam = +y;
    } else if (this.loaiKy === 'nam' && this.selectedKy) {
      this.filter.nam = +this.selectedKy;
      this.filter.denThangLuyKe = this.selectedDenThang ?? undefined;
    } else if (this.loaiKy === 'ngay' && this.selectedKy) {
      this.filter.ngay = this.selectedKy;
    }
  }

  // ── Load report — dispatch theo loại báo cáo, gọi API thật ─────────────
  loadReport(): void {
    this.updateFilter();
    this.loading.set(true);

    switch (this.activeReport()) {
      case 'tnt-am':
        this.mpaService.getTopAm(this.filter, 'tong-tnt', 10).subscribe({
          next: r => { this.applyAmData(r.success ? r.data : []); this.loading.set(false); },
          error: () => { this.applyAmData([]); this.loading.set(false); }
        });
        break;

      case 'tnt-kh':
        this.mpaService.getTopKh(this.filter, 'tong-tnt', 10).subscribe({
          next: r => { this.applyKhData(r.success ? r.data : []); this.loading.set(false); },
          error: () => { this.applyKhData([]); this.loading.set(false); }
        });
        break;

      case 'xu-huong':
        this.loadXuHuong();
        break;

      case 'hdv':
        this.mpaService.getPhongData(this.filter).subscribe({
          next: r => { this.applyPhongData(r.success ? r.data : [], 'hdvCuoiKy', 'hdvCuoiKyPrevious', 'hdvCuoiKyChangePercent'); this.loading.set(false); },
          error: () => { this.applyPhongData([], 'hdvCuoiKy', 'hdvCuoiKyPrevious', 'hdvCuoiKyChangePercent'); this.loading.set(false); }
        });
        break;

      case 'duno':
        this.mpaService.getPhongData(this.filter).subscribe({
          next: r => { this.applyPhongData(r.success ? r.data : [], 'duNo', 'duNoPrevious', 'duNoChangePercent'); this.loading.set(false); },
          error: () => { this.applyPhongData([], 'duNo', 'duNoPrevious', 'duNoChangePercent'); this.loading.set(false); }
        });
        break;

      default: // tnt-phong
        this.mpaService.getPhongData(this.filter).subscribe({
          next: r => { this.applyPhongData(r.success ? r.data : [], 'thuNhapThuan', 'thuNhapThuanPrevious', 'changePercent'); this.loading.set(false); },
          error: () => { this.applyPhongData([], 'thuNhapThuan', 'thuNhapThuanPrevious', 'changePercent'); this.loading.set(false); }
        });
    }
  }

  private loadXuHuong(): void {
    const metrics: { key: string; label: string; color: string }[] = [
      { key: 'tnt-hdv',      label: 'TNT HĐV FTP',   color: '#0f59a6' },
      { key: 'tnt-dich-vu',  label: 'TNT Dịch vụ',   color: '#009640' },
      { key: 'tnt-tin-dung', label: 'TNT Tín dụng',  color: '#7c3aed' },
    ];
    let done = 0;
    const datasets: ChartConfiguration<'line'>['data']['datasets'] = [];
    let labels: string[] = [];
    metrics.forEach(m => {
      this.mpaService.getTrend(this.filter, m.key).subscribe({
        next: r => {
          if (r.success && r.data) {
            labels = r.data.labels;
            datasets.push({ label: m.label, data: r.data.currentValues, borderColor: m.color, backgroundColor: 'transparent' });
          }
          done++;
          if (done === metrics.length) this.finishXuHuong(labels, datasets);
        },
        error: () => {
          done++;
          if (done === metrics.length) this.finishXuHuong(labels, datasets);
        }
      });
    });
  }

  private finishXuHuong(labels: string[], datasets: ChartConfiguration<'line'>['data']['datasets']): void {
    this.lineChartData.set({ labels, datasets });
    this.tableRows.set([]); // "Xu hướng theo tháng" hiển thị dạng đường, không có bảng xếp hạng theo dòng
    this.loading.set(false);
  }

  private applyPhongData(rows: PhongData[], valueKey: 'thuNhapThuan' | 'hdvCuoiKy' | 'duNo',
                          prevKey: 'thuNhapThuanPrevious' | 'hdvCuoiKyPrevious' | 'duNoPrevious',
                          pctKey: 'changePercent' | 'hdvCuoiKyChangePercent' | 'duNoChangePercent'): void {
    const sorted = [...rows].sort((a, b) => (b[valueKey] ?? 0) - (a[valueKey] ?? 0)).slice(0, 10);
    this.setChartsAndTable(
      sorted.map(r => r.tenDonViCap6),
      sorted.map(r => ({
        name: r.tenDonViCap6,
        value: r[valueKey] ?? 0,
        prev: r[prevKey] ?? 0,
        change: (r[valueKey] ?? 0) - (r[prevKey] ?? 0),
        changePct: r[pctKey] ?? 0,
        unit: 'Phòng/đơn vị'
      }))
    );
  }

  private applyAmData(rows: AmData[]): void {
    const sorted = [...rows].sort((a, b) => (b.value ?? b.thuNhapThuan ?? 0) - (a.value ?? a.thuNhapThuan ?? 0)).slice(0, 10);
    this.setChartsAndTable(
      sorted.map(r => r.tenAm),
      sorted.map(r => ({
        name: r.tenAm,
        value: r.value ?? r.thuNhapThuan ?? 0,
        prev: r.thuNhapThuanPrevious ?? 0,
        change: r.change ?? 0,
        changePct: r.changePercent ?? 0,
        unit: r.tenDonViCap6 || 'Cán bộ AM'
      }))
    );
  }

  private applyKhData(rows: KhachHangData[]): void {
    const sorted = [...rows].sort((a, b) => (b.value ?? b.thuNhapThuan ?? 0) - (a.value ?? a.thuNhapThuan ?? 0)).slice(0, 10);
    this.setChartsAndTable(
      sorted.map(r => r.tenKhachHang),
      sorted.map(r => ({
        name: r.tenKhachHang,
        value: r.value ?? r.thuNhapThuan ?? 0,
        prev: r.thuNhapThuanPrevious ?? 0,
        change: r.change ?? 0,
        changePct: r.changePercent ?? 0,
        unit: 'CIF ' + r.maKhCif
      }))
    );
  }

  private setChartsAndTable(labels: string[], rows: ReportRow[]): void {
    const values = rows.map(r => r.value);
    this.barChartData.set({
      labels,
      datasets: [{
        label: 'Giá trị',
        data: values,
        backgroundColor: this.reportColors.map(c => c + 'cc'),
        borderColor: this.reportColors,
        borderWidth: 1
      }]
    });
    this.pieChartData.set({
      labels,
      datasets: [{ data: values, backgroundColor: this.reportColors }]
    });
    this.tableRows.set(rows);
  }

  changeClass(v: number): string { return v > 0 ? 'up' : v < 0 ? 'down' : 'flat'; }
  changeIcon(v: number):  string { return v > 0 ? 'arrow_upward' : v < 0 ? 'arrow_downward' : 'remove'; }

  getActiveReportLabel(): string {
    return this.reportTypes.find(r => r.id === this.activeReport())?.label ?? '';
  }

  totalValue(): number { return this.tableRows().reduce((s, r) => s + r.value, 0); }
  totalPrev():  number { return this.tableRows().reduce((s, r) => s + r.prev,  0); }
}
