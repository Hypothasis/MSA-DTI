package br.com.dti.msa.controller;

import br.com.dti.msa.dto.KeycloakTokenResponseDTO;
import br.com.dti.msa.dto.LoginRequestDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@RestController
public class AuthenticationController {
    @Value("${keycloak.token-uri}")
    private String keycloakTokenUri;
    
    @Value("${keycloak.client-id}")
    private String clientId;
    
    @Value("${keycloak.client-secret}")
    private String clientSecret;

    @PostMapping("/token")
    public ResponseEntity<?> getToken(@RequestBody LoginRequestDTO loginRequest) {
        RestTemplate restTemplate = new RestTemplate();
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("client_id", clientId);
        map.add("client_secret", clientSecret);
        map.add("grant_type", "password");
        map.add("username", loginRequest.getUsername());
        map.add("password", loginRequest.getPassword());

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);

        try {
            // Envia a requisição para o Keycloak
            ResponseEntity<KeycloakTokenResponseDTO> response = restTemplate.postForEntity(
                keycloakTokenUri, request, KeycloakTokenResponseDTO.class);
            
            // Retorna o corpo da resposta do Keycloak para o cliente (Postman)
            return ResponseEntity.ok(response.getBody());
        } catch (HttpClientErrorException e) {
            // Se o Keycloak retornar um erro (ex: 401 Unauthorized), repassa o erro
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        }
    }
}
