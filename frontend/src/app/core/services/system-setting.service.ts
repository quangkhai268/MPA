import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse } from '../models/user.model';
import { SystemSetting, EmailTemplate } from '../models/mpa.model';

@Injectable({ providedIn: 'root' })
export class SystemSettingService {
  private api = environment.apiUrl;

  constructor(private http: HttpClient) {}

  getAll(): Observable<ApiResponse<SystemSetting[]>> {
    return this.http.get<ApiResponse<SystemSetting[]>>(`${this.api}/system-settings`);
  }

  updateBatch(items: { settingKey: string; settingValue: string }[]): Observable<ApiResponse<SystemSetting[]>> {
    return this.http.put<ApiResponse<SystemSetting[]>>(`${this.api}/system-settings`, items);
  }

  getEmailTemplates(): Observable<ApiResponse<EmailTemplate[]>> {
    return this.http.get<ApiResponse<EmailTemplate[]>>(`${this.api}/email-templates`);
  }

  updateEmailTemplate(id: number, tieuDe: string, noiDung: string, active: boolean): Observable<ApiResponse<EmailTemplate>> {
    return this.http.put<ApiResponse<EmailTemplate>>(`${this.api}/email-templates/${id}`, { tieuDe, noiDung, active });
  }
}
