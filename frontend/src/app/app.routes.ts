import { Routes } from '@angular/router';
import { authGuard, loginGuard } from './core/guards/auth.guard';

export const routes: Routes = [
  {
    path: 'login',
    canActivate: [loginGuard],
    loadComponent: () => import('./features/auth/login/login.component').then(m => m.LoginComponent)
  },
  {
    path: '',
    canActivate: [authGuard],
    loadComponent: () => import('./layout/main-layout/main-layout.component').then(m => m.MainLayoutComponent),
    children: [
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
      {
        path: 'dashboard',
        loadComponent: () => import('./features/dashboard/dashboard.component').then(m => m.DashboardComponent)
      },
      {
        path: 'du-lieu-mpa',
        loadComponent: () => import('./features/du-lieu-mpa/du-lieu-mpa.component').then(m => m.DuLieuMpaComponent)
      },
      {
        path: 'bao-cao',
        loadComponent: () => import('./features/bao-cao/bao-cao.component').then(m => m.BaoCaoComponent)
      },
      {
        path: 'giao-chi-tieu',
        loadComponent: () => import('./features/giao-chi-tieu/giao-chi-tieu.component').then(m => m.GiaoChiTieuComponent)
      },
      {
        path: 'quan-tri',
        loadComponent: () => import('./features/quan-tri/quan-tri.component').then(m => m.QuanTriComponent)
      },
      {
        path: 'quan-ly-am',
        loadComponent: () => import('./features/quan-ly-am/quan-ly-am.component').then(m => m.QuanLyAmComponent)
      },
      {
        path: 'khach-hang',
        loadComponent: () => import('./features/khach-hang/khach-hang.component').then(m => m.KhachHangComponent)
      },
      {
        path: 'khach-hang/:cif',
        loadComponent: () => import('./features/khach-hang/khach-hang-detail/khach-hang-detail.component').then(m => m.KhachHangDetailComponent)
      },
      {
        path: 'quan-ly-the',
        loadComponent: () => import('./features/quan-ly-the/quan-ly-the.component').then(m => m.QuanLyTheComponent)
      }
    ]
  },
  { path: '**', redirectTo: '' }
];
