package com.dariom.wds.config.swagger;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
    info = @Info(
        title = "Wordle Duel Service",
        version = "v1",
        description = "Backend service for the Wordle Duel game"
    )
)
public class OpenApiConfig {

}
