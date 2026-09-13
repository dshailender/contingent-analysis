package com.example.contingentanalysis;

import com.example.contingentanalysis.domain.blackscholes.BlackScholesCalculator;
import com.example.contingentanalysis.domain.breakpoints.BreakpointService;
import com.example.contingentanalysis.domain.capitalization.CapitalizationService;
import com.example.contingentanalysis.domain.claims.ClaimsService;
import com.example.contingentanalysis.domain.defaultscenario.DefaultScenarioService;
import com.example.contingentanalysis.domain.holdings.HoldingsService;
import com.example.contingentanalysis.domain.model.*;
import com.example.contingentanalysis.domain.opm.OpmService;
import com.example.contingentanalysis.domain.pipeline.ValuationPipelineService;
import com.example.contingentanalysis.domain.riskfree.RiskFreeRateService;
import com.example.contingentanalysis.domain.validation.ValuationValidationService;
import com.example.contingentanalysis.domain.waterfall.WaterfallService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

public class BoundaryConditionsTest {

    private CapitalizationService capitalizationService;
    private BreakpointService breakpointService;
    private ClaimsService claimsService;
    private OpmService opmService;
    private WaterfallService waterfallService;
    private HoldingsService holdingsService;
    private ValuationValidationService validationService;
    private RiskFreeRateService riskFreeRateService;
    private DefaultScenarioService defaultScenarioService;
    private ValuationPipelineService pipelineService;

    @BeforeEach
    void setUp() {
        capitalizationService = new CapitalizationService();
        breakpointService = new BreakpointService();
        claimsService = new ClaimsService();
        opmService = new OpmService(claimsService);
        waterfallService = new WaterfallService(claimsService);
        holdingsService = new HoldingsService();
        validationService = new ValuationValidationService();
        riskFreeRateService = new RiskFreeRateService();
        defaultScenarioService = new DefaultScenarioService();
        pipelineService = new ValuationPipelineService(
                validationService, capitalizationService, breakpointService, claimsService,
                opmService, waterfallService, holdingsService, riskFreeRateService
        );
    }

    @Test
    void testConversionPriceZero() {
        SecurityInput sec = new SecurityInput(
                "Series NonConvert", "Preferred Stock", 1000, null,
                10.0, 0.0, 1.0, "No", "NA", 1, null, null, "Annual", 0.0
        );
        List<DerivedSecurity> derived = capitalizationService.deriveCapitalization(List.of(sec), "2027-01-01", 1);
        assertThat(derived.get(0).getConversionRatio()).isEqualTo(0.0);
        assertThat(derived.get(0).getFullyDilutedShares()).isEqualTo(0.0);
        assertThat(derived.get(0).getTotalLiquidationPreference()).isEqualTo(10000.0);
    }

    @Test
    void testPariPassuSeniorityTies() {
        SecurityInput s1 = new SecurityInput("Series A-1", "Preferred Stock", 1000, null, 10.0, 10.0, 1.0, "NA", "NA", 1, null, null, "Annual", 0.0);
        SecurityInput s2 = new SecurityInput("Series A-2", "Preferred Stock", 2000, null, 10.0, 10.0, 1.0, "NA", "NA", 1, null, null, "Annual", 0.0);

        List<DerivedSecurity> derived = capitalizationService.deriveCapitalization(List.of(s1, s2), "2027-01-01", 1);
        List<BreakpointTier> bps = breakpointService.generateBreakpoints(derived);

        assertThat(bps).isNotEmpty();
        BreakpointTier tier1 = bps.get(0);
        assertThat(tier1.getEndEquity()).isCloseTo(30000.0, within(1e-2));
        assertThat(tier1.getClaimantsDescription()).contains("Series A-1");
        assertThat(tier1.getClaimantsDescription()).contains("Series A-2");

        List<ClaimTierAllocation> claims = claimsService.allocateClaimsByTier(derived, bps);
        ClaimTierAllocation t1Claims = claims.get(0);
        assertThat(t1Claims.getSharingPercentages().get("Series A-1")).isCloseTo(10000.0 / 30000.0, within(1e-5));
        assertThat(t1Claims.getSharingPercentages().get("Series A-2")).isCloseTo(20000.0 / 30000.0, within(1e-5));
    }

    @Test
    void testZeroDividendVsCompounding() {
        SecurityInput basePref = new SecurityInput(
                "Series DivTest", "Preferred Stock", 100, null,
                100.0, 100.0, 1.0, "NA", "NA", 1,
                "2025-01-01", 10.0, "Simple Interest", 0.0
        );

        String exitDate = "2027-01-01";
        DerivedSecurity dSimple = capitalizationService.deriveCapitalization(List.of(basePref), exitDate, 0).get(0);
        assertThat(dSimple.getPerShareDividend()).isCloseTo(20.0, within(1e-2));

        basePref.setCompoundingConvention("Annual");
        DerivedSecurity dAnnual = capitalizationService.deriveCapitalization(List.of(basePref), exitDate, 0).get(0);
        assertThat(dAnnual.getPerShareDividend()).isCloseTo(21.0, within(1e-2));

        basePref.setCompoundingConvention("Quarterly");
        DerivedSecurity dQuarterly = capitalizationService.deriveCapitalization(List.of(basePref), exitDate, 0).get(0);
        double expectedQ = 100.0 * (Math.pow(1.0 + 0.10 / 4.0, 8.0) - 1.0);
        assertThat(dQuarterly.getPerShareDividend()).isCloseTo(expectedQ, within(1e-2));
    }

    @Test
    void testEquityBelowFirstBreakpointWaterfall() {
        SecurityInput s1 = new SecurityInput("Series Senior", "Preferred Stock", 1000, null, 10.0, null, 1.0, "NA", "NA", 1, null, null, "Annual", 0.0);
        SecurityInput s2 = new SecurityInput("Common", "Common Stock", 1000, null, null, null, 1.0, "NA", "NA", 999, null, null, "Annual", 0.0);

        List<DerivedSecurity> derived = capitalizationService.deriveCapitalization(List.of(s1, s2), "2027-01-01", 1);
        List<BreakpointTier> bps = breakpointService.generateBreakpoints(derived);

        WaterfallResult wf = waterfallService.allocateWaterfall(derived, bps, 4000.0);
        Map<String, Double> pMap = wf.getDistribution().stream()
                .collect(Collectors.toMap(WaterfallAllocationItem::getSecurity, WaterfallAllocationItem::getProceeds));
        assertThat(pMap.get("Series Senior")).isCloseTo(4000.0, within(1e-2));
        assertThat(pMap.get("Common")).isCloseTo(0.0, within(1e-2));
    }

    @Test
    void testEquityFarAboveFinalBreakpoint() {
        SecurityInput s1 = new SecurityInput("Series Senior", "Preferred Stock", 1000, null, 10.0, 10.0, 1.0, "Yes", "NA", 1, null, null, "Annual", 0.0);
        SecurityInput s2 = new SecurityInput("Common", "Common Stock", 1000, null, null, null, 1.0, "NA", "NA", 999, null, null, "Annual", 0.0);

        List<DerivedSecurity> derived = capitalizationService.deriveCapitalization(List.of(s1, s2), "2027-01-01", 1);
        List<BreakpointTier> bps = breakpointService.generateBreakpoints(derived);

        WaterfallResult wf = waterfallService.allocateWaterfall(derived, bps, 100000.0);
        Map<String, Double> pMap = wf.getDistribution().stream()
                .collect(Collectors.toMap(WaterfallAllocationItem::getSecurity, WaterfallAllocationItem::getProceeds));

        assertThat(pMap.get("Series Senior")).isCloseTo(55000.0, within(1e-2));
        assertThat(pMap.get("Common")).isCloseTo(45000.0, within(1e-2));
        assertThat(wf.getTotalProceeds()).isCloseTo(100000.0, within(1e-2));
    }

    @Test
    void testBlackScholesBoundaryConditions() {
        double r = BlackScholesCalculator.toContinuousRate(0.05);

        double cZeroK = BlackScholesCalculator.blackScholesCall(100.0, 0.0, r, 0.3, 1.0, 0.0);
        assertThat(cZeroK).isCloseTo(100.0, within(1e-5));

        double cZeroT = BlackScholesCalculator.blackScholesCall(120.0, 100.0, r, 0.3, 0.0, 0.0);
        assertThat(cZeroT).isCloseTo(20.0, within(1e-5));

        double cZeroS = BlackScholesCalculator.blackScholesCall(0.0, 100.0, r, 0.3, 1.0, 0.0);
        assertThat(cZeroS).isEqualTo(0.0);
    }

    @Test
    void testAutoResolveCalibrationSecurityOnDelete() {
        ValuationRequest req = defaultScenarioService.getDefaultValuationRequest();
        assertThat(req.getCalibrationSecurityName()).isEqualTo("Series I");

        // Delete Series I from calibration cap table
        req.getCalibrationSecurities().remove(0);
        assertThat(req.getCalibrationSecurities().get(0).getSecurity()).isEqualTo("Series H");

        // Leave calibrationSecurityName as 'Series I' to simulate client state
        ValuationResponse res = pipelineService.calculateValuation(req);
        assertThat(res).isNotNull();
        assertThat(res.getCalibrationSecurityName()).isEqualTo("Series H");
        assertThat(res.getCalibrationSolvedEquity()).isGreaterThan(0.0);
        assertThat(res.getCalibrationOpm().getPerShareValues()).containsKey("Series H");
        assertThat(res.getCalibrationOpm().getPerShareValues()).doesNotContainKey("Series I");
    }

    @Test
    void testAutoResolveCalibrationSecurityOnRename() {
        ValuationRequest req = defaultScenarioService.getDefaultValuationRequest();
        req.getCalibrationSecurities().get(0).setSecurity("Series I-Priced");
        req.setCalibrationSecurityName("Series I"); // Mismatched old name

        ValuationResponse res = pipelineService.calculateValuation(req);
        assertThat(res).isNotNull();
        assertThat(res.getCalibrationSecurityName()).isEqualTo("Series I-Priced");
        assertThat(res.getCalibrationSolvedEquity()).isGreaterThan(0.0);
        assertThat(res.getCalibrationOpm().getPerShareValues()).containsKey("Series I-Priced");
    }

    @Test
    void testCapTableOnlyCommonStock() {
        ValuationRequest req = defaultScenarioService.getDefaultValuationRequest();
        SecurityInput commonSec = new SecurityInput("Common Stock", "Common Stock", 1000000.0, null, null, null, 0.0, "NA", "NA", 999, null, null, "Annual", 0.0);

        req.setCalibrationSecurities(new ArrayList<>(List.of(commonSec)));
        req.setValuationSecurities(new ArrayList<>(List.of(commonSec)));
        req.setCalibrationSecurityName("NonExistentPreferred");
        req.setTransactionPrice(10.0);
        req.setHoldings(new ArrayList<>());

        ValuationResponse res = pipelineService.calculateValuation(req);
        assertThat(res).isNotNull();
        assertThat(res.getCalibrationSecurityName()).isEqualTo("Common Stock");
        assertThat(res.getCalibrationSolvedEquity()).isCloseTo(10000000.0, within(10000.0)); // within 0.1%
        assertThat(res.getCalibrationOpm().getPerShareValues().get("Common Stock")).isCloseTo(10.0, within(0.05));
    }

    @Test
    void testModifyCapTableSharesAndMultipliers() {
        ValuationRequest req = defaultScenarioService.getDefaultValuationRequest();
        ValuationResponse baseRes = pipelineService.calculateValuation(req);
        double baseEq = baseRes.getCalibrationSolvedEquity();

        // Double Series I shares
        req.getCalibrationSecurities().get(0).setShares(req.getCalibrationSecurities().get(0).getShares() * 2.0);
        ValuationResponse modRes = pipelineService.calculateValuation(req);

        // Doubling shares of calibration security at same price must increase solved equity
        assertThat(modRes.getCalibrationSolvedEquity()).isGreaterThan(baseEq);
    }
}

