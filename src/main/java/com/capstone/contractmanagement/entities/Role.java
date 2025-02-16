package com.capstone.contractmanagement.entities;

import jakarta.persistence.*;
import lombok.*;

@Builder
@Entity
@Table(name = "roles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "role_id")
    private Long id;

    @Column(name = "role_name")
    private String roleName;

    public static String ADMIN = "ADMIN";
    public static String MANAGER = "MANAGER";
    public static String STAFF = "STAFF";


}
