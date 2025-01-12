package com.capstone.contractmanagement.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "reports")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "report_id")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "contract_id", nullable = false)
    private Contract contract; // Relationship với Contract

    @Column(name = "report_type", nullable = false, length = 100)
    private String reportType; // Loại báo cáo (Thống kê, Giám sát)

    @Column(name = "content", nullable = false, length = 1000)
    private String content; // Nội dung báo cáo

    @Column(name = "create_at", nullable = false)
    private LocalDateTime createdAt; // Thời gian tạo báo cáo

}
