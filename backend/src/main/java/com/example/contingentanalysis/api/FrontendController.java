package com.example.contingentanalysis.api;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

@Controller
public class FrontendController {

    private final Resource indexHtmlResource;

    public FrontendController() {
        Path frontendDist = Path.of("../frontend/dist/frontend/browser/index.html").toAbsolutePath().normalize();
        if (!Files.exists(frontendDist)) {
            frontendDist = Path.of("frontend/dist/frontend/browser/index.html").toAbsolutePath().normalize();
        }
        if (Files.exists(frontendDist)) {
            this.indexHtmlResource = new FileSystemResource(frontendDist);
        } else {
            this.indexHtmlResource = null;
        }
    }

    @GetMapping(value = "/", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public ResponseEntity<?> root() {
        if (indexHtmlResource != null && indexHtmlResource.exists()) {
            return ResponseEntity.ok(indexHtmlResource);
        }
        return ResponseEntity.ok(Map.of(
                "app", "Contingent Claims Analysis Valuation Engine",
                "version", "1.0.0",
                "docs", "/docs"
        ));
    }
}

