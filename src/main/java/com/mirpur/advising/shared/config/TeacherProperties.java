package com.mirpur.advising.shared.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "teacher") public record TeacherProperties(int maxCourses) {
}
