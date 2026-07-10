package com.mpa.util;

import com.mpa.entity.CampaignCriteria;
import com.mpa.entity.ThePhatHanh;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Chuyển danh sách tiêu chí chiến dịch (field/value phẳng) thành Specification lọc ThePhatHanh:
 * OR giữa các value cùng field, AND giữa các field khác nhau.
 */
public final class CampaignCriteriaSpecificationBuilder {

    private CampaignCriteriaSpecificationBuilder() {}

    public static Specification<ThePhatHanh> build(List<CampaignCriteria> criteriaList) {
        if (criteriaList == null || criteriaList.isEmpty()) {
            return Specification.where(null);
        }
        Map<String, List<String>> grouped = criteriaList.stream()
                .collect(Collectors.groupingBy(CampaignCriteria::getTieuChiField,
                        Collectors.mapping(CampaignCriteria::getTieuChiValue, Collectors.toList())));

        Specification<ThePhatHanh> spec = Specification.where(null);
        for (Map.Entry<String, List<String>> entry : grouped.entrySet()) {
            String field = entry.getKey();
            List<String> values = entry.getValue();
            Specification<ThePhatHanh> fieldSpec = (root, query, cb) -> root.get(field).in(values);
            spec = spec.and(fieldSpec);
        }
        return spec;
    }
}
