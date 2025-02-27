package com.capstone.contractmanagement.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_trail")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditTrail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    // Quan hệ Many-to-One với Contract
    @ManyToOne
    @JsonIgnore
    @JoinColumn(name = "contract_id", nullable = false)
    private Contract contract;

    @Column(name = "changed_by", nullable = false, length = 100)
    private String changedBy;

    @Column(name = "change_timestamp", nullable = false)
    private LocalDateTime changeTimestamp;

    @Column(name = "change_summary", length = 1000)
    private String changeSummary;

    @Column(name = "old_value", columnDefinition = "TEXT")
    private String oldValue; // Dùng String để lưu JSON cũ

    @Column(name = "new_value", columnDefinition = "TEXT")
    private String newValue; // Dùng String để lưu JSON mới
}
