package br.com.dti.msa.controller;

import jakarta.servlet.RequestDispatcher;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.WebAttributes;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(CustomErrorController.class)
@AutoConfigureMockMvc(addFilters = false)
public class CustomErrorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void testHandleError_AccessDenied_Returns403() throws Exception {
        mockMvc.perform(get("/error")
                .requestAttr(WebAttributes.ACCESS_DENIED_403, new AccessDeniedException("Denied")))
                .andExpect(view().name("error/403"));
    }

    @Test
    public void testHandleError_NotFound_Returns404() throws Exception {
        mockMvc.perform(get("/error")
                .requestAttr(RequestDispatcher.ERROR_STATUS_CODE, 404))
                .andExpect(view().name("error/404"));
    }

    @Test
    public void testHandleError_InternalServerError_Returns500() throws Exception {
        mockMvc.perform(get("/error")
                .requestAttr(RequestDispatcher.ERROR_STATUS_CODE, 500))
                .andExpect(view().name("error/500"));
    }

    @Test
    public void testHandleError_GenericError_ReturnsError() throws Exception {
        mockMvc.perform(get("/error"))
                .andExpect(view().name("error/error"));
    }
}