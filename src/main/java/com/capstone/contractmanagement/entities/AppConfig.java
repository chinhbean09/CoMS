package com.capstone.contractmanagement.entities;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "app_configs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppConfig {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "config_key", unique = true, nullable = false, length = 100)
    private String key;

    @Column(name = "config_value", columnDefinition = "TEXT")
    private String value;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
}