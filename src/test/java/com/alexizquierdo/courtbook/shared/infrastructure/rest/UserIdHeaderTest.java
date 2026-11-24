package com.alexizquierdo.courtbook.shared.infrastructure.rest;

import org.junit.jupiter.api.Test;
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

    @Test
    void resolvesAValidHeaderAsUuid() {
        UUID id = UUID.randomUUID();

        assertThat(mvc.get().uri("/probe").header(UserIdHeader.NAME, id.toString()))
                .hasStatus(HttpStatus.OK)
                .hasBodyTextEqualTo(id.toString());
    }

    @Test
    void returns400WhenTheHeaderIsMissing() {
        MvcTestResult result = mvc.get().uri("/probe").exchange();

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("BAD_REQUEST");
    }

    @Test
    void returns400WhenTheHeaderIsNotAUuid() {
        MvcTestResult result = mvc.get().uri("/probe").header(UserIdHeader.NAME, "not-a-uuid").exchange();

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("BAD_REQUEST");
    }
}
