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
import { MatTabsModule } from '@angular/material/tabs';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { NgChartsModule } from 'ng2-charts';
import { Chart, ChartData, ChartConfiguration, registerables } from 'chart.js';
import { MpaService } from '../../core/services/mpa.service';
import { AuthService } from '../../core/services/auth.service';

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
    MatIconModule, MatButtonModule, MatFormFieldModule,
    MatInputModule, MatSelectModule, MatDatepickerModule,
    MatNativeDateModule, MatProgressSpinnerModule,
    MatTabsModule, MatSnackBarModule, NgChartsModule
  ],
  templateUrl: './bao-cao.component.html',
  styleUrl: './bao-cao.component.scss'
})
export class BaoCaoComponent implements OnInit {
  private mpaService = inject(MpaService);
  private snack      = inject(MatSnackBar);
  auth               = inject(AuthService);

  loading     = signal(false);
  exporting   = signal(false);
  activeReport = signal('tnt-phong');

  fromDate = new Date(new Date().getFullYear(), new Date().getMonth(), 1);
  toDate   = new Date();
  selectedPhong = '';
  phongList: { ma: string; ten: string }[] = [];

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
    plugins: { legend: { display: false } },
    scales: {
      x: { grid: { display: false }, ticks: { font: { size: 11 }, color: '#9ca3af' } },
      y: { grid: { color: '#f3f4f6' }, ticks: { font: { size: 11 }, color: '#9ca3af', callback: v => `${(v as number).toFixed(0)}t` } }
    },
    elements: { bar: { borderRadius: 6 } }
  };

  lineChartOptions: ChartConfiguration<'line'>['options'] = {
    responsive: true, maintainAspectRatio: false,
    interaction: { mode: 'index', intersect: false },
    plugins: {
      legend: { display: true, position: 'top', labels: { font: { size: 11 }, boxWidth: 10, padding: 14 } }
    },
    scales: {
      x: { grid: { display: false }, ticks: { font: { size: 11 }, color: '#9ca3af' } },
      y: { grid: { color: '#f3f4f6' }, ticks: { font: { size: 11 }, color: '#9ca3af', callback: v => `${(v as number).toFixed(0)}t` } }
    },
    elements: { line: { tension: 0.4, borderWidth: 2 }, point: { radius: 3, hoverRadius: 6 } }
  };

  doughnutOptions: ChartConfiguration<'doughnut'>['options'] = {
    responsive: true, maintainAspectRatio: false,
    plugins: { legend: { display: true, position: 'right', labels: { font: { size: 11 }, padding: 14 } } },
    cutout: '60%'
  };

  tableRows = signal<ReportRow[]>([]);

  ngOnInit(): void {
    this.loadPhongList();
    this.loadReport();
  }

  selectReport(id: string): void {
    this.activeReport.set(id);
    this.loadReport();
  }

  loadReport(): void {
    this.loading.set(true);
    setTimeout(() => {
      this.buildMockCharts();
      this.buildMockTable();
      this.loading.set(false);
    }, 600);
  }

  private buildMockCharts(): void {
    const months = ['T1','T2','T3','T4','T5','T6'];
    const phongs = ['DNNVV TH','KHCN TH','HÀ THÀNH','HÀ TRUNG','KHCN HK','HOÀN KIẾM'];
    const colors = ['#0f59a6','#009640','#7c3aed','#f59e0b','#ef4444','#06b6d4'];

    switch (this.activeReport()) {
      case 'tnt-phong':
        this.barChartData.set({
          labels: phongs,
          datasets: [{
            label: 'TNT (triệu)',
            data: phongs.map(() => +(1000 + Math.random() * 5000).toFixed(0)),
            backgroundColor: colors.map(c => c + 'cc'),
            borderColor: colors,
            borderWidth: 1
          }]
        });
        this.pieChartData.set({
          labels: phongs,
          datasets: [{ data: phongs.map(() => +(1000 + Math.random() * 5000).toFixed(0)), backgroundColor: colors }]
        });
        break;

      case 'xu-huong':
        this.lineChartData.set({
          labels: months,
          datasets: [
            { label: 'TNT HĐV FTP', data: months.map(() => +(500+Math.random()*2000).toFixed(0)), borderColor: '#0f59a6', backgroundColor: 'transparent' },
            { label: 'TNT Dịch vụ', data: months.map(() => +(200+Math.random()*1000).toFixed(0)), borderColor: '#009640', backgroundColor: 'transparent' },
            { label: 'TNT Tín dụng', data: months.map(() => +(800+Math.random()*3000).toFixed(0)), borderColor: '#7c3aed', backgroundColor: 'transparent' },
          ]
        });
        break;

      default:
        this.barChartData.set({
          labels: phongs,
          datasets: [{
            label: 'Giá trị',
            data: phongs.map(() => +(500 + Math.random() * 4000).toFixed(0)),
            backgroundColor: colors.map(c => c + 'cc'),
            borderColor: colors,
            borderWidth: 1
          }]
        });
    }
  }

  private buildMockTable(): void {
    const names = ['PHÒNG DNNVV.CN.TÂY HỒ','PHÒNG KHCN.CN.TÂY HỒ','PGD HÀ THÀNH.CN.HOÀN KIẾM','POD HÀ TRUNG.CN.HOÀN KIẾM','PHÒNG KHCN.CN.HOÀN KIẾM','PGD HOÀN KIẾM.CN.HOÀN KIẾM'];
    const rows: ReportRow[] = names.map(name => {
      const value = +(1000 + Math.random() * 5000).toFixed(3);
      const prev  = +(value * (0.8 + Math.random() * 0.4)).toFixed(3);
      const change = +(value - prev).toFixed(3);
      const pct    = +((change / prev) * 100).toFixed(1);
      return { name, value, prev, change, changePct: pct, unit: 'triệu' };
    }).sort((a, b) => b.value - a.value);
    this.tableRows.set(rows);
  }

  private loadPhongList(): void {
    this.mpaService.getPhongList().subscribe({
      next: r => { if (r.success) this.phongList = r.data; },
      error: () => {}
    });
  }

  exportExcel(): void {
    this.exporting.set(true);
    this.mpaService.exportExcel({}).subscribe({
      next: blob => {
        this.mpaService.downloadBlob(blob, `bao-cao-${this.activeReport()}.xlsx`);
        this.exporting.set(false);
      },
      error: () => {
        this.snack.open('Xuất Excel thất bại', 'Đóng', { duration: 3000 });
        this.exporting.set(false);
      }
    });
  }

  exportPdf(): void {
    this.exporting.set(true);
    this.mpaService.exportPdf({}).subscribe({
      next: blob => {
        this.mpaService.downloadBlob(blob, `bao-cao-${this.activeReport()}.pdf`);
        this.exporting.set(false);
      },
      error: () => {
        this.snack.open('Xuất PDF thất bại', 'Đóng', { duration: 3000 });
        this.exporting.set(false);
      }
    });
  }

  changeClass(v: number): string { return v > 0 ? 'up' : v < 0 ? 'down' : 'flat'; }
  changeIcon(v: number):  string { return v > 0 ? 'arrow_upward' : v < 0 ? 'arrow_downward' : 'remove'; }
  formatDate(d: Date):    string { return d.toISOString().split('T')[0]; }

  getActiveReportLabel(): string {
    return this.reportTypes.find(r => r.id === this.activeReport())?.label ?? '';
  }

  totalValue(): number { return this.tableRows().reduce((s, r) => s + r.value, 0); }
  totalPrev():  number { return this.tableRows().reduce((s, r) => s + r.prev,  0); }
}
