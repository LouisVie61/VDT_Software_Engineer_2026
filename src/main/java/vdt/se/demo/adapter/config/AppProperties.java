package vdt.se.demo.adapter.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private final User user = new User();
    private final Elasticsearch elasticsearch = new Elasticsearch();
    private final Ingest ingest = new Ingest();
    private final Llm llm = new Llm();

    public User getUser() {
        return user;
    }

    public Elasticsearch getElasticsearch() {
        return elasticsearch;
    }

    public Ingest getIngest() {
        return ingest;
    }

    public Llm getLlm() {
        return llm;
    }

    public static class User {
        private String defaultId = "soc-analyst-demo";

        public String getDefaultId() {
            return defaultId;
        }

        public void setDefaultId(String defaultId) {
            this.defaultId = defaultId;
        }
    }

    public static class Elasticsearch {
        private String eventsIndex = "soc-events";

        public String getEventsIndex() {
            return eventsIndex;
        }

        public void setEventsIndex(String eventsIndex) {
            this.eventsIndex = eventsIndex;
        }
    }

    public static class Ingest {
        private int batchSize = 1000;

        public int getBatchSize() {
            return batchSize;
        }

        public void setBatchSize(int batchSize) {
            this.batchSize = batchSize;
        }
    }

    public static class Llm {
        private String providerOrder = "GEMINI,GROQ";
        private final Gemini gemini = new Gemini();
        private final Groq groq = new Groq();

        public String getProviderOrder() {
            return providerOrder;
        }

        public void setProviderOrder(String providerOrder) {
            this.providerOrder = providerOrder;
        }

        public Gemini getGemini() {
            return gemini;
        }

        public Groq getGroq() {
            return groq;
        }
    }

    public static class Gemini {
        private String model = "gemini-2.5-flash";
        private String apiKey;

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }
    }

    public static class Groq {
        private String model = "llama-3.3-70b-versatile";
        private String apiKey;
        private String baseUrl = "https://api.groq.com/openai/v1/chat/completions";

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }
    }
}
