# MPA Management System – Project Context

> File này cung cấp toàn bộ ngữ cảnh dự án cho AI assistant (Claude, Gemini, Copilot, …).
> Đọc file này trước khi thực hiện bất kỳ thay đổi nào trong codebase.

---

## 1. Tổng Quan Dự Án

**Tên dự án:** Hệ thống Quản lý MPA & Doanh thu Chi nhánh – BIDV

**Mục tiêu:**
Xây dựng một hệ thống web nội bộ cho ngân hàng BIDV để:
- Theo dõi và phân tích **dữ liệu MPA** (Mã AM – cán bộ khách hàng) theo chi nhánh, phòng ban
- Hiển thị **dashboard tổng quan** về hoạt động kinh doanh: thu nhập thuần, dư nợ, huy động vốn
- Cho phép **import dữ liệu từ file Excel** theo từng kỳ (ngày/tháng/quý/năm)
- Xuất báo cáo, so sánh xu hướng theo thời gian
- Phân quyền người dùng theo cấp bậc

---

## 2. Tech Stack

| Layer | Công nghệ |
|---|---|
| **Frontend** | Angular 17+ (Standalone Components), Angular Material, Chart.js / ng2-charts |
| **Backend** | Spring Boot 3.x, Java 17, Spring Security + JWT, Spring Data JPA |
| **Database** | PostgreSQL |
| **Build Tool** | Maven (backend), Angular CLI (frontend) |
| **File Import** | Apache POI (backend đọc Excel), XLSX.js (frontend preview) |
| **Export** | Apache POI (Excel), iText / JasperReports (PDF) |

**Ports mặc định:**
- Frontend: `http://localhost:4200`
- Backend: `http://localhost:8080`
- PostgreSQL: `localhost:5432`, database: `mpa_db`

---

## 3. Cấu Trúc Thư Mục

```
G:\Project\MPA\
├── claude.md               ← File này
├── backend\                ← Spring Boot project
│   ├── src\main\java\com\mpa\
│   │   ├── config\         # Security, CORS, JWT, Swagger config
│   │   ├── controller\     # REST Controllers
│   │   ├── dto\            # Request / Response DTOs
│   │   ├── entity\         # JPA Entities (ánh xạ bảng DB)
│   │   ├── exception\      # GlobalExceptionHandler, custom exceptions
│   │   ├── repository\     # Spring Data JPA Repositories
│   │   ├── service\        # Business logic (interface + impl)
│   │   └── util\           # JwtUtil, ExcelHelper, DateUtil
│   └── src\main\resources\
│       ├── application.yml
│       └── application-dev.yml
└── frontend\               ← Angular project
    └── src\app\
        ├── core\           # AuthGuard, JwtInterceptor, AuthService
        ├── shared\         # Shared components: DataTable, ConfirmDialog, Spinner
        ├── layout\         # Sidebar, Header, MainLayout
        └── features\
            ├── auth\       # /login
            ├── dashboard\  # /dashboard (trang chủ)
            ├── du-lieu-mpa\# /du-lieu-mpa (danh sách, import)
            ├── bao-cao\    # /bao-cao (báo cáo, xuất file)
            └── quan-tri\   # /quan-tri (user, phân quyền)
```

---

## 4. Database Schema

### Bảng chính: `public.du_lieu_mpa`

Đây là bảng dữ liệu trung tâm, lưu toàn bộ dữ liệu MPA được import từ file Excel hàng kỳ.

```sql
CREATE TABLE public.du_lieu_mpa (
    id                          serial4 NOT NULL,

    -- Thời gian
    ngay                        date NULL,
    thang                       int2 NULL,
    quy                         varchar(10) NULL,      -- Ví dụ: 'Q1', 'Q2', ...
    nam                         int2 NULL,

    -- Đơn vị AM (cán bộ khách hàng)
    ma_am                       varchar(20) NULL,      -- Mã cán bộ AM
    ten_am                      varchar(255) NULL,     -- Tên cán bộ AM

    -- Đơn vị cấp 6 (phòng/chi nhánh)
    ma_don_vi_cap_6             varchar(20) NULL,
    ten_don_vi_cap_6            varchar(255) NULL,

    -- Sản phẩm cấp 5
    ma_sp_cap_5                 varchar(20) NULL,
    ten_sp_cap_5                varchar(255) NULL,

    -- Phân khúc khách hàng cấp 2
    ma_phan_khuc_kh_cap_2       varchar(255) NULL,
    ten_phan_khuc_kh_cap_2      varchar(255) NULL,

    -- Khách hàng
    ma_kh_cif                   varchar(50) NOT NULL,  -- Mã CIF (khóa nghiệp vụ)
    ten_khach_hang              varchar(255) NULL,

    -- Kỳ hạn
    ky_han_cap_2                varchar(100) NULL,

    -- Chỉ số tài chính (đơn vị: triệu VND, 3 chữ số thập phân)
    thu_nhap_thuan_hdv_ftp      numeric(18,3) DEFAULT 0 NULL,  -- TNT HĐV FTP
    thu_nhap_thuan_dich_vu      numeric(18,3) DEFAULT 0 NULL,  -- TNT Dịch vụ
    thu_nhap_thuan_tin_dung     numeric(18,3) DEFAULT 0 NULL,  -- TNT Tín dụng
    thu_nhap_thuan              numeric(18,3) DEFAULT 0 NULL,  -- Tổng TNT
    du_no_tin_dung_cuoi_ky      numeric(18,3) DEFAULT 0 NULL,  -- Dư nợ tín dụng cuối kỳ
    huy_dong_von_binh_quan      numeric(18,3) DEFAULT 0 NULL,  -- HĐV bình quân
    huy_dong_von_cuoi_ky        numeric(18,3) DEFAULT 0 NULL,  -- HĐV cuối kỳ

    -- Metadata
    ngay_tao                    timestamp DEFAULT CURRENT_TIMESTAMP NULL,
    sheetname                   varchar(20) NULL,      -- Tên sheet Excel nguồn

    CONSTRAINT du_lieu_mpa_pkey PRIMARY KEY (id)
);
```

**Ghi chú quan trọng:**
- Tất cả chỉ số tài chính lưu theo **triệu VND**, 3 chữ số thập phân.
- `sheetname` lưu tên sheet trong file Excel để truy vết nguồn gốc.
- `ma_kh_cif` là mã định danh khách hàng nghiệp vụ (không phải PK DB).
- Có thể có nhiều dòng cùng `ma_kh_cif` nếu thuộc nhiều sản phẩm/kỳ hạn khác nhau.

---

### Bảng chỉ tiêu KPI: `public.thuc_hien_bsc_chi_nhanh`

Bảng lưu **chỉ tiêu kế hoạch BSC** theo chi nhánh / đơn vị cấp 6, dùng để so sánh thực tế với kế hoạch trên dashboard KPI.

```sql
CREATE TABLE public.thuc_hien_bsc_chi_nhanh (
    id                      serial4 NOT NULL,
    ngay                    date NULL,
    thang                   int2 NULL,
    quy                     varchar(10) NULL,
    nam                     int2 NOT NULL,
    ma_cn                   varchar(20) NULL,       -- Mã chi nhánh
    ten_cn                  varchar(255) NULL,      -- Tên chi nhánh
    thu_nhap_thuan_hdv_ftp  numeric(18, 3) DEFAULT 0 NULL,
    thu_nhap_thuan_dich_vu  numeric(18, 3) DEFAULT 0 NULL,
    thu_nhap_thuan_tin_dung numeric(18, 3) DEFAULT 0 NULL,
    thu_nhap_thuan          numeric(18, 3) DEFAULT 0 NULL,
    du_no_tin_dung_cuoi_ky  numeric(18, 3) DEFAULT 0 NULL,
    huy_dong_von_binh_quan  numeric(18, 3) DEFAULT 0 NULL,
    huy_dong_von_cuoi_ky    numeric(18, 3) DEFAULT 0 NULL,
    ngay_tao                timestamp DEFAULT CURRENT_TIMESTAMP NULL,
    casa_binh_quan          numeric(18, 3) DEFAULT 0 NULL,
    type_data               int4 DEFAULT 0 NULL,    -- 0 = kế hoạch, 1 = thực tế (hoặc theo quy ước nghiệp vụ)
    ma_don_vi_cap_6         varchar(20) NULL,
    ten_don_vi_cap_6        varchar(255) NULL,
    CONSTRAINT thuc_hien_bsc_chi_nhanh_pkey PRIMARY KEY (id)
);
```

**7 tiêu chí KPI lấy từ bảng này:**

| # | Tên KPI | Cột trong DB | Ghi chú |
|---|---|---|---|
| 1 | HĐV cuối kỳ | `huy_dong_von_cuoi_ky` | Huy động vốn số dư cuối kỳ |
| 2 | CASA bình quân | `casa_binh_quan` | Số dư CASA bình quân trong kỳ |
| 3 | Tổng TNT | `thu_nhap_thuan` | Tổng thu nhập thuần |
| 4 | Dư nợ tín dụng | `du_no_tin_dung_cuoi_ky` | Dư nợ tín dụng cuối kỳ |
| 5 | TNT từ dịch vụ | `thu_nhap_thuan_dich_vu` | Thu nhập thuần từ dịch vụ |
| 6 | TNT từ HĐV | `thu_nhap_thuan_hdv_ftp` | Thu nhập thuần từ huy động vốn (FTP) |
| 7 | TNT từ tín dụng | `thu_nhap_thuan_tin_dung` | Thu nhập thuần từ tín dụng |

**Cách sử dụng trên dashboard KPI:**
- Toàn bộ dữ liệu chỉ tiêu KPI (kế hoạch) và thực hiện đều lấy từ bảng `thuc_hien_bsc_chi_nhanh`.
- **`type_data`**:
  - `type_data = 0`: Chỉ tiêu BSC cả năm (kế hoạch năm).
  - `type_data = 1`: Dữ liệu thực hiện theo kỳ (tháng cụ thể được filter).
  - `type_data = 5`: Dữ liệu thực hiện theo kỳ (quý thể được filter).
  - `type_data = 6`: Dữ liệu thực hiện theo kỳ (năm thể được filter).
- **Lọc theo kỳ**: Khi filter theo Tháng, Năm hoặc Quý → lấy dòng có `type_data = 1` khớp với điều kiện filter (`nam`, `thang` hoặc `quy`).
- **Tính % hoàn thành KPI (`kpi-kh-pct`)**:
  - Tử số: giá trị cột tại dòng có `type_data = 1` theo kỳ được filter.
  - Mẫu số: giá trị cột tương ứng tại dòng BSC cả năm (`type_data = 0`, cùng `nam` và `ma_don_vi_cap_6`).
  - Công thức: `% hoàn thành = cột_thực_hiện (type_data=1) / cột_BSC_năm (type_data=0) * 100`
- **So sánh kỳ trước**: Lấy thêm dòng `type_data = 1` của kỳ liền trước để tính % tăng/giảm thực hiện so kỳ trước.
- Lọc theo `ma_don_vi_cap_6` để hiển thị KPI riêng từng phòng/đơn vị.

---

### View quản lý thẻ tín dụng: `public.v_the_phat_hanh`

View tổng hợp thông tin thẻ tín dụng đã phát hành, là nguồn dữ liệu **duy nhất** cho toàn bộ tính năng "Quản lý thẻ" (`/quan-ly-the`): danh sách thẻ, báo cáo thẻ chưa kích hoạt, báo cáo chưa phát sinh giao dịch (chưa PSGD), báo cáo doanh số miễn phí thường niên (PTN), và màn hình chi tiết thẻ (`/quan-ly-the/:id`).

**Nguồn dữ liệu (base tables):**
```sql
FROM the_phat_hanh tph
  LEFT JOIN card_client_code ccc ON ccc.client_name = tph.nhom_kh_the
  LEFT JOIN LATERAL (
    SELECT ... FROM card_fee_rule r
    WHERE r.code = tph.product_code AND ...
    ORDER BY (khớp thuoc_client_code trước, khac_client_code sau)
    LIMIT 1
  ) cfr ON true
```
- `the_phat_hanh`: bảng gốc, mỗi dòng là 1 thẻ đã phát hành (không lưu lịch sử/snapshot — chỉ có trạng thái hiện tại).
- `card_client_code`: map `nhom_kh_the` (tên nhóm KH) → `client_code`.
- `card_fee_rule`: bảng quy tắc phí thường niên theo `product_code`, dùng để suy ra `doanh_so_mien_ptn` (mức doanh số cần đạt để miễn phí thường niên) và `loai_match` (thẻ có thuộc client_code ưu đãi hay không).

**Các cột chính (đầy đủ trong view, ánh xạ 1-1 qua entity `ThePhatHanh`):**

| Nhóm | Cột | Ghi chú |
|---|---|---|
| Định danh thẻ | `id`, `card_id`, `so_the_da_phat_hanh`, `loai_the` (MAIN_CARD/…), `hinh_thuc_the` (PHYSICAL/DIGITAL), `plastic_status`, `ly_do_phat_hanh` | |
| Trạng thái | `trang_thai_the`, `so_ngay_chua_kich_hoat`, `ngay_phat_hanh_the`, `ngay_cap_nhat_trang_thai_card_contract` (≈ ngày kích hoạt), `ngay_cap_nhat_trang_thai_issuing_contract` (≈ ngày PSGD đầu tiên) | Không có bảng lịch sử/snapshot — ngày kích hoạt/PSGD suy ra trực tiếp từ 2 cột timestamp này, không phải tính delta. |
| Hợp đồng phát hành (IC) | `issuing_contract_nbr`, `hmtd_issuing_contract`, `thoi_han_hmtd`, `trang_thai_issuing_contract`, `am_issuing_contract`, `cn_qlt` (chi nhánh quản lý), `liab_top_contract` | |
| Chủ thẻ | `so_cif_khach_hang_pht`, `ho_ten_khach_hang_pht`, `cif_chu_the_chinh`, `ten_chu_the_chinh`, `sdt`, `email`, `so_gttt` (CMND/CCCD, đã mask), `sinh_trac_hoc_khach_hang` | |
| Phí thường niên / PTN | `doanh_so_giao_dich_mien_ptn` (doanh số thực tế), `doanh_so_mien_ptn` (mức cần đạt, từ `card_fee_rule.so_tien`), `so_tien_phi_thuong_nien`, `muc_phi_thuong_nien_the`, `ngay_thu_phi_thuong_nien_gan_nhat`, `dac_quyen_the` | % hoàn thành PTN = `doanh_so_giao_dich_mien_ptn / doanh_so_mien_ptn * 100`. |
| Khác | `am_card`, `ma_can_bo_gioi_thieu`, `kenh_phat_hanh`, `nhom_kh_the`, `client_code`, `thuoc_client_code`, `khac_client_code`, `loai_match`, `product_code`, `thoi_han_hieu_luc_the` | |
| Computed | `loai_the_tin_dung` (`CASE WHEN left(product_code,3) IN ('PVJ','PVC','PMC') THEN 'TDQT' ELSE 'KHAC'`) | |

**Giới hạn dữ liệu quan trọng** (không có nguồn nào trong DB, các màn hình liên quan phải hiển thị `—`):
- Không có **dư nợ hiện tại, lãi phải thu, phí phải thu, chi tiêu miễn phí, ngày chốt sao kê** — không có bảng dư nợ/sao kê thẻ.
- Không có **lịch sử/snapshot giao dịch theo thời gian** — `the_phat_hanh` chỉ lưu trạng thái hiện tại, không phù hợp để vẽ biểu đồ "doanh số theo ngày/tháng".
- Không có **mã lãi suất, hạng thẻ** — không có cột tương ứng ở bất kỳ bảng nào.
- `ly_do_phat_hanh`, `liab_top_contract`, `so_gttt` tồn tại sẵn ở bảng gốc `the_phat_hanh` nhưng phải **ALTER VIEW** để expose (đã thực hiện — append cuối danh sách cột để không phá vỡ `CREATE OR REPLACE VIEW`).

**Dùng ở đâu trong code:**
- Entity: `backend/src/main/java/com/mpa/entity/ThePhatHanh.java` (`@Table(name = "v_the_phat_hanh")`).
- DTO danh sách: `ThePhatHanhResponse`; DTO chi tiết: `ThePhatHanhDetailResponse`.
- API: `GET /api/the-phat-hanh` (danh sách, filter, phân trang), `GET /api/the-phat-hanh/{id}` (chi tiết), `GET /api/the-phat-hanh/summary` (KPI tổng quan).

---

## 5. Thiết Kế UI/UX (theo ảnh tham khảo)

### 5.1 Màu sắc & Branding
- **Primary color**: `#005BAA` (BIDV xanh dương đậm)
- **Accent/action**: `#009640` (xanh lá BIDV)
- **Background**: `#F4F6F9` (xám nhạt)
- **Sidebar**: `#1C3557` (navy dark)
- **Card background**: `#FFFFFF`
- **Text chính**: `#1A1A2E`
- **Text phụ**: `#6B7280`
- **Warning**: `#F59E0B` (vàng cam)
- **Danger/giảm**: `#EF4444` (đỏ)
- **Success/tăng**: `#10B981` (xanh lá)

### 5.2 Font
- **Font chính**: `Inter` hoặc `Roboto` (Google Fonts)
- Headings: `Inter SemiBold`
- Body: `Inter Regular`
- Số liệu tài chính: `Inter Medium`, monospace fallback

### 5.3 Layout chính
```
┌──────────────────────────────────────────────────────┐
│  HEADER: Logo BIDV | Breadcrumb | Notifications | User│
├──────────┬───────────────────────────────────────────┤
│          │  FILTER BAR: Khoảng thời gian | Phân loại │
│ SIDEBAR  ├───────────────────────────────────────────┤
│          │  KPI CARDS (6 cards ngang)                │
│  Nav:    ├───────────────────────────────────────────┤
│  - Tổng  │  BIỂU ĐỒ XU HƯỚNG (line chart)           │
│    quan  ├───────────────────────────────────────────┤
│  - DS    │  BIỂU ĐỒ TNT THEO PHÒNG (multi-line)     │
│    đơn   ├───────────────────────────────────────────┤
│  - Định  │  BẢNG SO SÁNH PHÒNG (bar chart + table)  │
│    danh  ├───────────────────────────────────────────┤
│  - SP &  │  BẢNG CHI TIẾT PHÒNG/AM                  │
│    DV    ├───────────────────────────────────────────┤
│  - Phân  │  BIẾN ĐỘNG KHÁCH HÀNG                    │
│    quyền │  Top 10 tăng | Top 10 giảm               │
│  - BC &  ├───────────────────────────────────────────┤
│    TK    │  TOP PHÒNG | TOP AM | TOP 8 KHÁCH HÀNG   │
│  - Cài   │                                           │
│    đặt   │                                           │
└──────────┴───────────────────────────────────────────┘
```

### 5.4 Các thành phần Dashboard chính

#### A. Filter Bar (bộ lọc toàn trang)
- **Khoảng thời gian**: Date picker range (từ ngày → đến ngày), preset: 7 ngày qua, 30 ngày, Quý, Năm
- **Phân loại**: Dropdown lọc theo `sheetname` hoặc `ma_phan_khuc_kh_cap_2`
- Nút **"Lưu tùy chọn"** và **"Đặt lại"**

#### B. KPI Cards (7 thẻ thống kê – nguồn từ `thuc_hien_bsc_chi_nhanh`)

Mỗi card hiển thị **giá trị thực tế** (từ `thuc_hien_bsc_chi_nhanh`) so với **chỉ tiêu kế hoạch** (từ `thuc_hien_bsc_chi_nhanh`), kèm % hoàn thành và xu hướng so kỳ trước.

| # | Card | Cột thực tế (`thuc_hien_bsc_chi_nhanh`) | Cột kế hoạch (`thuc_hien_bsc_chi_nhanh`) | Đơn vị hiển thị |
|---|---|---|---|---|
| 1 | HĐV cuối kỳ | `SUM(huy_dong_von_cuoi_ky)` | `huy_dong_von_cuoi_ky` | Tỷ VND |
| 2 | CASA bình quân | `SUM(huy_dong_von_binh_quan)` *(proxy)* | `casa_binh_quan` | Tỷ VND |
| 3 | Tổng TNT | `SUM(thu_nhap_thuan)` | `thu_nhap_thuan` | Tỷ VND |
| 4 | Dư nợ tín dụng | `SUM(du_no_tin_dung_cuoi_ky)` | `du_no_tin_dung_cuoi_ky` | Tỷ VND |
| 5 | TNT từ dịch vụ | `SUM(thu_nhap_thuan_dich_vu)` | `thu_nhap_thuan_dich_vu` | Tỷ VND |
| 6 | TNT từ HĐV | `SUM(thu_nhap_thuan_hdv_ftp)` | `thu_nhap_thuan_hdv_ftp` | Tỷ VND |
| 7 | TNT từ tín dụng | `SUM(thu_nhap_thuan_tin_dung)` | `thu_nhap_thuan_tin_dung` | Tỷ VND |

Mỗi card có:
- Icon bên trái
- Số liệu thực tế lớn + badge **% hoàn thành kế hoạch** (xanh ≥ 100%, vàng 80–99%, đỏ < 80%)
- Dòng phụ: giá trị kế hoạch + % tăng/giảm so kỳ trước
- Mini sparkline chart nhỏ phía dưới (xu hướng theo tháng)

#### C. Biểu đồ xu hướng hoạt động (Line Chart)
- **Dữ liệu**: `thu_nhap_thuan` theo `ngay`
- **Trục X**: Ngày (trong khoảng thời gian filter)
- **Trục Y**: Giá trị (triệu VND)
- **Các series**: Có thể so sánh nhiều phòng/tổng

#### D. Xu hướng TNT theo Phòng (Multi-line Chart)
- **Dữ liệu**: `SUM(thu_nhap_thuan)` GROUP BY `ten_don_vi_cap_6`, `thang`
- **Switch tab**: Quý | 20 ngày | 90 ngày | Tất cả
- **Dropdown**: Lọc Quý, Doanh số, Tổng TNT
- Mỗi phòng 1 màu đường riêng

#### E. Bảng So sánh Phòng – Tháng hiện tại
- **Horizontal bar chart**: Xếp hạng phòng theo `thu_nhap_thuan`
- Cột bên cạnh hiển thị giá trị số (đơn vị: tỷ VND)
- Màu xanh = tăng, đỏ = giảm so kỳ trước
- Toggle: Tổng KH | Dưới kế hoạch | Đạt kế hoạch

#### F. Bảng Chi tiết Phòng/AM
Bảng dạng ma trận với các cột:
| Cột | Nguồn |
|---|---|
| Phòng/AM | `ten_don_vi_cap_6` / `ten_am` |
| HĐV cuối ký | `SUM(huy_dong_von_cuoi_ky)` |
| Căn bằng quân | `SUM(huy_dong_von_binh_quan)` |
| Số NV | COUNT DISTINCT `ma_am` |
| TNT tổng KH | `SUM(thu_nhap_thuan)` |
| TNT trả KH | calculated |
| TNT tổng | final |

Có phân cấp: Phòng → danh sách AM bên trong (expand/collapse)

#### G. Biến động Khách hàng
- **Top 10 tăng**: Khách hàng có `thu_nhap_thuan` tăng mạnh nhất so kỳ trước
- **Top 10 giảm**: Khách hàng có `thu_nhap_thuan` giảm mạnh nhất
- Hiển thị: Rank, Tên KH, Mã CIF, Đơn vị, Giá trị thay đổi (+ màu)

#### H. Top Rankings (cuối trang)
- **Top phòng**: Xếp hạng `ten_don_vi_cap_6` theo `SUM(thu_nhap_thuan)`
- **Top cán bộ AM**: Xếp hạng `ten_am` theo `SUM(thu_nhap_thuan)`
- **Top 8 khách hàng**: Xếp hạng `ten_khach_hang` theo `SUM(thu_nhap_thuan)`

---

## 6. Phân Quyền Người Dùng (RBAC)

| Quyền | ADMIN | BRANCH_MANAGER | EMPLOYEE |
|---|---|---|---|
| Xem dashboard toàn hệ thống | ✅ | ❌ | ❌ |
| Xem dashboard chi nhánh mình | ✅ | ✅ | ❌ |
| Xem dữ liệu cá nhân (AM) | ✅ | ✅ | ✅ |
| Import file Excel MPA | ✅ | ✅ | ❌ |
| Xóa/sửa dữ liệu | ✅ | ❌ | ❌ |
| Xuất báo cáo Excel/PDF | ✅ | ✅ | ❌ |
| Quản lý người dùng | ✅ | ❌ | ❌ |
| Cài đặt hệ thống | ✅ | ❌ | ❌ |

**Roles enum (backend):**
```
ROLE_ADMIN          → Quản trị hệ thống
ROLE_BRANCH_MANAGER → Quản lý chi nhánh (chỉ xem dữ liệu chi nhánh mình)
ROLE_EMPLOYEE       → Nhân viên AM (chỉ xem dữ liệu cá nhân)
```

---

## 7. Import File Excel (Luồng nghiệp vụ)

### Cấu trúc file Excel mẫu
File `.xlsx` có thể có nhiều sheet. Mỗi sheet tương ứng với 1 loại dữ liệu (giá trị `sheetname`).

**Mapping cột Excel → bảng DB:**
| Cột trong Excel | Cột trong DB |
|---|---|
| Ngày | `ngay` |
| Tháng | `thang` |
| Quý | `quy` |
| Năm | `nam` |
| Mã AM | `ma_am` |
| Tên AM | `ten_am` |
| Mã đơn vị cấp 6 | `ma_don_vi_cap_6` |
| Tên đơn vị cấp 6 | `ten_don_vi_cap_6` |
| Mã SP cấp 5 | `ma_sp_cap_5` |
| Tên SP cấp 5 | `ten_sp_cap_5` |
| Mã phân khúc KH cấp 2 | `ma_phan_khuc_kh_cap_2` |
| Tên phân khúc KH cấp 2 | `ten_phan_khuc_kh_cap_2` |
| Mã KH CIF | `ma_kh_cif` |
| Tên khách hàng | `ten_khach_hang` |
| Kỳ hạn cấp 2 | `ky_han_cap_2` |
| Thu nhập thuần HĐV FTP | `thu_nhap_thuan_hdv_ftp` |
| Thu nhập thuần dịch vụ | `thu_nhap_thuan_dich_vu` |
| Thu nhập thuần tín dụng | `thu_nhap_thuan_tin_dung` |
| Thu nhập thuần | `thu_nhap_thuan` |
| Dư nợ tín dụng cuối kỳ | `du_no_tin_dung_cuoi_ky` |
| HĐV bình quân | `huy_dong_von_binh_quan` |
| HĐV cuối kỳ | `huy_dong_von_cuoi_ky` |

### Luồng import
1. Người dùng upload file `.xlsx` từ giao diện
2. Frontend preview danh sách sheets và số dòng
3. Người dùng chọn sheet(s) cần import, xác nhận
4. Backend (`POST /api/mpa/import`) đọc file bằng Apache POI
5. Validate dữ liệu (kiểm tra `ma_kh_cif` không rỗng, kiểu số hợp lệ)
6. Bulk insert vào `du_lieu_mpa` (batch size 500)
7. Trả về kết quả: số dòng thành công / lỗi / bỏ qua
8. Lưu log import

---

## 8. API Endpoints (tóm tắt)

### Auth
| Method | Path | Mô tả |
|---|---|---|
| POST | `/api/auth/login` | Đăng nhập, trả về JWT |
| POST | `/api/auth/refresh` | Refresh token |
| POST | `/api/auth/logout` | Logout |

### Dữ liệu MPA
| Method | Path | Mô tả |
|---|---|---|
| GET | `/api/mpa` | Danh sách dữ liệu (filter, phân trang) |
| POST | `/api/mpa/import` | Import file Excel |
| DELETE | `/api/mpa/{id}` | Xóa bản ghi |
| DELETE | `/api/mpa/batch` | Xóa nhiều bản ghi |

### Dashboard & Báo cáo
| Method | Path | Mô tả |
|---|---|---|
| GET | `/api/dashboard/kpi` | KPI tổng quan (count, sum, % thay đổi) |
| GET | `/api/dashboard/trend` | Xu hướng theo ngày/tháng |
| GET | `/api/dashboard/by-phong` | So sánh theo phòng/đơn vị |
| GET | `/api/dashboard/top-am` | Top cán bộ AM |
| GET | `/api/dashboard/top-kh` | Top khách hàng |
| GET | `/api/dashboard/bien-dong-kh` | Biến động khách hàng (tăng/giảm) |
| GET | `/api/reports/export/excel` | Xuất báo cáo Excel |
| GET | `/api/reports/export/pdf` | Xuất báo cáo PDF |

### Quản trị
| Method | Path | Mô tả |
|---|---|---|
| GET | `/api/users` | Danh sách người dùng |
| POST | `/api/users` | Tạo người dùng |
| PUT | `/api/users/{id}` | Cập nhật người dùng |
| DELETE | `/api/users/{id}` | Xóa người dùng |

---

## 9. Quy Ước Code

### Backend (Java / Spring Boot)
- **Naming**: camelCase cho biến/method, PascalCase cho class
- **Entity fields**: snake_case trong DB, camelCase trong Java (dùng `@Column(name = "...")`)
- **DTO**: Tách riêng Request DTO và Response DTO
- **Exception handling**: Dùng `@RestControllerAdvice` với `GlobalExceptionHandler`
- **Response format chuẩn:**
  ```json
  {
    "success": true,
    "message": "...",
    "data": { ... },
    "timestamp": "2024-01-01T00:00:00"
  }
  ```
- **Pagination**: Dùng Spring Pageable, trả về `Page<T>`

### Frontend (Angular / TypeScript)
- **Naming**: camelCase cho biến, PascalCase cho class/interface, kebab-case cho file
- **Interfaces**: Đặt trong `models/` hoặc `shared/models/`
- **Services**: Mỗi feature có service riêng, gọi API qua `HttpClient`
- **State management**: Dùng RxJS `BehaviorSubject` (không dùng NgRx trừ khi cần)
- **Style**: SCSS riêng cho mỗi component, biến màu dùng CSS custom properties
- **Số tài chính**: Format với pipe `| number:'1.0-3'` và thêm đơn vị "tỷ" hoặc "triệu"

---

## 10. Environment & Configuration

### Backend `application.yml`
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/mpa_db
    username: ${DB_USERNAME:postgres}
    password: ${DB_PASSWORD:postgres}
  jpa:
    hibernate:
      ddl-auto: validate  # KHÔNG dùng create/create-drop trên production
    show-sql: false
  servlet:
    multipart:
      max-file-size: 50MB
      max-request-size: 50MB

app:
  jwt:
    secret: ${JWT_SECRET:your-secret-key-here}
    expiration: 86400000       # 24 giờ
    refresh-expiration: 604800000  # 7 ngày
  cors:
    allowed-origins:
      - http://localhost:4200
```

### Frontend `environment.ts`
```typescript
export const environment = {
  production: false,
  apiUrl: 'http://localhost:8080/api',
  appName: 'BIDV MPA Dashboard',
  version: '1.0.0'
};
```

---

## 11. Lưu Ý Quan Trọng

> ⚠️ **Không tự ý thay đổi schema DB** – Mọi thay đổi cần thông qua migration script (Flyway hoặc Liquibase).

> ⚠️ **Dữ liệu tài chính là nhạy cảm** – Luôn kiểm tra phân quyền ở cả frontend (route guard) và backend (Spring Security).

> ⚠️ **Import Excel có thể lớn** – File có thể tới 50MB với hàng chục nghìn dòng. Phải dùng streaming (SXSSFWorkbook/SAX parser) để tránh OutOfMemory.

> 💡 **Đơn vị tài chính**: Toàn bộ hiển thị UI dùng đơn vị **tỷ VND** (chia giá trị DB cho 1000), nhưng lưu DB là **triệu VND** (3 chữ số thập phân).

> 💡 **Múi giờ**: Toàn bộ timestamp dùng `Asia/Ho_Chi_Minh` (UTC+7). Cấu hình `spring.jpa.properties.hibernate.jdbc.time_zone=Asia/Ho_Chi_Minh`.
