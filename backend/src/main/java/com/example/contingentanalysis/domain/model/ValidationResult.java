package com.example.contingentanalysis.domain.model;

import java.util.ArrayList;
import java.util.List;

public class ValidationResult {
    private boolean valid;
    private List<String> issues = new ArrayList<>();

    public ValidationResult() {
    }

    public ValidationResult(boolean valid, List<String> issues) {
        this.valid = valid;
        this.issues = issues != null ? issues : new ArrayList<>();
    }

    public boolean isValid() { return valid; }
    public void setValid(boolean valid) { this.valid = valid; }

    public List<String> getIssues() { return issues; }
    public void setIssues(List<String> issues) { this.issues = issues != null ? issues : new ArrayList<>(); }
}

