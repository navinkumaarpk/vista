package com.vista;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import com.vista.monitor.MonitorProperties;

@SpringBootApplication
@EnableConfigurationProperties(MonitorProperties.class)
public class VistaApplication {

	public static void main(String[] args) {
		SpringApplication.run(VistaApplication.class, args);
	}

}
