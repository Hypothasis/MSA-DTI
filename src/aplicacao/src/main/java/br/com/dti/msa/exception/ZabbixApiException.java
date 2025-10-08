package br.com.dti.msa.exception;

public class ZabbixApiException extends RuntimeException {
    public ZabbixApiException(String message) {
        super(message);
    }
}