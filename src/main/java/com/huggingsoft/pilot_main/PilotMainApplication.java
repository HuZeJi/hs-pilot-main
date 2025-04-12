package com.huggingsoft.pilot_main;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableJpaRepositories(basePackages = "com.huggingsoft.pilot_main.repository")
@EntityScan(basePackages = {"com.hsoft.model.entities"})
@ComponentScan(basePackages = {"com.huggingsoft.pilot_main", "com.hsoft.model"})
public class PilotMainApplication {

	public static void main(String[] args) {
		SpringApplication.run(PilotMainApplication.class, args);
	}

}
