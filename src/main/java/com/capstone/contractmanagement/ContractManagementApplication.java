package com.capstone.contractmanagement;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ContractManagementApplication {

	public static void main(String[] args) {
		SpringApplication.run(ContractManagementApplication.class, args);
	}

}
