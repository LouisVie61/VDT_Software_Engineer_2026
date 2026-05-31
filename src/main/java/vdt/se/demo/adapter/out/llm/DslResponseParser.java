package vdt.se.demo.adapter.out.llm;

import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import vdt.se.demo.domain.exception.BadQueryException;

@Component
public class DslResponseParser {

    private final ObjectMapper objectMapper;

    public DslResponseParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public JsonNode parse(String rawContent) {
        try {
            JsonNode root = objectMapper.readTree(stripMarkdown(rawContent));
            if (root == null || !root.isObject()) {
                throw new BadQueryException("LLM returned Elasticsearch DSL that is not a JSON object");
            }
            return root;
        } catch (BadQueryException e) {
            throw e;
        } catch (Exception e) {
            throw new BadQueryException("LLM returned invalid Elasticsearch DSL JSON", e);
        }
    }

    private String stripMarkdown(String content) {
        if (content == null || content.isBlank()) {
            throw new BadQueryException("LLM response is empty");
        }
        return content.replace("```json", "").replace("```", "").trim();
    }
}
