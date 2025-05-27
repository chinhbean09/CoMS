package com.capstone.contractmanagement.entities;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "departments")
@Data
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Department {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "department_name")
    private String departmentName;
}
