package br.com.agenteflutuanteia.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class AiClientConfig {


    @Value("${gemini.api-url}")
    private String apiUrl;

    @Bean
    public RestClient aiRestClient() {
        return RestClient.builder()
                .baseUrl(apiUrl)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }
}


