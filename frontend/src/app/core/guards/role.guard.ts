import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

/**
 * Chặn truy cập route theo role — bổ sung lớp bảo vệ THẬT ở phía route, không chỉ ẩn/hiện
 * menu sidebar (trước đây gõ thẳng URL vẫn vào được dù không đúng role). Backend cũng chặn
 * độc lập qua @PreAuthorize/SecurityConfig — đây chỉ là lớp UX, không phải điểm bảo mật duy nhất.
 */
export function roleGuard(...roles: string[]): CanActivateFn {
  return () => {
    const auth = inject(AuthService);
    const router = inject(Router);

    if (auth.hasRole(...roles)) return true;

    router.navigate(['/dashboard']);
    return false;
  };
}
