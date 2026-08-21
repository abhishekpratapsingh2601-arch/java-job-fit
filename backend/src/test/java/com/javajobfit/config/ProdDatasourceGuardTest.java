package com.javajobfit.config;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class ProdDatasourceGuardTest {

    @Test
    void refusesToStartWhenDatabaseUrlIsMissing() {
        assertThatThrownBy(() -> ProdDatasourceGuard.validate(""))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("DATABASE_URL");
    }

    @Test
    void refusesToStartOnTheEmbeddedH2Fallback() {
        assertThatThrownBy(() -> ProdDatasourceGuard.validate("jdbc:h2:file:./data/javajobfit;AUTO_SERVER=TRUE"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("DATABASE_URL");
    }

    @Test
    void acceptsARealPostgresUrl() {
        assertThatCode(() -> ProdDatasourceGuard.validate(
                "jdbc:postgresql://aws-1-ap-northeast-1.pooler.supabase.com:5432/postgres?sslmode=require"))
                .doesNotThrowAnyException();
    }
}
