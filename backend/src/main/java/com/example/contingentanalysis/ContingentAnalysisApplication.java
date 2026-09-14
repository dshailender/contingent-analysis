package com.example.contingentanalysis;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.modulith.Modulithic;

@Modulithic(systemName = "ContingentClaimsValuation", sharedModules = {"model"})
@SpringBootApplication
public class ContingentAnalysisApplication {

    public static void main(String[] args) {
        SpringApplication.run(ContingentAnalysisApplication.class, args);
    }
}

