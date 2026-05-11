package br.com.agenteflutuanteia;

import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

@SpringBootApplication
public class AgenteFlutuanteIaApplication extends SpringBootServletInitializer {

    public static void main(String[] args) {
        SpringApplication.run(AgenteFlutuanteIaApplication.class, args);
    }

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(AgenteFlutuanteIaApplication.class);
    }
}
