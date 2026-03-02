package com.skilora.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Centralized environment and configuration loader.
 *
 * Resolution order (highest priority wins):
 * <ol>
 *   <li>System environment variables ({@code System.getenv()})</li>
 *   <li>{@code .env} file at project root</li>
 *   <li>{@code config/application.properties} on classpath</li>
 * </ol>
 *
 * Singleton — use {@link #get(String)} or {@link #get(String, String)}.
 */
public final class EnvConfig {

    private static final Logger logger = LoggerFactory.getLogger(EnvConfig.class);

    /** Entries loaded from the .env file */
    private final Map<String, String> dotEnv;

    /** Entries loaded from application.properties */
    private final Properties appProps;

    // ── Singleton ──

    private static volatile EnvConfig instance;

    private EnvConfig() {
        this.dotEnv = loadDotEnv();
        this.appProps = loadAppProperties();
        logger.info("EnvConfig initialised — {} .env entries, {} app-properties entries",
                dotEnv.size(), appProps.size());
    }

    private static EnvConfig instance() {
        if (instance == null) {
            synchronized (EnvConfig.class) {
                if (instance == null) {
                    instance = new EnvConfig();
                }
            }
        }
        return instance;
    }

    // ── Public API ──

    /**
     * Resolve a configuration value.
     *
     * @param key property name (e.g. {@code "stripe.secret.key"}) <b>or</b>
     *            environment-variable name (e.g. {@code "STRIPE_SECRET_KEY"})
     * @return the resolved value, or {@code null} if not found anywhere
     */
    public static String get(String key) {
        return get(key, null);
    }

    /**
     * Resolve a configuration value with a fallback default.
     *
     * @param key          property name or env-var name
     * @param defaultValue returned when the key is not found in any source
     * @return the resolved value
     */
    public static String get(String key, String defaultValue) {
        EnvConfig cfg = instance();

        // 1. Derive both forms of the key
        String envKey = toEnvKey(key);   // stripe.secret.key -> STRIPE_SECRET_KEY
        String propKey = toPropKey(key); // STRIPE_SECRET_KEY -> stripe.secret.key

        // 2. System environment variables (highest priority)
        String val = System.getenv(envKey);
        if (val != null && !val.isBlank()) return val;

        // 3. .env file
        val = cfg.dotEnv.get(envKey);
        if (val != null && !val.isBlank()) return val;

        // 4. application.properties (check raw value,
        //    and resolve ${ENV:default} patterns if present)
        val = cfg.appProps.getProperty(propKey);
        if (val != null && !val.isBlank()) {
            String resolved = resolveInterpolation(val, cfg);
            if (resolved != null && !resolved.isBlank()) return resolved;
        }

        return defaultValue;
    }

    // ── Loaders ──

    private static Map<String, String> loadDotEnv() {
        Map<String, String> env = new HashMap<>();
        Path dotEnvPath = Paths.get(".env");
        if (!Files.exists(dotEnvPath)) {
            dotEnvPath = Paths.get(System.getProperty("user.dir", "."), ".env");
        }
        if (Files.exists(dotEnvPath)) {
            try (BufferedReader reader = Files.newBufferedReader(dotEnvPath)) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("#")) continue;
                    int eq = line.indexOf('=');
                    if (eq > 0) {
                        String k = line.substring(0, eq).trim();
                        String v = line.substring(eq + 1).trim();
                        // Strip surrounding quotes
                        if (v.length() >= 2
                                && ((v.startsWith("\"") && v.endsWith("\""))
                                || (v.startsWith("'") && v.endsWith("'")))) {
                            v = v.substring(1, v.length() - 1);
                        }
                        env.put(k, v);
                    }
                }
                logger.info("Loaded {} entries from {}", env.size(), dotEnvPath.toAbsolutePath());
            } catch (IOException e) {
                logger.warn("Could not read .env file: {}", e.getMessage());
            }
        } else {
            logger.info("No .env file found — relying on environment variables and application.properties");
        }
        return env;
    }

    private static Properties loadAppProperties() {
        Properties props = new Properties();
        try (InputStream is = EnvConfig.class.getClassLoader()
                .getResourceAsStream("config/application.properties")) {
            if (is != null) {
                props.load(is);
            } else {
                logger.warn("config/application.properties not found on classpath");
            }
        } catch (IOException e) {
            logger.error("Failed to load application.properties", e);
        }
        return props;
    }

    // ── Key conversion helpers ──

    /** {@code stripe.secret.key} → {@code STRIPE_SECRET_KEY} */
    private static String toEnvKey(String key) {
        return key.replace('.', '_').replace('-', '_').toUpperCase();
    }

    /** {@code STRIPE_SECRET_KEY} → {@code stripe.secret.key} */
    private static String toPropKey(String key) {
        return key.replace('_', '.').toLowerCase();
    }

    // ── Interpolation resolver ──

    /**
     * Resolves {@code ${ENV_VAR:default}} patterns found in property values.
     * <p>
     * If the value does not contain {@code ${}, it is returned as-is.
     */
    private static String resolveInterpolation(String raw, EnvConfig cfg) {
        if (raw == null) return null;
        if (!raw.contains("${")) return raw;

        // Pattern: ${ENV_VAR:defaultValue}
        int start = raw.indexOf("${");
        int end = raw.indexOf('}', start);
        if (end < 0) return raw;

        String inner = raw.substring(start + 2, end); // e.g. "STRIPE_SECRET_KEY:sk_test_PLACEHOLDER"
        String envVar;
        String defVal;
        int colon = inner.indexOf(':');
        if (colon >= 0) {
            envVar = inner.substring(0, colon).trim();
            defVal = inner.substring(colon + 1).trim();
        } else {
            envVar = inner.trim();
            defVal = null;
        }

        // Try System.getenv
        String resolved = System.getenv(envVar);
        if (resolved != null && !resolved.isBlank()) return resolved;

        // Try .env
        resolved = cfg.dotEnv.get(envVar);
        if (resolved != null && !resolved.isBlank()) return resolved;

        // Fallback to default
        return defVal;
    }
}
