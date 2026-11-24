package com.alexizquierdo.courtbook.shared.infrastructure.rest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

// No real endpoint reads X-User-Id yet, so a probe controller stands in for them.
@WebMvcTest(UserIdHeaderTest.ProbeController.class)
@Import(UserIdHeaderTest.ProbeController.class)
class UserIdHeaderTest {

    @RestController
    static class ProbeController {

        @GetMapping("/probe")
        String probe(@RequestHeader(UserIdHeader.NAME) UUID requesterId) {
            return requesterId.toString();
        }
    }

    @Autowired
    private MockMvcTester mvc;

    @ParameterizedTest
    @ValueSource(strings = {"3f2b8c1e-7a4d-4e9b-9c61-2d5e8f0a1b7c", "3F2B8C1E-7A4D-4E9B-9C61-2D5E8F0A1B7C"})
    void resolvesAValidHeaderAsUuid(String header) {
        assertThat(mvc.get().uri("/probe").header(UserIdHeader.NAME, header))
                .hasStatus(HttpStatus.OK)
                .hasBodyTextEqualTo("3f2b8c1e-7a4d-4e9b-9c61-2d5e8f0a1b7c");
    }

    @Test
    void returns400WhenTheHeaderIsMissing() {
        MvcTestResult result = mvc.get().uri("/probe").exchange();

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("BAD_REQUEST");
    }

    // The last two are accepted by UUID.fromString, which is lenient.
    @ParameterizedTest
    @ValueSource(strings = {"not-a-uuid", "1-2-3-4-5", "+0000001-0000-0000-0000-000000000000"})
    void returns400WhenTheHeaderIsNotAUuid(String header) {
        MvcTestResult result = mvc.get().uri("/probe").header(UserIdHeader.NAME, header).exchange();

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("BAD_REQUEST");
    }
}
