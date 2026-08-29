package com.marat.taskmanager.securetaskmanager.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "security.rate-limit")
public class RateLimitProperties {

    private boolean enabled;

    private EndpointLimit login = new EndpointLimit();

    private EndpointLimit register = new EndpointLimit();

    @Getter
    @Setter
    public static class EndpointLimit {

        private long capacity;

        private long refillTokens;

        private long refillMinutes;
    }
}