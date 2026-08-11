import { Component, OnInit, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { AuthService } from '../../core/services/auth.service';
import { UserService } from '../../core/services/user.service';
import { User, UserRequest } from '../../core/models/user.model';

@Component({
  selector: 'app-quan-tri',
  standalone: true,
  imports: [
    CommonModule, FormsModule, ReactiveFormsModule,
    MatIconModule, MatButtonModule, MatFormFieldModule,
    MatInputModule, MatSelectModule, MatProgressSpinnerModule,
    MatTooltipModule, MatSnackBarModule, MatSlideToggleModule
  ],
  templateUrl: './quan-tri.component.html',
  styleUrl: './quan-tri.component.scss'
})
export class QuanTriComponent implements OnInit {
  private fb      = inject(FormBuilder);
  private snack   = inject(MatSnackBar);
  private userSvc = inject(UserService);
  auth            = inject(AuthService);

  loading       = signal(true);
  saving        = signal(false);
  showModal     = signal(false);
  editMode      = signal(false);
  searchText    = '';
  filterRole    = '';
  activeTab     = signal<'users' | 'settings'>('users');

  users    = signal<User[]>([]);
  filtered = signal<User[]>([]);

  roles = [
    { value: 'ROLE_ADMIN',          label: 'Quản trị viên',      color: '#ef4444' },
    { value: 'ROLE_BRANCH_MANAGER', label: 'Quản lý chi nhánh',  color: '#f59e0b' },
    { value: 'ROLE_EMPLOYEE',       label: 'Nhân viên AM',        color: '#009640' },
  ];

  form: FormGroup = this.fb.group({
    username:   ['', [Validators.required, Validators.minLength(3)]],
    fullName:   ['', Validators.required],
    email:      ['', [Validators.required, Validators.email]],
    role:       ['ROLE_EMPLOYEE', Validators.required],
    maDonViCap6:[''],
    password:   [''],
    active:     [true]
  });

  editingId: number | null = null;

  // System settings
  systemSettings = {
    maxFileSize: 50,
    sessionTimeout: 480,
    enableAuditLog: true,
    maintenanceMode: false,
    defaultPageSize: 20,
  };

  ngOnInit(): void {
    if (!this.auth.isAdmin()) return;
    this.loadUsers();
  }

  private loadUsers(): void {
    this.loading.set(true);
    this.userSvc.getUsers().subscribe({
      next: res => {
        if (res.success) {
          this.users.set(res.data.content);
          this.applyFilter();
        } else {
          this.snack.open(res.message || 'Lỗi khi tải danh sách người dùng', 'Đóng', { duration: 3000 });
        }
        this.loading.set(false);
      },
      error: err => {
        this.snack.open(err?.error?.message || 'Lỗi khi tải danh sách người dùng', 'Đóng', { duration: 3000 });
        this.loading.set(false);
      }
    });
  }

  applyFilter(): void {
    let list = this.users();
    if (this.searchText) {
      const q = this.searchText.toLowerCase();
      list = list.filter(u =>
        u.fullName.toLowerCase().includes(q) ||
        u.username.toLowerCase().includes(q) ||
        u.email.toLowerCase().includes(q)
      );
    }
    if (this.filterRole) list = list.filter(u => u.role === this.filterRole);
    this.filtered.set(list);
  }

  getRoleLabel(role: string): string { return this.roles.find(r => r.value === role)?.label ?? role; }
  getRoleColor(role: string): string { return this.roles.find(r => r.value === role)?.color ?? '#9ca3af'; }

  openCreate(): void {
    this.editMode.set(false);
    this.editingId = null;
    this.form.reset({ role: 'ROLE_EMPLOYEE', active: true });
    this.form.get('username')?.enable();
    this.form.get('password')?.setValidators([Validators.required, Validators.minLength(8)]);
    this.form.get('password')?.updateValueAndValidity();
    this.showModal.set(true);
  }

  openEdit(u: User): void {
    this.editMode.set(true);
    this.editingId = u.id;
    this.form.patchValue({ ...u, password: '' });
    // Không cho đổi username sau khi tạo — khớp đúng quy ước backend (username là khoá bất biến).
    this.form.get('username')?.disable();
    this.form.get('password')?.clearValidators();
    this.form.get('password')?.setValidators([Validators.minLength(8)]);
    this.form.get('password')?.updateValueAndValidity();
    this.showModal.set(true);
  }

  closeModal(): void { this.showModal.set(false); }

  save(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.saving.set(true);
    const val = this.form.getRawValue() as UserRequest;
    // Rỗng = không đổi mật khẩu (chỉ áp dụng khi sửa — khi tạo mới password đã required).
    if (this.editMode() && !val.password) delete (val as Partial<UserRequest>).password;

    const req$ = this.editMode() && this.editingId != null
      ? this.userSvc.updateUser(this.editingId, val)
      : this.userSvc.createUser(val);

    req$.subscribe({
      next: res => {
        this.saving.set(false);
        if (res.success) {
          this.snack.open(this.editMode() ? 'Cập nhật người dùng thành công' : 'Tạo người dùng thành công', 'Đóng', { duration: 3000 });
          this.showModal.set(false);
          this.loadUsers();
        } else {
          this.snack.open(res.message || 'Lỗi khi lưu người dùng', 'Đóng', { duration: 3000 });
        }
      },
      error: err => {
        this.saving.set(false);
        this.snack.open(err?.error?.message || 'Lỗi khi lưu người dùng', 'Đóng', { duration: 3000 });
      }
    });
  }

  toggleActive(u: User): void {
    this.userSvc.setUserActive(u.id, !u.active).subscribe({
      next: res => {
        if (res.success) {
          this.users.update(list => list.map(x => x.id === u.id ? { ...x, active: !x.active } : x));
          this.applyFilter();
          this.snack.open(`Tài khoản ${u.fullName} đã được ${!u.active ? 'kích hoạt' : 'vô hiệu hoá'}`, 'Đóng', { duration: 2500 });
        } else {
          this.snack.open(res.message || 'Lỗi khi cập nhật trạng thái', 'Đóng', { duration: 3000 });
        }
      },
      error: err => {
        this.snack.open(err?.error?.message || 'Lỗi khi cập nhật trạng thái', 'Đóng', { duration: 3000 });
      }
    });
  }

  /** Không có xoá cứng — chỉ vô hiệu hoá tài khoản (giữ lại lịch sử tham chiếu created_by/
   *  updated_by ở các bảng khác, tránh rủi ro xoá dữ liệu không cần thiết cho hệ thống ngân hàng). */
  deactivateUser(u: User): void {
    if (!u.active) return;
    if (!confirm(`Vô hiệu hoá tài khoản "${u.fullName}"? Tài khoản sẽ không thể đăng nhập cho tới khi được kích hoạt lại.`)) return;
    this.toggleActive(u);
  }

  saveSettings(): void {
    this.snack.open('Đã lưu cài đặt hệ thống', 'Đóng', { duration: 3000 });
  }

  get statsTotal():   number { return this.users().length; }
  get statsAdmin():   number { return this.users().filter(u => u.role === 'ROLE_ADMIN').length; }
  get statsManager(): number { return this.users().filter(u => u.role === 'ROLE_BRANCH_MANAGER').length; }
  get statsEmployee():number { return this.users().filter(u => u.role === 'ROLE_EMPLOYEE').length; }
  get statsActive():  number { return this.users().filter(u => u.active).length; }
}
