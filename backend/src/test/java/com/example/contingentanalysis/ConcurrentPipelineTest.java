package com.example.contingentanalysis;

import com.example.contingentanalysis.domain.defaultscenario.DefaultScenarioService;
import com.example.contingentanalysis.domain.model.ValuationRequest;
import com.example.contingentanalysis.domain.model.ValuationResponse;
import com.example.contingentanalysis.domain.pipeline.ValuationPipelineService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

@SpringBootTest
public class ConcurrentPipelineTest {

    @Autowired
    private ValuationPipelineService pipelineService;

    @Autowired
    private DefaultScenarioService defaultScenarioService;

    @Test
    void testConcurrentValuationPipelineExecution() throws Exception {
        int threadCount = 20;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        List<Callable<ValuationResponse>> tasks = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            tasks.add(() -> {
                ValuationRequest req = defaultScenarioService.getDefaultValuationRequest();
                return pipelineService.calculateValuation(req);
            });
        }

        List<Future<ValuationResponse>> futures = executor.invokeAll(tasks);
        executor.shutdown();

        assertThat(futures).hasSize(threadCount);

        Double referenceSolvedEquity = null;
        for (Future<ValuationResponse> future : futures) {
            ValuationResponse response = future.get();
            assertThat(response).isNotNull();
            assertThat(response.getCompanyName()).isEqualTo("TADO");

            double solvedEquity = response.getCalibrationSolvedEquity();
            assertThat(solvedEquity).isCloseTo(287252502.92, within(1.0));

            if (referenceSolvedEquity == null) {
                referenceSolvedEquity = solvedEquity;
            } else {
                // Assert bitwise identical calculation across concurrent threads
                assertThat(solvedEquity).isEqualTo(referenceSolvedEquity);
            }

            double seriesICal = response.getCalibrationOpm().getPerShareValues().get("Series I");
            assertThat(seriesICal).isCloseTo(2021.90, within(0.01));

            double seriesIVal = response.getValuationOpm().getPerShareValues().get("Series I");
            assertThat(seriesIVal).isCloseTo(1992.50, within(0.05));

            double seriesHVal = response.getValuationOpm().getPerShareValues().get("Series H");
            assertThat(seriesHVal).isCloseTo(2391.29, within(0.05));
        }
    }
}

