package com.qros.modules.table.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "table-session")
public class TableSessionProperties {

    private long noOrderExpireMinutes = 30;
}
