package com.capstone.contractmanagement.entities.addendum;

import com.capstone.contractmanagement.entities.*;
import com.capstone.contractmanagement.entities.approval_workflow.ApprovalWorkflow;
import com.capstone.contractmanagement.entities.contract.Contract;
import com.capstone.contractmanagement.enums.AddendumStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

//Phụ lục là một tài liệu bổ sung, ghi nhận các thay đổi cụ thể trong điều khoản hợp đồng mà không thay thế toàn bộ hợp đồng gốc.
//Đây là thực tiễn phổ biến trong pháp lý, giúp giữ nguyên tính liên tục của hợp đồng
//và dễ dàng theo dõi những thay đổi riêng lẻ (ví dụ: "Điều khoản X sửa từ A thành B").
@Entity
@Table(name = "addenda")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Addendum {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Tiêu đề phụ lục
    @Column(name = "title", nullable = false, length = 200)
    private String title;

    // Nội dung phụ lục, ghi nhận các thay đổi cụ thể
    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    // Ngày có hiệu lực của phụ lục
    @Column(name = "effective_date")
    private LocalDateTime effectiveDate;

    @Column(name = "contract_number")
    private String contractNumber;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private AddendumStatus status;

//    @Column(name = "created_by")
//    private String createdBy;

    // Thời gian tạo phụ lục
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    // Thời gian cập nhật phụ lục
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Liên kết với hợp đồng gốc
    @ManyToOne
    @JoinColumn(name = "contract_id", nullable = true)
    private Contract contract;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;


    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approval_workflow_id")
    @JsonIgnore
    private ApprovalWorkflow approvalWorkflow;

    @Column(name = "signed_file_path")
    private String signedFilePath;

    @Column(name = "signed_by")
    private String signedBy;

    @Column(name = "signed_at")
    private LocalDateTime signedAt;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "addendum_signed_url", joinColumns = @JoinColumn(name = "addendum_id"))
    @Column(name = "signed_addendum_url")
    private List<String> signedAddendumUrls; // Lưu nhiều URL trong một trường

    @OneToMany(mappedBy = "addendum", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<AddendumPaymentSchedule> paymentSchedules = new ArrayList<>();

    // Mối quan hệ với AddendumAdditionalTermDetail
    @OneToMany(mappedBy = "addendum", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<AddendumAdditionalTermDetail> additionalTermDetails = new ArrayList<>();

    // Mối quan hệ với AddendumTerm
    @OneToMany(mappedBy = "addendum", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<AddendumTerm> addendumTerms = new ArrayList<>();

    @OneToMany(mappedBy = "addendum", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<AddendumItem> addendumItems = new ArrayList<>();


    @Column(name = "extend_contract_date")
    private LocalDateTime extendContractDate; // Ngày gia hạn hợp đồng

    @Column(name = "contract_expiration_date")
    private LocalDateTime contractExpirationDate; // Ngày hết hạn hợp đồng


    @Column(name = "is_effective_notified")
    private Boolean isEffectiveNotified = false;

    @Column(name = "is_expiry_notified")
    private Boolean isExpiryNotified = false;

    @Column(name = "contract_content", columnDefinition = "TEXT")
    private String contractContent;


}
