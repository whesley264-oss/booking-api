package com.template.booking.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .components(new Components())
                .info(new Info()
                        .title("Booking API")
                        .version("1.0.0")
                        .description("API de Agendamento de Recursos com gestão de conflitos de horário")
                        .contact(new Contact()
                                .name("Template Team")
                                .email("template@example.com")));
    }
}
