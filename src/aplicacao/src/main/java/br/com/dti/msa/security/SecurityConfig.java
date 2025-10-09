package br.com.dti.msa.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.security.oauth2.client.oidc.web.logout.OidcClientInitiatedLogoutSuccessHandler;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Configuration
public class SecurityConfig {

    // Injeta o repositório de clientes OAuth2
    private final ClientRegistrationRepository clientRegistrationRepository;

    public SecurityConfig(ClientRegistrationRepository clientRegistrationRepository) {
        this.clientRegistrationRepository = clientRegistrationRepository;
    }

    // Injeta o client-id do seu application.properties
    @Value("${keycloak.client-id}")
    private String keycloakClientId;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // 1. Configura o CSRF para ser ignorado apenas nas rotas de API
            .csrf(csrf -> csrf
                .ignoringRequestMatchers("/token", "/admin/api/**")
            )
            // 2. Configura as regras de autorização
            .authorizeHttpRequests(auth -> auth
                // Rotas públicas
                .requestMatchers("/public/**", "/stylesheets/**", "/javascript/**", "/image/**").permitAll()
                .requestMatchers("/", "/error").permitAll()
                .requestMatchers(HttpMethod.GET,"/host/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/token").permitAll()
                
                // Rotas da aplicação web (páginas)
                .requestMatchers("/admin/search/**").hasAuthority("search_admin")
                .requestMatchers("/admin/create/**").hasAuthority("create_admin")
                .requestMatchers("/admin/**").hasAuthority("index_admin")

                // Regras da API RESTful
                .requestMatchers(HttpMethod.POST, "/admin/api/hosts").hasAuthority("ADMIN_CREATE")
                .requestMatchers(HttpMethod.GET, "/admin/api/hosts",  "/admin/api/hosts/**").hasAuthority("ADMIN_READ")
                .requestMatchers(HttpMethod.PUT, "/admin/api/hosts/**").hasAuthority("ADMIN_UPDATE")
                .requestMatchers(HttpMethod.DELETE, "/admin/api/hosts/**").hasAuthority("ADMIN_DELETE")
                
                // Qualquer outra requisição precisa de autenticação
                .anyRequest().authenticated()
            )
            // 3. Habilita o login via navegador
            .oauth2Login(oauth2 -> oauth2
                .userInfoEndpoint(userInfo -> userInfo.oidcUserService(oidcUserService()))
            )
            // 4. Habilita a validação de Bearer Token
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt
                .jwtAuthenticationConverter(new KeycloakJwtAuthenticationConverter(keycloakClientId))
            ))
            // 5. Configura o logout
            .logout(logout -> logout
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

    //Cria o bean do manipulador de logout
    private LogoutSuccessHandler oidcLogoutSuccessHandler() {
        OidcClientInitiatedLogoutSuccessHandler successHandler = 
                new OidcClientInitiatedLogoutSuccessHandler(this.clientRegistrationRepository);

        // Define para qual URL o Keycloak deve redirecionar o usuário APÓS o logout
        successHandler.setPostLogoutRedirectUri("{baseUrl}"); // Redireciona para a raiz da aplicação

        return successHandler;
    }
}