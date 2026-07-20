export interface FileImportResult {
  tenFile: string;
  loaiFile: string;   // MPA | ISS_02 | ISS_06 | ISS_15 | UNKNOWN
  soDong: number;
  trangThai: string;  // SUCCESS | FAILED | UNSUPPORTED
  ghiChu: string | null;
}

export interface UploadBatchResult {
  thoiGian: string;
  nguoiUpload: string | null;
  ngayDuLieu: string;
  soFile: number;
  tongDong: number;
  trangThai: string;  // SUCCESS | PARTIAL | FAILED | UNSUPPORTED
  files: FileImportResult[];
}

export interface UploadHistoryItem {
  id: number;
  thoiGian: string;
  nguoiUpload: string | null;
  ngayDuLieu: string | null;
  soFile: number;
  tongDong: number;
  trangThai: string;
}
