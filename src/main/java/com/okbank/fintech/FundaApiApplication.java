package com.okbank.fintech;

import com.okbank.fintech.global.security.JwtProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(JwtProperties.class)
public class FundaApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(FundaApiApplication.class, args);
	}

}
