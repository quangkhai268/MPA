# Nguyên tắc tải dữ liệu MPA (`/tai-du-lieu-len`)

> Tổng hợp toàn bộ nguyên tắc đã xây dựng cho việc import file **MPA** vào bảng `du_lieu_mpa` qua menu "Tải dữ liệu lên". Phần ISS_02 (thẻ) đã có từ trước, xem chi tiết ở `quanlythe.md` mục 2 — file này chỉ tập trung vào MPA. Tham khảo thêm `CLAUDE.md` mục 4 (schema `du_lieu_mpa`) và mục 7 (luồng import gốc).

---

## 1. Tổng quan menu

Component dùng chung cho mọi loại file: `tai-du-lieu-len.component.ts/html/scss`. Nhận file `.xlsx/.xls/.zip` (zip tự giải nén trong bộ nhớ), không có UI chọn loại file — **loại file được suy ra tự động**:
- Ở **tầng ngoài**: `UploadServiceImpl.classify(fileName)` nhận diện file MPA nếu tên file chứa chuỗi `"MPA"` (không phân biệt hoa/thường) → route sang `DuLieuMpaImportService`.
- Ở **tầng trong**: 4 loại kỳ con của MPA (Tháng/Quý/Năm/Ngày) được nhận diện dựa vào **header dòng đầu tiên trong file**, không dựa vào tên file — xem mục 2.

API không đổi so với ISS_02: `POST /api/upload` (multipart `files[]` + `ngayDuLieu` + `nguoiUpload`), `GET /api/upload/history`.

---

## 2. Cấu trúc file Excel MPA — 4 loại kỳ

File MPA có 4 biến thể, phân biệt bằng **cột đầu tiên**. Toàn bộ đều là dữ liệu **lũy tiến (cumulative)** từ đầu kỳ đến thời điểm chốt:

| Loại (`loai_ky`) | Cột đầu tiên | Định dạng ví dụ | Khoảng lũy tiến |
|---|---|---|---|
| `THANG` | `Tháng` | `"2026 / 05"` | 1 tháng trọn vẹn (VD T5: 1/5–31/5) |
| `QUY` | `Quý` | `"2026-Q1"` | 1 quý trọn vẹn (VD Q1: 1/1–31/3) |
| `NAM` | `Năm` | `"2026"` | Từ đầu năm đến hết tháng gần nhất đã đóng — **mốc di chuyển theo từng lần import** |
| `NGAY` | *(không có cột kỳ — cột đầu là `Mã - Tên AM`)* | — | Từ đầu năm đến T-1 (luôn là hôm qua) |

14 cột còn lại **giống hệt nhau ở cả 4 loại**: `Mã - Tên AM`, `Mã - Tên đơn vị tổ chức cấp 6`, `Mã - Tên SP cấp 5`, `Mã - Tên phân khúc KH cấp 2`, `Mã KH (CIF)`, `Tên khách hàng`, `Kỳ hạn cấp 2`, và 7 chỉ số tài chính (Thu nhập thuần từ HĐV/dịch vụ/tín dụng, Thu nhập thuần, Dư nợ tín dụng cuối kỳ, Huy động vốn bình quân/cuối kỳ). Việc map cột dựa theo **tên header** (đã chuẩn hoá khoảng trắng), không theo vị trí — không bị ảnh hưởng khi có/không có cột kỳ ở đầu.

**Thứ tự nhận diện loại kỳ** (trong `DuLieuMpaImportServiceImpl.processHeaderRow()`): có cột `"Tháng"` → THANG; else có `"Quý"` → QUY; else có `"Năm"` → NAM; else có `"Mã - Tên AM"` (không có cả 3 cột trên) → NGAY; không khớp gì → `FAILED`.

---

## 3. Quy tắc điền cột theo từng loại kỳ

| `loai_ky` | `ngay` | `thang` | `quy` | `nam` | Nguồn giá trị |
|---|---|---|---|---|---|
| `THANG` | NULL | tách từ cột "Tháng" (vế sau `/`) | NULL | tách từ cột "Tháng" (vế trước `/`) | nội dung file |
| `QUY` | NULL | NULL | `"Q" + số quý` từ cột "Quý" | tách từ cột "Quý" | nội dung file |
| `NAM` | NULL | **tách từ TÊN FILE** (token `T<số>`, VD `T5`) | NULL | từ cột "Năm" | nội dung file + tên file |
| `NGAY` | hôm nay − 1 ngày | NULL | NULL | năm của `ngay` | tự sinh lúc import, không đọc từ file |

**Vì sao NAM cần tách tháng từ tên file**: file Năm chỉ có cột "Năm" (không có cột tháng), nhưng lại là dữ liệu lũy kế "đến 1 tháng đang di chuyển" — cần biết chính xác lũy kế đến tháng nào. Quy ước bắt buộc: **tên file phải chứa `T<tháng>`**, ví dụ `MPA 2026 (lũy kế đến T5).xlsx` → tháng 5. Quét toàn bộ token `T\d{1,2}` trong tên file, lấy **khớp cuối cùng**, validate 1–12; không tìm được → `FAILED` với thông báo yêu cầu đặt tên đúng quy ước.

---

## 4. Cột `loai_ky` — bắt buộc để tránh đếm trùng KPI

Bảng `du_lieu_mpa` **không có sẵn** cột phân loại kỳ. Vì cột `nam` được điền ở **cả 4 loại**, nếu không phân biệt được nguồn gốc, các query SUM theo năm sẽ cộng gộp nhầm dữ liệu Tháng + Quý + Năm + Ngày của cùng 1 năm → sai KPI nghiêm trọng. Đã thêm cột `loai_ky varchar(10)` (giá trị `THANG`/`QUY`/`NAM`/`NGAY`) vào `du_lieu_mpa`, và **toàn bộ query gộp/SUM/COUNT theo kỳ** trong `DuLieuMpaRepository` đều bắt buộc có điều kiện `AND d.loaiKy = '...'`.

Dữ liệu cũ (~410k dòng nạp thủ công trước khi có tính năng import) được backfill `loai_ky='THANG'`; ~245k dòng cũ khác có `nam`/`thang` đều NULL được giữ `loai_ky` NULL (vốn dĩ đã "vô hình" với mọi query lọc theo kỳ, không đổi hành vi).

---

## 5. Quy tắc thay thế (replace) khi commit — khác nhau theo loại

Dùng cơ chế **staging table** (`du_lieu_mpa_staging`) giống ISS_02: stage toàn bộ file trong phiên trước, chỉ khi có ít nhất 1 file thành công mới `commitStagedData()` trong **1 transaction** (nếu lỗi giữa chừng, rollback toàn bộ — dữ liệu cũ không mất). Khác biệt với ISS_02 (TRUNCATE toàn bảng): mỗi `loai_ky` có phạm vi xoá riêng, vì Tháng/Quý là kỳ đã đóng cố định còn Năm/Ngày là lũy kế tới mốc di chuyển:

| `loai_ky` | Phạm vi DELETE trước khi INSERT | Lý do |
|---|---|---|
| `THANG` | Khớp đúng `(nam, thang)` | Kỳ đã đóng cố định — chỉ sửa/thay đúng tháng đó, các tháng khác giữ nguyên |
| `QUY` | Khớp đúng `(nam, quy)` | Tương tự Tháng |
| `NAM` | Khớp đúng `(nam, thang)` — `thang` ở đây là **mốc lũy kế** | Cho phép giữ **nhiều mốc lũy kế cùng lúc** (đến T3, T4, T5...) — xem mục 6 |
| `NGAY` | Khớp `nam` (toàn bộ) | Mỗi lần import là bản lũy kế đầu năm→T-1 hoàn toàn mới, không cộng dồn theo từng ngày |

---

## 6. Giữ nhiều mốc lũy kế cho loại Năm

**Vấn đề đã phát hiện khi dùng thực tế**: ban đầu loại NAM chỉ xoá theo `nam` (giống Ngày) — nghĩa là import file "đến T6" sẽ xoá mất dữ liệu "đến T5" đã import trước đó, không còn cách nào xem lại báo cáo "5 tháng đầu năm". Đã sửa: NAM giờ xoá theo `(nam, thang)` — thang chính là mốc lũy kế tách từ tên file (mục 3) — nên các mốc **khác nhau** của cùng 1 năm coi là **độc lập, cùng tồn tại**; chỉ import lại đúng mốc cũ (cùng tên file `T<n>`) mới thay thế mốc đó.

**Giới hạn phạm vi quan trọng**: chỉ có **1 nơi trong code thực sự dùng dữ liệu Năm từ `du_lieu_mpa`** — `DashboardServiceImpl.mpaAmKh()`, cấp cột "Số KH" trong bảng so sánh phòng (`GET /api/dashboard/by-phong`). Các KPI card chính, biểu đồ xu hướng, AM detail khi lọc "Năm" đều đọc từ `thuc_hien_bsc_chi_nhanh` (bảng KPI riêng, ngoài phạm vi — xem mục 9). Nghĩa là ô chọn phụ "Lũy kế đến tháng" trên dashboard **chỉ ảnh hưởng cột Số KH**, chưa ảnh hưởng toàn bộ dashboard.

Dashboard: khi `loaiKy=nam`, thêm ô chọn phụ **"Lũy kế đến tháng"** (mặc định = mốc mới nhất, lấy qua `GET /api/dashboard/nam-luy-ke-options?nam=...`), gửi kèm `denThangLuyKe` khi gọi `GET /api/dashboard/by-phong`. Nếu không truyền, backend tự lấy mốc mới nhất (`MAX(thang)`).

Các method liên quan trong `DuLieuMpaRepository` đã đổi tên + bắt buộc tham số `thang`: `sumByNamLuyKe`, `phongByNamLuyKe`, `phongKhCountByNamLuyKe`, `cifSumByNamLuyKe`, `phongBenchmarkByNamLuyKe` (trước đó không có tham số `thang` — nguy hiểm vì 1 năm có thể có nhiều mốc).

---

## 7. Cột "Tên file" ở lịch sử upload

Mỗi file MPA import thành công tự sinh 1 **nhãn kỳ thân thiện** (`kyLabel`), build trong `DuLieuMpaImportServiceImpl` từ kỳ của **dòng dữ liệu hợp lệ đầu tiên**:

| Loại | Ví dụ nhãn |
|---|---|
| THANG | `Tháng 5/2026` |
| QUY | `Quý 1/2026` |
| NAM | `Năm 2026 (lũy kế đến T5)` |
| NGAY | `Ngày 21/07/2026` |

File không phải MPA (ISS_02...) hoặc không xác định được kỳ → hiển thị tên file gốc. 1 phiên upload có thể có nhiều file → cột "Tên file" ở lịch sử upload gộp nhãn các file **thành công** trong phiên đó, cách nhau bởi dấu phẩy (`UploadServiceImpl.buildKyLabel()`).

---

## 8. Đọc file — SAX streaming

Vì mỗi file MPA có thể hơn 100 nghìn dòng, dùng **SAX streaming** (`XSSFReader` + `XSSFSheetXMLHandler`, không dùng DOM `WorkbookFactory` như ISS_02) để tránh tốn bộ nhớ/thời gian đọc. Batch insert vào staging qua `JdbcTemplate.batchUpdate`, lô 500 dòng. Chỉ hỗ trợ `.xlsx` (OOXML) — file `.xls` cũ (BIFF8, giới hạn 65.536 dòng) không tương thích, trả `FAILED` rõ ràng thay vì fallback.

Dòng bị bỏ qua (không tính lỗi cả file) khi: `Mã KH (CIF)` rỗng, hoặc kỳ không parse được (Tháng/Quý/Năm sai định dạng). Cột `"Mã - Tên X"` không có dấu `" - "` thì giữ nguyên chuỗi làm mã, để tên trống (không fail dòng).

---

## 9. Trạng thái DB — script đã chạy / đang chờ

| Script | Nội dung | Trạng thái |
|---|---|---|
| `backend/sql/du_lieu_mpa_import_setup.sql` | Thêm cột `loai_ky`, backfill dữ liệu cũ, tạo bảng `du_lieu_mpa_staging`, tạo index | ✅ Đã chạy |
| `backend/sql/upload_history_ky_label.sql` | Thêm cột `ky_label varchar(255)` vào `upload_history` | ⏳ **Chưa chạy** — lịch sử upload đang lỗi vì backend đã restart với code mới nhưng thiếu cột này |

**Việc tồn đọng cần xử lý thủ công trên `mpa_db`:**
1. **Sequence lệch**: `du_lieu_mpa_id_seq` đang ở giá trị `2` trong khi `MAX(id)` thực tế là `661925` (do dữ liệu cũ nạp thủ công không qua sequence) → import sẽ lỗi `duplicate key value violates unique constraint "du_lieu_mpa_pkey"`. Sửa: `SELECT setval('du_lieu_mpa_id_seq', (SELECT MAX(id) FROM du_lieu_mpa));`
2. **146.578 dòng `loai_ky='NAM'` với `thang IS NULL`**: từ lần import file Năm trước khi có logic tách tháng từ tên file — dữ liệu "mồ côi", không khớp mốc lũy kế nào. Nên xoá rồi import lại: `DELETE FROM du_lieu_mpa WHERE loai_ky = 'NAM' AND thang IS NULL;`
3. Chạy `upload_history_ky_label.sql` ở trên.

---

## 10. Ngoài phạm vi (để plan riêng sau)

Snapshot dữ liệu từ `du_lieu_mpa` sang `thuc_hien_bsc_chi_nhanh` / `thuc_hien_bsc_khach_hang` (2 bảng KPI dashboard chính) — cả hai hiện **100% read-only từ code** (không có service/scheduler nào ghi vào), populate thủ công qua script DB ngoài. Cần điều tra kỹ ý nghĩa từng giá trị `type_data` (0/1/2/3/5/6) trước khi thiết kế đường ghi, vì đây là dữ liệu KPI ngân hàng đang dùng thật.

---

## 11. File liên quan

**Backend:**
- `backend/sql/du_lieu_mpa_import_setup.sql`, `backend/sql/upload_history_ky_label.sql`
- `backend/src/main/java/com/mpa/service/DuLieuMpaImportService.java` + `impl/DuLieuMpaImportServiceImpl.java`
- `backend/src/main/java/com/mpa/entity/DuLieuMpa.java` (field `loaiKy`)
- `backend/src/main/java/com/mpa/repository/DuLieuMpaRepository.java`
- `backend/src/main/java/com/mpa/service/impl/UploadServiceImpl.java`
- `backend/src/main/java/com/mpa/dto/FileImportResult.java` (field `kyLabel`)
- `backend/src/main/java/com/mpa/entity/UploadHistory.java`, `backend/src/main/java/com/mpa/dto/UploadHistoryResponse.java`
- `backend/src/main/java/com/mpa/service/DashboardService.java` + `impl/DashboardServiceImpl.java`, `backend/src/main/java/com/mpa/controller/DashboardController.java` (endpoint `nam-luy-ke-options`, tham số `denThangLuyKe`)

**Frontend:**
- `frontend/src/app/features/tai-du-lieu-len/tai-du-lieu-len.component.html` (cột "Tên file")
- `frontend/src/app/core/models/upload.model.ts` (`UploadHistoryItem.kyLabel`)
- `frontend/src/app/features/dashboard/dashboard.component.ts` + `.html` (ô chọn "Lũy kế đến tháng")
- `frontend/src/app/core/models/mpa.model.ts` (`FilterParams.denThangLuyKe`)
- `frontend/src/app/core/services/mpa.service.ts` (`getNamLuyKeOptions`)
