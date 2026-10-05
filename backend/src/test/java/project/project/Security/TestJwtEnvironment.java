package project.project.Security;

import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.Map;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

/** Available only on the test classpath; never uses a production JWT secret. */
public final class TestJwtEnvironment implements EnvironmentPostProcessor {
    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        byte[] key = new byte[32];
        new SecureRandom().nextBytes(key);
        environment.getPropertySources().addFirst(new MapPropertySource("random-test-jwt",
                Map.of("JWT_SECRET", HexFormat.of().formatHex(key))));
    }
}
