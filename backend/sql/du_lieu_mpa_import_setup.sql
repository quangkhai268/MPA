-- ============================================================================
-- Setup cho tính năng import du_lieu_mpa (4 loại kỳ: Tháng/Quý/Năm/Ngày)
-- Chạy TAY (không tự động từ code) — theo CLAUDE.md §11 (không tự ý đổi schema).
-- Review kỹ trước khi chạy trên mpa_db.
-- ============================================================================

-- 1) Thêm cột phân loại kỳ vào bảng sống
--    Giá trị hợp lệ: 'THANG', 'QUY', 'NAM', 'NGAY'
--    Mục đích: các loại kỳ khác nhau đều populate cột "nam", nếu không phân
--    biệt được loại thì các query SUM/COUNT theo năm (vd DuLieuMpaRepository.sumByNam)
--    sẽ cộng gộp nhầm dữ liệu Tháng + Quý + Năm + Ngày của cùng 1 năm.
ALTER TABLE public.du_lieu_mpa ADD COLUMN loai_ky varchar(10) NULL;

-- 2) Backfill dữ liệu cũ đang có dạng "Tháng" (thang+nam có giá trị, quy/ngay NULL)
--    Đây là toàn bộ ~410k dòng nạp thủ công trước đây (nam=2026, thang 2-5).
UPDATE public.du_lieu_mpa
SET loai_ky = 'THANG'
WHERE thang IS NOT NULL AND nam IS NOT NULL AND quy IS NULL AND ngay IS NULL;

-- ~245k dòng cũ có nam/thang đều NULL: CỐ Ý không gán loai_ky (giữ NULL).
-- Các dòng này vốn dĩ đã không khớp bất kỳ filter theo kỳ nào trong dashboard hiện tại
-- (WHERE thang=x AND nam=y sẽ không bao giờ match NULL), nên việc thêm điều kiện
-- loai_ky vào các query sau này không làm thay đổi hành vi đối với nhóm dòng này.

-- 3) Bảng staging cho luồng import mới — mirror cột bảng sống + loai_ky.
--    Dùng để nạp dữ liệu tạm trước khi commit (xem DuLieuMpaImportServiceImpl),
--    tránh việc file lỗi giữa chừng làm hỏng dữ liệu đang có trên bảng sống.
CREATE TABLE public.du_lieu_mpa_staging (
    id                      serial4 NOT NULL,
    loai_ky                 varchar(10) NULL,
    ngay                    date NULL,
    thang                   int2 NULL,
    quy                     varchar(10) NULL,
    nam                     int2 NULL,
    ma_am                   varchar(20) NULL,
    ten_am                  varchar(255) NULL,
    ma_don_vi_cap_6         varchar(20) NULL,
    ten_don_vi_cap_6        varchar(255) NULL,
    ma_sp_cap_5             varchar(20) NULL,
    ten_sp_cap_5            varchar(255) NULL,
    ma_phan_khuc_kh_cap_2   varchar(255) NULL,
    ten_phan_khuc_kh_cap_2  varchar(255) NULL,
    ma_kh_cif               varchar(50) NOT NULL,
    ten_khach_hang          varchar(255) NULL,
    ky_han_cap_2            varchar(100) NULL,
    thu_nhap_thuan_hdv_ftp  numeric(18,3) DEFAULT 0 NULL,
    thu_nhap_thuan_dich_vu  numeric(18,3) DEFAULT 0 NULL,
    thu_nhap_thuan_tin_dung numeric(18,3) DEFAULT 0 NULL,
    thu_nhap_thuan          numeric(18,3) DEFAULT 0 NULL,
    du_no_tin_dung_cuoi_ky  numeric(18,3) DEFAULT 0 NULL,
    huy_dong_von_binh_quan  numeric(18,3) DEFAULT 0 NULL,
    huy_dong_von_cuoi_ky    numeric(18,3) DEFAULT 0 NULL,
    ngay_tao                timestamp DEFAULT CURRENT_TIMESTAMP NULL,
    sheetname               varchar(255) NULL,
    CONSTRAINT du_lieu_mpa_staging_pkey PRIMARY KEY (id)
);

-- 4) Index phục vụ cả DELETE-theo-kỳ lúc commit lẫn các query dashboard đọc theo kỳ
CREATE INDEX idx_du_lieu_mpa_loai_ky ON public.du_lieu_mpa (loai_ky, nam, thang, quy, ngay);
