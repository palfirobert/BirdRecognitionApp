package com.example.birdrecognitionapp.utils;

import org.mindrot.jbcrypt.BCrypt;

public class PasswordUtils {
    /**
     * Hash a password using BCrypt.
     *
     * @param password the password to hash
     * @return the hashed password
     */
    public static String hashPassword(String password) {
        return BCrypt.hashpw(password, BCrypt.gensalt());
    }

    /**
     * Check a password against a hashed password.
     *
     * @param password       the plain text password
     * @param hashedPassword the hashed password to check against
     * @return true if the password matches the hash, false otherwise
     */
    public static boolean checkPassword(String password, String hashedPassword) {
        return BCrypt.checkpw(password, hashedPassword);
    }
}
