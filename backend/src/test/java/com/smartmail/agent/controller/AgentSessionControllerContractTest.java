package com.smartmail.agent.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AgentSessionControllerContractTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void currentMailSessionAcceptsItemIdAlias() throws Exception {
        String token = registerAndGetToken();

        mockMvc.perform(post("/api/v1/agent/sessions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "scope": "CURRENT_MAIL",
                                  "itemId": 88
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.scope").value("CURRENT_MAIL"))
                .andExpect(jsonPath("$.data.context.mailItemId").value(88));
    }

    @Test
    void sessionMessageApiAcceptsContentAliasAndReturnsDisabledWhenAiPluginIsOff() throws Exception {
        String token = registerAndGetToken();

        String sessionJson = mockMvc.perform(post("/api/v1/agent/sessions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "scope": "GLOBAL",
                                  "context": {}
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.scope").value("GLOBAL"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String sessionId = objectMapper.readTree(sessionJson).at("/data/sessionId").asText();
        assertThat(sessionId).isNotBlank();

        mockMvc.perform(post("/api/v1/agent/sessions/{sessionId}/messages", sessionId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content":"帮我搜索 Google 的邮件"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.status").value("DISABLED"))
                .andExpect(jsonPath("$.data.assistantMessage.role").value("ASSISTANT"))
                .andExpect(jsonPath("$.data.pendingActions").isArray());
    }

    private String registerAndGetToken() throws Exception {
        String email = "agent-" + System.nanoTime() + "@smail.com";
        String response = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "username": "Agent Tester",
                                  "password": "Password123!"
                                }
                                """.formatted(email)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode root = objectMapper.readTree(response);
        return root.at("/data/token").asText();
    }
}