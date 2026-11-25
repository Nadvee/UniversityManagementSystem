package com.mirpur.advising.seed;

import java.util.*;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component public class DataSeeder {
    private final AdminSeeder admin;
    private final ReferenceDataSeeder reference;
    public DataSeeder(AdminSeeder admin, ReferenceDataSeeder reference) {
        this.admin = admin;
        this.reference = reference;
    }
    @Transactional public void seed() {
        admin.seed();
        reference.seed();
    }
}
