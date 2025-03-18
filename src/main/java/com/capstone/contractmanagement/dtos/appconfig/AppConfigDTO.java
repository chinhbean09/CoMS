package com.capstone.contractmanagement.dtos.appconfig;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppConfigDTO {

    private String key;
    private String value;
    private String description;
}
