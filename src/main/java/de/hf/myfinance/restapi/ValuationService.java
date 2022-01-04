package de.hf.myfinance.restapi;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;


public interface ValuationService {

	@GetMapping("/")
	String index();


}