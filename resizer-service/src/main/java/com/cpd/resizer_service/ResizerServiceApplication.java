package com.cpd.resizer_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class ResizerServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(ResizerServiceApplication.class, args);
	}

}
