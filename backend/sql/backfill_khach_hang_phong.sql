-- Backfill ma_don_vi_cap_6 / ten_don_vi_cap_6 cho các dòng thuc_hien_bsc_khach_hang
-- đã đồng bộ trước khi BscSyncServiceImpl được sửa để ghi 2 cột này (2026-07-23).
-- Chỉ điền thêm cho các dòng đang NULL — không đụng tới số liệu tài chính.

UPDATE thuc_hien_bsc_khach_hang t
SET ma_don_vi_cap_6 = s.ma_don_vi_cap_6,
    ten_don_vi_cap_6 = s.ten_don_vi_cap_6
FROM (
    SELECT ma_kh_cif, ma_am,
           MAX(ma_don_vi_cap_6) AS ma_don_vi_cap_6,
           MAX(replace(ten_don_vi_cap_6, '_CN TAY HO', '')) AS ten_don_vi_cap_6
    FROM du_lieu_mpa
    WHERE ma_don_vi_cap_6 IS NOT NULL
    GROUP BY ma_kh_cif, ma_am
) s
WHERE t.ma_kh_cif = s.ma_kh_cif
  AND (t.ma_am = s.ma_am OR (t.ma_am IS NULL AND s.ma_am IS NULL))
  AND t.ma_don_vi_cap_6 IS NULL;
