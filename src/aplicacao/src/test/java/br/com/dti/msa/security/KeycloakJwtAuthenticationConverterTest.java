package br.com.dti.msa.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class KeycloakJwtAuthenticationConverterTest {

    private KeycloakJwtAuthenticationConverter converter;
    private final String clientId = "msa-client";

    @BeforeEach
    public void setUp() {
        converter = new KeycloakJwtAuthenticationConverter(clientId);
    }

    @Test
    public void testConvert_WithValidResourceAccess_ExtractsRolesCorrectly() {
        Map<String, Object> rolesMap = Map.of("roles", List.of("ADMIN_READ", "ADMIN_CREATE"));
        Map<String, Object> resourceAccessMap = Map.of(clientId, rolesMap);

        Jwt jwt = Jwt.withTokenValue("fake-token")
                .header("alg", "none")
                .claim("resource_access", resourceAccessMap)
                .build();

        AbstractAuthenticationToken authToken = converter.convert(jwt);

        assertNotNull(authToken);
        Collection<GrantedAuthority> authorities = authToken.getAuthorities();
        
        assertTrue(authorities.stream().anyMatch(a -> a.getAuthority().equals("ADMIN_READ")));
        assertTrue(authorities.stream().anyMatch(a -> a.getAuthority().equals("ADMIN_CREATE")));
    }

    @Test
    public void testConvert_WithoutResourceAccess_ReturnsEmptyAuthorities() {
        Jwt jwt = Jwt.withTokenValue("fake-token")
                .header("alg", "none")
                .claim("sub", "user-123")
                .build();

        AbstractAuthenticationToken authToken = converter.convert(jwt);

        assertNotNull(authToken);
        
        boolean hasAdminRole = authToken.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().contains("ADMIN"));
        
        assertFalse(hasAdminRole, "Não deveria conter roles de admin se o resource_access estiver vazio");
    }
}