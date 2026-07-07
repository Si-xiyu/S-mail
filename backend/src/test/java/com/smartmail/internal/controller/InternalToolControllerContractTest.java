package com.smartmail.internal.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class InternalToolControllerContractTest {

    private static final String INTERNAL_TOKEN = "smartmail-internal-dev-token";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void contextEndpointRequiresToken() throws Exception {
        mockMvc.perform(get("/internal/v1/tools/mail-items/1/context")
                        .param("userId", "1"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    void contextEndpointAcceptsValidTokenAndReturnsUnifiedResponse() throws Exception {
        // No test data, so the service returns 404, but the token must be accepted.
        mockMvc.perform(get("/internal/v1/tools/mail-items/1/context")
                        .header("X-Internal-Token", INTERNAL_TOKEN)
                        .param("userId", "1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    void searchEndpointAcceptsValidTokenAndReturnsRecordsArray() throws Exception {
        mockMvc.perform(get("/internal/v1/tools/mail-search")
                        .header("X-Internal-Token", INTERNAL_TOKEN)
                        .param("userId", "1")
                        .param("keyword", "project")
                        .param("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void analysisResultsEndpointAcceptsValidToken() throws Exception {
        String payload = """
                {
                  "mailId": 1,
                  "userId": 1,
                  "resultType": "ANALYSIS",
                  "resultJson": "{\\"summary\\":[]}",
                  "status": "SUCCEEDED"
                }
                """;
        // No visible mailbox item for mailId=1, service returns 404, but token is accepted.
        mockMvc.perform(post("/internal/v1/tools/analysis-results")
                        .header("X-Internal-Token", INTERNAL_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    void mailActionsExecuteEndpointRequiresToken() throws Exception {
        mockMvc.perform(post("/internal/v1/tools/mail-actions/execute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":1,\"itemId\":1,\"action\":\"MARK_READ\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
    }
}
