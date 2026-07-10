package com.mpa.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "campaign_criteria")
@Data
public class CampaignCriteria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "campaign_id")
    private Integer campaignId;

    @Column(name = "tieu_chi_field")
    private String tieuChiField;

    @Column(name = "tieu_chi_value")
    private String tieuChiValue;
}
