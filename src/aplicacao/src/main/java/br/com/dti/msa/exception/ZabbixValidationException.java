package br.com.dti.msa.exception;

public class ZabbixValidationException extends Exception {
    
    public ZabbixValidationException(String message) {
        // A palavra 'super(message)' passa a mensagem de erro para a classe pai (Exception)
        super(message);
    }
}