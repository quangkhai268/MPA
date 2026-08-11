// Core models
export interface User {
  id: number;
  username: string;
  fullName: string;
  email: string;
  role: 'ROLE_ADMIN' | 'ROLE_BRANCH_MANAGER' | 'ROLE_EMPLOYEE';
  maDonViCap6?: string;
  tenDonViCap6?: string;
  maAm?: string;
  active: boolean;
  mustChangePassword?: boolean;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  user: User;
}

export interface LoginRequest {
  username: string;
  password: string;
}

export interface RefreshTokenRequest {
  refreshToken: string;
}

export interface ChangePasswordRequest {
  currentPassword: string;
  newPassword: string;
}

/** Request tạo/sửa tài khoản người dùng — chỉ ADMIN gọi được (menu Quản trị hệ thống). */
export interface UserRequest {
  username: string;
  fullName: string;
  email: string;
  role: 'ROLE_ADMIN' | 'ROLE_BRANCH_MANAGER' | 'ROLE_EMPLOYEE';
  maDonViCap6?: string;
  /** Rỗng khi sửa = không đổi mật khẩu. Bắt buộc khi tạo mới. */
  password?: string;
  active: boolean;
}

export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
  timestamp: string;
  /** Mã lỗi máy đọc được, VD "MUST_CHANGE_PASSWORD" — null với hầu hết response. */
  errorCode?: string | null;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}
