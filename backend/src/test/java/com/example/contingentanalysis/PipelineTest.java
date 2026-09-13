package com.example.contingentanalysis;

import com.example.contingentanalysis.domain.breakpoints.BreakpointService;
import com.example.contingentanalysis.domain.capitalization.CapitalizationService;
import com.example.contingentanalysis.domain.claims.ClaimsService;
import com.example.contingentanalysis.domain.defaultscenario.DefaultScenarioService;
import com.example.contingentanalysis.domain.holdings.HoldingsService;
import com.example.contingentanalysis.domain.model.ValuationRequest;
import com.example.contingentanalysis.domain.model.ValuationResponse;
import com.example.contingentanalysis.domain.opm.OpmService;
import com.example.contingentanalysis.domain.pipeline.ValuationPipelineService;
import com.example.contingentanalysis.domain.riskfree.RiskFreeRateService;
import com.example.contingentanalysis.domain.validation.ValuationValidationService;
import com.example.contingentanalysis.domain.waterfall.WaterfallService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

public class PipelineTest {

    private DefaultScenarioService defaultScenarioService;
    private ValuationValidationService validationService;
    private ValuationPipelineService pipelineService;

    @BeforeEach
    void setUp() {
        CapitalizationService capitalizationService = new CapitalizationService();
        BreakpointService breakpointService = new BreakpointService();
        ClaimsService claimsService = new ClaimsService();
        OpmService opmService = new OpmService(claimsService);
        WaterfallService waterfallService = new WaterfallService(claimsService);
        HoldingsService holdingsService = new HoldingsService();
        validationService = new ValuationValidationService();
        RiskFreeRateService riskFreeRateService = new RiskFreeRateService();
        defaultScenarioService = new DefaultScenarioService();
        pipelineService = new ValuationPipelineService(
                validationService, capitalizationService, breakpointService, claimsService,
                opmService, waterfallService, holdingsService, riskFreeRateService
        );
    }

    @Test
    void testDefaultScenarioAndPipeline() {
        ValuationRequest req = defaultScenarioService.getDefaultValuationRequest();
        assertThat(req.getCompanyName()).isEqualTo("TADO");
        assertThat(req.getCalibrationSecurities()).hasSize(16);
        assertThat(req.getValuationSecurities()).hasSize(16);
        assertThat(req.getHoldings()).hasSize(7);

        // Validate
        List<String> issues = validationService.validateValuationRequest(req);
        assertThat(issues).isEmpty();

        // Calculate
        ValuationResponse resp = pipelineService.calculateValuation(req);
        assertThat(resp.getCompanyName()).isEqualTo("TADO");
        assertThat(resp.getCalibrationSolvedEquity()).isCloseTo(287252502.92, within(1.0));
        assertThat(resp.getConcludedEquityValue()).isCloseTo(287252502.92, within(1.0));

        // Series I per-share in calibration OPM
        double seriesICal = resp.getCalibrationOpm().getPerShareValues().get("Series I");
        assertThat(seriesICal).isCloseTo(2021.90, within(0.01));

        // Series I and H per-share in valuation OPM
        double seriesIVal = resp.getValuationOpm().getPerShareValues().get("Series I");
        assertThat(seriesIVal).isCloseTo(1992.50, within(0.05));

        double seriesHVal = resp.getValuationOpm().getPerShareValues().get("Series H");
        assertThat(seriesHVal).isCloseTo(2391.29, within(0.05));

        // Holdings summary
        assertThat(resp.getHoldings().getTotalCost()).isCloseTo(13299194.0, within(1.0));
        assertThat(resp.getHoldings().getTotalValue()).isCloseTo(17568066.01, within(5.0));
        assertThat(resp.getHoldings().getConsolidatedMoic()).isNotNull();
        assertThat(resp.getHoldings().getConsolidatedMoic()).isCloseTo(1.32, within(0.01));
    }
}

