package com.dataloader.config;

import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.servers.Server;

@Configuration
@OpenAPIDefinition(

		info = @Info(
				title = "CSV Upload Task",
				version = "1.0", 
				description = "CSV Upload Task API Documentation",
				contact = @Contact(url = "http://localhost:8080/swagger-ui/index.html#/")),
		servers = {
				@Server(
						url = "http://Localhost:8080", 
						description = "CSV Upload Task Open API url"
				) }

)
public class SwaggerConfig {

}
