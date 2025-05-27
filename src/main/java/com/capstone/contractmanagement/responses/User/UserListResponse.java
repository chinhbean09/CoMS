package com.capstone.contractmanagement.responses.User;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@AllArgsConstructor
@Data
@Builder
@NoArgsConstructor
public class UserListResponse {
    private List<UserResponse> users;
    private int totalPages;
    private String message;

}
