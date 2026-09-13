package com.example.contingentanalysis.domain.defaultscenario;

import com.example.contingentanalysis.domain.model.HoldingInput;
import com.example.contingentanalysis.domain.model.SecurityInput;
import com.example.contingentanalysis.domain.model.ValuationRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Service
public class DefaultScenarioService {

    private final ObjectMapper objectMapper = new ObjectMapper();

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

    private SecurityInput rawToSecurity(JsonNode raw) {
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
                secName,
                subtype,
                shares != null ? shares : 0.0,
                ep,
                oip,
                cp,
                lm != null ? lm : 1.0,
                part,
                cap,
                seniority,
                issueDate,
                divRate,
                cc,
                divPaid != null ? divPaid : 0.0
        );
    }

    public ValuationRequest getDefaultValuationRequest() {
        Path p1 = Path.of("../golden_reference/golden_reference_run.json").toAbsolutePath().normalize();
        Path p2 = Path.of("golden_reference/golden_reference_run.json").toAbsolutePath().normalize();

        JsonNode root;
        try {
            if (Files.exists(p1)) {
                root = objectMapper.readTree(Files.newInputStream(p1));
            } else if (Files.exists(p2)) {
                root = objectMapper.readTree(Files.newInputStream(p2));
            } else {
                throw new IllegalStateException("golden_reference_run.json not found in " + p1 + " or " + p2);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to load golden reference default scenario: " + e.getMessage(), e);
        }

        JsonNode inputs = root.get("inputs");

        List<SecurityInput> calSecs = new ArrayList<>();
        if (inputs.has("PREPOP_CAL")) {
            for (JsonNode row : inputs.get("PREPOP_CAL")) {
                calSecs.add(rawToSecurity(row));
            }
        }

        List<SecurityInput> valSecs = new ArrayList<>();
        if (inputs.has("PREPOP_VAL")) {
            for (JsonNode row : inputs.get("PREPOP_VAL")) {
                valSecs.add(rawToSecurity(row));
            }
        }

        List<HoldingInput> holdings = List.of(
                new HoldingInput("S2G Fund I", "Series F1", 51, 65305),
                new HoldingInput("S2G Fund I", "Series H", 4000, 7000000),
                new HoldingInput("S2G Fund I", "Common Stock", 300, 500000),
                new HoldingInput("S2G Fund II", "Series H", 2500, 4000000),
                new HoldingInput("S2G Fund II", "Common Stock", 300, 450000),
                new HoldingInput("S2G Fund III", "Series H", 594, 1088229),
                new HoldingInput("S2G Fund III", "Common Stock", 137, 195660)
        );

        ValuationRequest req = new ValuationRequest();
        req.setCompanyName("TADO");
        req.setClientName("S2G Investments");
        req.setReportStatus("DRAFT - For Discussion Purposes Only");
        req.setReportPurpose("Valuation Analysis");
        req.setReportPurposeManual("");
        req.setCalibrationDate(inputs.has("calDate") ? inputs.get("calDate").asText() : "2025-02-26");
        req.setValuationDate(inputs.has("valDate") ? inputs.get("valDate").asText() : "2026-06-30");
        req.setExitDate(inputs.has("exitDate") ? inputs.get("exitDate").asText() : "2027-06-30");
        req.setDayCountBasis(inputs.has("basis") ? inputs.get("basis").asInt() : 1);
        req.setReportCurrency("EUR");
        req.setDisplayUnits("actual");
        req.setCalibrationSecurities(calSecs);
        req.setValuationSecurities(valSecs);
        req.setCalibrationSecurityName(inputs.has("calSec") ? inputs.get("calSec").asText() : "Series I");

        double txPrice = inputs.has("txPrice") ? inputs.get("txPrice").asDouble() : 2021.90;
        req.setTransactionPrice(txPrice);

        double rfCal = inputs.has("rfCal") ? inputs.get("rfCal").asDouble() : 0.021;
        req.setRfCalibration(rfCal < 1.0 ? rfCal * 100.0 : rfCal);

        double volCal = inputs.has("volCal") ? inputs.get("volCal").asDouble() : 0.35;
        req.setVolCalibration(volCal < 1.0 ? volCal * 100.0 : volCal);

        double qCal = inputs.has("qCal") ? inputs.get("qCal").asDouble() : 0.0;
        req.setDividendYieldCalibration(qCal * 100.0);

        double rfVal = inputs.has("rfVal") ? inputs.get("rfVal").asDouble() : 0.024;
        req.setRfValuation(rfVal < 1.0 ? rfVal * 100.0 : rfVal);

        double volVal = inputs.has("volVal") ? inputs.get("volVal").asDouble() : 0.35;
        req.setVolValuation(volVal < 1.0 ? volVal * 100.0 : volVal);

        double qVal = inputs.has("qVal") ? inputs.get("qVal").asDouble() : 0.0;
        req.setDividendYieldValuation(qVal * 100.0);

        req.setMarketAdjustment(inputs.has("marketAdj") ? inputs.get("marketAdj").asDouble() : 0.0);
        req.setCompanyAdjustment(inputs.has("compAdj") ? inputs.get("compAdj").asDouble() : 0.0);
        req.setWaterfallEquitySource("concluded");
        req.setManualWaterfallEquity(229900000.0);
        req.setHoldings(new ArrayList<>(holdings));

        return req;
    }
}

