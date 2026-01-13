package com.mesh;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class MeshApplication {

	public static void main(String[] args) {
		SpringApplication.run(MeshApplication.class, args);
	}

}
