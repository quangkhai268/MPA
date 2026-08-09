-- Thêm cột ngay_thu_phi_thuong_tien_tiep_theo (date) vào the_phat_hanh + staging.
-- Giá trị được TÍNH TỰ ĐỘNG ở bước commitStagedData() (Java) khi import ISS_02, KHÔNG tính
-- ở đây — cột chỉ cần tồn tại sẵn trong schema trước khi import chạy.
-- Chạy TAY (không tự động từ code) theo CLAUDE.md §11.

ALTER TABLE public.the_phat_hanh ADD COLUMN ngay_thu_phi_thuong_tien_tiep_theo date NULL;
ALTER TABLE public.the_phat_hanh_staging ADD COLUMN ngay_thu_phi_thuong_tien_tiep_theo date NULL;

-- Expose qua view v_the_phat_hanh — append cuối danh sách cột (giữ nguyên convention hiện có)
-- để không phá vỡ các chỗ khác đang SELECT * hoặc dựa vào thứ tự cột cũ.
CREATE OR REPLACE VIEW public.v_the_phat_hanh AS
SELECT tph.id,
    tph.cn_qlt,
    tph.so_cif_khach_hang_pht,
    tph.ho_ten_khach_hang_pht,
    tph.sinh_trac_hoc_khach_hang,
    tph.sdt,
    tph.email,
    tph.thoi_han_hmtd,
    tph.issuing_contract_nbr,
    tph.product_code,
    tph.trang_thai_issuing_contract,
    tph.cif_chu_the_chinh,
    tph.ten_chu_the_chinh,
    tph.hmtd_issuing_contract,
    tph.am_issuing_contract,
    tph.loai_the,
    tph.so_the_da_phat_hanh,
    tph.card_id,
    tph.trang_thai_the,
    tph.hinh_thuc_the,
    tph.plastic_status,
    tph.so_ngay_chua_kich_hoat,
    tph.ngay_phat_hanh_the,
    tph.thoi_han_hieu_luc_the,
    tph.am_card,
    tph.ma_can_bo_gioi_thieu,
    tph.doanh_so_giao_dich_mien_ptn,
    tph.ngay_thu_phi_thuong_nien_gan_nhat,
    tph.muc_phi_thuong_nien_the,
    tph.so_tien_phi_thuong_nien,
    tph.dac_quyen_the,
    tph.kenh_phat_hanh,
    tph.nhom_kh_the,
    tph.ngay_cap_nhat_trang_thai_issuing_contract,
    tph.ngay_cap_nhat_trang_thai_card_contract,
    ccc.client_code,
    cfr.thuoc_client_code,
    cfr.khac_client_code,
    cfr.loai_match,
    cfr.so_tien AS doanh_so_mien_ptn,
    CASE
        WHEN "left"(tph.product_code::text, 3) = ANY (ARRAY['PVJ'::text, 'PVC'::text, 'PMC'::text]) THEN 'TDQT'::text
        ELSE 'KHAC'::text
    END AS loai_the_tin_dung,
    tph.ly_do_phat_hanh,
    tph.liab_top_contract,
    tph.so_gttt,
    tph.ngay_thu_phi_thuong_tien_tiep_theo
   FROM the_phat_hanh tph
     LEFT JOIN card_client_code ccc ON ccc.client_name::text = tph.nhom_kh_the::text
     LEFT JOIN LATERAL ( SELECT r.thuoc_client_code,
            r.khac_client_code,
            r.so_tien,
                CASE
                    WHEN ((','::text || r.thuoc_client_code) || ','::text) ~~ (('%,'::text || ccc.client_code::text) || ',%'::text) THEN 'THUOC'::text
                    ELSE 'KHAC'::text
                END AS loai_match
           FROM card_fee_rule r
          WHERE r.code::text = tph.product_code::text AND ccc.client_code IS NOT NULL AND (((','::text || r.thuoc_client_code) || ','::text) ~~ (('%,'::text || ccc.client_code::text) || ',%'::text) OR r.khac_client_code IS NOT NULL AND btrim(r.khac_client_code) <> ''::text)
          ORDER BY (
                CASE
                    WHEN ((','::text || r.thuoc_client_code) || ','::text) ~~ (('%,'::text || ccc.client_code::text) || ',%'::text) THEN 0
                    ELSE 1
                END)
         LIMIT 1) cfr ON true;

-- Backfill dữ liệu hiện có (các dòng vốn NULL vì import trước đó chưa có cột này) — dùng ĐÚNG
-- công thức đang chạy trong ThePhatHanhImportServiceImpl.commitStagedData():
--   • Card Auto-Closed / Card Closed / Card Fraud / Card Lost => NULL
--   • ngay_thu_phi_thuong_nien_gan_nhat còn ở tương lai so với hôm nay (CURRENT_DATE) => giữ
--     nguyên (chưa cộng năm)
--   • còn lại => + 1 năm
UPDATE public.the_phat_hanh
SET ngay_thu_phi_thuong_tien_tiep_theo = CASE
        WHEN trang_thai_the IN ('Card Auto-Closed', 'Card Closed', 'Card Fraud', 'Card Lost') THEN NULL
        WHEN CURRENT_DATE < ngay_thu_phi_thuong_nien_gan_nhat THEN ngay_thu_phi_thuong_nien_gan_nhat
        ELSE (ngay_thu_phi_thuong_nien_gan_nhat + INTERVAL '1 year')::date
    END;
