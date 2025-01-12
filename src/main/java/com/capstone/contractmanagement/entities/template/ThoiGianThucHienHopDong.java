package com.capstone.contractmanagement.entities.template;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "thoi_gian_thuc_hien_hop_dong")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ThoiGianThucHienHopDong {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "start_date")
    private LocalDateTime startDate;

    @Column(name = "end_date")
    private LocalDateTime endDate;

    @Column(name = "content", length = 500)
    private String content;

    // Relationship
    @ManyToOne
    @JoinColumn(name = "template_id", referencedColumnName = "template_id")
    private Template template;
}
