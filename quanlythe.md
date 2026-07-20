# Chức năng Quản lý thẻ (`quan-ly-the`)

> Tổng hợp toàn bộ nội dung đã xây dựng cho menu **Quản lý thẻ tín dụng** — nguồn dữ liệu, các màn hình, API, và hệ thống cảnh báo/chiến dịch tự động. Tham khảo thêm `CLAUDE.md` mục "View quản lý thẻ tín dụng: v_the_phat_hanh" cho chi tiết schema DB gốc.

---

## 1. Nguồn dữ liệu

- **View `v_the_phat_hanh`**: nguồn dữ liệu thẻ duy nhất, join từ `the_phat_hanh` (bảng gốc, do ETL ngoài quản lý, chỉ lưu trạng thái hiện tại — không có lịch sử) + `card_client_code` + `card_fee_rule`.
- **Giới hạn quan trọng**: không có dư nợ/lãi/phí phải thu, không có lịch sử giao dịch theo thời gian, không có mã lãi suất/hạng thẻ — các màn hình liên quan hiển thị `—` cho các trường này.
- Entity backend: `ThePhatHanh` (`@Table(name = "v_the_phat_hanh")`).

---

## 2. Tải dữ liệu lên (`/tai-du-lieu-len`, admin only)

> Trước đây `the_phat_hanh` (bảng gốc phía sau view `v_the_phat_hanh`) hoàn toàn do ETL ngoài quản lý. Tính năng này cho phép admin tự nạp trực tiếp file ISS_02 (danh sách thẻ đã phát hành) vào bảng `the_phat_hanh` qua UI, không cần chờ ETL ngoài.

Component: `tai-du-lieu-len.component.ts/html/scss`. Giao diện: khu vực kéo-thả nhiều file (`.xlsx`, `.xls`, `.zip` tự trích, tối đa 50MB/file), ô chọn "Ngày dữ liệu (T-1)" (mặc định = hôm qua), nút "Tải lên", và bảng "Lịch sử upload" phân trang (thời gian, người upload, ngày data, số file, tổng dòng, trạng thái). Toast thông báo kết quả tự vẽ (không dùng MatSnackBar, theo pattern đã dùng ở `cai-dat-canh-bao`).

### 2.1 Phân loại file
Mỗi file trong 1 phiên upload được phân loại theo **tên file** (không phân biệt hoa/thường, chỉ cần chứa chuỗi khớp):
- `ISS_02` → nạp vào `the_phat_hanh` (**đã implement đầy đủ**, xem 2.2).
- `MPA`, `ISS_06`, `ISS_15` → nhận diện được tên nhưng **chưa hỗ trợ** (`trangThai = UNSUPPORTED`), chờ yêu cầu cụ thể về cấu trúc cột/bảng đích.
- File `.zip` được tự động giải nén trong bộ nhớ (`ZipInputStream`), mỗi entry `.xlsx`/`.xls` bên trong được phân loại độc lập như một file riêng.

### 2.2 Luồng nạp ISS_02 → `the_phat_hanh`
`ThePhatHanhImportServiceImpl` đọc Excel bằng Apache POI, khớp cột theo **tên header** (không theo vị trí cột cố định) — chống lệch dữ liệu nếu cột trong file bị đổi thứ tự. 39 cột Excel ↔ 39 cột DB khớp 1-1 (xem danh sách cột đầy đủ trong code, ví dụ `CARD ID` → `card_id`, `Ngày phát hành thẻ` → `ngay_phat_hanh_the`...). Parse chịu được cả ô dạng số (ID không bị thêm đuôi `.0`) lẫn ô dạng chuỗi, nhiều định dạng ngày (`HH:mm:ss dd-MM-yyyy`, `yyyy-MM-dd`, ô ngày gốc của Excel).

**Vì `the_phat_hanh` chỉ lưu trạng thái hiện tại (không có lịch sử) và không có ràng buộc unique trên `card_id`**, mỗi lần nạp là một **bản thay thế toàn bộ** (không phải insert cộng dồn hay upsert), dùng bảng đệm `the_phat_hanh_staging` để đảm bảo an toàn:

1. `clearStaging()` — dọn sạch staging đầu mỗi phiên (phòng phiên trước lỗi giữa chừng còn sót).
2. `stageFile()` — đọc từng file ISS_02 trong phiên, ghi các dòng hợp lệ vào `the_phat_hanh_staging` (batch insert lô 500 qua `JdbcTemplate`). Dòng trắng bị bỏ qua; thiếu cột bắt buộc → cả file `FAILED`, không cố nạp dữ liệu sai định dạng.
3. **Chỉ khi có ít nhất 1 file stage thành công**, `commitStagedData()` mới chạy — và chạy trong **1 transaction duy nhất**: `TRUNCATE the_phat_hanh` → `INSERT INTO the_phat_hanh SELECT * FROM the_phat_hanh_staging` → `TRUNCATE the_phat_hanh_staging`. Nếu bất kỳ bước nào lỗi, Postgres rollback toàn bộ (kể cả `TRUNCATE`) — dữ liệu cũ **không bao giờ mất** nếu quá trình nạp mới thất bại giữa chừng.
4. Nếu toàn bộ file trong phiên đều `FAILED`/`UNSUPPORTED` (0 dòng nào được stage), bước 3 bị bỏ qua hoàn toàn — bảng `the_phat_hanh` không bị đụng tới.

Bảng `upload_history` ghi 1 dòng/phiên (không phải 1 dòng/file), gồm tổng số file, tổng dòng, trạng thái tổng hợp (`SUCCESS` toàn bộ thành công · `PARTIAL` vừa thành công vừa lỗi/chưa hỗ trợ · `FAILED` toàn bộ lỗi · `UNSUPPORTED` toàn bộ chưa hỗ trợ), và cột `chi_tiet` (text, breakdown từng file — chưa hiển thị lên UI, dùng để tra cứu qua DB khi cần debug).

**Đã kiểm thử trực tiếp trên DB** (backup 22.436 dòng gốc trước khi test, restore lại sau khi xong): parse đúng dữ liệu mẫu thật do người dùng cung cấp, ID dạng số không bị lỗi `.0`/scientific notation, file không hỗ trợ (`MPA_*.xlsx`) bị bỏ qua đúng cách và không đụng bảng sống, upload `.zip` chứa file ISS_02 giải nén và nạp thành công, restore lại đúng 22.436 dòng + KPI summary khớp 100% với trước khi test.

### 2.3 API
| Method | Path | Mô tả |
|---|---|---|
| POST | `/api/upload` | Multipart `files[]` + `ngayDuLieu` + `nguoiUpload` — xử lý 1 phiên upload, trả `UploadBatchResult` (breakdown từng file) |
| GET | `/api/upload/history` | Lịch sử upload, phân trang, sắp xếp mới nhất trước |

---

## 3. Màn hình danh sách & báo cáo (`/quan-ly-the`)

Component: `quan-ly-the.component.ts/html/scss`. 4 tab, dùng chung 1 bảng lọc qua `ThePhatHanhRepository.search()`:

| Tab | Nội dung | Ghi chú |
|---|---|---|
| **Danh sách thẻ** | Bảng đầy đủ: STT, số thẻ (+ loại thẻ + nhóm KH), chủ thẻ, sản phẩm, hạn mức, doanh số/mức PTN, trạng thái, phí thường niên, kích hoạt/PSGD, ngày phát hành | Filter: search text, trạng thái, loại thẻ, sản phẩm, checkbox chưa kích hoạt/chưa PSGD/chưa đạt PTN/**đạt PTN** |
| **Báo cáo chưa kích hoạt** | Banner cảnh báo + dropdown "Chưa kích hoạt trên (ngày)" (mặc định lấy từ cấu hình hệ thống `CHUA_KICH_HOAT_SO_NGAY`, có thể override thủ công) + bảng: STT, thẻ, chủ thẻ, sản phẩm, ngày phát hành, số ngày chờ, liên hệ (SĐT đã format bỏ dấu `/`) | |
| **Báo cáo chưa PSGD** | Dùng chung filter/table với tab Danh sách thẻ, set `chuaPsgd=true` | |
| **Báo cáo doanh số miễn PTN** | Dùng chung filter/table, set `chuaDatPtn=true` | Ngưỡng miễn phí thường niên (PTN), khác với "mốc doanh số theo thời gian" ở mục 5.4 |

**Filter**: `datPtn` (checkbox "Đạt doanh số miễn PTN") là điều kiện bù của `chuaDatPtn` — repo query `doanhSoGiaoDichMienPtn >= doanhSoMienPtn` (đã kiểm chứng: 889 + 21.547 = 22.436, đúng tổng số thẻ).

KPI cards đầu trang (nguồn `TheSummaryResponse`, `GET /api/the-phat-hanh/summary`):
1. Tổng số thẻ
2. Thẻ đang khóa
3. Hạn mức cấp = `SUM(hmtd_issuing_contract)`, đơn vị tỷ VND (chia raw VND cho 1e9 — **lưu ý**: cột này lưu VND thật, không phải "triệu VND" như bảng `du_lieu_mpa`)
4. **Doanh số giao dịch miễn PTN** = `SUM(doanh_so_giao_dich_mien_ptn)` (trước đây gắn nhầm nhãn "Dư nợ" dù cùng 1 số liệu — đã đổi lại tên cho đúng bản chất, không đổi cách tính)
5. **KH đạt miễn PTN (TDQT)** = `soTdqtDatPtn / tongSoTdqt` — chỉ tính trên thẻ `loai_the_tin_dung = 'TDQT'` (trước đây tính trên toàn bộ thẻ, không phân biệt loại — đã sửa theo đúng yêu cầu nghiệp vụ)

**Giữ trạng thái khi back từ trang chi tiết thẻ**: toàn bộ filter/tab/trang hiện tại được đồng bộ vào query string URL (`syncQueryParams()`, `replaceUrl: true`) mỗi khi `loadPage()` chạy, và được đọc lại từ URL khi component khởi tạo lại. `goToDetail()` truyền kèm `returnUrl` (URL đầy đủ có filter) cho trang chi tiết; `goBack()` ở trang chi tiết điều hướng thẳng về `returnUrl` đó thay vì `/quan-ly-the` trống. Vị trí cuộn (`scrollTop` của `main.main-content`, không phải `window`) cũng được lưu vào `sessionStorage` trước khi rời trang và khôi phục lại (double `requestAnimationFrame`) sau khi bảng load xong dữ liệu.

Nút admin trên header: **Chiến dịch** → `/quan-ly-the/chien-dich`, **Cài đặt cảnh báo** → `/quan-ly-the/cai-dat` (chỉ hiện khi `auth.isAdmin()`).

---

## 4. Màn hình chi tiết thẻ (`/quan-ly-the/:id`)

Component: `the-detail/the-detail.component.ts/html/scss`.

- Header: nút quay lại, số thẻ, sản phẩm + ngày phát hành, nút **KH 360°** (điều hướng sang `/khach-hang/:cif`).
- Cột trái: thẻ visual (BIDV, trạng thái, số thẻ, chủ thẻ, hiệu lực, mạng lưới Visa/JCB/Mastercard, hợp đồng, HMTD), panel "Dư nợ hiện tại" (toàn bộ `—` vì không có nguồn dữ liệu), panel "Trạng thái sử dụng thẻ" (ngày phát hành/kích hoạt/PSGD).
- Cột phải: **biểu đồ Doanh số giao dịch theo thời gian** (ng2-charts, tab Theo ngày/Theo tháng, dữ liệu từ `GET /api/the-doanh-so/card/{cardId}/series` — xem mục 6.2; hiển thị thông báo "chưa có dữ liệu" nếu chưa đủ snapshot), panel "Doanh số miễn phí thường niên" (% hoàn thành, banner đạt/chưa đạt, chính sách áp dụng).
- 3 khối "Thông tin thẻ" / "Hợp đồng phát hành (IC)" / "Thông tin chủ thẻ": hiển thị đầy đủ field từ `ThePhatHanhDetailResponse`, các field không có nguồn (hạng thẻ, mã lãi suất, năm đầu/mã phí) hiển thị `—`.
- Khối **"Giao thẻ & cán bộ phụ trách"** (cuối trang): Địa chỉ giao thẻ hiển thị `—` (chưa có nguồn dữ liệu trong DB). CN phát hành / CN giao đều lấy từ `cn_qlt` (view chỉ có 1 cột chi nhánh, không phân biệt 2 khái niệm này). AM thẻ = `am_issuing_contract`. CB giới thiệu = `ma_can_bo_gioi_thieu` (field này đã có sẵn ở entity/view nhưng trước đây chưa được expose qua `ThePhatHanhDetailResponse`, đã bổ sung).

Backend: `ThePhatHanhDetailResponse`, `GET /api/the-phat-hanh/{id}`.

---

## 5. Hệ thống cảnh báo tự động qua email

> Yêu cầu gốc: gửi email tự động cho 3 nhóm khách hàng (chưa kích hoạt, chưa PSGD, chưa đạt doanh số theo mốc thời gian), tất cả ngưỡng "X ngày" là **tham số hệ thống thật** (lưu DB, admin chỉnh qua UI). Chỉ bỏ qua SMS, email được xây dựng gửi thật qua SMTP.

### 5.1 Hạ tầng dùng chung
- **Bảng `system_setting`** (key-value): `CARD_EMAIL_ENABLED` (mặc định **`false`** — công tắc an toàn tổng), `CARD_TEST_EMAIL_OVERRIDE` (nếu có giá trị, MỌI email thật redirect về đây), `CHUA_KICH_HOAT_SO_NGAY`/`_LAP_LAI_SO_NGAY`, `CHUA_PSGD_SO_NGAY`/`_LAP_LAI_SO_NGAY`.
- **Bảng `email_template`**: 3 mẫu cố định (`CHUA_KICH_HOAT`, `CHUA_PSGD`, `DOANH_SO_MOC`), có tiêu đề + nội dung HTML với placeholder `{{tenKhachHang}} {{soThe}} {{soNgay}} {{doanhSoHienTai}} {{nguongDoanhSo}}`.
- **Bảng `email_log`**: nhật ký mọi lần gửi (card_id, loại thông báo, email_to, trạng thái SUCCESS/FAILED/SKIPPED_DISABLED/SKIPPED_DEDUP, campaign_id/milestone_id nếu có).
- **`EmailService`**: bọc `JavaMailSender` (SMTP cấu hình qua biến môi trường `MAIL_HOST/PORT/USERNAME/PASSWORD` trong `application.yml`, chưa điền giá trị thật). `resolveRecipient()` là điểm chặn an toàn duy nhất — mọi luồng gửi đều đi qua đây trước khi ra SMTP thật.
- **Dedup quan trọng**: chỉ log **SUCCESS** mới được tính là "đã gửi" để chặn gửi trùng trong chu kỳ lặp lại — log `SKIPPED_DISABLED`/`FAILED` KHÔNG chặn lần thử tiếp theo (đã kiểm chứng qua test: bật `CARD_EMAIL_ENABLED` sau khi tắt vẫn gửi lại được ngay).

### 5.2 Báo cáo chưa kích hoạt → email
- Điều kiện: `soNgayChuaKichHoat > CHUA_KICH_HOAT_SO_NGAY` và có email.
- Lặp lại tối đa 1 lần mỗi `CHUA_KICH_HOAT_LAP_LAI_SO_NGAY` ngày cho cùng 1 thẻ.
- `CardNotificationService.processChuaKichHoat()`, scheduler chạy 7:00 hằng ngày, endpoint thủ công `POST /api/card-notifications/chua-kich-hoat/run?testMode=`.

### 5.3 Báo cáo chưa PSGD → email
- Điều kiện: đã kích hoạt (`soNgayChuaKichHoat = 0`) nhưng chưa có doanh số, và đã kích hoạt quá `CHUA_PSGD_SO_NGAY` ngày.
- Lặp lại tối đa 1 lần mỗi `CHUA_PSGD_LAP_LAI_SO_NGAY` ngày.
- `CardNotificationService.processChuaPsgd()`, scheduler 7:15, endpoint `POST /api/card-notifications/chua-psgd/run`.

### 5.4 Mốc doanh số theo thời gian → email
- **Bảng `card_revenue_milestone`**: danh sách mốc (số ngày kể từ phát hành ↔ ngưỡng doanh số), CRUD qua `CardRevenueMilestoneController` (`/api/card-revenue-milestones`).
- Với mỗi mốc active, tìm thẻ phát hành đúng ngày tương ứng (hôm nay − số ngày mốc) mà doanh số hiện tại < ngưỡng → gửi email nhắc, dedup theo (milestone, thẻ) — chỉ gửi 1 lần/mốc/thẻ.
- `CardMilestoneEvaluationService.evaluateAndNotify()`, scheduler 7:30, endpoint `POST /api/card-revenue-milestones/run`.

### 5.5 Lịch chạy tổng hợp
`CardNotificationScheduler`: 3 job (chưa kích hoạt 7:00, chưa PSGD 7:15, mốc doanh số 7:30) — mỗi job đều tôn trọng `CARD_EMAIL_ENABLED` qua `EmailService`.

---

## 6. Doanh số theo thời gian (snapshot lịch sử)

> `v_the_phat_hanh` chỉ có 1 con số doanh số lũy kế hiện tại — không có lịch sử. Để dựng báo cáo theo ngày/tháng/quý/năm, hệ thống tự chụp snapshot định kỳ (độc lập với việc `the_phat_hanh` được ETL ngoài refresh thế nào). **Giới hạn chấp nhận: lịch sử chỉ có từ ngày tính năng bắt đầu chạy, không có dữ liệu quá khứ.**

### 6.1 Snapshot hằng ngày
- **Bảng `the_doanh_so_snapshot`** (`card_id`, `ngay_snapshot`, `doanh_so_luy_ke`, `so_ngay_tu_phat_hanh`, unique theo `(card_id, ngay_snapshot)`).
- `SnapshotService.runDailySnapshot()`: duyệt toàn bộ thẻ, upsert (native `ON CONFLICT DO UPDATE`) 1 dòng/thẻ/ngày — idempotent, chạy lại trong ngày không tạo lịch sử giả.
- `SnapshotScheduler` chạy 6:30 hằng ngày; trigger thủ công `POST /api/the-doanh-so/snapshot/run-now`.

### 6.2 Báo cáo doanh số phát sinh
- `TheDoanhSoSnapshotService`: tính **delta** giữa các snapshot liên tiếp (âm → clamp về 0, coi là reset chu kỳ PTN), gộp theo ngày/tháng/quý/năm.
  - `getSeries(cardId, granularity)` → `GET /api/the-doanh-so/card/{cardId}/series` — dùng cho biểu đồ trang chi tiết thẻ.
  - `getBaoCaoTongHop(granularity)` → `GET /api/the-doanh-so/bao-cao` — tổng hợp toàn danh mục (chưa có tab riêng ở frontend, sẵn sàng để bổ sung).

---

## 7. Chiến dịch khuyến mại thẻ (`/quan-ly-the/chien-dich`)

- **Bảng `campaign`** (tên, mô tả, thời gian áp dụng, trạng thái DRAFT/ACTIVE/PAUSED/ENDED, tiêu đề + nội dung email riêng) và **`campaign_criteria`** (field/value phẳng — OR trong cùng field, AND giữa các field khác nhau: `loaiThe`, `productCode`, `hinhThucThe`, `nhomKhThe`, `trangThaiThe`).
- `CampaignService`: CRUD, `preview()`/`previewByCriteria()` (đếm + mẫu 20 thẻ khớp tiêu chí, dùng `Specification<ThePhatHanh>` qua `CampaignCriteriaSpecificationBuilder`), `send()` (gửi email tới toàn bộ thẻ khớp, dedup theo SUCCESS, ghi `email_log` với `campaign_id`).
- Frontend: `chien-dich-list.component` (danh sách, xóa), `chien-dich-form.component` (tạo/sửa, chọn tiêu chí dạng chip, xem trước đối tượng realtime, gửi chiến dịch có xác nhận + chế độ thử nếu email đang tắt, tab lịch sử gửi).
- API: `/api/campaigns` (CRUD), `/{id}/preview`, `/preview-by-criteria`, `/{id}/send`, `/{id}/logs`, `/criteria-options`.

---

## 8. Màn hình Cài đặt cảnh báo (`/quan-ly-the/cai-dat`, admin only)

Component: `cai-dat-canh-bao.component.ts/html/scss`. Gồm:
1. **Công tắc an toàn**: bật/tắt gửi email thật (`CARD_EMAIL_ENABLED`) + ô email test override.
2. **Ngưỡng cảnh báo**: 4 ô nhập số ngày (chưa kích hoạt + lặp lại, chưa PSGD + lặp lại).
3. **Mốc doanh số theo thời gian**: bảng CRUD inline (thêm/sửa/xóa mốc ngày ↔ ngưỡng doanh số).
4. **Chạy thử job**: nút chạy thủ công cho 3 job cảnh báo + job snapshot, hiển thị kết quả (`JobRunResult`: eligible/sent/skippedDedup/skippedDisabled/failed) không cần chờ lịch tự động.
5. **Mẫu email**: sửa tiêu đề/nội dung 3 template.
6. **Nhật ký gửi email**: bảng phân trang từ `email_log`.

---

## 9. Danh sách API backend (`com.mpa.controller`)

| Controller | Endpoint chính |
|---|---|
| `ThePhatHanhController` | `GET /api/the-phat-hanh`, `/{id}`, `/summary`, `/trang-thai-options`, `/hinh-thuc-options`, `/product-options` |
| `UploadController` | `POST /api/upload` (multipart, nạp file ISS_02...), `GET /api/upload/history` |
| `SystemSettingController` | `GET/PUT /api/system-settings` |
| `EmailTemplateController` | `GET /api/email-templates`, `PUT /{id}` |
| `EmailLogController` | `GET /api/email-logs` (phân trang, filter cardId/loaiThongBao/campaignId) |
| `CardNotificationController` | `POST /api/card-notifications/chua-kich-hoat/run`, `/chua-psgd/run` |
| `CardRevenueMilestoneController` | `GET/POST/PUT/DELETE /api/card-revenue-milestones`, `POST /run` |
| `TheDoanhSoSnapshotController` | `GET /api/the-doanh-so/card/{cardId}/series`, `/bao-cao`, `POST /snapshot/run-now` |
| `CampaignController` | `GET/POST/PUT/DELETE /api/campaigns`, `/{id}/preview`, `/preview-by-criteria`, `/{id}/send`, `/{id}/logs`, `/criteria-options` |

---

## 10. Tối ưu hiệu năng (đã phát hiện & fix qua kiểm thử thực tế ~22.400 thẻ)

Khi test job/chiến dịch trên toàn bộ danh mục thẻ (~22.436 thẻ), phát hiện 3 lớp vấn đề N+1 query khiến các thao tác vốn chỉ mất vài giây bị treo hàng chục phút:

1. **`the_doanh_so_snapshot` upsert từng dòng** (`SnapshotServiceImpl`) — gọi repository 1 lần/thẻ → **20+ phút**. Fix: `JdbcTemplate.batchUpdate()` theo lô 500 dòng → **3.1 giây**.
2. **Dedup kiểm tra "đã gửi chưa" từng thẻ** (`CardNotificationServiceImpl`, `CardMilestoneEvaluationServiceImpl`, `CampaignServiceImpl`) — mỗi thẻ gọi riêng `EmailLogRepository`. Fix: fetch 1 lần toàn bộ log `SUCCESS` liên quan (theo loại/milestone/campaign) thành `Set<String>` rồi so khớp trong bộ nhớ.
3. **Ghi `email_log` từng dòng** sau khi gửi — cùng vấn đề như (1). Fix: gom vào `List<EmailLog>` trong lúc lặp, ghi hàng loạt cuối cùng qua `EmailLogBatchWriter` (JdbcTemplate, lô 500).
4. **Đọc `system_setting` từng thẻ** — `EmailService.isEnabled()`/`resolveRecipient()` gọi `SystemSettingService` (vốn query DB) mỗi lần gửi. Fix: thêm cache trong bộ nhớ (`SystemSettingCache`, Spring `@Cacheable`/`@EnableCaching`), tự động xóa cache (`@CacheEvict`) ngay khi admin lưu cấu hình mới qua `SystemSettingServiceImpl.updateBatch()`.

**Kết quả sau khi fix** (đo trên chiến dịch khớp 22.113 thẻ `MAIN_CARD`): xem trước (`preview`) ~1.5s, gửi thật (`send`, email tắt nên toàn bộ `SKIPPED_DISABLED`) ~3-4s, snapshot toàn bộ danh mục ~3.1s. Đã xác minh: idempotent (chạy lại không tạo dòng trùng), dedup đúng theo trạng thái `SUCCESS`, cache tự cập nhật ngay khi đổi cấu hình (không bị stale).

**Bài học áp dụng cho code mới**: bất kỳ vòng lặp nào chạy trên toàn bộ danh mục thẻ (hàng chục nghìn dòng) tuyệt đối không được gọi repository/service theo kiểu 1-query-mỗi-vòng-lặp — luôn fetch hàng loạt trước vòng lặp hoặc ghi hàng loạt sau vòng lặp.

---

## 11. Ghi chú vận hành

- Dự án **không có Flyway/Liquibase** — mọi bảng mới (`system_setting`, `email_template`, `email_log`, `card_revenue_milestone`, `the_doanh_so_snapshot`, `campaign`, `campaign_criteria`, `the_phat_hanh_staging`, `upload_history`) được tạo bằng SQL thủ công chạy 1 lần qua `psql`, `ddl-auto` giữ nguyên `none`.
- SMTP thật chưa được cấu hình (chỉ có placeholder `localhost:25` trong `application.yml`) — cần điền `MAIL_HOST/PORT/USERNAME/PASSWORD/FROM` thật trước khi bật `CARD_EMAIL_ENABLED` ở production.
- Tất cả 3 job cảnh báo + job snapshot đều có endpoint chạy thủ công để test mà không cần chờ lịch `@Scheduled`.
- **Không được bật `CARD_EMAIL_ENABLED=true` hoặc điền SMTP thật khi chưa có xác nhận rõ ràng từ người dùng** — hệ thống hiện đang trong giai đoạn test, dữ liệu khách hàng trong `v_the_phat_hanh` là dữ liệu thật.
- **`POST /api/upload` với file ISS_02 sẽ TRUNCATE + nạp lại toàn bộ `the_phat_hanh`** khi có ít nhất 1 file trong phiên stage thành công — đây là hành vi cố ý (bảng chỉ lưu trạng thái hiện tại, không phải append), nhưng cần lưu ý khi test: nên backup (`CREATE TABLE ... AS SELECT * FROM the_phat_hanh`) trước khi thử với dữ liệu thật, vì thao tác này không có "undo" ngoài restore thủ công từ backup.
