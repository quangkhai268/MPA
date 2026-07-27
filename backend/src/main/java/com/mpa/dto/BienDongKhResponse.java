package com.mpa.dto;

import java.util.List;

public record BienDongKhResponse(List<KhachHangTopResponse> tang, List<KhachHangTopResponse> giam) {
}
