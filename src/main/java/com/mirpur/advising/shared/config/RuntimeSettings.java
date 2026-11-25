package com.mirpur.advising.shared.config;
import org.springframework.stereotype.Component;
@Component public record RuntimeSettings(AppProperties app,TeacherProperties teacher) {
    public static RuntimeSettings defaults() { return new RuntimeSettings(new AppProperties("mirpur","admin","mirpur-local-demo-secret-change-before-deployment-2026",8,true,600,30,10),new TeacherProperties(4)); }
}
