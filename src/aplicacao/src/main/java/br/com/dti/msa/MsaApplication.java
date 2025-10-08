package br.com.dti.msa;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MsaApplication {

    public static void main(String[] args) {
        SpringApplication.run(MsaApplication.class, args);
    }

}
