import { Component, OnInit, signal, inject } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

import { AuthService } from '../../core/services/auth.service';
import { UploadService } from '../../core/services/upload.service';
import { UploadHistoryItem } from '../../core/models/upload.model';

@Component({
  selector: 'app-tai-du-lieu-len',
  standalone: true,
  imports: [CommonModule, FormsModule, DatePipe, MatIconModule, MatProgressSpinnerModule],
  templateUrl: './tai-du-lieu-len.component.html',
  styleUrl: './tai-du-lieu-len.component.scss'
})
export class TaiDuLieuLenComponent implements OnInit {
  auth = inject(AuthService);
  private uploadService = inject(UploadService);

  selectedFiles = signal<File[]>([]);
  ngayDuLieu = this.defaultNgayDuLieu();
  dragOver = signal(false);
  uploading = signal(false);

  history = signal<UploadHistoryItem[]>([]);
  historyLoading = signal(false);
  historyPage = 0;
  historyTotalPages = 0;
  readonly historyPageSize = 10;

  toast = signal<{ text: string; success: boolean } | null>(null);
  private toastTimer: ReturnType<typeof setTimeout> | null = null;

  readonly acceptExt = ['.xlsx', '.xls', '.zip'];
  readonly maxFileSizeBytes = 50 * 1024 * 1024;

  ngOnInit(): void {
    if (!this.auth.isAdmin()) return;
    this.loadHistory(0);
  }

  private defaultNgayDuLieu(): string {
    const d = new Date();
    d.setDate(d.getDate() - 1);
    return d.toISOString().slice(0, 10);
  }

  // ── Chọn / kéo thả file ──────────────────────────────────────────────
  onDragOver(event: DragEvent): void {
    event.preventDefault();
    this.dragOver.set(true);
  }

  onDragLeave(event: DragEvent): void {
    event.preventDefault();
    this.dragOver.set(false);
  }

  onDrop(event: DragEvent): void {
    event.preventDefault();
    this.dragOver.set(false);
    if (event.dataTransfer?.files?.length) this.addFiles(Array.from(event.dataTransfer.files));
  }

  onFileInputChange(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files?.length) this.addFiles(Array.from(input.files));
    input.value = '';
  }

  private addFiles(files: File[]): void {
    const valid: File[] = [];
    for (const f of files) {
      const ext = f.name.slice(f.name.lastIndexOf('.')).toLowerCase();
      if (!this.acceptExt.includes(ext)) {
        this.notify(`✗ "${f.name}" không đúng định dạng (.xlsx, .xls, .zip)`, false);
        continue;
      }
      if (f.size > this.maxFileSizeBytes) {
        this.notify(`✗ "${f.name}" vượt quá 50 MB`, false);
        continue;
      }
      valid.push(f);
    }
    if (valid.length) this.selectedFiles.update(list => [...list, ...valid]);
  }

  removeFile(index: number): void {
    this.selectedFiles.update(list => list.filter((_, i) => i !== index));
  }

  formatSize(bytes: number): string {
    if (bytes >= 1024 * 1024) return (bytes / (1024 * 1024)).toFixed(2) + ' MB';
    if (bytes >= 1024) return (bytes / 1024).toFixed(1) + ' KB';
    return bytes + ' B';
  }

  // ── Tải lên ──────────────────────────────────────────────────────────
  taiLen(): void {
    const files = this.selectedFiles();
    if (!files.length || this.uploading()) return;

    this.uploading.set(true);
    const nguoiUpload = this.auth.currentUser()?.fullName ?? null;
    this.uploadService.upload(files, this.ngayDuLieu, nguoiUpload).subscribe({
      next: res => {
        this.uploading.set(false);
        if (res.success) {
          const r = res.data;
          if (r.trangThai === 'SUCCESS') {
            this.notify(`✓ Tải lên thành công — ${r.soFile} file, ${r.tongDong.toLocaleString('vi-VN')} dòng`, true);
          } else if (r.trangThai === 'PARTIAL') {
            this.notify(`⚠ Tải lên một phần thành công — ${r.tongDong.toLocaleString('vi-VN')} dòng. Xem chi tiết từng file.`, false);
          } else if (r.trangThai === 'UNSUPPORTED') {
            this.notify('✗ Không có file nào được hỗ trợ trong lần tải này', false);
          } else {
            this.notify('✗ Tải lên thất bại', false);
          }
          this.selectedFiles.set([]);
          this.loadHistory(0);
        } else {
          this.notify('✗ Tải lên thất bại: ' + res.message, false);
        }
      },
      error: (err) => {
        this.uploading.set(false);
        this.notify('✗ Tải lên thất bại: ' + (err?.error?.message || err?.message || 'lỗi không xác định'), false);
      }
    });
  }

  // ── Lịch sử ──────────────────────────────────────────────────────────
  loadHistory(page: number): void {
    this.historyPage = page;
    this.historyLoading.set(true);
    this.uploadService.getHistory(page, this.historyPageSize).subscribe({
      next: res => {
        if (res.success) {
          this.history.set(res.data.content);
          this.historyTotalPages = res.data.totalPages;
        }
        this.historyLoading.set(false);
      },
      error: () => this.historyLoading.set(false)
    });
  }

  trangThaiLabel(tt: string): string {
    switch (tt) {
      case 'SUCCESS': return 'Thành công';
      case 'PARTIAL': return 'Một phần';
      case 'FAILED': return 'Thất bại';
      case 'UNSUPPORTED': return 'Chưa hỗ trợ';
      default: return tt;
    }
  }

  trangThaiClass(tt: string): string {
    switch (tt) {
      case 'SUCCESS': return 'badge-green';
      case 'PARTIAL': return 'badge-yellow';
      case 'FAILED': return 'badge-red';
      default: return 'badge-gray';
    }
  }

  // ── Toast ────────────────────────────────────────────────────────────
  private notify(message: string, success: boolean): void {
    if (this.toastTimer) clearTimeout(this.toastTimer);
    this.toast.set({ text: message, success });
    this.toastTimer = setTimeout(() => this.toast.set(null), success ? 3500 : 6000);
  }

  closeToast(): void {
    if (this.toastTimer) clearTimeout(this.toastTimer);
    this.toast.set(null);
  }
}
