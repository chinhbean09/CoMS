package com.capstone.contractmanagement.entities;

import com.capstone.contractmanagement.entities.contract_template.ContractTemplate;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;

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

    @Column(name = "phone_number", length = 10)
    private String phoneNumber;

    @Column(name = "address", length = 200)
    private String address;

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

    @Column(name = "facebook_account_id")
    private String facebookAccountId;

    @Column(name = "google_account_id")
    private String googleAccountId;

    @Column(name = "avatar")
    private String avatar;

    @Column(name = "is_ceo", nullable = true)
    private Boolean isCeo  = Boolean.FALSE;

    @Column(name = "failed_login_attempts")
    private Integer failedLoginAttempts = 0;

    @OneToMany(mappedBy = "createdBy", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<ContractTemplate> contractTemplates = new ArrayList<>();


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
        if (Boolean.TRUE.equals(isCeo)) {
            authorityList.add(new SimpleGrantedAuthority("ROLE_CEO"));
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
