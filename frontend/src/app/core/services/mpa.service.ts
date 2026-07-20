import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse, PageResponse } from '../models/user.model';
import {
  DashboardKpi, TrendChartData, PhongData, PhongTrendSeries, AmData, AmDetailData,
  KhachHangData, DuLieuMpa, ImportResult, FilterParams,
  BscSoSanhResult, ChiTieuBscRequest, ChiTieuQuanLyRow, UnitOption,
  ThongTinAmItem, ThongTinAmSaveRequest,
  ThongTinKhachHangItem, ThongTinKhachHangSaveRequest, KhachHangChiTiet,
  ThePhatHanhItem, TheSummary, ThePhatHanhDetail, KhachHangTheSummary,
  EmailLogItem, JobRunResult, CardRevenueMilestone, RevenueSeriesResponse
} from '../models/mpa.model';

@Injectable({ providedIn: 'root' })
export class MpaService {
  private api = environment.apiUrl;

  constructor(private http: HttpClient) {}

  // --- Dashboard ---
  getDashboardKpi(filter: FilterParams): Observable<ApiResponse<DashboardKpi>> {
    return this.http.get<ApiResponse<DashboardKpi>>(`${this.api}/dashboard/kpi`, {
      params: this.toParams(filter)
    });
  }

  getTrend(filter: FilterParams, metricKey = 'tong-tnt'): Observable<ApiResponse<TrendChartData>> {
    return this.http.get<ApiResponse<TrendChartData>>(`${this.api}/dashboard/trend`, {
      params: this.toParams({ ...filter, metricKey })
    });
  }

  getPhongData(filter: FilterParams): Observable<ApiResponse<PhongData[]>> {
    return this.http.get<ApiResponse<PhongData[]>>(`${this.api}/dashboard/by-phong`, {
      params: this.toParams(filter)
    });
  }

  getPhongTrend(filter: FilterParams, metricKey = 'tong-tnt'): Observable<ApiResponse<PhongTrendSeries[]>> {
    return this.http.get<ApiResponse<PhongTrendSeries[]>>(`${this.api}/dashboard/phong-trend`, {
      params: this.toParams({ ...filter, metricKey })
    });
  }

  getTopAm(filter: FilterParams, limit = 10): Observable<ApiResponse<AmData[]>> {
    return this.http.get<ApiResponse<AmData[]>>(`${this.api}/dashboard/top-am`, {
      params: this.toParams({ ...filter, limit })
    });
  }

  getTopKh(filter: FilterParams, limit = 8): Observable<ApiResponse<KhachHangData[]>> {
    return this.http.get<ApiResponse<KhachHangData[]>>(`${this.api}/dashboard/top-kh`, {
      params: this.toParams({ ...filter, limit })
    });
  }

  getBienDongKh(filter: FilterParams): Observable<ApiResponse<{ tang: KhachHangData[]; giam: KhachHangData[] }>> {
    return this.http.get<ApiResponse<{ tang: KhachHangData[]; giam: KhachHangData[] }>>(`${this.api}/dashboard/bien-dong-kh`, {
      params: this.toParams(filter)
    });
  }

  getAmDetail(filter: FilterParams, maDonViCap6: string): Observable<ApiResponse<AmDetailData[]>> {
    const soVoi = filter.soVoi ?? 'ky-truoc';
    return this.http.get<ApiResponse<AmDetailData[]>>(`${this.api}/dashboard/am-detail`, {
      params: this.toParams({ ...filter, maDonViCap6, soVoi })
    });
  }

  // --- MPA Data ---
  getDuLieuMpa(filter: FilterParams, page = 0, size = 20): Observable<ApiResponse<PageResponse<DuLieuMpa>>> {
    return this.http.get<ApiResponse<PageResponse<DuLieuMpa>>>(`${this.api}/mpa`, {
      params: this.toParams({ ...filter, page, size })
    });
  }

  importExcel(formData: FormData): Observable<ApiResponse<ImportResult>> {
    return this.http.post<ApiResponse<ImportResult>>(`${this.api}/mpa/import`, formData);
  }

  deleteMpa(id: number): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(`${this.api}/mpa/${id}`);
  }

  deleteBatch(ids: number[]): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(`${this.api}/mpa/batch`, { body: ids });
  }

  // --- Export ---
  exportExcel(filter: FilterParams): Observable<Blob> {
    return this.http.get(`${this.api}/reports/export/excel`, {
      params: this.toParams(filter),
      responseType: 'blob'
    });
  }

  exportPdf(filter: FilterParams): Observable<Blob> {
    return this.http.get(`${this.api}/reports/export/pdf`, {
      params: this.toParams(filter),
      responseType: 'blob'
    });
  }

  // --- Dropdown data ---
  getPhongList(): Observable<ApiResponse<{ ma: string; ten: string }[]>> {
    return this.http.get<ApiResponse<{ ma: string; ten: string }[]>>(`${this.api}/mpa/phong-list`);
  }

  getAmList(maDonViCap6?: string): Observable<ApiResponse<{ ma: string; ten: string }[]>> {
    const params = maDonViCap6 ? new HttpParams().set('maDonViCap6', maDonViCap6) : {};
    return this.http.get<ApiResponse<{ ma: string; ten: string }[]>>(`${this.api}/mpa/am-list`, { params });
  }

  // --- Utility ---
  private toParams(obj: Record<string, any>): HttpParams {
    let params = new HttpParams();
    Object.entries(obj).forEach(([k, v]) => {
      if (v !== null && v !== undefined && v !== '') {
        params = params.set(k, String(v));
      }
    });
    return params;
  }

  // --- Giao chỉ tiêu BSC ---
  getBscSoSanh(loaiKy: string, selectedKy: string, doiTuong: string): Observable<ApiResponse<BscSoSanhResult>> {
    return this.http.get<ApiResponse<BscSoSanhResult>>(`${this.api}/giao-chi-tieu/so-sanh`, {
      params: new HttpParams()
        .set('loaiKy', loaiKy)
        .set('selectedKy', selectedKy)
        .set('doiTuong', doiTuong)
    });
  }

  getQuanLyList(loaiKy: string, selectedKy: string, doiTuong: string): Observable<ApiResponse<ChiTieuQuanLyRow[]>> {
    return this.http.get<ApiResponse<ChiTieuQuanLyRow[]>>(`${this.api}/giao-chi-tieu/quan-ly`, {
      params: new HttpParams()
        .set('loaiKy', loaiKy)
        .set('selectedKy', selectedKy)
        .set('doiTuong', doiTuong)
    });
  }

  saveChiTieu(request: ChiTieuBscRequest): Observable<ApiResponse<void>> {
    return this.http.post<ApiResponse<void>>(`${this.api}/giao-chi-tieu/them`, request);
  }

  deleteChiTieu(id: number): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(`${this.api}/giao-chi-tieu/${id}`);
  }

  getPhongListForCt(): Observable<ApiResponse<UnitOption[]>> {
    return this.http.get<ApiResponse<UnitOption[]>>(`${this.api}/giao-chi-tieu/phong-list`);
  }

  getCnList(): Observable<ApiResponse<UnitOption[]>> {
    return this.http.get<ApiResponse<UnitOption[]>>(`${this.api}/giao-chi-tieu/cn-list`);
  }

  getAmListForCt(): Observable<ApiResponse<UnitOption[]>> {
    return this.http.get<ApiResponse<UnitOption[]>>(`${this.api}/giao-chi-tieu/am-list`);
  }

  // --- Quản lý cán bộ AM ---
  getQuanLyAmList(search: string, page: number, size: number): Observable<ApiResponse<PageResponse<ThongTinAmItem>>> {
    const params = new HttpParams()
      .set('search', search)
      .set('page', String(page))
      .set('size', String(size));
    return this.http.get<ApiResponse<PageResponse<ThongTinAmItem>>>(`${this.api}/quan-ly-am`, { params });
  }

  getQuanLyAmAll(search: string): Observable<ApiResponse<ThongTinAmItem[]>> {
    const params = new HttpParams().set('search', search);
    return this.http.get<ApiResponse<ThongTinAmItem[]>>(`${this.api}/quan-ly-am/all`, { params });
  }

  createQuanLyAm(request: ThongTinAmSaveRequest): Observable<ApiResponse<ThongTinAmItem>> {
    return this.http.post<ApiResponse<ThongTinAmItem>>(`${this.api}/quan-ly-am`, request);
  }

  updateQuanLyAm(id: number, request: ThongTinAmSaveRequest): Observable<ApiResponse<ThongTinAmItem>> {
    return this.http.put<ApiResponse<ThongTinAmItem>>(`${this.api}/quan-ly-am/${id}`, request);
  }

  deleteQuanLyAm(id: number): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(`${this.api}/quan-ly-am/${id}`);
  }

  getAmDropdown(): Observable<ApiResponse<{ ma: string; ten: string }[]>> {
    return this.http.get<ApiResponse<{ ma: string; ten: string }[]>>(`${this.api}/quan-ly-am/am-dropdown`);
  }

  // --- Khách hàng ---
  getKhachHangList(search: string, typeKhachHang: number | null, page: number, size: number): Observable<ApiResponse<PageResponse<ThongTinKhachHangItem>>> {
    let params = new HttpParams()
      .set('search', search)
      .set('page', String(page))
      .set('size', String(size));
    if (typeKhachHang !== null && typeKhachHang !== undefined) {
      params = params.set('typeKhachHang', String(typeKhachHang));
    }
    return this.http.get<ApiResponse<PageResponse<ThongTinKhachHangItem>>>(`${this.api}/khach-hang`, { params });
  }

  getKhachHangTypes(): Observable<ApiResponse<number[]>> {
    return this.http.get<ApiResponse<number[]>>(`${this.api}/khach-hang/types`);
  }

  createKhachHang(request: ThongTinKhachHangSaveRequest): Observable<ApiResponse<ThongTinKhachHangItem>> {
    return this.http.post<ApiResponse<ThongTinKhachHangItem>>(`${this.api}/khach-hang`, request);
  }

  updateKhachHang(id: number, request: ThongTinKhachHangSaveRequest): Observable<ApiResponse<ThongTinKhachHangItem>> {
    return this.http.put<ApiResponse<ThongTinKhachHangItem>>(`${this.api}/khach-hang/${id}`, request);
  }

  deleteKhachHang(id: number): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(`${this.api}/khach-hang/${id}`);
  }

  getKhachHangChiTiet(
    cif: string,
    nam: number,
    thang?: number | null,
    quy?: string | null,
    soVoi: string = 'ky-truoc'
  ): Observable<ApiResponse<KhachHangChiTiet>> {
    let params = new HttpParams().set('nam', String(nam)).set('soVoi', soVoi);
    if (thang != null) params = params.set('thang', String(thang));
    if (quy != null)   params = params.set('quy', quy);
    return this.http.get<ApiResponse<KhachHangChiTiet>>(
      `${this.api}/khach-hang/${encodeURIComponent(cif)}/chi-tiet`, { params }
    );
  }

  // --- Quản lý thẻ ---
  getTheList(
    search: string, trangThai: string, hinhThuc: string, productCode: string,
    loaiTheTinDung: string,
    chuaKichHoat: boolean, soNgayMin: number, chuaPsgd: boolean, chuaDatPtn: boolean, datPtn: boolean,
    page: number, size: number
  ): Observable<ApiResponse<PageResponse<ThePhatHanhItem>>> {
    let params = new HttpParams()
      .set('search', search)
      .set('trangThai', trangThai)
      .set('hinhThuc', hinhThuc)
      .set('productCode', productCode)
      .set('loaiTheTinDung', loaiTheTinDung)
      .set('chuaKichHoat', String(chuaKichHoat))
      .set('soNgayMin', String(soNgayMin))
      .set('chuaPsgd', String(chuaPsgd))
      .set('chuaDatPtn', String(chuaDatPtn))
      .set('datPtn', String(datPtn))
      .set('page', String(page))
      .set('size', String(size));
    return this.http.get<ApiResponse<PageResponse<ThePhatHanhItem>>>(`${this.api}/the-phat-hanh`, { params });
  }

  getTheDetail(id: number): Observable<ApiResponse<ThePhatHanhDetail>> {
    return this.http.get<ApiResponse<ThePhatHanhDetail>>(`${this.api}/the-phat-hanh/${id}`);
  }

  getTheSummary(): Observable<ApiResponse<TheSummary>> {
    return this.http.get<ApiResponse<TheSummary>>(`${this.api}/the-phat-hanh/summary`);
  }

  getKhachHangTheSummary(cif: string): Observable<ApiResponse<KhachHangTheSummary>> {
    return this.http.get<ApiResponse<KhachHangTheSummary>>(`${this.api}/the-phat-hanh/theo-khach-hang/${cif}`);
  }

  getTheTrangThaiOptions(): Observable<ApiResponse<string[]>> {
    return this.http.get<ApiResponse<string[]>>(`${this.api}/the-phat-hanh/trang-thai-options`);
  }

  getTheHinhThucOptions(): Observable<ApiResponse<string[]>> {
    return this.http.get<ApiResponse<string[]>>(`${this.api}/the-phat-hanh/hinh-thuc-options`);
  }

  getTheProductOptions(): Observable<ApiResponse<string[]>> {
    return this.http.get<ApiResponse<string[]>>(`${this.api}/the-phat-hanh/product-options`);
  }

  // --- Cảnh báo / nhật ký email ---
  getEmailLogs(
    cardId: string | null, loaiThongBao: string | null, campaignId: number | null,
    page: number, size: number
  ): Observable<ApiResponse<PageResponse<EmailLogItem>>> {
    let params = new HttpParams()
      .set('page', String(page))
      .set('size', String(size));
    if (cardId) params = params.set('cardId', cardId);
    if (loaiThongBao) params = params.set('loaiThongBao', loaiThongBao);
    if (campaignId != null) params = params.set('campaignId', String(campaignId));
    return this.http.get<ApiResponse<PageResponse<EmailLogItem>>>(`${this.api}/email-logs`, { params });
  }

  runChuaKichHoatJob(): Observable<ApiResponse<JobRunResult>> {
    return this.http.post<ApiResponse<JobRunResult>>(`${this.api}/card-notifications/chua-kich-hoat/run`, {});
  }

  runChuaPsgdJob(): Observable<ApiResponse<JobRunResult>> {
    return this.http.post<ApiResponse<JobRunResult>>(`${this.api}/card-notifications/chua-psgd/run`, {});
  }

  // --- Mốc doanh số theo thời gian ---
  getRevenueMilestones(): Observable<ApiResponse<CardRevenueMilestone[]>> {
    return this.http.get<ApiResponse<CardRevenueMilestone[]>>(`${this.api}/card-revenue-milestones`);
  }

  createRevenueMilestone(m: { soNgayTuPhatHanh: number; nguongDoanhSo: number; moTa: string; active: boolean }): Observable<ApiResponse<CardRevenueMilestone>> {
    return this.http.post<ApiResponse<CardRevenueMilestone>>(`${this.api}/card-revenue-milestones`, m);
  }

  updateRevenueMilestone(id: number, m: { soNgayTuPhatHanh: number; nguongDoanhSo: number; moTa: string; active: boolean }): Observable<ApiResponse<CardRevenueMilestone>> {
    return this.http.put<ApiResponse<CardRevenueMilestone>>(`${this.api}/card-revenue-milestones/${id}`, m);
  }

  deleteRevenueMilestone(id: number): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(`${this.api}/card-revenue-milestones/${id}`);
  }

  runMilestoneJob(): Observable<ApiResponse<JobRunResult>> {
    return this.http.post<ApiResponse<JobRunResult>>(`${this.api}/card-revenue-milestones/run`, {});
  }

  // --- Doanh số theo thời gian (snapshot) ---
  getRevenueSeries(cardId: string, granularity: 'ngay' | 'thang' | 'quy' | 'nam'): Observable<ApiResponse<RevenueSeriesResponse>> {
    return this.http.get<ApiResponse<RevenueSeriesResponse>>(`${this.api}/the-doanh-so/card/${cardId}/series`, { params: { granularity } });
  }

  getBaoCaoDoanhSoTongHop(granularity: 'ngay' | 'thang' | 'quy' | 'nam'): Observable<ApiResponse<RevenueSeriesResponse>> {
    return this.http.get<ApiResponse<RevenueSeriesResponse>>(`${this.api}/the-doanh-so/bao-cao`, { params: { granularity } });
  }

  runSnapshotJob(): Observable<ApiResponse<number>> {
    return this.http.post<ApiResponse<number>>(`${this.api}/the-doanh-so/snapshot/run-now`, {});
  }

  downloadBlob(blob: Blob, filename: string): void {
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = filename;
    a.click();
    URL.revokeObjectURL(url);
  }
}
