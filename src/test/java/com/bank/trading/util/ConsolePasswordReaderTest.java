package com.bank.trading.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.*;

class ConsolePasswordReaderTest {

    @Test
    @DisplayName("readPassword should read input correctly from Scanner fallback")
    void testReadPasswordFromScanner() {
        String inputData = "mySecretPassword123\n";
        Scanner scanner = new Scanner(new ByteArrayInputStream(inputData.getBytes()));

        String pass = ConsolePasswordReader.readPassword(scanner, "Password: ");
        assertEquals("mySecretPassword123", pass);
    }

    @Test
    @DisplayName("readConfirmedPassword should accept matching passwords")
    void testReadConfirmedPasswordMatching() {
        String inputData = "Pass123!\nPass123!\n";
        Scanner scanner = new Scanner(new ByteArrayInputStream(inputData.getBytes()));

        String pass = ConsolePasswordReader.readConfirmedPassword(scanner, "Password: ", "Confirm Password: ");
        assertEquals("Pass123!", pass);
    }

    @Test
    @DisplayName("readConfirmedPassword should re-prompt on mismatch until valid match is supplied")
    void testReadConfirmedPasswordMismatchThenMatch() {
        String inputData = "Pass123!\nWrongPass!\nPass123!\nPass123!\n";
        Scanner scanner = new Scanner(new ByteArrayInputStream(inputData.getBytes()));

        String pass = ConsolePasswordReader.readConfirmedPassword(scanner, "Password: ", "Confirm Password: ");
        assertEquals("Pass123!", pass);
    }
}
