import { Component, OnInit, signal, inject } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';

import { AuthService } from '../../../../core/services/auth.service';
import { CampaignService } from '../../../../core/services/campaign.service';
import { SystemSettingService } from '../../../../core/services/system-setting.service';
import { Campaign, CampaignCriteria, CampaignPreview, EmailLogItem, JobRunResult } from '../../../../core/models/mpa.model';

interface CriteriaFieldDef {
  key: string;
  label: string;
}

@Component({
  selector: 'app-chien-dich-form',
  standalone: true,
  imports: [CommonModule, FormsModule, DatePipe, RouterModule, MatIconModule, MatProgressSpinnerModule, MatSnackBarModule],
  templateUrl: './chien-dich-form.component.html',
  styleUrl: './chien-dich-form.component.scss'
})
export class ChienDichFormComponent implements OnInit {
  auth = inject(AuthService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private campaignService = inject(CampaignService);
  private settingService = inject(SystemSettingService);
  private snack = inject(MatSnackBar);

  campaignId: number | null = null;
  loading = signal(false);
  saving = signal(false);

  readonly fieldDefs: CriteriaFieldDef[] = [
    { key: 'loaiThe', label: 'Loại thẻ' },
    { key: 'productCode', label: 'Sản phẩm' },
    { key: 'hinhThucThe', label: 'Hình thức' },
    { key: 'nhomKhThe', label: 'Nhóm khách hàng' },
    { key: 'trangThaiThe', label: 'Trạng thái thẻ' },
  ];
  criteriaOptions = signal<Record<string, string[]>>({});
  selected: Record<string, Set<string>> = {};

  // ── Form fields ────────────────────────────────────────────────────────
  tenChienDich = '';
  moTa = '';
  ngayBatDau: string | null = null;
  ngayKetThuc: string | null = null;
  trangThai = 'DRAFT';
  tieuDeEmail = '';
  noiDungEmail = '';

  preview = signal<CampaignPreview | null>(null);
  previewLoading = signal(false);

  emailEnabled = false;
  sending = signal(false);
  lastSendResult = signal<JobRunResult | null>(null);

  logs = signal<EmailLogItem[]>([]);
  logsLoading = signal(false);

  ngOnInit(): void {
    if (!this.auth.isAdmin()) return;
    this.fieldDefs.forEach(f => this.selected[f.key] = new Set<string>());

    this.campaignService.getCriteriaOptions().subscribe(res => {
      if (res.success) this.criteriaOptions.set(res.data);
    });

    this.settingService.getAll().subscribe(res => {
      if (res.success) {
        const enabled = res.data.find(s => s.settingKey === 'CARD_EMAIL_ENABLED');
        this.emailEnabled = enabled?.settingValue === 'true';
      }
    });

    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      this.campaignId = Number(idParam);
      this.loadCampaign(this.campaignId);
      this.loadLogs();
    } else {
      this.runPreview();
    }
  }

  private loadCampaign(id: number): void {
    this.loading.set(true);
    this.campaignService.getById(id).subscribe({
      next: res => {
        if (res.success) {
          const c = res.data;
          this.tenChienDich = c.tenChienDich;
          this.moTa = c.moTa ?? '';
          this.ngayBatDau = c.ngayBatDau;
          this.ngayKetThuc = c.ngayKetThuc;
          this.trangThai = c.trangThai;
          this.tieuDeEmail = c.tieuDeEmail ?? '';
          this.noiDungEmail = c.noiDungEmail ?? '';
          c.criteria.forEach(cr => this.selected[cr.tieuChiField]?.add(cr.tieuChiValue));
          this.runPreview();
        }
        this.loading.set(false);
      },
      error: () => this.loading.set(false)
    });
  }

  goBack(): void {
    this.router.navigate(['/quan-ly-the/chien-dich']);
  }

  toggleValue(field: string, value: string): void {
    const set = this.selected[field];
    if (set.has(value)) set.delete(value); else set.add(value);
    this.runPreview();
  }

  isSelected(field: string, value: string): boolean {
    return this.selected[field]?.has(value) ?? false;
  }

  private buildCriteria(): CampaignCriteria[] {
    const list: CampaignCriteria[] = [];
    for (const field of Object.keys(this.selected)) {
      for (const value of this.selected[field]) {
        list.push({ tieuChiField: field, tieuChiValue: value });
      }
    }
    return list;
  }

  private buildRequest() {
    return {
      tenChienDich: this.tenChienDich,
      moTa: this.moTa || null,
      ngayBatDau: this.ngayBatDau,
      ngayKetThuc: this.ngayKetThuc,
      trangThai: this.trangThai,
      tieuDeEmail: this.tieuDeEmail || null,
      noiDungEmail: this.noiDungEmail || null,
      criteria: this.buildCriteria()
    };
  }

  runPreview(): void {
    this.previewLoading.set(true);
    this.campaignService.previewByCriteria(this.buildRequest()).subscribe({
      next: res => {
        if (res.success) this.preview.set(res.data);
        this.previewLoading.set(false);
      },
      error: () => this.previewLoading.set(false)
    });
  }

  save(): void {
    if (!this.tenChienDich.trim()) {
      this.snack.open('Vui lòng nhập tên chiến dịch', 'Đóng', { duration: 3000 });
      return;
    }
    this.saving.set(true);
    const req = this.buildRequest();
    const obs = this.campaignId
      ? this.campaignService.update(this.campaignId, req)
      : this.campaignService.create(req);
    obs.subscribe({
      next: res => {
        this.saving.set(false);
        if (res.success) {
          this.snack.open('Đã lưu chiến dịch', 'Đóng', { duration: 3000 });
          if (!this.campaignId) this.router.navigate(['/quan-ly-the/chien-dich', res.data.id]);
        } else {
          this.snack.open('Lỗi khi lưu: ' + res.message, 'Đóng', { duration: 4000 });
        }
      },
      error: () => {
        this.saving.set(false);
        this.snack.open('Lỗi khi lưu chiến dịch', 'Đóng', { duration: 4000 });
      }
    });
  }

  sendCampaign(): void {
    if (!this.campaignId) return;
    const testMode = !this.emailEnabled;
    if (!confirm(testMode
      ? 'Email đang TẮT — đây chỉ là chạy thử (không gửi thật). Tiếp tục?'
      : `Gửi email thật tới ${this.preview()?.soLuong ?? 0} khách hàng thỏa tiêu chí?`)) return;

    this.sending.set(true);
    this.campaignService.send(this.campaignId, testMode).subscribe({
      next: res => {
        this.sending.set(false);
        if (res.success) {
          this.lastSendResult.set(res.data);
          this.snack.open(`Đã gửi ${res.data.sent}/${res.data.eligible}`, 'Đóng', { duration: 4000 });
          this.loadLogs();
        } else {
          this.snack.open('Lỗi khi gửi: ' + res.message, 'Đóng', { duration: 4000 });
        }
      },
      error: () => {
        this.sending.set(false);
        this.snack.open('Lỗi khi gửi chiến dịch', 'Đóng', { duration: 4000 });
      }
    });
  }

  loadLogs(): void {
    if (!this.campaignId) return;
    this.logsLoading.set(true);
    this.campaignService.getLogs(this.campaignId, 0, 20).subscribe({
      next: res => {
        if (res.success) this.logs.set(res.data.content);
        this.logsLoading.set(false);
      },
      error: () => this.logsLoading.set(false)
    });
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
