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

export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
  timestamp: string;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}
