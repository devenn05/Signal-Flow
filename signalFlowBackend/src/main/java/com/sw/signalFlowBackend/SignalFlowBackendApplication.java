package com.sw.signalFlowBackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SignalFlowBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(SignalFlowBackendApplication.class, args);
	}

}
