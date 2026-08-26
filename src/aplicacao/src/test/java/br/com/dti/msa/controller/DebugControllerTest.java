package br.com.dti.msa.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DebugController.class)
public class DebugControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void testGetCurrentUser_Authenticated_ReturnsClaims() throws Exception {
        mockMvc.perform(get("/debug/user")
                .with(oidcLogin().idToken(token -> {
                    token.claim("preferred_username", "msa_admin");
                    token.claim("name", "Administrador MSA");
                })))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username_from_token").value("msa_admin"))
                .andExpect(jsonPath("$.user_full_name").value("Administrador MSA"));
    }
}