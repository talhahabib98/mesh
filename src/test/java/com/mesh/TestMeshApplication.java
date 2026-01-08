package com.mesh;

import org.springframework.boot.SpringApplication;

public class TestMeshApplication {

	public static void main(String[] args) {
		SpringApplication.from(MeshApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
