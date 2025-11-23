package com.cpd.watermarker_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class WatermarkerServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(WatermarkerServiceApplication.class, args);
	}

}
