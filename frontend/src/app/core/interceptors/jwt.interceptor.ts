import { HttpInterceptorFn, HttpRequest, HttpHandlerFn, HttpEvent, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { Observable, catchError, throwError } from 'rxjs';
import { Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

export const jwtInterceptor: HttpInterceptorFn = (
  req: HttpRequest<unknown>,
  next: HttpHandlerFn
): Observable<HttpEvent<unknown>> => {
  const auth = inject(AuthService);
  const router = inject(Router);
  const token = auth.getToken();

  if (token) {
    req = req.clone({
      setHeaders: { Authorization: `Bearer ${token}` }
    });
  }

  return next(req).pipe(
    catchError((err: HttpErrorResponse) => {
      if (err.status === 401) {
        // Phiên hết hạn/token không hợp lệ — xoá session cục bộ và về trang login.
        auth.logout();
      } else if (err.status === 403 && err.error?.errorCode === 'MUST_CHANGE_PASSWORD') {
        // Cờ mustChangePassword đổi thành true GIỮA phiên (VD admin vừa reset mật khẩu) mà
        // guard lúc vào trang chưa bắt được — bắt ở đây làm lớp dự phòng.
        router.navigate(['/doi-mat-khau']);
      }
      // Các lỗi 403 khác (không đúng quyền nhưng không liên quan đổi mật khẩu) để component
      // tự xử lý — không auto-logout vì token vẫn hợp lệ, chỉ là không đủ quyền thao tác đó.
      return throwError(() => err);
    })
  );
};
