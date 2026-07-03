package vdt.se.demo.adapter.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import vdt.se.demo.application.service.intent.FieldValueRegistry;
import vdt.se.demo.application.service.intent.ConfirmationIntentValidator;
import vdt.se.demo.application.service.intent.SearchFilterValidator;
import vdt.se.demo.application.service.intent.SearchIntentNormalizer;
import vdt.se.demo.application.service.intent.SearchSchemaRegistry;
import vdt.se.demo.application.service.intent.SearchTimeExpressionResolver;
import vdt.se.demo.application.service.intent.TemporalValueResolver;
import vdt.se.demo.application.service.template.CanonicalPlanBuilder;
import vdt.se.demo.application.service.template.GroupByResolver;
import vdt.se.demo.application.service.template.TemplateIntentSelector;
import vdt.se.demo.application.service.template.TemplateSelectionService;

@Configuration
public class IntentTemplateConfig {
    @Bean
    SearchIntentNormalizer searchIntentNormalizer(SearchTimeExpressionResolver timeExpressionResolver,
                                                  SearchFilterValidator searchFilterValidator) {
        return new SearchIntentNormalizer(timeExpressionResolver, searchFilterValidator);
    }

    @Bean
    SearchFilterValidator searchFilterValidator(SearchSchemaRegistry searchSchemaRegistry,
                                                FieldValueRegistry fieldValueRegistry) {
        return new SearchFilterValidator(searchSchemaRegistry, fieldValueRegistry);
    }

    @Bean
    SearchSchemaRegistry searchSchemaRegistry() {
        return new SearchSchemaRegistry();
    }

    @Bean
    ConfirmationIntentValidator confirmationIntentValidator(SearchSchemaRegistry searchSchemaRegistry) {
        return new ConfirmationIntentValidator(searchSchemaRegistry);
    }

    @Bean
    FieldValueRegistry fieldValueRegistry() {
        return new FieldValueRegistry();
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
    TemplateSelectionService templateSelectionService(SearchSchemaRegistry searchSchemaRegistry) {
        return new TemplateSelectionService(searchSchemaRegistry);
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
