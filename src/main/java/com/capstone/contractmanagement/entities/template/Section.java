package com.capstone.contractmanagement.entities.template;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Table(name = "sections")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Section {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "section_name")
    private String sectionName;

    @Column(name = "section_order")
    private int order;

    @Column(name = "is_custom")
    private boolean isCustom;

    @ManyToMany(mappedBy = "sections")
    private List<Template> templates;
}
