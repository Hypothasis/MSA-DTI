package br.com.dti.msa.integration.zabbix.dto;

import br.com.dti.msa.exception.ZabbixApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ZabbixClientTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private ZabbixClient zabbixClient;

    @BeforeEach
    public void setUp() {
        ReflectionTestUtils.setField(zabbixClient, "restTemplate", restTemplate);
        ReflectionTestUtils.setField(zabbixClient, "zabbixApiUrl", "http://localhost:8080/api_jsonrpc.php");
        ReflectionTestUtils.setField(zabbixClient, "authToken", "token-falso-123");
    }

    @Test
    public void testHostExists_True() {
        String jsonResponse = "{\"jsonrpc\":\"2.0\",\"result\":1,\"id\":1}";
        ResponseEntity<String> responseEntity = new ResponseEntity<>(jsonResponse, HttpStatus.OK);

        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(String.class)
        )).thenReturn(responseEntity);

        boolean exists = zabbixClient.hostExists(10084L);

        assertTrue(exists);
    }

    @Test
    public void testHostExists_False_WhenApiReturnsError() {
        String jsonResponse = "{\"jsonrpc\":\"2.0\",\"error\":{\"code\":-32602,\"message\":\"Invalid params.\",\"data\":\"No permissions to referred object or it does not exist!\"},\"id\":1}";
        ResponseEntity<String> responseEntity = new ResponseEntity<>(jsonResponse, HttpStatus.OK);

        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(String.class)
        )).thenReturn(responseEntity);

        boolean exists = zabbixClient.hostExists(9999L);

        assertFalse(exists);
    }

    @Test
    public void testGetSingleItemValue_Found() {
        String jsonResponse = "{\"jsonrpc\":\"2.0\",\"result\":[{\"key_\":\"cpu-uso\",\"lastvalue\":\"45.5\"}],\"id\":3}";
        ResponseEntity<String> responseEntity = new ResponseEntity<>(jsonResponse, HttpStatus.OK);

        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(String.class)
        )).thenReturn(responseEntity);

        String value = zabbixClient.getSingleItemValue(10084L, "cpu-uso");

        assertNotNull(value);
        assertEquals("45.5", value);
    }

    @Test
    public void testGetSingleItemValue_NotFound() {
        String jsonResponse = "{\"jsonrpc\":\"2.0\",\"result\":[],\"id\":3}";
        ResponseEntity<String> responseEntity = new ResponseEntity<>(jsonResponse, HttpStatus.OK);

        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(String.class)
        )).thenReturn(responseEntity);

        String value = zabbixClient.getSingleItemValue(10084L, "chave-inexistente");

        assertNull(value);
    }

    @Test
    public void testTestConnection_Success() {
        String jsonResponse = "{\"jsonrpc\":\"2.0\",\"result\":\"6.0.0\",\"id\":99}";
        ResponseEntity<String> responseEntity = new ResponseEntity<>(jsonResponse, HttpStatus.OK);

        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(String.class)
        )).thenReturn(responseEntity);

        assertDoesNotThrow(() -> zabbixClient.testConnection());
    }

    @Test
    public void testTestConnection_ThrowsException_OnInvalidResponse() {
        String jsonResponse = "{\"jsonrpc\":\"2.0\",\"error\":\"Internal Error\"}";
        ResponseEntity<String> responseEntity = new ResponseEntity<>(jsonResponse, HttpStatus.OK);

        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(String.class)
        )).thenReturn(responseEntity);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            zabbixClient.testConnection();
        });
        
        assertTrue(exception.getMessage().contains("Não foi possível conectar à API do Zabbix"));    
    }
}