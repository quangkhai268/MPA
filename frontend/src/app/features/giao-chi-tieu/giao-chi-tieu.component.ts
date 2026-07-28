import { Component, OnInit, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { NgChartsModule } from 'ng2-charts';
import { Chart, ChartConfiguration, ChartData, ChartDataset, registerables } from 'chart.js';
import { Observable, of } from 'rxjs';
import { map } from 'rxjs/operators';

import { MpaService } from '../../core/services/mpa.service';
import {
  BscSoSanhResult, BscSoSanhRow,
  ChiTieuBscRequest, ChiTieuQuanLyRow, UnitOption, XuHuongResponse
} from '../../core/models/mpa.model';

Chart.register(...registerables);

type LoaiKy = 'thang' | 'quy' | 'nam' | 'ngay';
type DoiTuong = 'phong' | 'chi-nhanh' | 'am';
type ActiveTab = 'so-sanh' | 'xu-huong' | 'quan-ly';

@Component({
  selector: 'app-giao-chi-tieu',
  standalone: true,
  imports: [
    CommonModule, FormsModule,
    MatIconModule, MatButtonModule, MatCheckboxModule,
    MatProgressSpinnerModule, MatTooltipModule, NgChartsModule
  ],
  templateUrl: './giao-chi-tieu.component.html',
  styleUrl: './giao-chi-tieu.component.scss'
})
export class GiaoChiTieuComponent implements OnInit {
  private mpaService = inject(MpaService);

  // ── Toast ────────────────────────────────────────────────────────
  toast = signal<{ text: string; success: boolean } | null>(null);
  private toastTimer: ReturnType<typeof setTimeout> | null = null;

  // ── Confirm dialog (thay cho window.confirm để tự chỉnh giao diện) ──────
  showConfirmDialog = signal(false);
  confirmMessage    = signal('');
  private confirmResolve: ((ok: boolean) => void) | null = null;

  private askConfirm(message: string): Promise<boolean> {
    this.confirmMessage.set(message);
    this.showConfirmDialog.set(true);
    return new Promise(resolve => { this.confirmResolve = resolve; });
  }

  onConfirmDialog(ok: boolean): void {
    this.showConfirmDialog.set(false);
    this.confirmResolve?.(ok);
    this.confirmResolve = null;
  }

  // ── Filter state ──────────────────────────────────────────────────
  loaiKy: LoaiKy    = 'thang';
  selectedNam        = new Date().getFullYear();
  selectedThang      = new Date().getMonth() + 1;
  selectedQuy        = 'Q' + Math.ceil((new Date().getMonth() + 1) / 3);
  doiTuong: DoiTuong = 'chi-nhanh';

  // Lọc theo Phòng ban — chỉ áp dụng khi Đối tượng giao là Phòng nghiệp vụ hoặc Cán bộ AM
  filterPhongBan = '';
  filterPhongOptions = signal<UnitOption[]>([]);

  // Tìm theo Tên cán bộ / Mã AM — chỉ áp dụng khi Đối tượng giao là Cán bộ AM (lọc trên
  // danh sách đã tải, không gọi lại API vì danh sách AM không quá lớn).
  filterAmSearch = '';

  // "Lũy kế đến tháng" (chỉ áp dụng khi Loại kỳ = Năm — mốc lũy kế mới nhất đã đồng bộ từ du_lieu_mpa)
  namLuyKeOptions: number[] = [];
  selectedDenThang: number | null = null;

  // Ngày gần nhất có dữ liệu thực hiện (chỉ áp dụng khi Loại kỳ = Ngày)
  latestNgay = signal<string | null>(null);

  // ── UI state ──────────────────────────────────────────────────────
  showDelta   = false;
  activeTab: ActiveTab = 'so-sanh';
  loading     = signal(false);
  data        = signal<BscSoSanhResult | null>(null);

  // ── Quản lý tab state ─────────────────────────────────────────────
  quanLyList    = signal<ChiTieuQuanLyRow[]>([]);
  loadingQuanLy = signal(false);

  // ── Xu hướng tab state ────────────────────────────────────────────
  xuHuongLoading = signal(false);
  xuHuongCharts  = signal<{ metricKey: string; label: string; data: ChartData<'bar'> }[]>([]);

  readonly xuHuongChartOptions: ChartConfiguration<'bar'>['options'] = {
    responsive: true, maintainAspectRatio: false,
    interaction: { mode: 'index', intersect: false },
    plugins: {
      legend: { display: true, position: 'top', labels: { font: { size: 11 }, boxWidth: 10, padding: 12 } },
      tooltip: { callbacks: { label: ctx => ` ${ctx.dataset.label}: ${this.formatTy(ctx.raw as number)}` } }
    },
    scales: {
      x: { grid: { display: false }, ticks: { font: { size: 11 }, color: '#9ca3af' } },
      y: { grid: { color: '#f3f4f6' }, ticks: { font: { size: 11 }, color: '#9ca3af', maxTicksLimit: 6, callback: v => this.formatTy(+v) } }
    }
  };

  // ── Dialog state ──────────────────────────────────────────────────
  showDialog   = signal(false);
  savingDialog = signal(false);
  dlgDoiTuong: DoiTuong = 'phong';
  dlgMaUnit    = '';
  dlgTenUnit   = '';
  dlgChiTieu   = 'tong-tnt';
  dlgMucTieu: number | null = null;
  dlgMucTieuDisplay = '';
  unitOptions  = signal<UnitOption[]>([]);

  // Đang sửa 1 dòng có sẵn (khác null) hay thêm mới (null) — quyết định tiêu đề dialog
  // và tự điền lại "Mục tiêu" theo giá trị hiện có mỗi khi đổi "Chỉ tiêu" trong dialog.
  editingRow = signal<ChiTieuQuanLyRow | null>(null);

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

  ngOnInit(): void {
    this.load();
    this.mpaService.getPhongListForCt().subscribe(res => {
      if (res.success) this.filterPhongOptions.set(res.data);
    });
  }

  // ── Derived values ────────────────────────────────────────────────
  get selectedKy(): string {
    // "Ngày" so với chỉ tiêu BSC theo năm đã khai báo — dùng chung selectedKy với "Năm".
    if (this.loaiKy === 'nam' || this.loaiKy === 'ngay') return String(this.selectedNam);
    if (this.loaiKy === 'quy')  return `${this.selectedQuy}/${this.selectedNam}`;
    return `${String(this.selectedThang).padStart(2, '0')}/${this.selectedNam}`;
  }

  get periodLabel(): string {
    if (this.loaiKy === 'ngay') {
      const ngay = this.latestNgay();
      return `Năm ${this.selectedNam} (so với dữ liệu ngày${ngay ? ' ' + ngay : ' gần nhất'})`;
    }
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
    const base = `So sánh thực hiện vs kế hoạch — ${this.periodLabel} · Theo ${this.doiTuongLabel}`;
    if (this.doiTuong === 'am' && this.data()) {
      return `${base} (${this.filteredSoSanhRows.length} cán bộ AM)`;
    }
    return base;
  }

  // Chỉ Phòng/AM mới có khái niệm "phòng ban" để lọc — Chi nhánh chỉ có 1 dòng duy nhất.
  get showPhongBanFilter(): boolean {
    return this.doiTuong === 'phong' || this.doiTuong === 'am';
  }

  // Tìm theo Tên cán bộ / Mã AM — lọc trên danh sách đã tải (không gọi lại API).
  get showAmSearch(): boolean {
    return this.doiTuong === 'am';
  }

  private matchAmSearch(maUnit: string, tenUnit: string): boolean {
    const q = this.filterAmSearch.trim().toLowerCase();
    if (!q) return true;
    return tenUnit.toLowerCase().includes(q) || maUnit.toLowerCase().includes(q);
  }

  get filteredSoSanhRows(): BscSoSanhRow[] {
    const rows = this.data()?.rows ?? [];
    if (!this.showAmSearch) return rows;
    return rows.filter(r => this.matchAmSearch(r.maUnit, r.tenUnit));
  }

  get filteredQuanLyRows(): ChiTieuQuanLyRow[] {
    const rows = this.quanLyList();
    if (!this.showAmSearch) return rows;
    return rows.filter(r => this.matchAmSearch(r.maUnit, r.tenUnit));
  }

  // ── Load so-sánh ─────────────────────────────────────────────────
  load(): void {
    this.loading.set(true);
    const maDonViCap6 = this.showPhongBanFilter ? this.filterPhongBan : undefined;
    this.mpaService.getBscSoSanh(this.loaiKy, this.selectedKy, this.doiTuong, maDonViCap6).subscribe({
      next: res => {
        if (res.success) this.data.set(res.data);
        this.loading.set(false);
      },
      error: () => this.loading.set(false)
    });
  }

  onDoiTuongChange(): void {
    this.filterPhongBan = '';
    this.filterAmSearch = '';
    this.onFilterChange();
  }

  onFilterChange(): void {
    if (this.loaiKy === 'nam') {
      this.loadNamLuyKeOptions();
    } else if (this.loaiKy === 'ngay') {
      this.loadLatestNgay();
    }
    this.load();
    if (this.activeTab === 'quan-ly')  this.loadQuanLy();
    if (this.activeTab === 'xu-huong') this.loadXuHuong();
  }

  private loadNamLuyKeOptions(): void {
    this.mpaService.getNamLuyKeOptions(this.selectedNam).subscribe({
      next: r => {
        this.namLuyKeOptions = r.success && r.data ? r.data : [];
        this.selectedDenThang = this.namLuyKeOptions.length ? this.namLuyKeOptions[0] : null;
      },
      error: () => {
        this.namLuyKeOptions = [];
        this.selectedDenThang = null;
      }
    });
  }

  private loadLatestNgay(): void {
    this.mpaService.getGiaoChiTieuLatestNgay().subscribe({
      next: r => this.latestNgay.set(r.success ? r.data : null),
      error: () => this.latestNgay.set(null)
    });
  }

  // ── Load quản lý ─────────────────────────────────────────────────
  loadQuanLy(): void {
    this.loadingQuanLy.set(true);
    const maDonViCap6 = this.showPhongBanFilter ? this.filterPhongBan : undefined;
    this.mpaService.getQuanLyList(this.loaiKy, this.selectedKy, this.doiTuong, maDonViCap6).subscribe({
      next: res => {
        if (res.success) this.quanLyList.set(res.data);
        this.loadingQuanLy.set(false);
      },
      error: () => this.loadingQuanLy.set(false)
    });
  }

  async deleteChiTieu(id: number): Promise<void> {
    const ok = await this.askConfirm('Xác nhận xóa chỉ tiêu này?');
    if (!ok) return;
    this.mpaService.deleteChiTieu(id).subscribe(res => {
      if (res.success) this.loadQuanLy();
    });
  }

  // ── Dialog ────────────────────────────────────────────────────────
  openDialog(): void {
    this.editingRow.set(null);
    this.dlgDoiTuong = this.doiTuong;
    this.dlgMaUnit   = '';
    this.dlgTenUnit  = '';
    this.dlgChiTieu  = 'tong-tnt';
    this.dlgMucTieu  = null;
    this.dlgMucTieuDisplay = '';
    this.loadUnitOptions(this.dlgDoiTuong);
    this.showDialog.set(true);
  }

  // Sửa 1 dòng có sẵn trong bảng "Quản lý chỉ tiêu" — tái dùng dialog Thêm chỉ tiêu,
  // điền sẵn đơn vị + giá trị hiện tại của chỉ tiêu đang chọn (mặc định Tổng TNT).
  openEditDialog(row: ChiTieuQuanLyRow): void {
    this.editingRow.set(row);
    this.dlgDoiTuong = this.doiTuong;
    this.dlgMaUnit   = row.maUnit;
    this.dlgTenUnit  = row.tenUnit;
    this.dlgChiTieu  = 'tong-tnt';
    this.loadUnitOptions(this.dlgDoiTuong);
    this.applyRowValueToDlg(row, this.dlgChiTieu);
    this.showDialog.set(true);
  }

  closeDialog(): void { this.showDialog.set(false); }

  // Khi đang sửa 1 dòng có sẵn, đổi "Chỉ tiêu" sẽ tự điền lại "Mục tiêu" theo giá trị
  // hiện có của dòng đó cho chỉ tiêu vừa chọn — khỏi phải tra bảng rồi gõ lại thủ công.
  onDlgChiTieuChange(): void {
    const row = this.editingRow();
    if (row) this.applyRowValueToDlg(row, this.dlgChiTieu);
  }

  private applyRowValueToDlg(row: ChiTieuQuanLyRow, chiTieu: string): void {
    const field = this.chiTieuFieldMap[chiTieu];
    const val = field ? Number(row[field]) : 0;
    this.dlgMucTieu = val || null;
    this.dlgMucTieuDisplay = val ? val.toLocaleString('vi-VN') : '';
  }

  onDlgDoiTuongChange(dt: DoiTuong): void {
    this.editingRow.set(null);
    this.dlgDoiTuong = dt;
    this.dlgMaUnit   = '';
    this.dlgTenUnit  = '';
    this.loadUnitOptions(dt);
  }

  loadUnitOptions(doiTuong: DoiTuong): void {
    // AM: nếu filter bên trên đang chọn sẵn 1 phòng ban, chỉ lấy các cán bộ AM thuộc phòng đó
    // (tra từ thong_tin_am — danh sách cán bộ, không phụ thuộc kỳ).
    const amPhong = doiTuong === 'am' && this.filterPhongBan ? this.filterPhongBan : undefined;
    const obs$ = doiTuong === 'chi-nhanh' ? this.mpaService.getCnList()
               : doiTuong === 'am'        ? this.mpaService.getAmListForCt(amPhong)
               :                            this.mpaService.getPhongListForCt();
    obs$.subscribe(res => {
      if (!res.success) return;
      let options = res.data;

      // AM: nếu filter bên trên đang có từ khóa tìm kiếm, thu hẹp danh sách theo đúng từ khóa đó.
      if (doiTuong === 'am' && this.filterAmSearch.trim()) {
        options = this.narrowAmOptions(options, this.filterAmSearch).list;
      }

      this.unitOptions.set(options);

      // Chi nhánh: chỉ có 1 chi nhánh vận hành nên tự chọn sẵn, đỡ phải bấm chọn mỗi lần.
      if (doiTuong === 'chi-nhanh' && options.length > 0 && !this.dlgMaUnit) {
        this.dlgMaUnit  = options[0].ma;
        this.dlgTenUnit = options[0].ten;
      }
      // Phòng: nếu filter bên trên đang chọn sẵn 1 phòng ban, tự điền theo — khỏi phải chọn lại.
      if (doiTuong === 'phong' && this.filterPhongBan && !this.dlgMaUnit) {
        const opt = options.find(u => u.ma === this.filterPhongBan);
        if (opt) {
          this.dlgMaUnit  = opt.ma;
          this.dlgTenUnit = opt.ten;
        }
      }
      // AM: sau khi lọc theo phòng + từ khóa, nếu chỉ còn đúng 1 mã AM thì tự chọn luôn.
      if (doiTuong === 'am' && options.length === 1 && !this.dlgMaUnit) {
        this.dlgMaUnit  = options[0].ma;
        this.dlgTenUnit = options[0].ten;
      }
    });
  }

  onUnitSelect(): void {
    const opt = this.unitOptions().find(u => u.ma === this.dlgMaUnit);
    if (opt) this.dlgTenUnit = opt.ten;
  }

  // Thu hẹp danh sách AM theo từ khóa — dùng chung cho dialog "Thêm chỉ tiêu" (loadUnitOptions)
  // và tab "Xu hướng" (resolveScopeForXuHuong). Nếu từ khóa rỗng hoặc không khớp gì, giữ
  // nguyên danh sách gốc (permissive — phù hợp UX dialog); `single` chỉ có giá trị khi thu hẹp
  // được đúng 1 kết quả.
  private narrowAmOptions(options: UnitOption[], search: string): { list: UnitOption[]; single: UnitOption | null } {
    const q = search.trim().toLowerCase();
    if (!q) return { list: options, single: options.length === 1 ? options[0] : null };
    const narrowed = options.filter(u => u.ten.toLowerCase().includes(q) || u.ma.toLowerCase().includes(q));
    if (narrowed.length > 0) return { list: narrowed, single: narrowed.length === 1 ? narrowed[0] : null };
    return { list: options, single: null };
  }

  // Gõ số tự thêm dấu . phân cách hàng nghìn để dễ nhìn — chỉ giữ lại chữ số,
  // dlgMucTieu (giá trị gửi lên DB) luôn là số nguyên sạch, không có dấu chấm.
  onMucTieuInput(raw: string): void {
    const digits = raw.replace(/\D/g, '');
    this.dlgMucTieu = digits ? Number(digits) : null;
    this.dlgMucTieuDisplay = digits ? Number(digits).toLocaleString('vi-VN') : '';
  }

  get dlgCanSave(): boolean {
    return !!this.dlgMaUnit && this.dlgMucTieu !== null && !this.savingDialog();
  }

  // Ánh xạ giá trị dlgChiTieu → field tương ứng trên ChiTieuQuanLyRow, dùng để kiểm tra
  // xem chỉ tiêu này đã được khai báo (khác 0) cho đúng đơn vị/kỳ hay chưa trước khi lưu.
  private readonly chiTieuFieldMap: Record<string, keyof ChiTieuQuanLyRow> = {
    'hdv-cuoi-ky': 'hdvCuoiKy',
    'casa-bq':     'casaBinhQuan',
    'du-no':       'duNoTinDung',
    'tnt-dv':      'tntDichVu',
    'tnt-hdv':     'tntHdvFtp',
    'tnt-td':      'tntTinDung',
    'tong-tnt':    'thuNhapThuan'
  };

  saveDialog(): void {
    if (!this.dlgCanSave) return;
    this.savingDialog.set(true);
    this.mpaService.getQuanLyList(this.loaiKy, this.selectedKy, this.dlgDoiTuong).subscribe({
      next: res => this.confirmOverwriteIfNeeded(res.success ? res.data : []),
      error: () => this.confirmOverwriteIfNeeded([])
    });
  }

  private async confirmOverwriteIfNeeded(existingList: ChiTieuQuanLyRow[]): Promise<void> {
    const existing = existingList.find(r => r.maUnit === this.dlgMaUnit);
    const field = this.chiTieuFieldMap[this.dlgChiTieu];
    const oldVal = existing && field ? Number(existing[field]) : 0;

    if (oldVal !== 0) {
      const chiTieuLabel = this.chiTieuOptions.find(c => c.value === this.dlgChiTieu)?.label ?? this.dlgChiTieu;
      const ok = await this.askConfirm(
        `Chỉ tiêu "${chiTieuLabel}" cho ${this.dlgTenUnit} đã được khai báo (${this.formatTy(oldVal)}).\n` +
        `Bạn có chắc chắn muốn khai báo lại (ghi đè giá trị cũ) không?`
      );
      if (!ok) {
        this.savingDialog.set(false);
        return;
      }
    }
    this.doSaveChiTieu();
  }

  private doSaveChiTieu(): void {
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
          this.notify('✓ Lưu chỉ tiêu thành công', true);
        } else {
          this.notify('✗ Lưu chỉ tiêu thất bại: ' + res.message, false);
        }
      },
      error: (err) => {
        this.savingDialog.set(false);
        this.notify('✗ Lưu chỉ tiêu thất bại: ' + (err?.error?.message || err?.message || 'lỗi không xác định'), false);
      }
    });
  }

  // ── Toast ────────────────────────────────────────────────────────
  private notify(message: string, success: boolean): void {
    if (this.toastTimer) clearTimeout(this.toastTimer);
    this.toast.set({ text: message, success });
    this.toastTimer = setTimeout(() => this.toast.set(null), success ? 3500 : 6000);
  }

  closeToast(): void {
    if (this.toastTimer) clearTimeout(this.toastTimer);
    this.toast.set(null);
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
    const ty = val / 1_000_000_000;
    const rounded = Math.round(ty);
    // Giá trị nhỏ (VD chỉ vài trăm triệu) làm tròn về 0 sẽ mất hết thông tin dù thực tế
    // khác 0 và có biến động — hiển thị thêm phần lẻ trong trường hợp này.
    if (rounded === 0 && ty !== 0) {
      return ty.toFixed(2).replace('.', ',') + ' tỷ';
    }
    return rounded.toLocaleString('vi-VN') + ' tỷ';
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
    if (tab === 'quan-ly')  this.loadQuanLy();
    if (tab === 'xu-huong') this.loadXuHuong();
  }

  // ── Xu hướng ──────────────────────────────────────────────────────
  // Thứ tự ưu tiên: đã thu hẹp về 1 mã AM cụ thể > đã chọn 1 phòng ban > toàn chi nhánh.
  private resolveScopeForXuHuong(): Observable<{ maDonViCap6?: string; maAm?: string }> {
    if (this.doiTuong === 'chi-nhanh') return of({});
    if (this.doiTuong === 'phong') {
      return of(this.filterPhongBan ? { maDonViCap6: this.filterPhongBan } : {});
    }
    // doiTuong === 'am'
    const amPhong = this.filterPhongBan || undefined;
    return this.mpaService.getAmListForCt(amPhong).pipe(
      map(res => {
        const options = res.success ? res.data : [];
        const single = this.narrowAmOptions(options, this.filterAmSearch).single;
        if (single) return { maDonViCap6: this.filterPhongBan || undefined, maAm: single.ma };
        if (this.filterPhongBan) return { maDonViCap6: this.filterPhongBan };
        return {};
      })
    );
  }

  loadXuHuong(): void {
    if (this.loaiKy === 'ngay') { this.xuHuongCharts.set([]); return; }
    this.xuHuongLoading.set(true);
    this.resolveScopeForXuHuong().subscribe(scope => {
      this.mpaService.getXuHuong(this.loaiKy, this.selectedNam, this.doiTuong, scope.maDonViCap6, scope.maAm).subscribe({
        next: res => {
          if (res.success) this.buildXuHuongCharts(res.data);
          this.xuHuongLoading.set(false);
        },
        error: () => this.xuHuongLoading.set(false)
      });
    });
  }

  private buildXuHuongCharts(data: XuHuongResponse): void {
    this.xuHuongCharts.set(data.metrics.map(m => ({
      metricKey: m.metricKey,
      label: m.metricLabel,
      data: {
        labels: data.periods,
        datasets: [
          { type: 'bar', label: 'Kế hoạch', data: m.keHoachValues,
            backgroundColor: '#F59E0B', borderRadius: 4, order: 2 },
          { type: 'line', label: 'Thực hiện', data: m.thucHienValues,
            borderColor: '#0f59a6', backgroundColor: 'transparent',
            tension: 0.35, borderWidth: 2.5, pointRadius: 3, order: 1 }
        ] as ChartDataset<'bar' | 'line'>[]
      } as ChartData<'bar'>
    })));
  }
}
