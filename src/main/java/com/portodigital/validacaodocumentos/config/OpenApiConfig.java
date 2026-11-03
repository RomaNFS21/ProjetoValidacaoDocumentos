package com.portodigital.validacaodocumentos.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuração da documentação Swagger / OpenAPI 3.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("API de Validação Automática de Documentos")
                        .version("1.0.0")
                        .description("API REST para validação inteligente de documentos (comprovantes de residência) com pré-processamento via OpenCV, extração OCR via Tesseract, estruturação com Spring AI e validação de regras de negócio em Java.")
                        .contact(new Contact()
                                .name("Porto Digital - Residência Tecnológica")
                                .email("suporte@portodigital.com"))
                        .license(new License().name("Apache 2.0").url("https://springdoc.org")));
    }
}
