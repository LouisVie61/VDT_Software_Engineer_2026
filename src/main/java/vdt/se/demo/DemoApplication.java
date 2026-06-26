package vdt.se.demo;

import java.util.TimeZone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.scheduling.annotation.EnableAsync;
import vdt.se.demo.adapter.out.elasticsearch.ElasticsearchDslTimeRangeEditor;
import vdt.se.demo.adapter.out.elasticsearch.ElasticsearchEventIndexAdapter;
import vdt.se.demo.adapter.out.elasticsearch.ElasticsearchHttpClient;
import vdt.se.demo.adapter.out.elasticsearch.ElasticsearchLatestTimestampResolver;
import vdt.se.demo.adapter.out.elasticsearch.ElasticsearchRelativeTimeQueryExecutionRefiner;
import vdt.se.demo.adapter.out.elasticsearch.ElasticsearchSearchResponseMapper;
import vdt.se.demo.adapter.out.elasticsearch.QueryExecutorAdapter;
import vdt.se.demo.adapter.out.elasticsearch.SocEventDocumentMapper;
import vdt.se.demo.adapter.out.elasticsearch.SocEventIndexDefinition;

@EnableAsync
@SpringBootApplication
@ComponentScan(excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = {
                ElasticsearchDslTimeRangeEditor.class,
                ElasticsearchEventIndexAdapter.class,
                ElasticsearchHttpClient.class,
                ElasticsearchLatestTimestampResolver.class,
                ElasticsearchRelativeTimeQueryExecutionRefiner.class,
                ElasticsearchSearchResponseMapper.class,
                QueryExecutorAdapter.class,
                SocEventDocumentMapper.class,
                SocEventIndexDefinition.class
        }
))
public class DemoApplication {

    public static void main(String[] args) {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
        SpringApplication.run(DemoApplication.class, args);
    }

}
