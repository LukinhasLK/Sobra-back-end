package com.Sobra.Aplicativo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class AplicativoApplication {

	public static void main(String[] args) {
		SpringApplication.run(AplicativoApplication.class, args);
	}

}
