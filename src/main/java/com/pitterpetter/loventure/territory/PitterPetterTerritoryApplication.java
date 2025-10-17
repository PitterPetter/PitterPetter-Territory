package com.pitterpetter.loventure.territory;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients(basePackages = "com.pitterpetter.loventure.territory")
public class PitterPetterTerritoryApplication {

	public static void main(String[] args) {
		SpringApplication.run(PitterPetterTerritoryApplication.class, args);
	}

}
