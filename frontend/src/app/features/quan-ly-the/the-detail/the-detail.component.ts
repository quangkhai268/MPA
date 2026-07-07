import { Component, OnInit, signal, inject } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

import { MpaService } from '../../../core/services/mpa.service';
import { ThePhatHanhDetail } from '../../../core/models/mpa.model';

@Component({
  selector: 'app-the-detail',
  standalone: true,
  imports: [CommonModule, DatePipe, RouterModule, MatIconModule, MatProgressSpinnerModule],
  templateUrl: './the-detail.component.html',
  styleUrl: './the-detail.component.scss'
})
export class TheDetailComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private mpaService = inject(MpaService);

  loading = signal(false);
  data = signal<ThePhatHanhDetail | null>(null);
  doanhSoTab: 'ngay' | 'thang' = 'ngay';

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (id) this.load(id);
  }

  private load(id: number): void {
    this.loading.set(true);
    this.mpaService.getTheDetail(id).subscribe({
      next: res => {
        if (res.success) this.data.set(res.data);
        this.loading.set(false);
      },
      error: () => this.loading.set(false)
    });
  }

  goBack(): void {
    this.router.navigate(['/quan-ly-the']);
  }

  goToKh360(): void {
    const d = this.data();
    const cif = d?.cifChuTheChinh || d?.soCifKhachHangPht;
    if (cif) this.router.navigate(['/khach-hang', cif]);
  }

  setDoanhSoTab(tab: 'ngay' | 'thang'): void {
    this.doanhSoTab = tab;
  }

  // ── Format helpers ────────────────────────────────────────────────────

  formatTrieu(value: number | null | undefined): string {
    if (value == null) return '—';
    return value.toLocaleString('vi-VN', { minimumFractionDigits: 2, maximumFractionDigits: 2 }) + ' triệu';
  }

  formatSoTienVnd(value: number | null | undefined): string {
    if (value == null) return '—';
    if (value >= 1_000_000_000) return this.trimNum(value / 1_000_000_000) + ' tỷ';
    if (value >= 10_000_000) return this.trimNum(value / 1_000_000) + ' triệu';
    return Math.round(value).toLocaleString('vi-VN');
  }

  private trimNum(v: number): string {
    if (Number.isInteger(v)) return v.toLocaleString('vi-VN');
    return parseFloat(v.toFixed(3)).toLocaleString('vi-VN', { maximumFractionDigits: 3 });
  }

  formatSdt(sdt: string | null | undefined): string {
    if (!sdt) return '—';
    return sdt.trim().replace(/^\/+|\/+$/g, '');
  }

  clamp100(v: number | null | undefined): number {
    return Math.min(v ?? 0, 100);
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

  daDatMucPtn(d: ThePhatHanhDetail): boolean {
    return (d.pctPtn ?? 0) >= 100;
  }

  soTienConThieuPtn(d: ThePhatHanhDetail): number {
    const target = d.doanhSoMienPtn ?? 0;
    const actual = d.doanhSoGiaoDichMienPtn ?? 0;
    return Math.max(target - actual, 0);
  }
}
