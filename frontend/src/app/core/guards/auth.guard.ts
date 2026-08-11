import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

export const authGuard: CanActivateFn = (route, state) => {
  const auth = inject(AuthService);
  const router = inject(Router);

  if (!auth.isLoggedIn()) {
    router.navigate(['/login']);
    return false;
  }

  // Tài khoản còn bị bắt buộc đổi mật khẩu (mới tạo/vừa bị admin reset) — chặn vào mọi
  // trang khác ngoại trừ chính trang đổi mật khẩu. Trường hợp cờ này đổi thành true GIỮA
  // phiên (không reload trang) được jwt.interceptor.ts xử lý riêng qua errorCode
  // MUST_CHANGE_PASSWORD trả về từ backend — guard này chỉ bắt được lúc vào lại/tải lại trang.
  if (auth.mustChangePassword() && !state.url.startsWith('/doi-mat-khau')) {
    router.navigate(['/doi-mat-khau']);
    return false;
  }

  return true;
};

export const loginGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);

  if (!auth.isLoggedIn()) return true;

  router.navigate(['/dashboard']);
  return false;
};
