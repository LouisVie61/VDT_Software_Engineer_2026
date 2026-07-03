package vdt.se.demo.adapter.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.ObjectMapper;
import vdt.se.demo.application.port.outboundPort.execution.QueryExecutorPort;
import vdt.se.demo.application.port.outboundPort.history.QueryHistoryPort;
import vdt.se.demo.application.port.outboundPort.llm.SummaryPort;
import vdt.se.demo.application.service.query.QueryCsvExportService;
import vdt.se.demo.application.service.query.QuerySummaryService;

import java.util.concurrent.Executor;

@Configuration
public class QuerySupportConfig {
    @Bean QuerySummaryService querySummaryService(SummaryPort port,
            @Qualifier("summaryTaskExecutor") Executor executor) { return new QuerySummaryService(port, executor); }
    @Bean QueryCsvExportService queryCsvExportService(QueryHistoryPort history, ObjectMapper mapper,
            QueryExecutorPort executor) { return new QueryCsvExportService(history, mapper, executor); }
}
