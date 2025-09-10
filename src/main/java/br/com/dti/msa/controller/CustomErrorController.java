package br.com.dti.msa.controller;

import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.WebAttributes; // <-- IMPORTANTE
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;

@Controller
public class CustomErrorController implements ErrorController {

    @RequestMapping("/error")
    public String handleError(HttpServletRequest request) {
        // Pega a exceção do atributo específico do SPRING SECURITY
        Object exception = request.getAttribute(WebAttributes.ACCESS_DENIED_403);
        
        // Se a exceção for de Acesso Negado, o status é 403
        if (exception instanceof AccessDeniedException) {
            return "error/403";
        }
        
        // Pega o status do erro do mecanismo padrão do Spring Boot
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        
        // Lógica para outros erros HTTP (404, 500, etc.)
        if (status != null) {
            int statusCode = Integer.parseInt(status.toString());

            if (statusCode == HttpStatus.NOT_FOUND.value()) {
                return "error/404";
            } else if (statusCode == HttpStatus.INTERNAL_SERVER_ERROR.value()) {
                return "error/500";
            }
        }

        // Retorna uma página de erro genérica para qualquer outro caso
        return "error/error";
    }
}