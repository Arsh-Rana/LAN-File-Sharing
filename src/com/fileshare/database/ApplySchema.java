package com.fileshare.database;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.Properties;

public class ApplySchema {
    public static void main(String[] args) {
        System.out.println("========================================================");
        System.out.println("  LAN File Sharing - MySQL Schema Application Tool");
        System.out.println("========================================================");

        File schemaFile = new File("sql/schema.sql");
        String cliPassword = null;

        for (int i = 0; i < args.length; i++) {
            if ("-p".equalsIgnoreCase(args[i]) && i + 1 < args.length) {
                cliPassword = args[++i];
            } else if (!args[i].startsWith("-")) {
                File f = new File(args[i]);
                if (f.exists()) {
                    schemaFile = f;
                }
            }
        }

        if (!schemaFile.exists()) {
            schemaFile = new File("schema.sql");
        }

        if (!schemaFile.exists()) {
            System.err.println("Error: Schema file not found: " + schemaFile.getAbsolutePath());
            System.exit(1);
        }

        System.out.println("Using schema file: " + schemaFile.getAbsolutePath());

        Properties props = new Properties();
        File propFile = new File("resources/db.properties");
        if (propFile.exists()) {
            try (InputStream in = new FileInputStream(propFile)) {
                props.load(in);
            } catch (Exception ignored) {}
        }

        String driver = props.getProperty("db.driver", "com.mysql.cj.jdbc.Driver");
        String url = props.getProperty("db.url", "jdbc:mysql://localhost:3306/fileshare_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&createDatabaseIfNotExist=true");
        String user = props.getProperty("db.user", "root");
        String password = cliPassword != null ? cliPassword : props.getProperty("db.password", "");

        try {
            Class.forName(driver);
            try (Connection conn = DriverManager.getConnection(url, user, password);
                 Statement stmt = conn.createStatement()) {

                System.out.println("Connected to MySQL successfully!");
                StringBuilder sb = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new FileReader(schemaFile))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        String trimmed = line.trim();
                        if (trimmed.startsWith("--") || trimmed.startsWith("/*") || trimmed.isEmpty()) {
                            continue;
                        }
                        sb.append(line).append("\n");
                    }
                }

                String[] statements = sb.toString().split(";");
                int count = 0;
                for (String rawSql : statements) {
                    String sql = rawSql.trim();
                    if (!sql.isEmpty()) {
                        try {
                            stmt.execute(sql);
                            count++;
                            System.out.println("  [OK] " + getFirstLine(sql));
                        } catch (Exception ex) {
                            System.err.println("  [FAIL] " + getFirstLine(sql) + " -> " + ex.getMessage());
                        }
                    }
                }

                System.out.println("\nSchema applied successfully (" + count + " statements executed).");
                System.out.println("========================================================");
            }
        } catch (Exception e) {
            System.err.println("Database operation failed: " + e.getMessage());
            System.exit(1);
        }
    }

    private static String getFirstLine(String sql) {
        int idx = sql.indexOf('\n');
        if (idx > 0) {
            return sql.substring(0, idx).trim();
        }
        return sql.trim();
    }
}
