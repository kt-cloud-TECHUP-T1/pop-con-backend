package com.t1.popcon.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@Profile("default || local")
public class SwaggerConfig implements WebMvcConfigurer {

	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		String projectPath = System.getProperty("user.dir");

		registry.addResourceHandler("/swagger-ui/**")
			.addResourceLocations(
				"file:" + projectPath + "/build/api-spec/",
				"classpath:/static/swagger-ui/"
			);

		registry.addResourceHandler("/favicon.ico")
			.addResourceLocations("classpath:/static/");
	}
}