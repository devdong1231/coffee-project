package com.coffeeproject.domain.outbox.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "coffee.outbox")
public class OutboxProperties {

    private String externalUrl;
    private int batchSize = 10;
    private int maxRetryCount = 3;
}
