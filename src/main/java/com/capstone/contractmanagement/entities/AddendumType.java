package com.capstone.contractmanagement.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "addendum_types")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddendumType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

}
