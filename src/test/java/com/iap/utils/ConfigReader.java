package com.iap.utils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Properties;
import java.util.regex.Pattern;

/**
 * Reads config.properties from the classpath. Any value can be overridden from the
 * command line with -Dkey=value (e.g. -Dbrowser=edge), which is how we switch browsers.
 */
public final class ConfigReader {

    private static final Properties PROPS = new Properties();

    /** Declared before the static block below, which uses it. */
    private static final Pattern SECRET_KEY = Pattern.compile("(?i).*(password|passwd|secret|token)$");

    static {
        try (InputStream in = ConfigReader.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (in == null) {
                throw new IllegalStateException("config.properties not found on the classpath");
            }
            PROPS.load(in);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to load config.properties", e);
        }
        encryptPlainSecretsInSourceFile();
    }

    /**
     * Automatic protection: any plain-text password/secret/token in src/test/resources/config.properties is
     * rewritten as ENC(...) the first time tests run. This run keeps using the value already loaded;
     * from the next run on it is decrypted from the file. If the file cannot be found or written, nothing
     * is changed and tests run normally.
     */
    private static void encryptPlainSecretsInSourceFile() {
        Path source = Paths.get("src", "test", "resources", "config.properties");
        if (!Files.isWritable(source)) {
            return;
        }
        try {
            List<String> lines = Files.readAllLines(source, StandardCharsets.ISO_8859_1);
            boolean changed = false;
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);
                int eq = line.indexOf('=');
                if (eq <= 0 || line.trim().startsWith("#")) {
                    continue;
                }
                String key = line.substring(0, eq).trim();
                String value = line.substring(eq + 1).trim();
                if (SECRET_KEY.matcher(key).matches() && !value.isEmpty() && !SecretCrypto.isEncrypted(value)) {
                    lines.set(i, line.substring(0, eq + 1) + SecretCrypto.encrypt(value));
                    changed = true;
                }
            }
            if (changed) {
                Files.write(source, lines, StandardCharsets.ISO_8859_1);
                System.out.println("config.properties: plain-text secrets were encrypted automatically.");
            }
        } catch (Exception e) {
            System.err.println("Could not auto-encrypt config.properties secrets: " + e.getMessage());
        }
    }

    private ConfigReader() {
    }

    /**
     * Lookup order: -Dkey=value, then environment variable (key upper-cased, dots to underscores, e.g.
     * valid.password -> VALID_PASSWORD), then config.properties. A value written as ENC(...) is decrypted
     * with the CONFIG_KEY passphrase.
     */
    public static String get(String key) {
        String value = lookup(key);
        if (value == null) {
            throw new IllegalArgumentException("Missing configuration key: " + key);
        }
        return resolve(value);
    }

    /** Like get(), but returns the default instead of throwing when the key is not set. */
    public static String getOrDefault(String key, String defaultValue) {
        String value = lookup(key);
        return value == null || value.trim().isEmpty() ? defaultValue : resolve(value);
    }

    private static String lookup(String key) {
        String value = System.getProperty(key);
        if (value == null) {
            value = System.getenv(key.toUpperCase().replace('.', '_').replace('-', '_'));
        }
        return value != null ? value : PROPS.getProperty(key);
    }

    private static String resolve(String raw) {
        String value = raw.trim();
        return SecretCrypto.isEncrypted(value) ? SecretCrypto.decrypt(value) : value;
    }

    public static int getInt(String key) {
        return Integer.parseInt(get(key));
    }

    public static boolean getBoolean(String key) {
        return Boolean.parseBoolean(get(key));
    }
}
