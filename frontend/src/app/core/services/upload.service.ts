import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse, PageResponse } from '../models/user.model';
import { UploadBatchResult, UploadHistoryItem } from '../models/upload.model';

@Injectable({ providedIn: 'root' })
export class UploadService {
  private api = environment.apiUrl;

  constructor(private http: HttpClient) {}

  upload(files: File[], ngayDuLieu: string, nguoiUpload: string | null): Observable<ApiResponse<UploadBatchResult>> {
    const formData = new FormData();
    files.forEach(f => formData.append('files', f, f.name));
    formData.append('ngayDuLieu', ngayDuLieu);
    if (nguoiUpload) formData.append('nguoiUpload', nguoiUpload);
    return this.http.post<ApiResponse<UploadBatchResult>>(`${this.api}/upload`, formData);
  }

  getHistory(page: number, size: number): Observable<ApiResponse<PageResponse<UploadHistoryItem>>> {
    return this.http.get<ApiResponse<PageResponse<UploadHistoryItem>>>(`${this.api}/upload/history`, {
      params: { page: String(page), size: String(size) }
    });
  }
}
