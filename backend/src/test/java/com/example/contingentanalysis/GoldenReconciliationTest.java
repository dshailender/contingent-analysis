package com.example.contingentanalysis;

import com.example.contingentanalysis.domain.breakpoints.BreakpointService;
import com.example.contingentanalysis.domain.capitalization.CapitalizationService;
import com.example.contingentanalysis.domain.claims.ClaimsService;
import com.example.contingentanalysis.domain.defaultscenario.DefaultScenarioService;
import com.example.contingentanalysis.domain.holdings.HoldingsService;
import com.example.contingentanalysis.domain.model.*;
import com.example.contingentanalysis.domain.opm.OpmService;
import com.example.contingentanalysis.domain.waterfall.WaterfallService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

public class GoldenReconciliationTest {

    private CapitalizationService capitalizationService;
    private BreakpointService breakpointService;
    private ClaimsService claimsService;
    private OpmService opmService;
    private WaterfallService waterfallService;
    private HoldingsService holdingsService;

    private JsonNode goldenData;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() throws Exception {
        capitalizationService = new CapitalizationService();
        breakpointService = new BreakpointService();
        claimsService = new ClaimsService();
        opmService = new OpmService(claimsService);
        waterfallService = new WaterfallService(claimsService);
        holdingsService = new HoldingsService();

        Path p1 = Path.of("../golden_reference/golden_reference_run.json").toAbsolutePath().normalize();
        Path p2 = Path.of("golden_reference/golden_reference_run.json").toAbsolutePath().normalize();
        Path fixture = Files.exists(p1) ? p1 : p2;
        goldenData = objectMapper.readTree(Files.newInputStream(fixture));
    }

    private SecurityInput mapRawToInput(JsonNode raw) {
        String secName = raw.has("Security") ? raw.get("Security").asText() : "";
        String subtype = raw.has("Security Subtype") ? raw.get("Security Subtype").asText() : "Preferred Stock";

        Double shares = parseNum(raw.get("Number of Shares"));
        Double ep = parseNum(raw.get("Wtd. Avg. Exercise Price"));
        Double oip = parseNum(raw.get("Original Issue Price"));
        Double cp = parseNum(raw.get("Conversion Price"));
        Double lm = parseNum(raw.get("Liquidation Multiplier"));
        String part = raw.has("Participation") ? raw.get("Participation").asText() : "NA";
        String cap = raw.has("Max Participation Cap") ? raw.get("Max Participation Cap").asText() : "NA";
        Double senNum = parseNum(raw.get("Seniority"));
        Integer seniority = senNum != null ? senNum.intValue() : null;
        String issueDate = (raw.has("Issue Date") && !raw.get("Issue Date").isNull()) ? raw.get("Issue Date").asText() : null;
        Double divRate = parseNum(raw.get("Annual Dividend Rate"));
        String cc = raw.has("Compounding Convention") ? raw.get("Compounding Convention").asText() : "Annual";
        Double divPaid = parseNum(raw.get("Dividends Paid To Date"));

        return new SecurityInput(
                secName, subtype, shares != null ? shares : 0.0, ep, oip, cp,
                lm != null ? lm : 1.0, part, cap, seniority, issueDate, divRate, cc,
                divPaid != null ? divPaid : 0.0
        );
    }

    private Double parseNum(JsonNode node) {
        if (node == null || node.isNull() || node.asText().trim().isEmpty() || node.asText().trim().equalsIgnoreCase("NA")) {
            return null;
        }
        String clean = node.asText().replace(",", "").trim();
        try {
            return Double.parseDouble(clean);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Test
    void testCalibrationDerivation() {
        JsonNode inputs = goldenData.get("inputs");
        List<SecurityInput> calInputs = new ArrayList<>();
        for (JsonNode r : inputs.get("PREPOP_CAL")) {
            calInputs.add(mapRawToInput(r));
        }

        List<DerivedSecurity> calDerived = capitalizationService.deriveCapitalization(
                calInputs, inputs.get("exitDate").asText(), inputs.get("basis").asInt());
        assertThat(calDerived).hasSize(inputs.get("PREPOP_CAL").size());

        JsonNode goldenCalRows = goldenData.get("calibration").get("rows");
        for (int i = 0; i < calDerived.size(); i++) {
            DerivedSecurity pyRow = calDerived.get(i);
            JsonNode jsRow = goldenCalRows.get(i);

            assertThat(pyRow.getSecurity()).isEqualTo(jsRow.get("Security").asText());
            assertThat(pyRow.getShares()).isCloseTo(jsRow.get("shares").asDouble(), within(pyRow.getShares() * 1e-5 + 1e-9));
            assertThat(pyRow.getTotalLiquidationPreference()).isCloseTo(jsRow.get("pref").asDouble(), within(1e-2));
            assertThat(pyRow.getFullyDilutedShares()).isCloseTo(jsRow.get("fd").asDouble(), within(pyRow.getFullyDilutedShares() * 1e-5 + 1e-9));
        }
    }

    @Test
    void testCalibrationBreakpoints() {
        JsonNode inputs = goldenData.get("inputs");
        List<SecurityInput> calInputs = new ArrayList<>();
        for (JsonNode r : inputs.get("PREPOP_CAL")) {
            calInputs.add(mapRawToInput(r));
        }

        List<DerivedSecurity> calDerived = capitalizationService.deriveCapitalization(
                calInputs, inputs.get("exitDate").asText(), inputs.get("basis").asInt());
        List<BreakpointTier> bps = breakpointService.generateBreakpoints(calDerived);
        JsonNode goldenBps = goldenData.get("calibration").get("breakpoints");

        assertThat(bps).hasSize(goldenBps.size());
        for (int i = 0; i < bps.size(); i++) {
            BreakpointTier pyBp = bps.get(i);
            JsonNode jsBp = goldenBps.get(i);

            assertThat(pyBp.getTier()).isEqualTo(jsBp.get(0).asInt());
            assertThat(pyBp.getStartEquity()).isCloseTo(jsBp.get(1).asDouble(), within(1e-2));
            assertThat(pyBp.getEndEquity()).isCloseTo(jsBp.get(2).asDouble(), within(1e-2));
        }
    }

    @Test
    void testCalibrationBacksolveAndAllocation() {
        JsonNode inputs = goldenData.get("inputs");
        List<SecurityInput> calInputs = new ArrayList<>();
        for (JsonNode r : inputs.get("PREPOP_CAL")) {
            calInputs.add(mapRawToInput(r));
        }

        List<DerivedSecurity> calDerived = capitalizationService.deriveCapitalization(
                calInputs, inputs.get("exitDate").asText(), inputs.get("basis").asInt());
        List<BreakpointTier> calBps = breakpointService.generateBreakpoints(calDerived);

        double solvedEq = opmService.backsolveEquity(
                calDerived,
                calBps,
                inputs.get("calDate").asText(),
                inputs.get("exitDate").asText(),
                inputs.get("rfCal").asDouble(),
                inputs.get("volCal").asDouble(),
                inputs.get("qCal").asDouble(),
                inputs.get("calSec").asText(),
                inputs.get("txPrice").asDouble(),
                inputs.get("basis").asInt()
        );

        double expectedSolved = goldenData.get("calibration").get("solvedEquity").asDouble();
        // Verify equity within $0.05
        assertThat(solvedEq).isCloseTo(expectedSolved, within(0.05));

        // Verify OPM allocation for Calibration Date
        OpmAllocationResult opmRes = opmService.allocateOpm(
                calDerived,
                calBps,
                solvedEq,
                inputs.get("calDate").asText(),
                inputs.get("exitDate").asText(),
                inputs.get("rfCal").asDouble(),
                inputs.get("volCal").asDouble(),
                inputs.get("qCal").asDouble(),
                inputs.get("basis").asInt()
        );

        // Series I per-share must match target price 2021.90 exactly
        assertThat(opmRes.getPerShareValues().get("Series I")).isCloseTo(inputs.get("txPrice").asDouble(), within(1e-4));

        JsonNode goldenAlloc = goldenData.get("calibration").get("opmAllocation").get("alloc");
        Iterator<String> fieldNames = goldenAlloc.fieldNames();
        while (fieldNames.hasNext()) {
            String secName = fieldNames.next();
            double goldenVal = goldenAlloc.get(secName).asDouble();
            assertThat(opmRes.getAllocatedValues().get(secName)).isCloseTo(goldenVal, within(1.0));
        }
    }

    @Test
    void testValuationAllocation() {
        JsonNode inputs = goldenData.get("inputs");
        List<SecurityInput> valInputs = new ArrayList<>();
        for (JsonNode r : inputs.get("PREPOP_VAL")) {
            valInputs.add(mapRawToInput(r));
        }

        List<DerivedSecurity> valDerived = capitalizationService.deriveCapitalization(
                valInputs, inputs.get("exitDate").asText(), inputs.get("basis").asInt());
        List<BreakpointTier> valBps = breakpointService.generateBreakpoints(valDerived);

        double adjEq = goldenData.get("valuation").get("adjustedEquity").asDouble();

        OpmAllocationResult opmVal = opmService.allocateOpm(
                valDerived,
                valBps,
                adjEq,
                inputs.get("valDate").asText(),
                inputs.get("exitDate").asText(),
                inputs.get("rfVal").asDouble(),
                inputs.get("volVal").asDouble(),
                inputs.get("qVal").asDouble(),
                inputs.get("basis").asInt()
        );

        JsonNode goldenAlloc = goldenData.get("valuation").get("opmAllocation").get("alloc");
        Iterator<String> fieldNames = goldenAlloc.fieldNames();
        while (fieldNames.hasNext()) {
            String secName = fieldNames.next();
            double goldenVal = goldenAlloc.get(secName).asDouble();
            assertThat(opmVal.getAllocatedValues().get(secName)).isCloseTo(goldenVal, within(1.0));
        }

        // Verify per-share concluded fair values
        assertThat(opmVal.getPerShareValues().get("Series I")).isCloseTo(1992.5034, within(0.01));
        assertThat(opmVal.getPerShareValues().get("Series H")).isCloseTo(2391.2852, within(0.01));
        assertThat(opmVal.getPerShareValues().get("Series G2")).isCloseTo(1463.5913, within(0.01));
        assertThat(opmVal.getPerShareValues().get("Common Stock")).isCloseTo(740.1469, within(0.01));
        assertThat(opmVal.getPerShareValues().get("Common Stock Options A")).isCloseTo(739.3276, within(0.01));
        assertThat(opmVal.getPerShareValues().get("Common Stock Options B")).isCloseTo(254.0772, within(0.01));
        assertThat(opmVal.getPerShareValues().get("Series H Warrants")).isCloseTo(740.1469, within(0.01));
    }

    @Test
    void testWaterfallAllocation() {
        JsonNode inputs = goldenData.get("inputs");
        List<SecurityInput> valInputs = new ArrayList<>();
        for (JsonNode r : inputs.get("PREPOP_VAL")) {
            valInputs.add(mapRawToInput(r));
        }

        List<DerivedSecurity> valDerived = capitalizationService.deriveCapitalization(
                valInputs, inputs.get("exitDate").asText(), inputs.get("basis").asInt());
        List<BreakpointTier> valBps = breakpointService.generateBreakpoints(valDerived);

        WaterfallResult wfRes = waterfallService.allocateWaterfall(valDerived, valBps, 229900000.0);
        assertThat(wfRes.getTotalProceeds()).isCloseTo(229900000.0, within(1.0));

        double totalPref = 0.0;
        for (DerivedSecurity r : valDerived) {
            totalPref += r.getTotalLiquidationPreference();
        }
        assertThat(totalPref).isCloseTo(195580334.84, within(1.0));
    }

    @Test
    void testClientHoldingsEvaluation() {
        JsonNode inputs = goldenData.get("inputs");
        List<SecurityInput> valInputs = new ArrayList<>();
        for (JsonNode r : inputs.get("PREPOP_VAL")) {
            valInputs.add(mapRawToInput(r));
        }

        List<DerivedSecurity> valDerived = capitalizationService.deriveCapitalization(
                valInputs, inputs.get("exitDate").asText(), inputs.get("basis").asInt());
        List<BreakpointTier> valBps = breakpointService.generateBreakpoints(valDerived);
        double adjEq = goldenData.get("valuation").get("adjustedEquity").asDouble();

        OpmAllocationResult opmVal = opmService.allocateOpm(
                valDerived,
                valBps,
                adjEq,
                inputs.get("valDate").asText(),
                inputs.get("exitDate").asText(),
                inputs.get("rfVal").asDouble(),
                inputs.get("volVal").asDouble(),
                inputs.get("qVal").asDouble(),
                inputs.get("basis").asInt()
        );

        List<HoldingInput> demoHoldings = List.of(
                new HoldingInput("S2G Fund I", "Series F1", 51, 65305),
                new HoldingInput("S2G Fund I", "Series H", 4000, 7000000),
                new HoldingInput("S2G Fund I", "Common Stock", 300, 500000),
                new HoldingInput("S2G Fund II", "Series H", 2500, 4000000),
                new HoldingInput("S2G Fund II", "Common Stock", 300, 450000),
                new HoldingInput("S2G Fund III", "Series H", 594, 1088229),
                new HoldingInput("S2G Fund III", "Common Stock", 137, 195660)
        );

        HoldingsSummary summary = holdingsService.evaluateClientHoldings(demoHoldings, valDerived, opmVal.getPerShareValues());

        HoldingResultItem f1Item = summary.getItems().stream()
                .filter(it -> "S2G Fund I".equals(it.getFund()) && "Series F1".equals(it.getSecurity()))
                .findFirst().orElseThrow();
        assertThat(f1Item.getConcludedFairValue()).isCloseTo(58800.70, within(1.0));
        assertThat(f1Item.getMoic()).isNotNull();
        assertThat(f1Item.getMoic()).isCloseTo(0.90, within(0.01));

        HoldingResultItem hItem = summary.getItems().stream()
                .filter(it -> "S2G Fund I".equals(it.getFund()) && "Series H".equals(it.getSecurity()))
                .findFirst().orElseThrow();
        assertThat(hItem.getConcludedFairValue()).isCloseTo(9565140.76, within(5.0));
        assertThat(hItem.getMoic()).isNotNull();
        assertThat(hItem.getMoic()).isCloseTo(1.366, within(0.01));

        assertThat(summary.getFundSubtotals()).hasSize(3);
        assertThat(summary.getTotalCost()).isCloseTo(13299194.0, within(1.0));
    }
}

