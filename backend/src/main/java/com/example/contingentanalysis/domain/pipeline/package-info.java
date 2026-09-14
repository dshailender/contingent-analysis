@org.springframework.modulith.ApplicationModule(
        id = "pipeline",
        displayName = "Valuation Pipeline Orchestrator",
        allowedDependencies = {
                "validation", "capitalization", "breakpoints", "claims",
                "opm", "waterfall", "holdings", "riskfree", "blackscholes", "date", "model"
        }
)
package com.example.contingentanalysis.domain.pipeline;

