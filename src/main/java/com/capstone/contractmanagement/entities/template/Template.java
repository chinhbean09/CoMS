package com.capstone.contractmanagement.entities.template;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Table(name = "templates")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Template {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "template_id")
    private Long id;

    @Column(name = "template_name", nullable = false, length = 200)
    private String templateName;

    @Column(name = "title", length = 200)
    private String title;

    @Lob
    @Column(name = "content")
    private String content;

    @Column(name = "created_by", length = 100)
    private String createdBy;

//    // Relationships
//    @OneToOne(mappedBy = "template")
//    private HieuLucHopDong hieuLucHopDong;
//
//    @OneToMany(mappedBy = "template")
//    private List<ThoiGianThucHienHopDong> thoiGianThucHienHopDong;
//
//    @OneToOne(mappedBy = "template")
//    private TieuDeHopDong tieuDeHopDong;

//    @ManyToMany
//    @JoinTable(
//            name = "can_cu_phap_li_templates",
//            joinColumns = @JoinColumn(name = "template_id"),
//            inverseJoinColumns = @JoinColumn(name = "can_cu_phap_li_id")
//    )
//    private List<CanCuPhapLi> canCuPhapLi;

    @ManyToMany
    @JoinTable(
            name = "section_templates",
            joinColumns = @JoinColumn(name = "template_id"),
            inverseJoinColumns = @JoinColumn(name = "section_id")
    )
    private List<Section> sections;

}
