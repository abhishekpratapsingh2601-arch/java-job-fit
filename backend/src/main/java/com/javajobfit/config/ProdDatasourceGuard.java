package com.javajobfit.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Profiles;

/**
 * Refuses to start the prod profile against the embedded H2 fallback.
 *
 * <p>{@code application.yml} defaults {@code spring.datasource.url} to a local H2 file so
 * development needs no setup. In prod that default is never correct: reports — including paid
 * ones — would land in a file inside an ephemeral container. A missing {@code DATABASE_URL}
 * does already fail the boot, but it fails deep inside Flyway's pool setup with
 * "Driver org.postgresql.Driver claims to not accept jdbcUrl", which says nothing about the
 * actual cause. This replaces that with one line naming the variable to set.
 *
 * <p>Runs as an {@link EnvironmentPostProcessor} rather than a bean because the check must
 * happen <em>before</em> any DataSource or Flyway bean is created — a {@code @Component}
 * (even a non-lazy one) loses that race, which is how the confusing error survived. Ordered
 * last so config files and profiles are fully resolved by the time it reads them.
 */
public class ProdDatasourceGuard implements EnvironmentPostProcessor, Ordered {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        if (!environment.acceptsProfiles(Profiles.of("prod"))) {
            return;
        }
        validate(environment.getProperty("spring.datasource.url", ""));
    }

    /** Visible for testing. */
    static void validate(String datasourceUrl) {
        if (datasourceUrl == null || datasourceUrl.isBlank()) {
            throw new IllegalStateException(
                    "DATABASE_URL is not set. The prod profile will not start without a real database.");
        }
        if (datasourceUrl.startsWith("jdbc:h2:")) {
            throw new IllegalStateException(
                    "DATABASE_URL is missing or misconfigured: the prod profile resolved to the embedded H2 "
                            + "fallback (" + datasourceUrl + "), which would store reports in an ephemeral "
                            + "container file. Set DATABASE_URL to the Supabase pooler JDBC URL.");
        }
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
