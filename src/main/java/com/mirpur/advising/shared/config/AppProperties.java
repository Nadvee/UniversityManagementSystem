package com.mirpur.advising.shared.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app") public record AppProperties(String namespace, String adminPassword, String jwtSecret, int tokenHours, boolean cacheEnabled, int holdSeconds, int cacheListMinutes, int cacheRoutineMinutes) {
}
