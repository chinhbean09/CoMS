package com.capstone.contractmanagement.repositories;

import com.capstone.contractmanagement.entities.template.Section;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ISectionRepository extends JpaRepository<Section, Long> {
}
