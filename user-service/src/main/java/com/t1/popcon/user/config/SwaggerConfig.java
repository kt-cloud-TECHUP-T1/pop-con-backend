package com.t1.popcon.user.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@Profile({"default", "local", "staging", "prod"})
public class SwaggerConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String projectPath = System.getProperty("user.dir");

        registry.addResourceHandler("/user/swagger-ui/**")
                .addResourceLocations(
                        "file:" + projectPath + "/build/api-spec/",
                        "classpath:/static/swagger-ui/"
                );

        registry.addResourceHandler("/user/favicon.ico")
                .addResourceLocations("classpath:/static/");
    }
}