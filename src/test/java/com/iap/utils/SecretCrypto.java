package com.iap.utils;

import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

/**
 * Encrypts/decrypts config values with AES-256-GCM. The key is derived from a passphrase that is NOT stored
 * in the repo: set it as the environment variable CONFIG_KEY (or -Dconfig.key=...).
 *
 * Encrypt a value (prints ENC(...) to paste into config.properties):
 *   $env:CONFIG_KEY = "your-passphrase"
 *   java -cp target\test-classes com.iap.utils.SecretCrypto "the-password"
 */
public final class SecretCrypto {

    private static final int SALT_BYTES = 16;
    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;
    private static final int PBKDF2_ITERATIONS = 120_000;

    private SecretCrypto() {
    }

    public static boolean isEncrypted(String value) {
        return value != null && value.startsWith("ENC(") && value.endsWith(")");
    }

    public static String encrypt(String plain) {
        try {
            byte[] salt = random(SALT_BYTES);
            byte[] iv = random(IV_BYTES);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, deriveKey(salt, true), new GCMParameterSpec(TAG_BITS, iv));
            byte[] cipherText = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));

            byte[] payload = new byte[SALT_BYTES + IV_BYTES + cipherText.length];
            System.arraycopy(salt, 0, payload, 0, SALT_BYTES);
            System.arraycopy(iv, 0, payload, SALT_BYTES, IV_BYTES);
            System.arraycopy(cipherText, 0, payload, SALT_BYTES + IV_BYTES, cipherText.length);
            return "ENC(" + Base64.getEncoder().encodeToString(payload) + ")";
        } catch (Exception e) {
            throw new IllegalStateException("Could not encrypt value", e);
        }
    }

    public static String decrypt(String encValue) {
        try {
            String b64 = encValue.substring(4, encValue.length() - 1);
            byte[] payload = Base64.getDecoder().decode(b64);
            byte[] salt = Arrays.copyOfRange(payload, 0, SALT_BYTES);
            byte[] iv = Arrays.copyOfRange(payload, SALT_BYTES, SALT_BYTES + IV_BYTES);
            byte[] cipherText = Arrays.copyOfRange(payload, SALT_BYTES + IV_BYTES, payload.length);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, deriveKey(salt, false), new GCMParameterSpec(TAG_BITS, iv));
            return new String(cipher.doFinal(cipherText), StandardCharsets.UTF_8);
        } catch (javax.crypto.AEADBadTagException e) {
            throw new IllegalStateException("Could not decrypt a config value: wrong CONFIG_KEY?", e);
        } catch (Exception e) {
            throw new IllegalStateException("Could not decrypt a config value", e);
        }
    }

    /** Local key file, outside the project/repo: C:\Users\&lt;you&gt;\.iap-selenium.key */
    private static Path keyFile() {
        return Paths.get(System.getProperty("user.home"), ".iap-selenium.key");
    }

    /**
     * Passphrase lookup: -Dconfig.key, then env CONFIG_KEY, then the local key file. When encrypting
     * and none exists yet, a random key is generated and saved to the key file automatically.
     */
    private static String passphrase(boolean createIfMissing) throws Exception {
        String passphrase = System.getProperty("config.key", System.getenv("CONFIG_KEY"));
        if (passphrase != null && !passphrase.isEmpty()) {
            return passphrase;
        }
        Path file = keyFile();
        if (Files.exists(file)) {
            return new String(Files.readAllBytes(file), StandardCharsets.UTF_8).trim();
        }
        if (!createIfMissing) {
            throw new IllegalStateException(
                    "No decryption key found. Set CONFIG_KEY (or -Dconfig.key), or copy the key file "
                            + file + " from the machine that encrypted config.properties.");
        }
        String generated = Base64.getEncoder().encodeToString(random(32));
        Files.write(file, generated.getBytes(StandardCharsets.UTF_8));
        System.out.println("Created local encryption key file: " + file + " (keep it private, never commit it)");
        return generated;
    }

    private static SecretKeySpec deriveKey(byte[] salt, boolean createIfMissing) throws Exception {
        String passphrase = passphrase(createIfMissing);
        PBEKeySpec spec = new PBEKeySpec(passphrase.toCharArray(), salt, PBKDF2_ITERATIONS, 256);
        byte[] key = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        return new SecretKeySpec(key, "AES");
    }

    private static byte[] random(int length) {
        byte[] bytes = new byte[length];
        new SecureRandom().nextBytes(bytes);
        return bytes;
    }

    /** Command-line helper: prints ENC(...) for the text given as the first argument. */
    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Usage: java -cp target\\test-classes com.iap.utils.SecretCrypto \"<text to encrypt>\"");
            System.exit(1);
        }
        System.out.println(encrypt(args[0]));
    }
}
