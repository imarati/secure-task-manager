package com.marat.taskmanager.securetaskmanager.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "app.pagination")
public class PaginationProperties {

    @Min(1)
    private int defaultPageSize = 10;

    @Min(1)
    @Max(1000)
    private int maxPageSize = 100;
}