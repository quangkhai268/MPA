import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse, PageResponse } from '../models/user.model';
import { Campaign, CampaignRequest, CampaignPreview, JobRunResult, EmailLogItem } from '../models/mpa.model';

@Injectable({ providedIn: 'root' })
export class CampaignService {
  private api = environment.apiUrl;

  constructor(private http: HttpClient) {}

  getAll(): Observable<ApiResponse<Campaign[]>> {
    return this.http.get<ApiResponse<Campaign[]>>(`${this.api}/campaigns`);
  }

  getById(id: number): Observable<ApiResponse<Campaign>> {
    return this.http.get<ApiResponse<Campaign>>(`${this.api}/campaigns/${id}`);
  }

  create(req: CampaignRequest): Observable<ApiResponse<Campaign>> {
    return this.http.post<ApiResponse<Campaign>>(`${this.api}/campaigns`, req);
  }

  update(id: number, req: CampaignRequest): Observable<ApiResponse<Campaign>> {
    return this.http.put<ApiResponse<Campaign>>(`${this.api}/campaigns/${id}`, req);
  }

  delete(id: number): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(`${this.api}/campaigns/${id}`);
  }

  preview(id: number): Observable<ApiResponse<CampaignPreview>> {
    return this.http.get<ApiResponse<CampaignPreview>>(`${this.api}/campaigns/${id}/preview`);
  }

  previewByCriteria(req: CampaignRequest): Observable<ApiResponse<CampaignPreview>> {
    return this.http.post<ApiResponse<CampaignPreview>>(`${this.api}/campaigns/preview-by-criteria`, req);
  }

  send(id: number): Observable<ApiResponse<JobRunResult>> {
    return this.http.post<ApiResponse<JobRunResult>>(`${this.api}/campaigns/${id}/send`, {});
  }

  getLogs(id: number, page: number, size: number): Observable<ApiResponse<PageResponse<EmailLogItem>>> {
    return this.http.get<ApiResponse<PageResponse<EmailLogItem>>>(`${this.api}/campaigns/${id}/logs`, { params: { page: String(page), size: String(size) } });
  }

  getCriteriaOptions(): Observable<ApiResponse<Record<string, string[]>>> {
    return this.http.get<ApiResponse<Record<string, string[]>>>(`${this.api}/campaigns/criteria-options`);
  }
}
