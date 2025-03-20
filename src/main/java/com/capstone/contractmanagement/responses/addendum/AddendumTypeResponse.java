package com.capstone.contractmanagement.responses.addendum;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AddendumTypeResponse {
    private Long addendumTypeId;
    private String name;
}
