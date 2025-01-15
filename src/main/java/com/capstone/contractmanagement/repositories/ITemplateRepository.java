package com.capstone.contractmanagement.repositories;

import com.capstone.contractmanagement.entities.Token;
import com.capstone.contractmanagement.entities.template.Template;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ITemplateRepository extends JpaRepository<Template, Long> {
}
