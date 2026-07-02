import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatMenuModule } from '@angular/material/menu';
import { MatBadgeModule } from '@angular/material/badge';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatDividerModule } from '@angular/material/divider';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [
    CommonModule, RouterModule, MatIconModule, MatButtonModule,
    MatMenuModule, MatBadgeModule, MatTooltipModule, MatDividerModule
  ],
  templateUrl: './header.component.html',
  styleUrl: './header.component.scss'
})
export class HeaderComponent {
  auth = inject(AuthService);
  router = inject(Router);
  notifCount = signal(3);

  logout(): void { this.auth.logout(); }

  get pageTitle(): string {
    const url = this.router.url;
    if (url.startsWith('/dashboard'))   return 'Dashboard tổng quan';
    if (url.startsWith('/du-lieu-mpa')) return 'Dữ liệu MPA';
    if (url.startsWith('/bao-cao'))     return 'Báo cáo & Thống kê';
    if (url.startsWith('/quan-tri'))    return 'Quản trị hệ thống';
    return 'BIDV MPA';
  }

  get pageSubtitle(): string {
    const now = new Date();
    return `Cập nhật lúc ${now.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' })} – ${now.toLocaleDateString('vi-VN', { weekday: 'long', day: '2-digit', month: '2-digit', year: 'numeric' })}`;
  }
}
