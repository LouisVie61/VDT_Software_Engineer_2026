package vdt.se.demo.application.service.patch;

import vdt.se.demo.domain.iql.IqlQuery;
import vdt.se.demo.domain.iql.PatchOperation;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Conservative backstop for patches that replace the prior topic instead of refining it. */
public final class PatchTopicChangeDetector {
    public boolean likelyTopicChange(IqlQuery previous, List<PatchOperation> operations) {
        if (previous == null || previous.filters().isEmpty() || operations == null) return false;
        Set<String> removed = new HashSet<>();
        Set<String> addedFields = new HashSet<>();
        boolean clearsGrouping = false;
        for (PatchOperation operation : operations) {
            if (operation == null || operation.op() == null) continue;
            if (operation.op() == PatchOperation.Type.REMOVE_FILTER && operation.filterId() != null)
                removed.add(operation.filterId());
            if (operation.op() == PatchOperation.Type.ADD_FILTER && operation.value() != null)
                addedFields.add(operation.value().path("field").asString());
            clearsGrouping |= operation.op() == PatchOperation.Type.CLEAR_GROUP_BY;
        }
        Set<String> previousFields = new HashSet<>();
        previous.filters().forEach(filter -> previousFields.add(filter.field()));
        boolean removesMost = removed.size() >= Math.max(1, (previous.filters().size() + 1) / 2);
        boolean unrelatedReplacement = !addedFields.isEmpty() && addedFields.stream().noneMatch(previousFields::contains);
        return removesMost && unrelatedReplacement && (previous.groupBy().isEmpty() || clearsGrouping);
    }
}
