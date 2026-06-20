package vdt.se.demo.adapter.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import vdt.se.demo.application.service.intent.SearchIntentNormalizer;
import vdt.se.demo.application.service.intent.SearchTimeExpressionResolver;
import vdt.se.demo.application.service.intent.SemanticAliasLexicon;
import vdt.se.demo.application.service.intent.SemanticResidualTextResolver;
import vdt.se.demo.application.service.intent.SemanticSpanResolver;
import vdt.se.demo.application.service.intent.SemanticTokenizer;
import vdt.se.demo.application.service.intent.TemporalSpanDetector;
import vdt.se.demo.application.service.intent.PhraseSpanDetector;
import vdt.se.demo.application.service.intent.TemporalValueResolver;
import vdt.se.demo.application.service.template.CanonicalPlanBuilder;
import vdt.se.demo.application.service.template.GroupByResolver;
import vdt.se.demo.application.service.template.TemplateIntentSelector;
import vdt.se.demo.application.service.template.TemplateSelectionService;

@Configuration
public class IntentTemplateConfig {
    @Bean
    SearchIntentNormalizer searchIntentNormalizer(SemanticResidualTextResolver residualTextResolver,
                                                  SemanticSpanResolver spanResolver,
                                                  SearchTimeExpressionResolver timeExpressionResolver) {
        return new SearchIntentNormalizer(residualTextResolver, spanResolver, timeExpressionResolver);
    }

    @Bean
    SemanticResidualTextResolver semanticResidualTextResolver(SemanticAliasLexicon lexicon,
                                                             SemanticSpanResolver spanResolver) {
        return new SemanticResidualTextResolver(lexicon, spanResolver);
    }

    @Bean
    SemanticAliasLexicon semanticAliasLexicon() {
        return new SemanticAliasLexicon();
    }

    @Bean
    SemanticSpanResolver semanticSpanResolver(SemanticAliasLexicon lexicon,
                                              SemanticTokenizer tokenizer,
                                              TemporalSpanDetector temporalSpanDetector,
                                              PhraseSpanDetector phraseSpanDetector) {
        return new SemanticSpanResolver(lexicon, tokenizer, temporalSpanDetector, phraseSpanDetector);
    }

    @Bean
    SemanticTokenizer semanticTokenizer() {
        return new SemanticTokenizer();
    }

    @Bean
    TemporalSpanDetector temporalSpanDetector() {
        return new TemporalSpanDetector();
    }

    @Bean
    PhraseSpanDetector phraseSpanDetector() {
        return new PhraseSpanDetector();
    }

    @Bean
    SearchTimeExpressionResolver searchTimeExpressionResolver(TemporalValueResolver temporalValueResolver) {
        return new SearchTimeExpressionResolver(temporalValueResolver);
    }

    @Bean
    TemporalValueResolver temporalValueResolver() {
        return new TemporalValueResolver();
    }

    @Bean
    TemplateSelectionService templateSelectionService() {
        return new TemplateSelectionService();
    }

    @Bean
    CanonicalPlanBuilder canonicalPlanBuilder(TemplateSelectionService templateSelectionService,
                                              TemplateIntentSelector templateIntentSelector,
                                              GroupByResolver groupByResolver) {
        return new CanonicalPlanBuilder(templateSelectionService, templateIntentSelector, groupByResolver);
    }

    @Bean
    TemplateIntentSelector templateIntentSelector() {
        return new TemplateIntentSelector();
    }

    @Bean
    GroupByResolver groupByResolver() {
        return new GroupByResolver();
    }
}
