package vdt.se.demo.adapter.out.llm;

import org.springframework.stereotype.Component;
import vdt.se.demo.domain.model.ExecutionResult;

@Component
public class DeterministicSummaryBuilder {

    public String build(ExecutionResult result) {
        return "Found " + result.totalCount() + " matching events. "
                + "Review the generated query and the highest-frequency aggregation buckets for investigation. "
                + "Start by checking repeated users, hosts, or IP addresses in the returned results.";
    }
}
