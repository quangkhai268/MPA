import { Component, OnInit, signal, inject } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatIconModule } from '@angular/material/icon';

import { MpaService } from '../../../core/services/mpa.service';
import { KhachHangChiTiet, KhachHangTheSummary, ThePhatHanhItem } from '../../../core/models/mpa.model';

@Component({
  selector: 'app-khach-hang-detail',
  standalone: true,
  imports: [CommonModule, DatePipe, FormsModule, RouterModule, MatProgressSpinnerModule, MatIconModule],
  templateUrl: './khach-hang-detail.component.html',
  styleUrl: './khach-hang-detail.component.scss'
})
export class KhachHangDetailComponent implements OnInit {
  private route  = inject(ActivatedRoute);
  private router = inject(Router);
  private mpaService = inject(MpaService);

  // ── State ─────────────────────────────────────────────────────────────
  loading  = signal(false);
  data     = signal<KhachHangChiTiet | null>(null);
  cif      = '';

  // ── Thẻ tín dụng ─────────────────────────────────────────────────────
  theLoading = signal(false);
  theSummary = signal<KhachHangTheSummary | null>(null);

  // ── Filter ─────────────────────────────────────────────────────────────
  selectedNam:   number = new Date().getFullYear();
  selectedThang: number | null = new Date().getMonth() + 1;
  selectedQuy:   string | null = null;
  soVoi: 'ky-truoc' | 'cung-ky-nam-truoc' = 'ky-truoc';

  readonly namOptions = Array.from({ length: 5 }, (_, i) => new Date().getFullYear() - i);
  readonly thangOptions = Array.from({ length: 12 }, (_, i) => i + 1);

  readonly PHAN_KHUC_LABELS: Record<number, string> = {
    1: 'KHOI BAN BUON',
    2: 'KHOI BAN LE',
    3: 'KHOI FDI',
    4: 'KH DAC BIET'
  };

  ngOnInit(): void {
    this.cif = this.route.snapshot.paramMap.get('cif') ?? '';
    this.load();
    this.loadTheSummary();
  }

  loadTheSummary(): void {
    if (!this.cif) return;
    this.theLoading.set(true);
    this.mpaService.getKhachHangTheSummary(this.cif).subscribe({
      next: res => {
        if (res.success) this.theSummary.set(res.data);
        this.theLoading.set(false);
      },
      error: () => this.theLoading.set(false)
    });
  }

  goToThe(id: number): void {
    this.router.navigate(['/quan-ly-the', id]);
  }

  goToTheList(): void {
    this.router.navigate(['/quan-ly-the'], { queryParams: { q: this.cif } });
  }

  // ── Thẻ tín dụng: format helpers ─────────────────────────────────────
  pctHmtd(card: ThePhatHanhItem): number {
    const hmtd = card.hmtdIssuingContract ?? 0;
    const doanhSo = card.doanhSoGiaoDichMienPtn ?? 0;
    if (hmtd <= 0) return 0;
    return Math.min((doanhSo / hmtd) * 100, 100);
  }

  soTienConThieuPtn(card: ThePhatHanhItem): number {
    const muc = card.doanhSoMienPtn ?? 0;
    const ds  = card.doanhSoGiaoDichMienPtn ?? 0;
    return Math.max(muc - ds, 0);
  }

  daDatMucPtn(card: ThePhatHanhItem): boolean {
    return (card.doanhSoGiaoDichMienPtn ?? 0) >= (card.doanhSoMienPtn ?? 0) && (card.doanhSoMienPtn ?? 0) > 0;
  }

  theTrangThaiClass(tt: string | null): string {
    if (!tt) return 'the-badge-gray';
    const norm = tt.trim().toLowerCase();
    if (norm === 'card ok') return 'the-badge-green';
    if (norm.includes('fraud') || norm.includes('lost') || norm.includes('suspend') || norm.includes('inactive')) return 'the-badge-red';
    if (norm.includes('closed') || norm.includes('not used')) return 'the-badge-gray';
    return 'the-badge-gray';
  }

  load(): void {
    if (!this.cif) return;
    this.loading.set(true);
    this.mpaService.getKhachHangChiTiet(
      this.cif,
      this.selectedNam,
      this.selectedThang,
      this.selectedQuy,
      this.soVoi
    ).subscribe({
      next: res => {
        if (res.success) this.data.set(res.data);
        this.loading.set(false);
      },
      error: () => this.loading.set(false)
    });
  }

  goBack(): void {
    this.router.navigate(['/khach-hang']);
  }

  setSoVoi(v: 'ky-truoc' | 'cung-ky-nam-truoc'): void {
    this.soVoi = v;
    this.load();
  }

  // ── Format helpers ─────────────────────────────────────────────────────

  private readonly TY = 1_000_000_000;
  private readonly TR = 1_000_000;

  /** Format value: đơn vị gốc là VNĐ → hiển thị tỷ (>=1 tỷ) hoặc triệu (<1 tỷ) */
  fmt(v: number | null | undefined): string {
    if (v == null) return '—';
    const abs = Math.abs(v);
    if (abs >= this.TY) return (v / this.TY).toFixed(2) + ' tỷ';
    return (v / this.TR).toFixed(2) + ' triệu';
  }

  /** Format chênh lệch (curr - prev) với dấu +/- */
  fmtDiff(curr: number, prev: number): string {
    const diff = curr - prev;
    const sign = diff >= 0 ? '+' : '';
    const abs  = Math.abs(diff);
    if (abs >= this.TY) return sign + (diff / this.TY).toFixed(2) + ' tỷ';
    return sign + (diff / this.TR).toFixed(2) + ' triệu';
  }

  /** Whether change is positive */
  isUp(curr: number, prev: number): boolean { return curr >= prev; }

  /** % change vs previous period, null if prev = 0 */
  pct(curr: number, prev: number): string | null {
    if (prev === 0) return null;
    return ((curr - prev) / Math.abs(prev) * 100).toFixed(2) + '%';
  }

  /** % vs benchmark, null if tb = 0 */
  pctVsTb(curr: number, tb: number): string | null {
    if (tb === 0) return null;
    const p = (curr - tb) / Math.abs(tb) * 100;
    return (p >= 0 ? '+' : '') + p.toFixed(2) + '%';
  }

  pctVsTbUp(curr: number, tb: number): boolean { return curr >= tb; }

  /** Sparkline SVG path from array of values */
  sparklinePath(values: number[]): string {
    if (!values || values.length < 2) return '';
    const max = Math.max(...values);
    const min = Math.min(...values);
    const range = max - min || 1;
    const W = 80, H = 28;
    return values.map((v, i) => {
      const x = (i / (values.length - 1)) * W;
      const y = H - ((v - min) / range) * H;
      return `${i === 0 ? 'M' : 'L'}${x.toFixed(1)},${y.toFixed(1)}`;
    }).join(' ');
  }

  phanKhucLabel(type: number | null): string {
    if (type == null) return '';
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

  maskGttt(val: string | null): string {
    if (!val) return '';
    if (val.length <= 3) return val;
    return val.slice(0, 3) + '*'.repeat(Math.max(0, val.length - 6)) + val.slice(-3);
  }
}
