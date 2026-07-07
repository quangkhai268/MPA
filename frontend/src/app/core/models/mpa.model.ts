export interface DuLieuMpa {
  id: number;
  ngay: string;
  thang: number;
  quy: string;
  nam: number;
  maAm: string;
  tenAm: string;
  maDonViCap6: string;
  tenDonViCap6: string;
  maSpCap5: string;
  tenSpCap5: string;
  maPhanKhucKhCap2: string;
  tenPhanKhucKhCap2: string;
  maKhCif: string;
  tenKhachHang: string;
  kyHanCap2: string;
  thuNhapThuanHdvFtp: number;
  thuNhapThuanDichVu: number;
  thuNhapThuanTinDung: number;
  thuNhapThuan: number;
  duNoTinDungCuoiKy: number;
  huyDongVonBinhQuan: number;
  huyDongVonCuoiKy: number;
  ngayTao: string;
  sheetname: string;
}

export interface DashboardKpi {
  // 7 chỉ tiêu kinh doanh – kỳ hiện tại (triệu VND)
  tongHdvCuoiKy: number;
  tongHdvBinhQuan: number;
  tongDuNo: number;
  tongTntDichVu: number;
  tongTntHdvFtp: number;
  tongTntTinDung: number;
  tongThuNhapThuan: number;
  // Kỳ trước (triệu VND)
  tongHdvCuoiKyPrev: number;
  tongHdvBinhQuanPrev: number;
  tongDuNoPrev: number;
  tongTntDichVuPrev: number;
  tongTntHdvFtpPrev: number;
  tongTntTinDungPrev: number;
  tongThuNhapThuanPrev: number;
  // Kế hoạch (triệu VND)
  khHdvCuoiKy: number;
  khHdvBinhQuan: number;
  khDuNo: number;
  khTntDichVu: number;
  khTntHdvFtp: number;
  khTntTinDung: number;
  khTnt: number;
  // Thống kê
  soKhachHang: number;
  soAm: number;
  soPhong: number;
}

export interface TrendDataPoint {
  label: string;
  value: number;
  date?: string;
}

export interface TrendChartData {
  currentYear: number;
  prevYear: number;       // 0 = no prev line (loaiKy=nam)
  labels: string[];
  currentValues: number[];
  prevValues: number[];
}

export interface PhongData {
  maDonViCap6: string;
  tenDonViCap6: string;
  thuNhapThuan: number;
  thuNhapThuanPrevious: number;
  duNo: number;
  hdvCuoiKy: number;
  hdvBinhQuan: number;
  thuNhapThuanDichVu: number;
  thuNhapThuanHdvFtp: number;
  thuNhapThuanTinDung: number;
  soKhachHang: number;
  soAm: number;
  changePercent: number | null;
  hdvCuoiKyChangePercent: number | null;
  hdvBinhQuanChangePercent: number | null;
  duNoChangePercent: number | null;
  tntDichVuChangePercent: number | null;
  tntHdvChangePercent: number | null;
  tntTinDungChangePercent: number | null;
}

export interface PhongTrendSeries {
  tenDonViCap6: string;
  values: number[];
}

export interface AmData {
  maAm: string;
  tenAm: string;
  tenDonViCap6: string;
  thuNhapThuan: number;
  duNo: number;
  hdvCuoiKy: number;
  soKhachHang: number;
}

export interface AmDetailData {
  maAm: string;
  tenAm: string;
  tenDonViCap6: string;
  hdvCuoiKy: number;
  hdvBinhQuan: number;
  duNo: number;
  thuNhapThuanDichVu: number;
  thuNhapThuanHdvFtp: number;
  thuNhapThuanTinDung: number;
  thuNhapThuan: number;
  // % thay đổi so kỳ trước (null = không có dữ liệu kỳ trước)
  hdvCuoiKyChangePct: number | null;
  hdvBinhQuanChangePct: number | null;
  duNoChangePct: number | null;
  tntDichVuChangePct: number | null;
  tntHdvChangePct: number | null;
  tntTinDungChangePct: number | null;
  thuNhapThuanChangePct: number | null;
}

export interface KhachHangData {
  maKhCif: string;
  tenKhachHang: string;
  tenDonViCap6: string;
  tenAm: string;
  thuNhapThuan: number;
  thuNhapThuanPrevious: number;
  change: number;
  changePercent: number;
}

export interface ImportResult {
  success: number;
  error: number;
  skip: number;
}

export interface BscSoSanhRow {
  maUnit: string;
  tenUnit: string;
  hdvCuoiKyTh: number;
  hdvCuoiKyKh: number;
  hdvCuoiKyPct: number | null;
  hdvCuoiKyDelta: number | null;
  casaBinhQuanTh: number;
  casaBinhQuanKh: number;
  casaBinhQuanPct: number | null;
  casaBinhQuanDelta: number | null;
  duNoTh: number;
  duNoKh: number;
  duNoPct: number | null;
  duNoDelta: number | null;
  tntDichVuTh: number;
  tntDichVuKh: number;
  tntDichVuPct: number | null;
  tntDichVuDelta: number | null;
  tntHdvTh: number;
  tntHdvKh: number;
  tntHdvPct: number | null;
  tntHdvDelta: number | null;
  tntTinDungTh: number;
  tntTinDungKh: number;
  tntTinDungPct: number | null;
  tntTinDungDelta: number | null;
  tntTh: number;
  tntKh: number;
  tntPct: number | null;
  tntDelta: number | null;
}

export interface BscSoSanhResult {
  datKeHoach: number;
  canhBao: number;
  ruiRo: number;
  chuaGiao: number;
  total: number;
  rows: BscSoSanhRow[];
}

export interface ChiTieuBscRequest {
  loaiKy: string;
  selectedKy: string;
  doiTuong: string;
  maUnit: string;
  tenUnit: string;
  chiTieu: string;
  mucTieu: number;
}

export interface ChiTieuQuanLyRow {
  id: number;
  maUnit: string;
  tenUnit: string;
  hdvCuoiKy: number;
  casaBinhQuan: number;
  duNoTinDung: number;
  tntDichVu: number;
  tntHdvFtp: number;
  tntTinDung: number;
  thuNhapThuan: number;
  ngayTao: string;
}

export interface UnitOption {
  ma: string;
  ten: string;
}

export interface FilterParams {
  fromDate?: string;
  toDate?: string;
  nam?: number;
  thang?: number;
  quy?: string;
  maDonViCap6?: string;
  maAm?: string;
  sheetname?: string;
  search?: string;
  page?: number;
  size?: number;
  loaiKy?: string;
  selectedKy?: string;
  soVoi?: string;
  phanKhuc?: string;
}

export interface ThongTinAmItem {
  id: number;
  maCn: string | null;
  maDonViCap6: string | null;
  tenPhong: string | null;
  tenCn: string | null;
  maAm: string;
  tenAm: string;
  soDienThoai: string | null;
  ngayBatDau: string | null;
  email: string | null;
  chucVu: string | null;
  loaiAm: string | null;
  trangThai: number;
}

export interface ThongTinAmSaveRequest {
  maCn: string | null;
  maDonViCap6: string | null;
  maAm: string;
  tenAm: string;
  soDienThoai: string | null;
  ngayBatDau: string | null;
  email: string | null;
  chucVu: string | null;
  loaiAm: string | null;
  trangThai: number;
}

export interface CanBoGroup {
  tenAm: string;
  maDonViCap6: string | null;
  tenPhong: string | null;
  items: ThongTinAmItem[];
  trangThai: number;
}

export interface ThongTinKhachHangItem {
  id: number;
  maCn: string | null;
  maDonViCap6: string | null;
  maAm: string | null;
  tenAm: string | null;
  tenPhong: string | null;
  maKhCif: string;
  tenKhachHang: string;
  soDienThoai: string | null;
  ngayBatDau: string | null;
  email: string | null;
  typeKhachHang: number | null;
  tenPhanKhuc: string | null;
  trangThai: number;
}

export interface ThongTinKhachHangSaveRequest {
  maCn: string | null;
  maDonViCap6: string | null;
  maAm: string | null;
  maKhCif: string;
  tenKhachHang: string;
  soDienThoai: string | null;
  ngayBatDau: string | null;
  email: string | null;
  typeKhachHang: number | null;
  trangThai: number;
}

// ── Quản lý thẻ ──────────────────────────────────────────────────────
export interface ThePhatHanhItem {
  id: number;
  soCifKhachHangPht: string | null;
  issuingContractNbr: string | null;
  productCode: string | null;
  trangThaiIssuingContract: string | null;
  trangThaiHienThi: string | null;
  cifChuTheChinh: string | null;
  tenChuTheChinh: string | null;
  hmtdIssuingContract: number | null;
  loaiThe: string | null;
  soTheDaPhatHanh: string | null;
  cardId: string | null;
  hinhThucThe: string | null;
  soNgayChuaKichHoat: number | null;
  ngayPhatHanhHienThi: string | null;
  thoiHanHieuLucThe: string | null;
  amCard: string | null;
  amIssuingContract: string | null;
  // Doanh số giao dịch thực tế
  doanhSoGiaoDichMienPtn: number | null;
  // Mức doanh số cần đạt để miễn phí thường niên (cfr.so_tien)
  doanhSoMienPtn: number | null;
  soTienPhiThuongNien: number | null;
  // ptnThreshold = doanhSoMienPtn (dùng cho progress bar)
  ptnThreshold: number | null;
  // % thực hiện = doanhSoGiaoDichMienPtn / doanhSoMienPtn * 100
  pctPtn: number | null;
  ngayKichHoat: string | null;
  ngayPsgd: string | null;
  nhomKhThe: string | null;
  kenhPhatHanh: string | null;
  mangLuoi: string | null;
  loaiTheTinDung: string | null;
  sdt: string | null;
  email: string | null;
}

// ── Chi tiết thẻ ─────────────────────────────────────────────────────
export interface ThePhatHanhDetail {
  id: number;
  soTheDaPhatHanh: string | null;
  productCode: string | null;
  ngayPhatHanhHienThi: string | null;
  trangThaiHienThi: string | null;
  tenChuTheChinh: string | null;
  thoiHanHieuLucThe: string | null;
  mangLuoi: string | null;
  loaiThe: string | null;
  hinhThucThe: string | null;
  plasticStatus: string | null;
  lyDoPhatHanh: string | null;
  kenhPhatHanh: string | null;
  nhomKhThe: string | null;
  dacQuyenThe: string | null;
  soNgayChuaKichHoat: number | null;

  issuingContractNbr: string | null;
  hmtdIssuingContract: number | null;
  thoiHanHmtd: string | null;
  trangThaiIssuingContract: string | null;
  amIssuingContract: string | null;
  cnQlt: string | null;
  liabTopContract: string | null;

  soCifKhachHangPht: string | null;
  hoTenKhachHangPht: string | null;
  cifChuTheChinh: string | null;
  sdt: string | null;
  email: string | null;
  soGttt: string | null;
  sinhTracHocKhachHang: string | null;

  doanhSoGiaoDichMienPtn: number | null;
  doanhSoMienPtn: number | null;
  pctPtn: number | null;
  soTienPhiThuongNien: number | null;
  mucPhiThuongNienThe: string | null;

  ngayKichHoat: string | null;
  ngayPsgd: string | null;
  ngayPhatHanhThe: string | null;
}

export interface TheSummary {
  tongSoThe: number;
  soTheHoatDong: number;
  soTheBiKhoa: number;
  soTheChuaKichHoat: number;
  soTheChuaPsgd: number;
  hanMucCap: number;
  duNo: number;
  tyLeDungHanMuc: number;
  soTheDatPtn: number;
  soTheChuaDatPtn: number;
}

export interface KhachHangChiTiet {
  // Thông tin khách hàng
  maKhCif: string;
  tenKhachHang: string;
  typeKhachHang: number | null;
  tenPhanKhuc: string | null;
  maAm: string | null;
  tenAm: string | null;
  maDonViCap6: string | null;
  tenDonViCap6: string | null;
  soDienThoai: string | null;
  email: string | null;
  ngayBatDau: string | null;
  // Kỳ hiện tại (triệu VND)
  hdvCuoiKy: number;
  casaBinhQuan: number;
  duNo: number;
  tntDichVu: number;
  tntHdv: number;
  tntTinDung: number;
  tongTnt: number;
  // Kỳ trước
  hdvCuoiKyPrev: number;
  casaBinhQuanPrev: number;
  duNoPrev: number;
  tntDichVuPrev: number;
  tntHdvPrev: number;
  tntTinDungPrev: number;
  tongTntPrev: number;
  // Trung bình phân khúc
  hdvCuoiKyTb: number;
  casaBinhQuanTb: number;
  duNoTb: number;
  tntDichVuTb: number;
  tntHdvTb: number;
  tntTinDungTb: number;
  tongTntTb: number;
  // Xu hướng (sparkline)
  trendLabels: string[];
  hdvCuoiKyTrend: number[];
  casaBinhQuanTrend: number[];
  duNoTrend: number[];
  tntDichVuTrend: number[];
  tntHdvTrend: number[];
  tntTinDungTrend: number[];
  tongTntTrend: number[];
  // Mô tả kỳ
  kyHienTai: string;
  kyTruoc: string;
}
