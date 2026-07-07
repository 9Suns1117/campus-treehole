package com.treehole.common;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.regex.Pattern;

public class PasswordUtil {
    private static final Pattern SHA256_HEX = Pattern.compile("^[a-fA-F0-9]{64}$");

    private PasswordUtil() {}

    public static String sha256(String rawPassword) {
        if (rawPassword == null) return null;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawPassword.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) sb.append('0');
                sb.append(hex);
            }
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("密码加密失败", e);
        }
    }

    public static boolean isSha256Hash(String value) {
        return value != null && SHA256_HEX.matcher(value).matches();
    }

    public static boolean matches(String rawPassword, String storedPassword) {
        if (rawPassword == null || storedPassword == null) return false;
        if (isSha256Hash(storedPassword)) {
            return sha256(rawPassword).equalsIgnoreCase(storedPassword);
        }
        return rawPassword.equals(storedPassword);
    }
}
