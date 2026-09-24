package com.mpa.service;

import com.mpa.dto.FileImportResult;

import java.io.InputStream;

public interface CardFeeRuleImportService {

    /** Đọc file "DS_PTN_*.xlsx/.xls" (sheet đầu tiên) và THAY THẾ TOÀN BỘ bảng card_fee_rule —
     *  bảng không có khóa nghiệp vụ ổn định (nhiều dòng có thể cùng code), nên mỗi lần tải lên
     *  coi như nạp lại toàn bộ danh sách quy tắc phí thường niên hiện hành. */
    FileImportResult importFile(InputStream excelStream, String fileName);
}
