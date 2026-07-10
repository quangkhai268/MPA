import { Component, OnInit, signal, inject } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';

import { AuthService } from '../../../core/services/auth.service';
import { CampaignService } from '../../../core/services/campaign.service';
import { Campaign } from '../../../core/models/mpa.model';

@Component({
  selector: 'app-chien-dich-list',
  standalone: true,
  imports: [CommonModule, DatePipe, RouterModule, MatIconModule, MatProgressSpinnerModule, MatSnackBarModule],
  templateUrl: './chien-dich-list.component.html',
  styleUrl: './chien-dich-list.component.scss'
})
export class ChienDichListComponent implements OnInit {
  auth = inject(AuthService);
  private campaignService = inject(CampaignService);
  private router = inject(Router);
  private snack = inject(MatSnackBar);

  loading = signal(false);
  campaigns = signal<Campaign[]>([]);

  ngOnInit(): void {
    if (!this.auth.isAdmin()) return;
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.campaignService.getAll().subscribe({
      next: res => {
        if (res.success) this.campaigns.set(res.data);
        this.loading.set(false);
      },
      error: () => this.loading.set(false)
    });
  }

  goBack(): void {
    this.router.navigate(['/quan-ly-the']);
  }

  goToCreate(): void {
    this.router.navigate(['/quan-ly-the/chien-dich/moi']);
  }

  goToEdit(id: number): void {
    this.router.navigate(['/quan-ly-the/chien-dich', id]);
  }

  deleteCampaign(c: Campaign, event: Event): void {
    event.stopPropagation();
    if (!confirm(`Xóa chiến dịch "${c.tenChienDich}"?`)) return;
    this.campaignService.delete(c.id).subscribe({
      next: res => {
        if (res.success) {
          this.snack.open('Đã xóa chiến dịch', 'Đóng', { duration: 3000 });
          this.load();
        } else {
          this.snack.open('Lỗi khi xóa: ' + res.message, 'Đóng', { duration: 4000 });
        }
      },
      error: () => this.snack.open('Lỗi khi xóa chiến dịch', 'Đóng', { duration: 4000 })
    });
  }

  trangThaiClass(tt: string): string {
    switch (tt) {
      case 'ACTIVE': return 'badge-green';
      case 'PAUSED': return 'badge-yellow';
      case 'ENDED': return 'badge-gray';
      default: return 'badge-blue';
    }
  }

  trangThaiLabel(tt: string): string {
    switch (tt) {
      case 'ACTIVE': return 'Đang chạy';
      case 'PAUSED': return 'Tạm dừng';
      case 'ENDED': return 'Đã kết thúc';
      default: return 'Nháp';
    }
  }
}
