package com.example.contingentanalysis.domain.pipeline;

import com.example.contingentanalysis.domain.blackscholes.BlackScholesCalculator;
import com.example.contingentanalysis.domain.breakpoints.BreakpointService;
import com.example.contingentanalysis.domain.capitalization.CapitalizationService;
import com.example.contingentanalysis.domain.claims.ClaimsService;
import com.example.contingentanalysis.domain.date.DateMath;
import com.example.contingentanalysis.domain.holdings.HoldingsService;
import com.example.contingentanalysis.domain.model.*;
import com.example.contingentanalysis.domain.opm.OpmService;
import com.example.contingentanalysis.domain.riskfree.RiskFreeRateService;
import com.example.contingentanalysis.domain.validation.ValuationValidationService;
import com.example.contingentanalysis.domain.waterfall.WaterfallService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ValuationPipelineService {

    private final ValuationValidationService validationService;
    private final CapitalizationService capitalizationService;
    private final BreakpointService breakpointService;
    private final ClaimsService claimsService;
    private final OpmService opmService;
    private final WaterfallService waterfallService;
    private final HoldingsService holdingsService;
    private final RiskFreeRateService riskFreeRateService;

    public ValuationPipelineService(ValuationValidationService validationService,
                                    CapitalizationService capitalizationService,
                                    BreakpointService breakpointService,
                                    ClaimsService claimsService,
                                    OpmService opmService,
                                    WaterfallService waterfallService,
                                    HoldingsService holdingsService,
                                    RiskFreeRateService riskFreeRateService) {
        this.validationService = validationService;
        this.capitalizationService = capitalizationService;
        this.breakpointService = breakpointService;
        this.claimsService = claimsService;
        this.opmService = opmService;
        this.waterfallService = waterfallService;
        this.holdingsService = holdingsService;
        this.riskFreeRateService = riskFreeRateService;
    }

    public ValuationResponse calculateValuation(ValuationRequest request) {
        List<String> issues = validationService.validateValuationRequest(request);
        if (!issues.isEmpty()) {
            throw new IllegalArgumentException(String.join("; ", issues));
        }

        // Terms and continuous rates
        double termCal = DateMath.yearFraction(request.getCalibrationDate(), request.getExitDate(), request.getDayCountBasis());
        double termVal = DateMath.yearFraction(request.getValuationDate(), request.getExitDate(), request.getDayCountBasis());
        double rfCalCont = BlackScholesCalculator.toContinuousRate(request.getRfCalibration() / 100.0) * 100.0;
        double rfValCont = BlackScholesCalculator.toContinuousRate(request.getRfValuation() / 100.0) * 100.0;

        // Display scaling
        double displayScale = 1.0;
        if ("millions".equalsIgnoreCase(request.getDisplayUnits())) {
            displayScale = 1e-6;
        } else if ("thousands".equalsIgnoreCase(request.getDisplayUnits())) {
            displayScale = 1e-3;
        }

        // Capitalization derivation
        List<DerivedSecurity> calDerived = capitalizationService.deriveCapitalization(
                request.getCalibrationSecurities(), request.getExitDate(), request.getDayCountBasis());
        List<DerivedSecurity> valDerived = capitalizationService.deriveCapitalization(
                request.getValuationSecurities(), request.getExitDate(), request.getDayCountBasis());

        // Breakpoint schedules
        List<BreakpointTier> calBps = breakpointService.generateBreakpoints(calDerived);
        List<BreakpointTier> valBps = breakpointService.generateBreakpoints(valDerived);

        // Claim tier allocations
        List<ClaimTierAllocation> calClaims = claimsService.allocateClaimsByTier(calDerived, calBps);
        List<ClaimTierAllocation> valClaims = claimsService.allocateClaimsByTier(valDerived, valBps);

        // Calibration backsolve
        double calSolvedEq = opmService.backsolveEquity(
                calDerived,
                calBps,
                request.getCalibrationDate(),
                request.getExitDate(),
                request.getRfCalibration() / 100.0,
                request.getVolCalibration() / 100.0,
                request.getDividendYieldCalibration() / 100.0,
                request.getCalibrationSecurityName(),
                request.getTransactionPrice(),
                request.getDayCountBasis()
        );

        // Calibration OPM allocation
        OpmAllocationResult calOpm = opmService.allocateOpm(
                calDerived,
                calBps,
                calSolvedEq,
                request.getCalibrationDate(),
                request.getExitDate(),
                request.getRfCalibration() / 100.0,
                request.getVolCalibration() / 100.0,
                request.getDividendYieldCalibration() / 100.0,
                request.getDayCountBasis()
        );

        // Valuation concluded equity value
        double marketAdj = request.getMarketAdjustment() / 100.0;
        double companyAdj = request.getCompanyAdjustment() / 100.0;
        double concludedEquity = calSolvedEq * (1.0 + marketAdj) * (1.0 + companyAdj);

        // Valuation OPM allocation
        OpmAllocationResult valOpm = opmService.allocateOpm(
                valDerived,
                valBps,
                concludedEquity,
                request.getValuationDate(),
                request.getExitDate(),
                request.getRfValuation() / 100.0,
                request.getVolValuation() / 100.0,
                request.getDividendYieldValuation() / 100.0,
                request.getDayCountBasis()
        );

        // Comparative waterfall
        double waterfallEquity = "concluded".equalsIgnoreCase(request.getWaterfallEquitySource())
                ? concludedEquity : request.getManualWaterfallEquity();

        WaterfallResult valWaterfall = waterfallService.allocateWaterfall(valDerived, valBps, waterfallEquity);
        WaterfallResult calWaterfall = waterfallService.allocateWaterfall(calDerived, calBps, calSolvedEq);
        List<ComparativeWaterfallItem> compWf = waterfallService.buildComparativeWaterfall(
                calDerived, valDerived, calWaterfall, valWaterfall);

        // Client holdings
        HoldingsSummary holdings = holdingsService.evaluateClientHoldings(
                request.getHoldings(), valDerived, valOpm.getPerShareValues());

        // Risk-free rate analyses
        RiskFreeRateAnalysis calRf = riskFreeRateService.buildRiskFreeAnalysis(
                request.getCalibrationDate(),
                request.getExitDate(),
                request.getRfCalibration(),
                "Calibration Risk-Free Rate",
                null,
                request.getDayCountBasis()
        );
        RiskFreeRateAnalysis valRf = riskFreeRateService.buildRiskFreeAnalysis(
                request.getValuationDate(),
                request.getExitDate(),
                request.getRfValuation(),
                "Valuation Risk-Free Rate",
                null,
                request.getDayCountBasis()
        );

        String reportPurposeStr = request.getReportPurpose();
        if ("Other (Specify Below)".equals(reportPurposeStr)
                && request.getReportPurposeManual() != null
                && !request.getReportPurposeManual().trim().isEmpty()) {
            reportPurposeStr = request.getReportPurposeManual();
        }

        ValuationResponse response = new ValuationResponse();
        response.setCompanyName(request.getCompanyName());
        response.setClientName(request.getClientName());
        response.setReportStatus(request.getReportStatus());
        response.setReportPurpose(reportPurposeStr);
        response.setReportCurrency(request.getReportCurrency());
        response.setDisplayUnits(request.getDisplayUnits());
        response.setDisplayScale(displayScale);
        response.setFirmLogoBase64(request.getFirmLogoBase64());
        response.setShowSecondaryCurrency(request.isShowSecondaryCurrency());
        response.setSecondaryCurrency(request.getSecondaryCurrency());
        response.setSecondaryFxRate(request.getSecondaryFxRate());

        response.setCalibrationDate(request.getCalibrationDate());
        response.setValuationDate(request.getValuationDate());
        response.setExitDate(request.getExitDate());
        response.setDayCountBasis(request.getDayCountBasis());
        response.setDayCountName(DateMath.BASIS_NAMES.getOrDefault(request.getDayCountBasis(), "Actual/Actual"));

        response.setTermCalibration(termCal);
        response.setTermValuation(termVal);
        response.setRfCalibrationEffective(request.getRfCalibration());
        response.setRfCalibrationContinuous(rfCalCont);
        response.setRfValuationEffective(request.getRfValuation());
        response.setRfValuationContinuous(rfValCont);

        response.setCalibrationSecurityName(request.getCalibrationSecurityName());
        response.setCalibrationDerivedSecurities(calDerived);
        response.setCalibrationBreakpoints(calBps);
        response.setCalibrationClaims(calClaims);
        response.setCalibrationSolvedEquity(calSolvedEq);
        response.setCalibrationOpm(calOpm);
        response.setCalibrationRfAnalysis(calRf);

        response.setValuationDerivedSecurities(valDerived);
        response.setValuationBreakpoints(valBps);
        response.setValuationClaims(valClaims);
        response.setConcludedEquityValue(concludedEquity);
        response.setValuationOpm(valOpm);
        response.setValuationRfAnalysis(valRf);

        response.setWaterfall(valWaterfall);
        response.setCalibrationWaterfall(calWaterfall);
        response.setComparativeWaterfall(compWf);
        response.setHoldings(holdings);
        response.setCalibrationVolatility(null);
        response.setValuationVolatility(null);

        return response;
    }
}

