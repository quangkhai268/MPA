-- Thêm cột hiển thị "Tên file" dạng nhãn kỳ thân thiện (VD "Tháng 5/2026", "Quý 1/2026")
-- trong bảng "Lịch sử upload". Chạy TAY (không tự động từ code) theo CLAUDE.md §11.

ALTER TABLE public.upload_history ADD COLUMN ky_label varchar(255) NULL;
