package vdt.se.demo.domain.iql;

import java.util.List;

public sealed interface ToolCallResult permits ToolCallResult.SearchEvents, ToolCallResult.AskClarification {
    record SearchEvents(Mode mode, IqlQuery query, List<PatchOperation> patchOps) implements ToolCallResult {
        public SearchEvents { patchOps = patchOps == null ? List.of() : List.copyOf(patchOps); }
    }
    enum Mode { NEW, PATCH }
    record AskClarification(Reason reason, String question, List<String> candidates) implements ToolCallResult {
        public AskClarification { candidates = candidates == null ? List.of() : List.copyOf(candidates); }
    }
    enum Reason { AMBIGUOUS_REFERENCE, MISSING_FIELD, UNSAFE_SCOPE, UNCLEAR_INTENT }
}
