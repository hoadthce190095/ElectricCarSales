package com.electriccarsales.service;

import util.DBContext;

import java.sql.Connection;
import java.util.Arrays;

/**
 * Creates the first Admin account after the database migration has been applied.
 */
public final class BootstrapAdmin {
    private BootstrapAdmin() {
    }

    public static void main(String[] args) throws Exception {
        String username = System.getenv().getOrDefault("ADMIN_USERNAME", "admin");
        if (!username.matches("[A-Za-z0-9_.-]{3,50}")) {
            throw new IllegalArgumentException("ADMIN_USERNAME must contain 3-50 valid characters");
        }
        String password = System.getenv("ADMIN_PASSWORD");
        if (password == null && System.console() != null) {
            char[] characters = System.console().readPassword("Admin password (10-128 characters): ");
            if (characters != null) {
                password = new String(characters);
                Arrays.fill(characters, '\0');
            }
        }
        PasswordHasher.validate(password);

        try (Connection connection = DBContext.openConnection()) {
            new AdminSetupService().create(connection, username, password, "Tạo Admin đầu tiên bằng công cụ cục bộ");
            System.out.println("Created initial Admin: " + username);
        }
    }
}
