package com.mirpur.advising.seed;

import com.mirpur.advising.shared.config.RuntimeSettings;
import com.mirpur.advising.account.PasswordSetupRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component public final class AdminSeeder implements Seeder {
    private final DataSeederRepository db;
    private final PasswordEncoder encoder;
    private final String password;
    private final PasswordSetupRepository passwords;
    public AdminSeeder(DataSeederRepository db, PasswordEncoder encoder, RuntimeSettings settings, PasswordSetupRepository passwords) {
        this.db = db;
        this.encoder = encoder;
        password = settings.app().adminPassword();
        this.passwords = passwords;
    }
    public void seed() {
        if (db.seedUsersCount() == 0) db.seedUsersUpdate("admin", encoder.encode(password));
        long adminId = passwords.lockAdmin();
        // Apply the requested admin credential on the first upgrade only, not every restart.
        if (!passwords.hasSetupRecord(adminId)) passwords.configureAdminOnce(adminId, encoder.encode(password));
    }
}
