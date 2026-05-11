package com.envoil.admin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableTransactionManagement
public class AdminBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(AdminBackendApplication.class, args);
    }
}
