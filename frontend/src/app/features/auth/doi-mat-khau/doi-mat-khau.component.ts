import { Component, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators, AbstractControl, ValidationErrors } from '@angular/forms';
import { Router } from '@angular/router';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { AuthService } from '../../../core/services/auth.service';

function passwordsMatch(control: AbstractControl): ValidationErrors | null {
  const newPass = control.get('newPassword')?.value;
  const confirm = control.get('confirmPassword')?.value;
  return newPass && confirm && newPass !== confirm ? { mismatch: true } : null;
}

@Component({
  selector: 'app-doi-mat-khau',
  standalone: true,
  imports: [
    CommonModule, ReactiveFormsModule,
    MatFormFieldModule, MatInputModule, MatButtonModule,
    MatIconModule, MatProgressSpinnerModule
  ],
  templateUrl: './doi-mat-khau.component.html',
  styleUrl: './doi-mat-khau.component.scss'
})
export class DoiMatKhauComponent {
  form = this.fb.group({
    currentPassword: ['', Validators.required],
    newPassword: ['', [Validators.required, Validators.minLength(8)]],
    confirmPassword: ['', Validators.required]
  }, { validators: passwordsMatch });

  loading = signal(false);
  error = signal('');
  showCurrent = signal(false);
  showNew = signal(false);
  bat_buoc = true;

  constructor(
    private fb: FormBuilder,
    private auth: AuthService,
    private router: Router
  ) {
    // Nếu user vào trang này khi KHÔNG bị bắt buộc đổi mật khẩu (VD tự bấm "Đổi mật khẩu"
    // trong menu sau này) thì vẫn cho phép — chỉ đánh dấu để đổi text/nút "Huỷ" nếu cần.
    this.bat_buoc = this.auth.mustChangePassword();
  }

  submit(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.loading.set(true);
    this.error.set('');

    const { currentPassword, newPassword } = this.form.value;
    this.auth.changePassword({ currentPassword: currentPassword!, newPassword: newPassword! }).subscribe({
      next: res => {
        this.loading.set(false);
        if (res.success) {
          this.router.navigate(['/dashboard']);
        } else {
          this.error.set(res.message || 'Đổi mật khẩu thất bại');
        }
      },
      error: err => {
        this.loading.set(false);
        this.error.set(err?.error?.message || 'Đổi mật khẩu thất bại');
      }
    });
  }
}
