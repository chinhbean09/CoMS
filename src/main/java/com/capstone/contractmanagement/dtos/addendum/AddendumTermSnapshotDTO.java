package com.capstone.contractmanagement.dtos.addendum;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddendumTermSnapshotDTO {
    // Đây là ID của term gốc (để tham chiếu nếu cần)
    private Long id;
}

