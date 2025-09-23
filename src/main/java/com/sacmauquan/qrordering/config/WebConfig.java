package com.sacmauquan.qrordering.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

  @Override
  public void addResourceHandlers(ResourceHandlerRegistry registry) {
    // Map URL /uploads/** -> thư mục thật ./uploads/ trong hệ thống file
    registry.addResourceHandler("/uploads/**")
            .addResourceLocations("file:uploads/")
            .setCachePeriod(3600); // cache 1h
  }
}
