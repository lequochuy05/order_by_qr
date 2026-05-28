package com.sacmauquan.qrordering.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * WebConfig - Configures web application resources and settings.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

  /**
   * Adds resource handlers to map URL patterns to file system locations.
   * 
   * @param registry Resource handler registry
   */
  @Override
  public void addResourceHandlers(@NonNull ResourceHandlerRegistry registry) {
    registry.addResourceHandler("/uploads/**")
        .addResourceLocations("file:uploads/")
        .setCachePeriod(3600);
  }
}
