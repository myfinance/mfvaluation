package de.hf.myfinance.mfvaluation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan("de.hf.myfinance")
public class ValuationServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(ValuationServiceApplication.class, args);
	}

}
