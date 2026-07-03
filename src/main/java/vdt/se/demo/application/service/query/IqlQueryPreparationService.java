package vdt.se.demo.application.service.query;

import vdt.se.demo.application.port.outboundPort.llm.LlmToolCallPort;
import vdt.se.demo.application.service.llm.LlmToolDefinitions;
import vdt.se.demo.application.service.patch.PatchApplierService;
import vdt.se.demo.application.service.reference.ReferenceResolverService;
import vdt.se.demo.application.service.validation.SchemaRegistry;
import vdt.se.demo.application.service.validation.ValidationResult;
import vdt.se.demo.domain.exception.BadQueryException;
import vdt.se.demo.domain.iql.*;

public final class IqlQueryPreparationService {
    private final LlmToolCallPort llm; private final LlmToolDefinitions tools; private final PatchApplierService patches;
    private final ReferenceResolverService references; private final SchemaRegistry schema;
    public IqlQueryPreparationService(LlmToolCallPort llm,LlmToolDefinitions tools,PatchApplierService patches,
            ReferenceResolverService references,SchemaRegistry schema){this.llm=llm;this.tools=tools;this.patches=patches;this.references=references;this.schema=schema;}
    public IqlQuery prepare(String text, SessionState previous) {
        ToolCallResult call=llm.invoke(text,previous,tools.all());
        if(call instanceof ToolCallResult.AskClarification c) throw new BadQueryException(c.question());
        ToolCallResult.SearchEvents search=(ToolCallResult.SearchEvents)call;
        IqlQuery query=search.mode()==ToolCallResult.Mode.PATCH?patches.apply(previous.lastQuery(),search.patchOps()):search.query();
        query=references.resolve(query,previous.lastResultSummary());
        ValidationResult result=schema.validate(query);
        if(!result.ok())throw new BadQueryException(String.join("; ",result.errors()));
        return query;
    }
}
