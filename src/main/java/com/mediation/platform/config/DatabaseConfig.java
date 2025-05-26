package com.mediation.platform.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableJpaRepositories(basePackages = "com.mediation.platform.repository")
@EnableTransactionManagement
public class DatabaseConfig {
    // Configuration automatique via Spring Boot
}
