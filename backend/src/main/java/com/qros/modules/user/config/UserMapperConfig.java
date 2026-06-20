package com.qros.modules.user.config;

import com.qros.modules.user.mapper.UserMapper;
import org.mapstruct.factory.Mappers;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UserMapperConfig {

    @Bean
    @ConditionalOnMissingBean(UserMapper.class)
    UserMapper userMapper() {
        return Mappers.getMapper(UserMapper.class);
    }
}
