package com.mpa.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class BscSoSanhResponse {
    private int datKeHoach;
    private int canhBao;
    private int ruiRo;
    private int chuaGiao;
    private int total;
    private List<BscSoSanhRowResponse> rows;
}
