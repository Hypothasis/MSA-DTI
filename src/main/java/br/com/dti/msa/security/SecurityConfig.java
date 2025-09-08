package br.com.dti.msa.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.oauth2.client.oidc.web.logout.OidcClientInitiatedLogoutSuccessHandler;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;


import java.util.Collection;
import java.util.stream.Collectors;
import java.util.List;
import java.util.Map;

@Configuration
public class SecurityConfig {

    // 1. Injeta o repositório de clientes OAuth2
    private final ClientRegistrationRepository clientRegistrationRepository;

    public SecurityConfig(ClientRegistrationRepository clientRegistrationRepository) {
        this.clientRegistrationRepository = clientRegistrationRepository;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf((csrf) -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/public/**", "/stylesheets/**", "/javascript/**", "/image/**").permitAll()
                .requestMatchers("/").permitAll()
                .requestMatchers("debug/**").permitAll()
                .requestMatchers("/host/**").hasAuthority("ADMIN")
                .anyRequest().authenticated()
            )
            .oauth2Login(oauth2 -> oauth2
                .userInfoEndpoint(userInfo -> userInfo.oidcUserService(oidcUserService()))
            )
            // 2. ADICIONA A CONFIGURAÇÃO DE LOGOUT
            .logout(logout -> logout
                // Define o "manipulador" que sabe como fazer logout no Keycloak
                .logoutSuccessHandler(oidcLogoutSuccessHandler())
            );

        return http.build();
    }

    @Bean
    public OAuth2UserService<OidcUserRequest, OidcUser> oidcUserService() {
        final OidcUserService delegate = new OidcUserService();
        
        return (userRequest) -> {
            OidcUser oidcUser = delegate.loadUser(userRequest);
            
            // --- INÍCIO DA LÓGICA DE CONVERSÃO DE ROLES ---
            
            // 1. Extrai o Client ID da própria requisição, sem precisar injetar valor
            String clientId = userRequest.getClientRegistration().getClientId();
            
            // 2. Acessa o objeto 'resource_access' do token
            Map<String, Object> resourceAccess = oidcUser.getClaimAsMap("resource_access");

            if (resourceAccess == null || resourceAccess.isEmpty() || !resourceAccess.containsKey(clientId)) {
                return new DefaultOidcUser(oidcUser.getAuthorities(), oidcUser.getIdToken(), oidcUser.getUserInfo());
            }

            Map<String, Object> clientAccess = (Map<String, Object>) resourceAccess.get(clientId);

            if (clientAccess == null || !clientAccess.containsKey("roles")) {
                return new DefaultOidcUser(oidcUser.getAuthorities(), oidcUser.getIdToken(), oidcUser.getUserInfo());
            }

            List<String> clientRoles = (List<String>) clientAccess.get("roles");
            
            // 3. Cria as permissões SEM o prefixo ROLE_, para funcionar com .hasAuthority("...")
            Collection<GrantedAuthority> authorities = clientRoles.stream()
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());
            
            // Adiciona as authorities originais para não perder outras permissões padrão
            authorities.addAll(oidcUser.getAuthorities());
            
            // --- FIM DA LÓGICA DE CONVERSÃO ---

            return new DefaultOidcUser(authorities, oidcUser.getIdToken(), oidcUser.getUserInfo());        
        };
    }

    // 3. Cria o bean do manipulador de logout
    private LogoutSuccessHandler oidcLogoutSuccessHandler() {
        OidcClientInitiatedLogoutSuccessHandler successHandler = 
                new OidcClientInitiatedLogoutSuccessHandler(this.clientRegistrationRepository);

        // Define para qual URL o Keycloak deve redirecionar o usuário APÓS o logout
        successHandler.setPostLogoutRedirectUri("{baseUrl}"); // Redireciona para a raiz da aplicação

        return successHandler;
    }
}