package com.capstone.contractmanagement.repositories;

import com.capstone.contractmanagement.entities.AuditTrail;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IAuditTrailRepository extends JpaRepository<AuditTrail, Long> {
}
