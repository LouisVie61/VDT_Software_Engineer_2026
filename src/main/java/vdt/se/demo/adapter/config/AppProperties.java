package vdt.se.demo.adapter.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;

@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private final User user = new User();
    private final Elasticsearch elasticsearch = new Elasticsearch();
    private final Ingest ingest = new Ingest();
    private final Llm llm = new Llm();
    private final Search search = new Search();

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

    public Search getSearch() {
        return search;
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
        private boolean initializeIndex = true;

        public String getEventsIndex() {
            return eventsIndex;
        }

        public void setEventsIndex(String eventsIndex) {
            this.eventsIndex = eventsIndex;
        }

        public boolean isInitializeIndex() {
            return initializeIndex;
        }

        public void setInitializeIndex(boolean initializeIndex) {
            this.initializeIndex = initializeIndex;
        }
    }

    public static class Ingest {
        private Path spoolRoot = Path.of("data", "ingest");
        private final RateLimit rateLimit = new RateLimit();

        public Path getSpoolRoot() {
            return spoolRoot;
        }

        public void setSpoolRoot(Path spoolRoot) {
            this.spoolRoot = spoolRoot;
        }

        public RateLimit getRateLimit() {
            return rateLimit;
        }
    }

    public static class RateLimit {
        private boolean enabled = true;
        private int maxRequests = 10;
        private int windowSeconds = 60;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getMaxRequests() {
            return maxRequests;
        }

        public void setMaxRequests(int maxRequests) {
            this.maxRequests = maxRequests;
        }

        public int getWindowSeconds() {
            return windowSeconds;
        }

        public void setWindowSeconds(int windowSeconds) {
            this.windowSeconds = windowSeconds;
        }
    }

    public static class Llm {
        private String providerOrder = "GEMINI,GROQ";
        private boolean summaryEnabled = true;
        private int connectTimeoutSeconds = 2;
        private int readTimeoutSeconds = 20;
        private final Gemini gemini = new Gemini();
        private final Groq groq = new Groq();

        public String getProviderOrder() {
            return providerOrder;
        }

        public void setProviderOrder(String providerOrder) {
            this.providerOrder = providerOrder;
        }

        public boolean isSummaryEnabled() {
            return summaryEnabled;
        }

        public void setSummaryEnabled(boolean summaryEnabled) {
            this.summaryEnabled = summaryEnabled;
        }

        public int getConnectTimeoutSeconds() {
            return connectTimeoutSeconds;
        }

        public void setConnectTimeoutSeconds(int connectTimeoutSeconds) {
            this.connectTimeoutSeconds = connectTimeoutSeconds;
        }

        public int getReadTimeoutSeconds() {
            return readTimeoutSeconds;
        }

        public void setReadTimeoutSeconds(int readTimeoutSeconds) {
            this.readTimeoutSeconds = readTimeoutSeconds;
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

    public static class Search {
        private String schemaVersion = "v5";
        private int confirmationTtlSeconds = 900;
        private int cacheTtlSeconds = 3600;

        public String getSchemaVersion() {
            return schemaVersion;
        }

        public void setSchemaVersion(String schemaVersion) {
            this.schemaVersion = schemaVersion;
        }

        public int getConfirmationTtlSeconds() {
            return confirmationTtlSeconds;
        }

        public void setConfirmationTtlSeconds(int confirmationTtlSeconds) {
            this.confirmationTtlSeconds = confirmationTtlSeconds;
        }

        public int getCacheTtlSeconds() {
            return cacheTtlSeconds;
        }

        public void setCacheTtlSeconds(int cacheTtlSeconds) {
            this.cacheTtlSeconds = cacheTtlSeconds;
        }
    }
}
