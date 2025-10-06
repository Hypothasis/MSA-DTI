package br.com.dti.msa.dto;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class KeycloakTokenResponseDTO {
    @JsonProperty("access_token")
    private String accessToken;

    @JsonProperty("expires_in")
    private int expiresIn;

    @JsonProperty("refresh_token")
    private String refreshToken;
}