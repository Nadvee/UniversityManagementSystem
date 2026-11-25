package com.mirpur.advising.seed;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
@Component public class SeedRunner implements CommandLineRunner {
    private final DataSeeder data;
    public SeedRunner(DataSeeder data) { this.data=data; }
    @Transactional public void run(String...args) { data.seed(); }
}
