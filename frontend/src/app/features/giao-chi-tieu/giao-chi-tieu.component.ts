import { Component, OnInit, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';

import { MpaService } from '../../core/services/mpa.service';
import {
  BscSoSanhResult, BscSoSanhRow,
  ChiTieuBscRequest, ChiTieuQuanLyRow, UnitOption
} from '../../core/models/mpa.model';

type LoaiKy = 'thang' | 'quy' | 'nam';
type DoiTuong = 'phong' | 'chi-nhanh' | 'am';
type ActiveTab = 'so-sanh' | 'xu-huong' | 'quan-ly';

@Component({
  selector: 'app-giao-chi-tieu',
  standalone: true,
  imports: [
    CommonModule, FormsModule,
    MatIconModule, MatButtonModule, MatCheckboxModule,
    MatProgressSpinnerModule, MatTooltipModule
  ],
  templateUrl: './giao-chi-tieu.component.html',
  styleUrl: './giao-chi-tieu.component.scss'
})
export class GiaoChiTieuComponent implements OnInit {
  private mpaService = inject(MpaService);

  // ── Filter state ──────────────────────────────────────────────────
  loaiKy: LoaiKy    = 'thang';
  selectedNam        = new Date().getFullYear();
  selectedThang      = new Date().getMonth() + 1;
  selectedQuy        = 'Q' + Math.ceil((new Date().getMonth() + 1) / 3);
  doiTuong: DoiTuong = 'phong';

  // ── UI state ──────────────────────────────────────────────────────
  showDelta   = false;
  activeTab: ActiveTab = 'so-sanh';
  loading     = signal(false);
  data        = signal<BscSoSanhResult | null>(null);

  // ── Quản lý tab state ─────────────────────────────────────────────
  quanLyList    = signal<ChiTieuQuanLyRow[]>([]);
  loadingQuanLy = signal(false);

  // ── Dialog state ──────────────────────────────────────────────────
  showDialog   = signal(false);
  savingDialog = signal(false);
  dlgDoiTuong: DoiTuong = 'phong';
  dlgMaUnit    = '';
  dlgTenUnit   = '';
  dlgChiTieu   = 'tong-tnt';
  dlgMucTieu: number | null = null;
  unitOptions  = signal<UnitOption[]>([]);

  // ── Static options ────────────────────────────────────────────────
  readonly namOptions   = Array.from({ length: 6 }, (_, i) => new Date().getFullYear() - i);
  readonly thangOptions = Array.from({ length: 12 }, (_, i) => i + 1);
  readonly quyOptions   = ['Q1', 'Q2', 'Q3', 'Q4'];

  readonly chiTieuOptions = [
    { value: 'tong-tnt',   label: 'Tổng TNT' },
    { value: 'hdv-cuoi-ky', label: 'HĐV cuối kỳ' },
    { value: 'casa-bq',    label: 'CASA bình quân' },
    { value: 'du-no',      label: 'Dư nợ tín dụng' },
    { value: 'tnt-dv',     label: 'TNT từ dịch vụ' },
    { value: 'tnt-hdv',    label: 'TNT từ HĐV' },
    { value: 'tnt-td',     label: 'TNT từ tín dụng' },
  ];

  readonly doiTuongLabelMap: Record<DoiTuong, string> = {
    'chi-nhanh': 'Chi nhánh',
    phong: 'Phòng',
    am: 'Cán bộ AM'
  };

  ngOnInit(): void { this.load(); }

  // ── Derived values ────────────────────────────────────────────────
  get selectedKy(): string {
    if (this.loaiKy === 'nam')  return String(this.selectedNam);
    if (this.loaiKy === 'quy')  return `${this.selectedQuy}/${this.selectedNam}`;
    return `${String(this.selectedThang).padStart(2, '0')}/${this.selectedNam}`;
  }

  get periodLabel(): string {
    if (this.loaiKy === 'nam')  return `Năm ${this.selectedNam}`;
    if (this.loaiKy === 'quy')  return `${this.selectedQuy}/${this.selectedNam}`;
    return `Tháng ${String(this.selectedThang).padStart(2, '0')}/${this.selectedNam}`;
  }

  get unitColumnLabel(): string {
    return { phong: 'PHÒNG / ĐƠN VỊ', 'chi-nhanh': 'CHI NHÁNH', am: 'CÁN BỘ AM' }[this.doiTuong];
  }

  get doiTuongLabel(): string {
    return this.doiTuongLabelMap[this.doiTuong];
  }

  get tableTitle(): string {
    return `So sánh thực hiện vs kế hoạch — ${this.periodLabel} · Theo ${this.doiTuongLabel}`;
  }

  // ── Load so-sánh ─────────────────────────────────────────────────
  load(): void {
    this.loading.set(true);
    this.mpaService.getBscSoSanh(this.loaiKy, this.selectedKy, this.doiTuong).subscribe({
      next: res => {
        if (res.success) this.data.set(res.data);
        this.loading.set(false);
      },
      error: () => this.loading.set(false)
    });
  }

  onFilterChange(): void {
    this.load();
    if (this.activeTab === 'quan-ly') this.loadQuanLy();
  }

  // ── Load quản lý ─────────────────────────────────────────────────
  loadQuanLy(): void {
    this.loadingQuanLy.set(true);
    this.mpaService.getQuanLyList(this.loaiKy, this.selectedKy, this.doiTuong).subscribe({
      next: res => {
        if (res.success) this.quanLyList.set(res.data);
        this.loadingQuanLy.set(false);
      },
      error: () => this.loadingQuanLy.set(false)
    });
  }

  deleteChiTieu(id: number): void {
    if (!confirm('Xác nhận xóa chỉ tiêu này?')) return;
    this.mpaService.deleteChiTieu(id).subscribe(res => {
      if (res.success) this.loadQuanLy();
    });
  }

  // ── Dialog ────────────────────────────────────────────────────────
  openDialog(): void {
    this.dlgDoiTuong = this.doiTuong;
    this.dlgMaUnit   = '';
    this.dlgTenUnit  = '';
    this.dlgChiTieu  = 'tong-tnt';
    this.dlgMucTieu  = null;
    this.loadUnitOptions(this.dlgDoiTuong);
    this.showDialog.set(true);
  }

  closeDialog(): void { this.showDialog.set(false); }

  onDlgDoiTuongChange(dt: DoiTuong): void {
    this.dlgDoiTuong = dt;
    this.dlgMaUnit   = '';
    this.dlgTenUnit  = '';
    this.loadUnitOptions(dt);
  }

  loadUnitOptions(doiTuong: DoiTuong): void {
    const obs$ = doiTuong === 'chi-nhanh' ? this.mpaService.getCnList()
               : doiTuong === 'am'        ? this.mpaService.getAmListForCt()
               :                            this.mpaService.getPhongListForCt();
    obs$.subscribe(res => {
      if (res.success) this.unitOptions.set(res.data);
    });
  }

  onUnitSelect(): void {
    const opt = this.unitOptions().find(u => u.ma === this.dlgMaUnit);
    if (opt) this.dlgTenUnit = opt.ten;
  }

  get dlgCanSave(): boolean {
    return !!this.dlgMaUnit && this.dlgMucTieu !== null && !this.savingDialog();
  }

  saveDialog(): void {
    if (!this.dlgCanSave) return;
    this.savingDialog.set(true);
    const req: ChiTieuBscRequest = {
      loaiKy:    this.loaiKy,
      selectedKy: this.selectedKy,
      doiTuong:  this.dlgDoiTuong,
      maUnit:    this.dlgMaUnit,
      tenUnit:   this.dlgTenUnit,
      chiTieu:   this.dlgChiTieu,
      mucTieu:   this.dlgMucTieu!
    };
    this.mpaService.saveChiTieu(req).subscribe({
      next: res => {
        this.savingDialog.set(false);
        if (res.success) {
          this.showDialog.set(false);
          this.loadQuanLy();
        }
      },
      error: () => this.savingDialog.set(false)
    });
  }

  // ── Cell helpers ──────────────────────────────────────────────────
  cellClass(pct: number | null): string {
    if (pct === null) return 'cell-chua';
    if (pct >= 100) return 'cell-dat';
    if (pct >= 80)  return 'cell-canh';
    return 'cell-rui';
  }

  pctText(pct: number | null): string {
    if (pct === null) return '—';
    return Math.round(pct) + '%';
  }

  formatTy(val: number): string {
    if (val === null || val === undefined) return '—';
    const ty = val / 1000;
    return ty.toFixed(2).replace('.', ',') + ' tỷ';
  }

  deltaSign(d: number | null): string {
    if (d === null) return '';
    return (d > 0 ? '+' : '') + d.toFixed(1).replace('.', ',') + '%';
  }

  exportExcel(): void {
    alert('Chức năng xuất Excel đang được phát triển.');
  }

  setTab(tab: ActiveTab): void {
    this.activeTab = tab;
    if (tab === 'quan-ly') this.loadQuanLy();
  }
}
