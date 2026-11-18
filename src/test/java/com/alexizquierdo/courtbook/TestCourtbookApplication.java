package com.alexizquierdo.courtbook;

import org.springframework.boot.SpringApplication;

public class TestCourtbookApplication {

	public static void main(String[] args) {
		SpringApplication.from(CourtbookApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
