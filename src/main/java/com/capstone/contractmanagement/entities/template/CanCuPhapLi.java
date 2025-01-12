package com.capstone.contractmanagement.entities.template;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "can_cu_phap_li")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CanCuPhapLi {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @ManyToMany(mappedBy = "canCuPhapLi")
    private List<Template> templates;
}
