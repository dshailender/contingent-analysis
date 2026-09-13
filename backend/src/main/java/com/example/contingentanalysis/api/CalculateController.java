package com.example.contingentanalysis.api;

import com.example.contingentanalysis.domain.date.DateMath;
import com.example.contingentanalysis.domain.defaultscenario.DefaultScenarioService;
import com.example.contingentanalysis.domain.model.ValidationResult;
import com.example.contingentanalysis.domain.model.ValuationRequest;
import com.example.contingentanalysis.domain.model.ValuationResponse;
import com.example.contingentanalysis.domain.pipeline.ValuationPipelineService;
import com.example.contingentanalysis.domain.validation.ValuationValidationService;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class CalculateController {

    private final ValuationPipelineService pipelineService;
    private final ValuationValidationService validationService;
    private final DefaultScenarioService defaultScenarioService;

    public CalculateController(ValuationPipelineService pipelineService,
                               ValuationValidationService validationService,
                               DefaultScenarioService defaultScenarioService) {
        this.pipelineService = pipelineService;
        this.validationService = validationService;
        this.defaultScenarioService = defaultScenarioService;
    }

    @PostMapping("/calculate")
    public ValuationResponse calculateModel(@RequestBody ValuationRequest request) {
        return pipelineService.calculateValuation(request);
    }

    @PostMapping("/validate")
    public ValidationResult validateModel(@RequestBody ValuationRequest request) {
        List<String> issues = validationService.validateValuationRequest(request);
        return new ValidationResult(issues.isEmpty(), issues);
    }

    @GetMapping("/scenario/default")
    public ValuationRequest getDefaultScenario() {
        return defaultScenarioService.getDefaultValuationRequest();
    }

    @GetMapping("/basis-conventions")
    public List<Map<String, Object>> getBasisConventions() {
        List<Map<String, Object>> conventions = new ArrayList<>();
        for (Map.Entry<Integer, String> entry : DateMath.BASIS_NAMES.entrySet()) {
            conventions.add(Map.of(
                    "id", entry.getKey(),
                    "name", entry.getValue()
            ));
        }
        return conventions;
    }
}

