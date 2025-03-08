package com.capstone.contractmanagement.entities.contract;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdditionalTermSnapshot {

    private Long termId;

    @Column(name = "term_label", columnDefinition = "TEXT") // Giới hạn termLabel
    private String termLabel;

    @Column(name = "term_value", columnDefinition = "TEXT") // Không giới hạn độ dài
    private String termValue;
}
