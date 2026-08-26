package br.com.dti.msa.controller;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
public class DebugController {

    @GetMapping("/debug/user")
    public Map<String, Object> getCurrentUser(@AuthenticationPrincipal OidcUser principal) {
        if (principal == null) {
            return Map.of("error", "Nenhum usuário autenticado.");
        }

        var authorities = principal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        Map<String, Object> claims = principal.getClaims();

        return Map.of(
            "username_from_token", principal.getPreferredUsername(),
            "user_full_name", principal.getFullName(),
            "authorities_processed_by_spring", authorities,
            "all_claims_from_keycloak", claims
        );
    }
}