import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse, PageResponse, User, UserRequest } from '../models/user.model';

/** Quản lý tài khoản người dùng — chỉ ADMIN gọi được (backend chặn qua @PreAuthorize +
 *  SecurityConfig, đây chỉ là lớp gọi API phía frontend). */
@Injectable({ providedIn: 'root' })
export class UserService {
  private api = environment.apiUrl;

  constructor(private http: HttpClient) {}

  getUsers(search = '', role?: string, active?: boolean, maDonViCap6?: string): Observable<ApiResponse<PageResponse<User>>> {
    let params = new HttpParams().set('search', search).set('page', 0).set('size', 1000);
    if (role) params = params.set('role', role);
    if (active !== undefined) params = params.set('active', active);
    if (maDonViCap6) params = params.set('maDonViCap6', maDonViCap6);
    return this.http.get<ApiResponse<PageResponse<User>>>(`${this.api}/users`, { params });
  }

  createUser(req: UserRequest): Observable<ApiResponse<User>> {
    return this.http.post<ApiResponse<User>>(`${this.api}/users`, req);
  }

  updateUser(id: number, req: UserRequest): Observable<ApiResponse<User>> {
    return this.http.put<ApiResponse<User>>(`${this.api}/users/${id}`, req);
  }

  setUserActive(id: number, active: boolean): Observable<ApiResponse<User>> {
    return this.http.patch<ApiResponse<User>>(`${this.api}/users/${id}/active`, null, { params: { active } });
  }
}
