package com.mpa.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class JobRunResult {
    private int eligible;
    private int sent;
    private int skippedDedup;
    private int skippedDisabled;
    private int failed;
}
