package com.smartmail.common.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerHttpTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ThrowingController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void mapsBusinessCodesToMatchingHttpStatuses() throws Exception {
        mockMvc.perform(get("/test/errors/401"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
        mockMvc.perform(get("/test/errors/403"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
        mockMvc.perform(get("/test/errors/404"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));
        mockMvc.perform(get("/test/errors/422"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(422));
    }

    @RestController
    static class ThrowingController {
        @GetMapping("/test/errors/401")
        void unauthorized() { throw new BusinessException(401, "unauthorized"); }

        @GetMapping("/test/errors/403")
        void forbidden() { throw new BusinessException(403, "forbidden"); }

        @GetMapping("/test/errors/404")
        void notFound() { throw new BusinessException(404, "not found"); }

        @GetMapping("/test/errors/422")
        void other() { throw new BusinessException(422, "invalid"); }
    }
}
