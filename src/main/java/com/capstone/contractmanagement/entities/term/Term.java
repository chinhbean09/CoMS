package com.capstone.contractmanagement.entities.term;

import com.capstone.contractmanagement.enums.TermStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "terms")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Term {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "term_id")
    private Long id;

    @Column(name = "label", nullable = false, columnDefinition = "TEXT")
    private String label;

    @Column(name = "value", nullable = false, columnDefinition = "TEXT")
    private String value;

    @Column(name = "clause_code")
    private String clauseCode;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    // Liên kết Many-to-One với TypeTerm
    @ManyToOne
    @JoinColumn(name = "type_term_id", nullable = false)
    private TypeTerm typeTerm;

    // Trạng thái của điều khoản: NEW (mới), OLD (cũ), SOFT_DELETED (đã xóa mềm)
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private TermStatus status = TermStatus.NEW;

    // Số phiên bản của điều khoản, update +=1
    @Column(name = "version", nullable = false)
    private Integer version = 1;

}
