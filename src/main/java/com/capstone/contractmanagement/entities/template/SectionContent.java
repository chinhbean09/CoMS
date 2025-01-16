package com.capstone.contractmanagement.entities.template;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "section_contents")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SectionContent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @ManyToOne
    @JoinColumn(name = "section_id", referencedColumnName = "id")
    private Section section;
}
