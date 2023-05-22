package com.example.servicedemo;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.data.annotation.Id;
import org.springframework.data.repository.CrudRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Repository;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;

@SpringBootApplication
public class ServiceDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(ServiceDemoApplication.class, args);
    }

    @Bean
    ApplicationListener<ApplicationReadyEvent> readyEventApplicationListener(CustomerRepository repository) {
        return event -> repository.findAll().forEach(System.out::println);
    }

}

record Customer(@Id Integer id, String name) {
}

@Repository
interface CustomerRepository extends CrudRepository<Customer, Integer> {
	Iterable<Customer> findByName(String name);
}

@Controller
@ResponseBody
class CustomersController {

    private final CustomerRepository customerRepository;
	private final ObservationRegistry registry;

    CustomersController(CustomerRepository customerRepository, ObservationRegistry registry) {
        this.customerRepository = customerRepository;
		this.registry = registry;
	}

    @GetMapping("/customers")
    public Iterable<Customer> getAll() {
        return customerRepository.findAll();
    }

	@GetMapping("/customers/{name}")
	public Iterable<Customer> getByName(@PathVariable String name) {
		Assert.state(Character.isUpperCase(name.charAt(0)), "name must start with uppercase");

		return Observation.createNotStarted("by-name", this.registry)
				.observe(() -> customerRepository.findByName(name));
	}
}

@ControllerAdvice
class ErrorHandlingControllerAdvise {
	@ExceptionHandler
	public ProblemDetail handle (IllegalStateException is, HttpServletRequest request) {
		request.getHeaderNames().asIterator().forEachRemaining(System.out::println);
		var pd = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST.value());
		pd.setDetail(is.getMessage());
		return pd;
	}
}
