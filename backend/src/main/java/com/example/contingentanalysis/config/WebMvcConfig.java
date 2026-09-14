package com.example.contingentanalysis.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Bean
    public com.fasterxml.jackson.databind.ObjectMapper objectMapper() {
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        mapper.setPropertyNamingStrategy(com.fasterxml.jackson.databind.PropertyNamingStrategies.SNAKE_CASE);
        mapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        return mapper;
    }

    @Bean
    public org.springframework.http.converter.json.MappingJackson2HttpMessageConverter mappingJackson2HttpMessageConverter(com.fasterxml.jackson.databind.ObjectMapper objectMapper) {
        return new org.springframework.http.converter.json.MappingJackson2HttpMessageConverter(objectMapper);
    }

    @org.springframework.beans.factory.annotation.Value("${app.cors.allowed-origins:http://localhost:4200,http://127.0.0.1:4200,http://localhost:8000,http://127.0.0.1:8000}")
    private String allowedOriginsConfig;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        String[] origins = java.util.Arrays.stream(allowedOriginsConfig.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty() && !s.equals("*"))
                .toArray(String[]::new);

        registry.addMapping("/**")
                .allowedOrigins(origins)
                .allowedMethods("GET", "POST", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("X-Request-Id", "Content-Disposition")
                .allowCredentials(false);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Resolve frontend dist path relative to project root or workspace
        Path frontendDist = Path.of("../frontend/dist/frontend/browser").toAbsolutePath().normalize();
        if (!Files.exists(frontendDist)) {
            frontendDist = Path.of("frontend/dist/frontend/browser").toAbsolutePath().normalize();
        }

        if (Files.exists(frontendDist)) {
            String location = frontendDist.toUri().toString();
            if (!location.endsWith("/")) {
                location = location + "/";
            }
            registry.addResourceHandler("/**")
                    .addResourceLocations(location)
                    .resourceChain(true)
                    .addResolver(new PathResourceResolver() {
                        @Override
                        protected Resource getResource(String resourcePath, Resource location) throws IOException {
                            if (resourcePath.startsWith("api/")) {
                                return null;
                            }
                            Resource requested = location.createRelative(resourcePath);
                            if (requested.exists() && requested.isReadable()) {
                                return requested;
                            }
                            Resource index = location.createRelative("index.html");
                            return (index.exists() && index.isReadable()) ? index : null;
                        }
                    });
        }
    }
}

