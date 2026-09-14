package com.example.contingentanalysis;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class ApplicationModulesTest {

    private final ApplicationModules modules = ApplicationModules.of(ContingentAnalysisApplication.class);

    @Test
    void verifiesModularStructure() {
        // Enforces that module boundaries, package-info definitions, and allowed dependencies are strictly honored.
        assertDoesNotThrow(() -> modules.verify());
    }

    @Test
    void writesModuleDocumentation() {
        // Generates C4 / PlantUML / AsciiDoc module diagrams and dependency maps into target/spring-modulith-docs
        assertDoesNotThrow(() -> new Documenter(modules)
                .writeDocumentation()
                .writeModulesAsPlantUml());
    }

    @Test
    void verifiesDetectedModules() {
        // Ensure all required domain modules and api are recognized by Modulith
        assertThat(modules.getModuleByName("api")).isPresent();
        assertThat(modules.getModuleByName("model")).isPresent();
        assertThat(modules.getModuleByName("pipeline")).isPresent();
        assertThat(modules.getModuleByName("blackscholes")).isPresent();
        assertThat(modules.getModuleByName("breakpoints")).isPresent();
        assertThat(modules.getModuleByName("capitalization")).isPresent();
        assertThat(modules.getModuleByName("capitaliq")).isPresent();
        assertThat(modules.getModuleByName("claims")).isPresent();
        assertThat(modules.getModuleByName("date")).isPresent();
        assertThat(modules.getModuleByName("defaultscenario")).isPresent();
        assertThat(modules.getModuleByName("exports")).isPresent();
        assertThat(modules.getModuleByName("holdings")).isPresent();
        assertThat(modules.getModuleByName("opm")).isPresent();
        assertThat(modules.getModuleByName("riskfree")).isPresent();
        assertThat(modules.getModuleByName("validation")).isPresent();
        assertThat(modules.getModuleByName("volatility")).isPresent();
        assertThat(modules.getModuleByName("waterfall")).isPresent();
    }
}
