import { Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, tap, catchError, throwError, of } from 'rxjs';
import { ApiResponse, AuthResponse, LoginRequest, User } from '../models/user.model';
import { environment } from '../../../environments/environment';

const MOCK_USERS: Record<string, { password: string; user: User; token: string }> = {
  'admin':      { password: 'admin123', token: 'mock-token-admin', user: { id: 1, username: 'admin',      fullName: 'Lê Quang Khải',  email: 'admin@bidv.com.vn',     role: 'ROLE_ADMIN',          maDonViCap6: '',     active: true } },
  'manager01':  { password: 'pass123',  token: 'mock-token-mgr01', user: { id: 2, username: 'manager01',  fullName: 'Trần Thị Hoàng Anh', email: 'hoang.anh@bidv.com.vn', role: 'ROLE_BRANCH_MANAGER', maDonViCap6: 'P001', active: true } },
  'manager02':  { password: 'pass123',  token: 'mock-token-mgr02', user: { id: 3, username: 'manager02',  fullName: 'Lê Văn Phúc',        email: 'van.phuc@bidv.com.vn',  role: 'ROLE_BRANCH_MANAGER', maDonViCap6: 'P002', active: true } },
  'am.nguyena': { password: 'pass123',  token: 'mock-token-am01',  user: { id: 4, username: 'am.nguyena', fullName: 'Nguyễn Văn A',       email: 'nguyen.a@bidv.com.vn',  role: 'ROLE_EMPLOYEE',       maDonViCap6: 'P001', active: true } },
};

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly TOKEN_KEY = 'mpa_access_token';
  private readonly REFRESH_KEY = 'mpa_refresh_token';
  private readonly USER_KEY = 'mpa_user';

  currentUser = signal<User | null>(this.loadUser());

  constructor(private http: HttpClient, private router: Router) {}

  login(req: LoginRequest): Observable<ApiResponse<AuthResponse>> {
    // Thử mock login trước khi gọi API thực
    const mock = MOCK_USERS[req.username];
    if (mock && mock.password === req.password) {
      const res: ApiResponse<AuthResponse> = {
        success: true, message: 'Đăng nhập thành công', timestamp: new Date().toISOString(),
        data: { accessToken: mock.token, refreshToken: mock.token + '-refresh', tokenType: 'Bearer', user: mock.user }
      };
      this.saveSession(res.data!);
      return of(res);
    }

    return this.http.post<ApiResponse<AuthResponse>>(`${environment.apiUrl}/auth/login`, req).pipe(
      tap(res => {
        if (res.success && res.data) {
          this.saveSession(res.data);
        }
      }),
      catchError(err => {
        // Nếu backend chưa chạy, thử mock một lần nữa với thông báo rõ ràng
        if (err.status === 0) {
          return throwError(() => ({ error: { message: 'Sai tên đăng nhập hoặc mật khẩu. Tài khoản demo: admin / admin123' } }));
        }
        return throwError(() => err);
      })
    );
  }

  logout(): void {
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
