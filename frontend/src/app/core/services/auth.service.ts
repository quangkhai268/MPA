import { Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, tap, catchError, throwError } from 'rxjs';
import {
  ApiResponse, AuthResponse, ChangePasswordRequest, LoginRequest, RefreshTokenRequest, User
} from '../models/user.model';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly TOKEN_KEY = 'mpa_access_token';
  private readonly REFRESH_KEY = 'mpa_refresh_token';
  private readonly USER_KEY = 'mpa_user';

  currentUser = signal<User | null>(this.loadUser());

  constructor(private http: HttpClient, private router: Router) {}

  login(req: LoginRequest): Observable<ApiResponse<AuthResponse>> {
    return this.http.post<ApiResponse<AuthResponse>>(`${environment.apiUrl}/auth/login`, req).pipe(
      tap(res => {
        if (res.success && res.data) {
          this.saveSession(res.data);
        }
      }),
      catchError(err => {
        if (err.status === 0) {
          return throwError(() => ({ error: { message: 'Không thể kết nối đến máy chủ. Vui lòng thử lại sau.' } }));
        }
        return throwError(() => err);
      })
    );
  }

  refresh(): Observable<ApiResponse<AuthResponse>> {
    const refreshToken = localStorage.getItem(this.REFRESH_KEY);
    const req: RefreshTokenRequest = { refreshToken: refreshToken ?? '' };
    return this.http.post<ApiResponse<AuthResponse>>(`${environment.apiUrl}/auth/refresh`, req).pipe(
      tap(res => {
        if (res.success && res.data) {
          this.saveSession(res.data);
        }
      })
    );
  }

  changePassword(req: ChangePasswordRequest): Observable<ApiResponse<void>> {
    return this.http.post<ApiResponse<void>>(`${environment.apiUrl}/auth/change-password`, req).pipe(
      tap(res => {
        if (res.success) {
          const user = this.currentUser();
          if (user) {
            const updated = { ...user, mustChangePassword: false };
            localStorage.setItem(this.USER_KEY, JSON.stringify(updated));
            this.currentUser.set(updated);
          }
        }
      })
    );
  }

  logout(): void {
    // Phase 1: chưa có revoke token thật ở backend — gọi trước khi xoá token cục bộ để sẵn
    // hook cho sau này; lỗi mạng/token hết hạn lúc gọi cũng không sao, vẫn xoá session cục bộ.
    this.http.post(`${environment.apiUrl}/auth/logout`, {}).subscribe({ next: () => {}, error: () => {} });
    localStorage.removeItem(this.TOKEN_KEY);
    localStorage.removeItem(this.REFRESH_KEY);
    localStorage.removeItem(this.USER_KEY);
    this.currentUser.set(null);
    this.router.navigate(['/login']);
  }

  getToken(): string | null {
    return localStorage.getItem(this.TOKEN_KEY);
  }

  isLoggedIn(): boolean {
    return !!this.getToken();
  }

  hasRole(...roles: string[]): boolean {
    const user = this.currentUser();
    return !!user && roles.includes(user.role);
  }

  isAdmin(): boolean {
    return this.hasRole('ROLE_ADMIN');
  }

  mustChangePassword(): boolean {
    return !!this.currentUser()?.mustChangePassword;
  }

  private saveSession(data: AuthResponse): void {
    localStorage.setItem(this.TOKEN_KEY, data.accessToken);
    localStorage.setItem(this.REFRESH_KEY, data.refreshToken);
    localStorage.setItem(this.USER_KEY, JSON.stringify(data.user));
    this.currentUser.set(data.user);
  }

  private loadUser(): User | null {
    try {
      const raw = localStorage.getItem(this.USER_KEY);
      return raw ? JSON.parse(raw) : null;
    } catch {
      return null;
    }
  }
}
