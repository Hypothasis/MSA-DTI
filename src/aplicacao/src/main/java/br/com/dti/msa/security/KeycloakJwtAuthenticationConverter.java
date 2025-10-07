package br.com.dti.msa.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class KeycloakJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final String clientId;

    public KeycloakJwtAuthenticationConverter(String clientId) {
        this.clientId = clientId;
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        // Primeiro, usa o conversor padrão para pegar os 'scopes' (SCOPE_email, etc.)
        JwtGrantedAuthoritiesConverter defaultConverter = new JwtGrantedAuthoritiesConverter();
        Collection<GrantedAuthority> authorities = defaultConverter.convert(jwt);

        // Extrai as roles do 'resource_access' do Keycloak
        Map<String, Object> resourceAccess = jwt.getClaimAsMap("resource_access");
        if (resourceAccess != null && resourceAccess.containsKey(clientId)) {
            Map<String, Object> clientAccess = (Map<String, Object>) resourceAccess.get(clientId);
            if (clientAccess.containsKey("roles")) {
                List<String> clientRoles = (List<String>) clientAccess.get("roles");
                // Mapeia as roles para SimpleGrantedAuthority e as combina com os scopes
                authorities = Stream.concat(authorities.stream(),
                                clientRoles.stream().map(SimpleGrantedAuthority::new))
                        .collect(Collectors.toList());
            }
        }

        // Retorna um novo token de autenticação com as permissões combinadas
        return new JwtAuthenticationToken(jwt, authorities);
    }
}