package com.capstone.contractmanagement.responses.User;

import com.capstone.contractmanagement.enums.DepartmentList;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@Data
@Builder
@NoArgsConstructor
public class UserListCustom {
    @JsonProperty("full_name")
    private String fullName;

    @JsonProperty("user_id")
    private Long userId;

    private DepartmentList department;
}
