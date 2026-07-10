import { Component, OnInit, signal, inject } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';

import { AuthService } from '../../../core/services/auth.service';
import { SystemSettingService } from '../../../core/services/system-setting.service';
import { MpaService } from '../../../core/services/mpa.service';
import { SystemSetting, EmailTemplate, EmailLogItem, JobRunResult, CardRevenueMilestone } from '../../../core/models/mpa.model';

@Component({
  selector: 'app-cai-dat-canh-bao',
  standalone: true,
  imports: [
    CommonModule, FormsModule, DatePipe, RouterModule,
    MatIconModule, MatProgressSpinnerModule, MatSlideToggleModule
  ],
  templateUrl: './cai-dat-canh-bao.component.html',
  styleUrl: './cai-dat-canh-bao.component.scss'
})
export class CaiDatCanhBaoComponent implements OnInit {
  auth = inject(AuthService);
  private settingService = inject(SystemSettingService);
  private mpaService = inject(MpaService);

  loading = signal(false);
  saving = signal(false);

  // ── Cấu hình dạng key → value để bind 2 chiều lên form ─────────────────
  settings = signal<Record<string, string>>({});

  templates = signal<EmailTemplate[]>([]);
  savingTemplateId = signal<number | null>(null);

  logs = signal<EmailLogItem[]>([]);
  logsLoading = signal(false);
  logPage = 0;
  logTotalPages = 0;
  readonly logPageSize = 10;

  runningJob = signal<string | null>(null);
  lastJobResult = signal<{ job: string; result: JobRunResult } | null>(null);

  milestones = signal<CardRevenueMilestone[]>([]);
  newMilestone = { soNgayTuPhatHanh: 30, nguongDoanhSo: 0, moTa: '' };
  savingMilestone = signal(false);
  snapshotRunning = signal(false);

  // ── Toast thông báo thành công/lỗi (tự vẽ, không phụ thuộc CDK overlay) ──
  toast = signal<{ text: string; success: boolean } | null>(null);
  private toastTimer: ReturnType<typeof setTimeout> | null = null;

  // ── Xem trước email ─────────────────────────────────────────────────────
  previewingTemplate = signal<{ tieuDe: string; noiDung: string } | null>(null);
  private readonly SAMPLE_PLACEHOLDERS: Record<string, string> = {
    tenKhachHang: 'NGUYỄN VĂN A',
    soThe: '970418xxxxxx1234',
    soNgay: '10',
    doanhSoHienTai: '5.000.000',
    nguongDoanhSo: '10.000.000'
  };

  ngOnInit(): void {
    if (!this.auth.isAdmin()) return;
    this.load();
    this.loadLogs(0);
    this.loadMilestones();
  }

  loadMilestones(): void {
    this.mpaService.getRevenueMilestones().subscribe({
      next: res => { if (res.success) this.milestones.set(res.data); }
    });
  }

  // ── Nhập số tiền có dấu phân cách nghìn (vd 1.000.000.000) ──────────────
  formatMoney(value: number | null | undefined): string {
    return (value ?? 0).toLocaleString('vi-VN');
  }

  private parseMoney(text: string): number {
    const digits = (text || '').replace(/[^\d]/g, '');
    return digits ? parseInt(digits, 10) : 0;
  }

  onMoneyFocus(event: FocusEvent, currentValue: number): void {
    (event.target as HTMLInputElement).value = String(currentValue ?? 0);
  }

  onMilestoneMoneyBlur(m: CardRevenueMilestone, event: FocusEvent): void {
    const input = event.target as HTMLInputElement;
    const parsed = this.parseMoney(input.value);
    m.nguongDoanhSo = parsed;
    input.value = this.formatMoney(parsed);
  }

  onNewMilestoneMoneyBlur(event: FocusEvent): void {
    const input = event.target as HTMLInputElement;
    const parsed = this.parseMoney(input.value);
    this.newMilestone.nguongDoanhSo = parsed;
    input.value = this.formatMoney(parsed);
  }

  addMilestone(): void {
    if (!this.newMilestone.soNgayTuPhatHanh || this.newMilestone.nguongDoanhSo == null) return;

    const daTonTai = this.milestones().some(m => m.soNgayTuPhatHanh === this.newMilestone.soNgayTuPhatHanh);
    if (daTonTai) {
      this.notify(`✗ Đã tồn tại mốc ${this.newMilestone.soNgayTuPhatHanh} ngày — vui lòng sửa mốc đó thay vì thêm mới`, false);
      return;
    }

    this.savingMilestone.set(true);
    this.mpaService.createRevenueMilestone({ ...this.newMilestone, active: true }).subscribe({
      next: res => {
        this.savingMilestone.set(false);
        if (res.success) {
          this.notify('Đã thêm mốc doanh số thành công', true);
          this.newMilestone = { soNgayTuPhatHanh: 30, nguongDoanhSo: 0, moTa: '' };
          this.loadMilestones();
        } else {
          this.notify('Thêm mốc doanh số thất bại: ' + res.message, false);
        }
      },
      error: () => { this.savingMilestone.set(false); this.notify('Thêm mốc doanh số thất bại', false); }
    });
  }

  saveMilestone(m: CardRevenueMilestone): void {
    this.mpaService.updateRevenueMilestone(m.id, {
      soNgayTuPhatHanh: m.soNgayTuPhatHanh, nguongDoanhSo: m.nguongDoanhSo, moTa: m.moTa ?? '', active: m.active
    }).subscribe({
      next: res => {
        if (res.success) this.notify('Đã lưu mốc doanh số thành công', true);
        else this.notify('Lưu mốc doanh số thất bại: ' + res.message, false);
      },
      error: () => this.notify('Lưu mốc doanh số thất bại', false)
    });
  }

  deleteMilestone(id: number): void {
    if (!confirm('Xóa mốc doanh số này?')) return;
    this.mpaService.deleteRevenueMilestone(id).subscribe({
      next: res => {
        if (res.success) { this.notify('Đã xóa mốc doanh số thành công', true); this.loadMilestones(); }
        else this.notify('Xóa mốc doanh số thất bại: ' + res.message, false);
      },
      error: () => this.notify('Xóa mốc doanh số thất bại', false)
    });
  }

  runSnapshotJob(): void {
    this.snapshotRunning.set(true);
    this.mpaService.runSnapshotJob().subscribe({
      next: res => {
        this.snapshotRunning.set(false);
        if (res.success) this.notify(`Đã snapshot ${res.data} thẻ thành công`, true);
        else this.notify('Chạy snapshot thất bại: ' + res.message, false);
      },
      error: () => { this.snapshotRunning.set(false); this.notify('Chạy snapshot thất bại', false); }
    });
  }

  private load(): void {
    this.loading.set(true);
    this.settingService.getAll().subscribe({
      next: res => {
        if (res.success) {
          const map: Record<string, string> = {};
          res.data.forEach(s => map[s.settingKey] = s.settingValue ?? '');
          this.settings.set(map);
        }
      }
    });
    this.settingService.getEmailTemplates().subscribe({
      next: res => {
        if (res.success) this.templates.set(res.data);
        this.loading.set(false);
      },
      error: () => this.loading.set(false)
    });
  }

  get(key: string): string {
    return this.settings()[key] ?? '';
  }

  set(key: string, value: string): void {
    this.settings.update(s => ({ ...s, [key]: value }));
  }

  get emailEnabled(): boolean {
    return this.get('CARD_EMAIL_ENABLED') === 'true';
  }
  set emailEnabled(v: boolean) {
    this.set('CARD_EMAIL_ENABLED', String(v));
  }

  saveSettings(): void {
    this.saving.set(true);
    const items = Object.entries(this.settings()).map(([settingKey, settingValue]) => ({ settingKey, settingValue }));
    this.settingService.updateBatch(items).subscribe({
      next: res => {
        this.saving.set(false);
        if (res.success) {
          this.notify('✓ Đã lưu cấu hình thành công', true);
        } else {
          this.notify('✗ Lưu cấu hình thất bại: ' + res.message, false);
        }
      },
      error: (err) => {
        this.saving.set(false);
        this.notify('✗ Lưu cấu hình thất bại: ' + (err?.message || 'lỗi không xác định'), false);
      }
    });
  }

  saveTemplate(t: EmailTemplate): void {
    this.savingTemplateId.set(t.id);
    this.settingService.updateEmailTemplate(t.id, t.tieuDe, t.noiDung, t.active).subscribe({
      next: res => {
        this.savingTemplateId.set(null);
        if (res.success) this.notify('✓ Đã lưu mẫu email thành công', true);
        else this.notify('✗ Lưu mẫu email thất bại: ' + res.message, false);
      },
      error: (err) => {
        this.savingTemplateId.set(null);
        this.notify('✗ Lưu mẫu email thất bại: ' + (err?.message || 'lỗi không xác định'), false);
      }
    });
  }

  loadLogs(page: number): void {
    this.logPage = page;
    this.logsLoading.set(true);
    this.mpaService.getEmailLogs(null, null, null, page, this.logPageSize).subscribe({
      next: res => {
        if (res.success) {
          this.logs.set(res.data.content);
          this.logTotalPages = res.data.totalPages;
        }
        this.logsLoading.set(false);
      },
      error: () => this.logsLoading.set(false)
    });
  }

  runJob(job: 'chua-kich-hoat' | 'chua-psgd' | 'moc-doanh-so'): void {
    this.runningJob.set(job);
    const testMode = !this.emailEnabled;
    const obs = job === 'chua-kich-hoat'
      ? this.mpaService.runChuaKichHoatJob(testMode)
      : job === 'chua-psgd'
        ? this.mpaService.runChuaPsgdJob(testMode)
        : this.mpaService.runMilestoneJob(testMode);
    obs.subscribe({
      next: res => {
        this.runningJob.set(null);
        if (res.success) {
          this.lastJobResult.set({ job, result: res.data });
          this.notify(`✓ Đã chạy job thành công — gửi ${res.data.sent}/${res.data.eligible} email`, true);
          this.loadLogs(0);
        } else {
          this.notify('✗ Chạy job thất bại: ' + res.message, false);
        }
      },
      error: () => {
        this.runningJob.set(null);
        this.notify('✗ Chạy job thất bại', false);
      }
    });
  }

  // ── Thông báo thành công/lỗi rõ ràng cho mọi thao tác lưu/chạy ──────────
  private notify(message: string, success: boolean): void {
    if (this.toastTimer) clearTimeout(this.toastTimer);
    this.toast.set({ text: message, success });
    this.toastTimer = setTimeout(() => this.toast.set(null), success ? 3000 : 6000);
  }

  closeToast(): void {
    if (this.toastTimer) clearTimeout(this.toastTimer);
    this.toast.set(null);
  }

  // ── Xem trước email ─────────────────────────────────────────────────────
  previewTemplate(t: EmailTemplate): void {
    let tieuDe = t.tieuDe || '';
    let noiDung = t.noiDung || '';
    for (const [key, value] of Object.entries(this.SAMPLE_PLACEHOLDERS)) {
      const pattern = new RegExp(`\\{\\{${key}\\}\\}`, 'g');
      tieuDe = tieuDe.replace(pattern, value);
      noiDung = noiDung.replace(pattern, value);
    }
    this.previewingTemplate.set({ tieuDe, noiDung });
  }

  closePreview(): void {
    this.previewingTemplate.set(null);
  }

  trangThaiClass(tt: string): string {
    switch (tt) {
      case 'SUCCESS': return 'badge-green';
      case 'FAILED': return 'badge-red';
      case 'SKIPPED_DEDUP': return 'badge-yellow';
      default: return 'badge-gray';
    }
  }
}
