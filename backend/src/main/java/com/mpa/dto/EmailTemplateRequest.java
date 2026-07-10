package com.mpa.dto;

import lombok.Data;

@Data
public class EmailTemplateRequest {
    private String tieuDe;
    private String noiDung;
    private Boolean active;
}
