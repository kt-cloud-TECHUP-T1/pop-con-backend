package com.t1.popcon.popup.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@Profile({"default", "local", "staging", "prod"})
public class SwaggerConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/popup/swagger-ui/**")
                .addResourceLocations("classpath:/static/swagger-ui/");

        registry.addResourceHandler("/popup/favicon.ico")
                .addResourceLocations("classpath:/static/");
    }
}
