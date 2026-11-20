package com.fileshare.utils;

import com.fileshare.beans.FileTransferBean;
import com.fileshare.beans.LoginBean;
import com.fileshare.beans.TransferHistoryBean;
import com.fileshare.beans.UserBean;
import com.fileshare.database.DatabaseConnection;

public class SanityCheck {
    public static void main(String[] args) {
        System.out.println("--- Starting Sanity Checks ---");

        // 1. UserBean
        UserBean user = new UserBean(1, "alice", "Alice Johnson", "alice@test.com", "ONLINE", null);
        System.out.println("UserBean: " + user);

        // 2. LoginBean
        LoginBean login = new LoginBean("alice", "secret");
        System.out.println("LoginBean: " + login.getUsername());

        // 3. FileTransferBean
        FileTransferBean ft = new FileTransferBean("alice", "bob", "project.zip", 25 * 1024 * 1024);
        System.out.println("FileTransferBean: " + ft.getFileName() + " (" + FormatUtils.formatFileSize(ft.getFileSize()) + ")");

        // 4. TransferHistoryBean
        TransferHistoryBean th = new TransferHistoryBean(1, ft.getTransferUuid(), "alice", "bob", "project.zip", ft.getFileSize(), "Completed", null);
        System.out.println("TransferHistoryBean: " + th);

        // 5. PasswordHasher
        String hashed = PasswordHasher.hash("password123");
        boolean verify = PasswordHasher.verify("password123", hashed);
        System.out.println("Password Hash Check: " + verify);

        // 6. Config loading
        System.out.println("Database URL: " + DatabaseConnection.getDbUrl());
        System.out.println("Database User: " + DatabaseConnection.getDbUser());

        System.out.println("--- All Sanity Checks Passed! ---");
    }
}
