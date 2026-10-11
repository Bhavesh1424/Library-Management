package com.campus.library.security;

import com.campus.library.dto.auth.LoginRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class RbacSecurityRulesTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String studentToken;
    private String librarianToken;

    @BeforeEach
    void setUp() throws Exception {
        studentToken = obtainToken("student@campus.edu", "password123");
        librarianToken = obtainToken("cs.librarian@campus.edu", "password123");
    }

    private String obtainToken(String email, String password) throws Exception {
        LoginRequest req = new LoginRequest(email, password);
        MvcResult res = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode json = objectMapper.readTree(res.getResponse().getContentAsString());
        return json.get("token").asText();
    }

    @Test
    @DisplayName("RBAC: ROLE_STUDENT cannot access librarian /dispatch endpoint (403 Forbidden)")
    void testStudentCannotDispatch() throws Exception {
        mockMvc.perform(put("/api/v1/transfers/1/dispatch")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("RBAC: ROLE_STUDENT cannot access librarian /checkout endpoint (403 Forbidden)")
    void testStudentCannotCheckout() throws Exception {
        mockMvc.perform(post("/api/v1/loans/checkout")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"barcode\":\"BC-CS-101\",\"studentId\":1}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("RBAC: ROLE_STUDENT cannot access admin /admin/audit-logs (403 Forbidden)")
    void testStudentCannotAccessAuditLogs() throws Exception {
        mockMvc.perform(get("/api/v1/admin/audit-logs")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("RBAC: ROLE_LIBRARIAN cannot place student transfer requests (403 Forbidden)")
    void testLibrarianCannotRequestTransfer() throws Exception {
        mockMvc.perform(post("/api/v1/transfers/request")
                        .header("Authorization", "Bearer " + librarianToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"copyId\":1,\"destinationBranchId\":1}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("OpenAPI: /v3/api-docs is publicly accessible and contains API specification")
    void testOpenApiDocsAvailable() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Centralized Library Inter-Branch Book Exchange API")));
    }
}
