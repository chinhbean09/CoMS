package com.capstone.contractmanagement.dtos.term;

import jakarta.persistence.Column;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class BatchCreateTermDTO {
    @NotNull(message = "Type term ID không được để trống")
    private Long typeTermId;

    @NotBlank(message = "Label không được để trống")
    private String label;

    @NotBlank(message = "Value không được để trống")
    @Column(columnDefinition = "TEXT")
    private String value;
}