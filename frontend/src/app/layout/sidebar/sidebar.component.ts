import { Component, signal, computed, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router, NavigationEnd } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { filter } from 'rxjs';
import { AuthService } from '../../core/services/auth.service';

interface NavItem {
  label: string;
  icon: string;
  route: string;
  roles?: string[];
  badge?: number;
}

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [CommonModule, RouterModule, MatIconModule, MatTooltipModule],
  templateUrl: './sidebar.component.html',
  styleUrl: './sidebar.component.scss'
})
export class SidebarComponent {
  auth = inject(AuthService);
  collapsed = signal(false);
  activeRoute = signal('');

  navItems: NavItem[] = [
    { label: 'Tổng quan',         icon: 'dashboard',            route: '/dashboard'    },
    { label: 'Dữ liệu MPA',       icon: 'table_chart',          route: '/du-lieu-mpa'  },
    { label: 'Khách hàng',        icon: 'people',               route: '/khach-hang'   },
    { label: 'Báo cáo & Thống kê',icon: 'bar_chart',            route: '/bao-cao'      },
    { label: 'Giao chỉ tiêu BSC', icon: 'assignment_turned_in', route: '/giao-chi-tieu'},
    { label: 'Quản lý cán bộ AM', icon: 'badge',                route: '/quan-ly-am'   },
    { label: 'Quản lý thẻ',       icon: 'credit_card',          route: '/quan-ly-the'  },
    { label: 'Quản trị hệ thống', icon: 'manage_accounts',      route: '/quan-tri', roles: ['ROLE_ADMIN'] },
  ];

  navSections: { label: string; items: NavItem[] }[] = [
    {
      label: 'Nghiệp vụ',
      items: [
        { label: 'Tổng quan',          icon: 'dashboard',            route: '/dashboard'      },
        { label: 'Dữ liệu MPA',        icon: 'table_chart',          route: '/du-lieu-mpa'    },
        { label: 'Khách hàng',         icon: 'people',               route: '/khach-hang'     },
        { label: 'Báo cáo & Thống kê', icon: 'bar_chart',            route: '/bao-cao'        },
        { label: 'Giao chỉ tiêu BSC',  icon: 'assignment_turned_in', route: '/giao-chi-tieu' },
      ]
    },
    {
      label: 'Quản trị',
      items: [
        { label: 'Quản lý cán bộ AM',  icon: 'badge',           route: '/quan-ly-am'                      },
        { label: 'Quản lý thẻ',        icon: 'credit_card',     route: '/quan-ly-the'                     },
        { label: 'Quản trị hệ thống',  icon: 'manage_accounts', route: '/quan-tri', roles: ['ROLE_ADMIN'] },
      ]
    }
  ];

  visibleItems = computed(() =>
    this.navItems.filter(item =>
      !item.roles || this.auth.hasRole(...item.roles)
    )
  );

  constructor(private router: Router) {
    this.router.events.pipe(filter(e => e instanceof NavigationEnd)).subscribe((e: any) => {
      this.activeRoute.set(e.urlAfterRedirects);
    });
    this.activeRoute.set(this.router.url);
  }

  isActive(route: string): boolean {
    return this.activeRoute().startsWith(route);
  }

  toggle(): void { this.collapsed.update(v => !v); }

  getRoleLabel(role?: string): string {
    const map: Record<string, string> = {
      'ROLE_ADMIN': 'Quản trị viên',
      'ROLE_BRANCH_MANAGER': 'Quản lý chi nhánh',
      'ROLE_EMPLOYEE': 'Nhân viên AM'
    };
    return role ? (map[role] || role) : '';
  }
}
