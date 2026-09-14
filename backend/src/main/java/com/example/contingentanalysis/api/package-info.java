@org.springframework.modulith.ApplicationModule(
        id = "api",
        displayName = "REST API Layer",
        allowedDependencies = {"pipeline", "validation", "defaultscenario", "riskfree", "exports", "capitaliq", "model"}
)
package com.example.contingentanalysis.api;

