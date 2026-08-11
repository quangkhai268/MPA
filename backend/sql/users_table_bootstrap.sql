-- Tạo bảng users cho hệ thống đăng nhập/RBAC thực (Phase 1 của security rollout).
-- Trước bản này, SecurityConfig permitAll() mọi request và đăng nhập chỉ là mock ở
-- frontend — bảng này thay thế hoàn toàn cơ chế đó. Chạy TAY (không tự động từ code)
-- theo CLAUDE.md §11, giống mọi script khác trong thư mục này.
--
-- Phase 2 (KHÔNG làm ở đây, chỉ ghi chú để dễ ALTER sau):
--   - failed_login_attempts int, locked_until timestamp  (khóa tài khoản sau N lần sai)
--   - password_changed_at timestamp                      (chính sách hết hạn mật khẩu)
--   - bảng audit_log riêng cho sự kiện đăng nhập/quản trị

CREATE TABLE public.users (
    id                    BIGSERIAL PRIMARY KEY,
    username              VARCHAR(50)  NOT NULL,
    password_hash         VARCHAR(100) NOT NULL,
    full_name             VARCHAR(150) NOT NULL,
    email                 VARCHAR(150) NOT NULL,
    role                  VARCHAR(30)  NOT NULL,   -- ROLE_ADMIN | ROLE_BRANCH_MANAGER | ROLE_EMPLOYEE
    ma_don_vi_cap_6       VARCHAR(20),
    active                BOOLEAN NOT NULL DEFAULT true,
    must_change_password  BOOLEAN NOT NULL DEFAULT true,
    created_at            TIMESTAMP NOT NULL DEFAULT now(),
    updated_at            TIMESTAMP NOT NULL DEFAULT now(),
    created_by            VARCHAR(50),
    updated_by            VARCHAR(50),
    CONSTRAINT users_username_key UNIQUE (username)
);
CREATE INDEX idx_users_ma_don_vi_cap_6 ON public.users(ma_don_vi_cap_6);
CREATE INDEX idx_users_role ON public.users(role);

-- Seed tài khoản admin đầu tiên (giải quyết bài toán "con gà quả trứng": chưa có admin
-- thì không đăng nhập được để tạo admin qua API). Hash bên dưới PHẢI được người vận hành
-- tự sinh lại — KHÔNG dùng chung 1 mật khẩu mẫu cho mọi môi trường. Sinh hash bằng
-- BCryptPasswordEncoder (đã có sẵn trên classpath qua spring-security-crypto), ví dụ chạy
-- trong thư mục backend sau khi đã `mvn -q dependency:build-classpath -Dmdep.outputFile=cp.txt`:
--
--   jshell --class-path "$(cat cp.txt)"
--   jshell> new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode("<mật khẩu tạm của bạn>")
--
-- Dán kết quả (bắt đầu bằng $2a$ hoặc $2b$) thay cho REPLACE_WITH_YOUR_OWN_BCRYPT_HASH.
-- Không commit mật khẩu tạm dạng plaintext lên git — chỉ trao đổi ngoài band (điện thoại/gặp trực tiếp).
-- Tài khoản này bị bắt buộc đổi mật khẩu ngay lần đăng nhập đầu tiên (must_change_password=true).
INSERT INTO public.users (username, password_hash, full_name, email, role, active, must_change_password)
VALUES ('admin', 'REPLACE_WITH_YOUR_OWN_BCRYPT_HASH', 'Quản trị viên hệ thống', 'admin@bidv.com.vn', 'ROLE_ADMIN', true, true);
