package com.jobscanner.scoring.unit;

import com.jobscanner.scoring.adapter.out.scorer.AnthropicJobScorer;
import com.jobscanner.scoring.domain.model.ScoringProfile;
import com.jobscanner.scoring.domain.value.JobListingDto;
import com.jobscanner.scoring.domain.value.ScoreResult;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AnthropicJobScorerTest {

    // Test parseResponse via reflection to verify JSON parsing without an HTTP call
    private ScoreResult invokeParseResponse(String text) throws Exception {
        AnthropicJobScorer scorer = new AnthropicJobScorer("fake-key", "test-model", "http://localhost");
        Method m = AnthropicJobScorer.class.getDeclaredMethod("parseResponse", String.class);
        m.setAccessible(true);
        return (ScoreResult) m.invoke(scorer, text);
    }

    @Test
    void parseResponse_validJson_extractsScoreAndReasoning() throws Exception {
        String json = """
                {"score": 88, "reasoning": "Strong match for Java/Spring Boot role"}
                """;
        ScoreResult result = invokeParseResponse(json);
        assertThat(result.score()).isEqualTo(88);
        assertThat(result.reasoning()).isEqualTo("Strong match for Java/Spring Boot role");
    }

    @Test
    void parseResponse_jsonInMarkdownBlock_extractsCorrectly() throws Exception {
        String text = "```json\n{\"score\": 65, \"reasoning\": \"Partial match\"}\n```";
        ScoreResult result = invokeParseResponse(text);
        assertThat(result.score()).isEqualTo(65);
    }

    @Test
    void parseResponse_malformedJson_returnsFallback() throws Exception {
        ScoreResult result = invokeParseResponse("not json at all");
        assertThat(result.score()).isEqualTo(50);
        assertThat(result.reasoning()).contains("Could not parse");
    }

    @Test
    void parseResponse_scoreClampedTo100() throws Exception {
        ScoreResult result = invokeParseResponse("{\"score\": 150, \"reasoning\": \"Too high\"}");
        assertThat(result.score()).isEqualTo(100);
    }

    @Test
    void parseResponse_scoreClampedTo0() throws Exception {
        ScoreResult result = invokeParseResponse("{\"score\": -10, \"reasoning\": \"Too low\"}");
        assertThat(result.score()).isEqualTo(0);
    }
}
