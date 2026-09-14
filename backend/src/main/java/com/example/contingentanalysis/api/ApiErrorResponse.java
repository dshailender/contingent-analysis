package com.example.contingentanalysis.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiErrorResponse(
        String detail,
        int status,
        String timestamp,
        String path,
        @JsonProperty("request_id")
        String requestId,
        List<String> errors
) {
    public static ApiErrorResponse of(String detail, int status, String path, String requestId) {
        return new ApiErrorResponse(detail, status, Instant.now().toString(), path, requestId, null);
    }

    public static ApiErrorResponse of(String detail, int status, String path, String requestId, List<String> errors) {
        return new ApiErrorResponse(detail, status, Instant.now().toString(), path, requestId, errors);
    }
}

