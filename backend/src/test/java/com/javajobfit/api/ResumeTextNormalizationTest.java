package com.javajobfit.api;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ResumeTextNormalizationTest {
    @Test
    void keepsLineStructureSoSectionsAndBulletsSurviveUpload() {
        String extracted = "  Rahul Verma \r\n\r\n\r\n\r\nEXPERIENCE\r\n-\tBuilt   REST APIs  in Java\n- Wrote JUnit tests  \n\n\n\nSKILLS\nJava, Spring Boot ";

        assertThat(ResumeExtractController.normalizeWhitespace(extracted))
                .isEqualTo("Rahul Verma\n\nEXPERIENCE\n- Built REST APIs in Java\n- Wrote JUnit tests\n\nSKILLS\nJava, Spring Boot");
    }
}
