package com.capstone.contractmanagement.entities.template;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "tieu_de_hop_dong")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TieuDeHopDong {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "created_date")
    private LocalDateTime createdDate;

    @Column(name = "contract_code", unique = true, length = 100)
    private String contractCode;

    // Relationship
    @OneToOne
    @JoinColumn(name = "template_id", referencedColumnName = "template_id")
    private Template template;
}
