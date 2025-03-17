    package com.capstone.contractmanagement.entities.contract_template;

    import com.fasterxml.jackson.annotation.JsonIgnore;
    import jakarta.persistence.*;
    import lombok.*;

    import java.util.ArrayList;
    import java.util.List;

    @Entity
    @Table(name = "contract_template_additional_term_details")
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public class ContractTemplateAdditionalTermDetail {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        @Column(name = "additional_term_id")
        private Long id;

        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "template_id")
        @JsonIgnore
        private ContractTemplate contractTemplate;

        @Column(name = "type_term_id", nullable = false)
        private Long typeTermId;

        // term id được chọn cho nhóm "Common"
        @ElementCollection
        @CollectionTable(name = "ct_additional_common", joinColumns = @JoinColumn(name = "additional_term_id"))
        @Column(name = "term_id")
        private List<Long> commonTermIds = new ArrayList<>();

        //  term id được chọn cho nhóm "A"
        @ElementCollection
        @CollectionTable(name = "ct_additional_a", joinColumns = @JoinColumn(name = "additional_term_id"))
        @Column(name = "term_id")
        private List<Long> aTermIds = new ArrayList<>();

        // term id được chọn cho nhóm "B"
        @ElementCollection
        @CollectionTable(name = "ct_additional_b", joinColumns = @JoinColumn(name = "additional_term_id"))
        @Column(name = "term_id")
        private List<Long> bTermIds = new ArrayList<>();

    }
