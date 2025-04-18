package com.capstone.contractmanagement.entities;

import com.capstone.contractmanagement.entities.approval_workflow.ApprovalWorkflow;
import com.capstone.contractmanagement.entities.contract.Contract;
import com.capstone.contractmanagement.entities.contract_template.ContractTemplate;
import com.capstone.contractmanagement.enums.DepartmentList;
import com.capstone.contractmanagement.enums.GenderList;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(name = "users")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class User extends BaseEntity implements UserDetails, OAuth2User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "full_name", length = 100)
    private String fullName;

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    @Column(name = "address", length = 200)
    private String address;

    @Column(name = "date_of_birth")
    private LocalDateTime DateOfBirth;

//    @Column(name = "department", length = 100)
//    @Enumerated(EnumType.STRING)
//    private DepartmentList department;

    @ManyToOne
    @JoinColumn(
            name = "department_id",
            foreignKey = @ForeignKey(
                    name = "fk_department",
                    foreignKeyDefinition = "FOREIGN KEY (department_id) REFERENCES departments(id) ON DELETE SET NULL"
            )
    )
    private Department department;

    @Column(name = "email")
    private String email;

    @Column(name = "password", length = 200, nullable = false)
    private String password;

    @Column(name = "is_active")
    private boolean active;

    @ManyToOne
    @JoinColumn(name = "role_id", columnDefinition = "bigint")
    private Role role;

    @Column(name = "modified_by")
    private String modifiedBy;

    @Column(name = "avatar")
    private String avatar;

    @Column(name = "gender")
    @Enumerated(EnumType.STRING)
    private GenderList gender;

    @Column(name = "failed_login_attempts")
    private Integer failedLoginAttempts = 0;

    @Column(name = "staff_code")
    private String staffCode;

    @OneToMany(mappedBy = "createdBy", fetch = FetchType.LAZY)
    @JsonIgnore
    private List<ContractTemplate> contractTemplates = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<Partner> partners = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<ApprovalWorkflow> approvalWorkflows = new ArrayList<>();

    @Override
    public Map<String, Object> getAttributes() {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("fullName", this.fullName);
        return attributes;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        List<SimpleGrantedAuthority> authorityList = new ArrayList<>();
        if (role != null) {
            authorityList.add(new SimpleGrantedAuthority("ROLE_" + role.getRoleName()));
        }
        return authorityList;
    }

    @Override
    public String getUsername() {
        if (email != null && !email.isEmpty()) {
            return email;
        } else if (phoneNumber != null && !phoneNumber.isEmpty()) {
            return phoneNumber;
        }
        return "";
    }

    public boolean isManager() {
        return role != null && "MANAGER".equalsIgnoreCase(role.getRoleName());
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public String getName() {
        return getAttribute("fullName");
    }
    @Override
    public String getPassword() {
        return this.password;
    }

}
