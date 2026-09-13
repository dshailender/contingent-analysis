package com.example.contingentanalysis.domain.validation;

import com.example.contingentanalysis.domain.model.SecurityInput;
import com.example.contingentanalysis.domain.model.ValuationRequest;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class ValuationValidationService {

    public List<String> validateSecurities(List<SecurityInput> securities, String contextLabel) {
        List<String> issues = new ArrayList<>();
        if (securities == null || securities.isEmpty()) {
            issues.add(contextLabel + ": At least one security class is required.");
            return issues;
        }

        Set<String> seenNames = new HashSet<>();
        for (int i = 0; i < securities.size(); i++) {
            int rowNum = i + 1;
            SecurityInput s = securities.get(i);
            String name = s.getSecurity() != null ? s.getSecurity().trim() : "";

            if (name.isEmpty()) {
                issues.add(String.format("%s (Row %d): Security name is required.", contextLabel, rowNum));
                continue;
            }

            if (seenNames.contains(name.toLowerCase(Locale.ROOT))) {
                issues.add(String.format("%s (Row %d): Duplicate security name '%s'.", contextLabel, rowNum, name));
            }
            seenNames.add(name.toLowerCase(Locale.ROOT));

            if (s.getShares() <= 0.0) {
                issues.add(String.format("%s (Row %d - %s): Number of shares must be greater than 0.", contextLabel, rowNum, name));
            }

            if ("Preferred Stock".equalsIgnoreCase(s.getSecuritySubtype())) {
                if (s.getOriginalIssuePrice() == null || s.getOriginalIssuePrice() <= 0.0) {
                    issues.add(String.format("%s (Row %d - %s): Original Issue Price must be greater than 0.", contextLabel, rowNum, name));
                }
                if (s.getSeniority() == null || s.getSeniority() <= 0) {
                    issues.add(String.format("%s (Row %d - %s): Seniority must be a positive integer (1 = most senior).", contextLabel, rowNum, name));
                }
                if (s.getConversionPrice() != null && s.getConversionPrice() < 0.0) {
                    issues.add(String.format("%s (Row %d - %s): Conversion Price cannot be negative (0 indicates non-converting).", contextLabel, rowNum, name));
                }
            } else if ("Option".equalsIgnoreCase(s.getSecuritySubtype()) || "Warrant".equalsIgnoreCase(s.getSecuritySubtype())) {
                if (s.getExercisePrice() != null && s.getExercisePrice() < 0.0) {
                    issues.add(String.format("%s (Row %d - %s): Weighted average exercise price cannot be negative.", contextLabel, rowNum, name));
                }
            }
        }

        return issues;
    }

    public List<String> validateValuationRequest(ValuationRequest req) {
        List<String> issues = new ArrayList<>();
        if (req == null) {
            issues.add("Request payload cannot be null.");
            return issues;
        }

        if (req.getCompanyName() == null || req.getCompanyName().trim().isEmpty()) {
            issues.add("Company Name is required.");
        }
        if (req.getClientName() == null || req.getClientName().trim().isEmpty()) {
            issues.add("Client Name is required.");
        }
        if (req.getCalibrationDate() == null || req.getCalibrationDate().trim().isEmpty()) {
            issues.add("Calibration Date is required.");
        }
        if (req.getValuationDate() == null || req.getValuationDate().trim().isEmpty()) {
            issues.add("Valuation Date is required.");
        }
        if (req.getExitDate() == null || req.getExitDate().trim().isEmpty()) {
            issues.add("Global Exit Date is required.");
        }

        if (req.getCalibrationDate() != null && req.getExitDate() != null
                && req.getCalibrationDate().compareTo(req.getExitDate()) >= 0) {
            issues.add("Calibration Date must precede Global Exit Date.");
        }
        if (req.getValuationDate() != null && req.getExitDate() != null
                && req.getValuationDate().compareTo(req.getExitDate()) >= 0) {
            issues.add("Valuation Date must precede Global Exit Date.");
        }

        // Validate cap tables
        issues.addAll(validateSecurities(req.getCalibrationSecurities(), "Calibration Date Cap Table"));
        issues.addAll(validateSecurities(req.getValuationSecurities(), "Valuation Date Cap Table"));

        // Validate and auto-resolve calibration security
        List<String> calNames = new ArrayList<>();
        if (req.getCalibrationSecurities() != null) {
            for (SecurityInput s : req.getCalibrationSecurities()) {
                if (s.getSecurity() != null && !s.getSecurity().trim().isEmpty()) {
                    calNames.add(s.getSecurity().trim());
                }
            }
        }

        if (calNames.isEmpty()) {
            issues.add("Calibration Date Cap Table must contain at least one valid security class.");
        } else {
            String currentTarget = req.getCalibrationSecurityName() != null ? req.getCalibrationSecurityName().trim() : "";
            if (!calNames.contains(currentTarget)) {
                // Graceful auto-resolution: pick first available Preferred Stock or first valid security
                String preferred = null;
                for (SecurityInput s : req.getCalibrationSecurities()) {
                    if ("Preferred Stock".equalsIgnoreCase(s.getSecuritySubtype())
                            && s.getSecurity() != null && !s.getSecurity().trim().isEmpty()) {
                        preferred = s.getSecurity().trim();
                        break;
                    }
                }
                req.setCalibrationSecurityName(preferred != null ? preferred : calNames.get(0));
            } else {
                req.setCalibrationSecurityName(currentTarget);
            }
        }

        if (req.getTransactionPrice() <= 0.0) {
            issues.add("Observed Transaction Price must be greater than 0.");
        }

        if (req.getVolCalibration() <= 0.0) {
            issues.add("Calibration Volatility must be greater than 0%.");
        }
        if (req.getVolValuation() <= 0.0) {
            issues.add("Valuation Volatility must be greater than 0%.");
        }

        return issues;
    }
}

