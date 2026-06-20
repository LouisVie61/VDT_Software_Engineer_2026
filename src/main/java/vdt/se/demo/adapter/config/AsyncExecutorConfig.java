package vdt.se.demo.adapter.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

@Configuration
public class AsyncExecutorConfig {
    @Bean(name = "auditTaskExecutor")
    ThreadPoolTaskExecutor auditTaskExecutor() {
        return executor("audit-", 2, 20, 200);
    }

    @Bean(name = "summaryTaskExecutor")
    ThreadPoolTaskExecutor summaryTaskExecutor() {
        return executor("summary-", 1, 4, 100);
    }

    @Bean(name = "diagnosticTaskExecutor")
    ThreadPoolTaskExecutor diagnosticTaskExecutor() {
        return executor("diagnostic-", 2, 8, 100);
    }

    private ThreadPoolTaskExecutor executor(String prefix, int corePoolSize, int maxPoolSize, int queueCapacity) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix(prefix);
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardPolicy());
        executor.initialize();
        return executor;
    }
}
