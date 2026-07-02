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

interface UserForm {
  id?: number;
  username: string;
  fullName: string;
  email: string;
  role: string;
  maDonViCap6: string;
  password?: string;
  active: boolean;
}

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
  private fb    = inject(FormBuilder);
  private snack = inject(MatSnackBar);
  auth          = inject(AuthService);

  loading       = signal(true);
  saving        = signal(false);
  showModal     = signal(false);
  editMode      = signal(false);
  searchText    = '';
  filterRole    = '';
  activeTab     = signal<'users' | 'settings'>('users');

  users    = signal<UserForm[]>([]);
  filtered = signal<UserForm[]>([]);

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
    setTimeout(() => {
      this.users.set(this.mockUsers());
      this.applyFilter();
      this.loading.set(false);
    }, 500);
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
    this.form.get('password')?.setValidators([Validators.required, Validators.minLength(6)]);
    this.form.get('password')?.updateValueAndValidity();
    this.showModal.set(true);
  }

  openEdit(u: UserForm): void {
    this.editMode.set(true);
    this.editingId = u.id ?? null;
    this.form.patchValue({ ...u, password: '' });
    this.form.get('password')?.clearValidators();
    this.form.get('password')?.updateValueAndValidity();
    this.showModal.set(true);
  }

  closeModal(): void { this.showModal.set(false); }

  save(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.saving.set(true);
    const val = this.form.value as UserForm;

    setTimeout(() => {
      if (this.editMode()) {
        this.users.update(list => list.map(u => u.id === this.editingId ? { ...u, ...val, id: u.id } : u));
        this.snack.open('Cập nhật người dùng thành công', 'Đóng', { duration: 3000 });
      } else {
        const newUser: UserForm = { ...val, id: Date.now() };
        this.users.update(list => [...list, newUser]);
        this.snack.open('Tạo người dùng thành công', 'Đóng', { duration: 3000 });
      }
      this.applyFilter();
      this.saving.set(false);
      this.showModal.set(false);
    }, 600);
  }

  toggleActive(u: UserForm): void {
    this.users.update(list => list.map(x => x.id === u.id ? { ...x, active: !x.active } : x));
    this.applyFilter();
    this.snack.open(`Tài khoản ${u.fullName} đã được ${!u.active ? 'kích hoạt' : 'vô hiệu'}`, 'Đóng', { duration: 2500 });
  }

  deleteUser(u: UserForm): void {
    if (!confirm(`Xoá tài khoản "${u.fullName}"?`)) return;
    this.users.update(list => list.filter(x => x.id !== u.id));
    this.applyFilter();
    this.snack.open('Đã xoá tài khoản', 'Đóng', { duration: 3000 });
  }

  saveSettings(): void {
    this.snack.open('Đã lưu cài đặt hệ thống', 'Đóng', { duration: 3000 });
  }

  get statsTotal():   number { return this.users().length; }
  get statsAdmin():   number { return this.users().filter(u => u.role === 'ROLE_ADMIN').length; }
  get statsManager(): number { return this.users().filter(u => u.role === 'ROLE_BRANCH_MANAGER').length; }
  get statsEmployee():number { return this.users().filter(u => u.role === 'ROLE_EMPLOYEE').length; }
  get statsActive():  number { return this.users().filter(u => u.active).length; }

  private mockUsers(): UserForm[] {
    return [
      { id:1, username:'admin',      fullName:'Lê Quang Khải',   email:'admin@bidv.com.vn',      role:'ROLE_ADMIN',          maDonViCap6:'',    active:true  },
      { id:2, username:'manager01',  fullName:'Trần Thị Hoàng Anh',  email:'hoang.anh@bidv.com.vn',  role:'ROLE_BRANCH_MANAGER', maDonViCap6:'P001',active:true  },
      { id:3, username:'manager02',  fullName:'Lê Văn Phúc',         email:'van.phuc@bidv.com.vn',   role:'ROLE_BRANCH_MANAGER', maDonViCap6:'P002',active:true  },
      { id:4, username:'am.nguyena', fullName:'Nguyễn Văn A',        email:'nguyen.a@bidv.com.vn',   role:'ROLE_EMPLOYEE',       maDonViCap6:'P001',active:true  },
      { id:5, username:'am.buikhanh',fullName:'Bùi Văn Khành',       email:'bui.khanh@bidv.com.vn',  role:'ROLE_EMPLOYEE',       maDonViCap6:'P002',active:true  },
      { id:6, username:'am.doquang', fullName:'Đỗ Văn Quang',        email:'do.quang@bidv.com.vn',   role:'ROLE_EMPLOYEE',       maDonViCap6:'P003',active:true  },
      { id:7, username:'am.tranb',   fullName:'Trần Thị B',          email:'tran.b@bidv.com.vn',     role:'ROLE_EMPLOYEE',       maDonViCap6:'P004',active:false },
      { id:8, username:'am.phamthe', fullName:'Phạm Thị E',          email:'pham.e@bidv.com.vn',     role:'ROLE_EMPLOYEE',       maDonViCap6:'P003',active:true  },
      { id:9, username:'am.trankhanh',fullName:'Trần Văn Khánh',     email:'tran.khanh@bidv.com.vn', role:'ROLE_EMPLOYEE',       maDonViCap6:'P006',active:true  },
    ];
  }
}
