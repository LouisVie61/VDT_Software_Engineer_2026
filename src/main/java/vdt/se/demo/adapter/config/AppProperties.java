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
        private String mode = "real";
        private String mockFixture = "classpath:llm/mock-search-events.json";
        private String providerOrder = "GPT,GEMINI,GROQ,OPENROUTER";
        private int connectTimeoutSeconds = 2;
        private int readTimeoutSeconds = 20;
        private final Gemini gemini = new Gemini();
        private final Groq groq = new Groq();
        private final Gpt gpt = new Gpt();
        private final OpenRouter openrouter = new OpenRouter();
        private final CircuitBreaker circuitBreaker = new CircuitBreaker();

        public String getMode() { return mode; }
        public void setMode(String mode) { this.mode = mode; }
        public String getMockFixture() { return mockFixture; }
        public void setMockFixture(String mockFixture) { this.mockFixture = mockFixture; }

        public String getProviderOrder() {
            return providerOrder;
        }

        public void setProviderOrder(String providerOrder) {
            this.providerOrder = providerOrder;
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

        public Gpt getGpt() {
            return gpt;
        }

        public OpenRouter getOpenrouter() { return openrouter; }

        public CircuitBreaker getCircuitBreaker() { return circuitBreaker; }
    }

    public static class CircuitBreaker {
        private int threshold = 3;
        private int windowSeconds = 60;
        private int cooldownSeconds = 60;

        public int getThreshold() { return threshold; }
        public void setThreshold(int threshold) { this.threshold = threshold; }
        public int getWindowSeconds() { return windowSeconds; }
        public void setWindowSeconds(int windowSeconds) { this.windowSeconds = windowSeconds; }
        public int getCooldownSeconds() { return cooldownSeconds; }
        public void setCooldownSeconds(int cooldownSeconds) { this.cooldownSeconds = cooldownSeconds; }
    }

    public static class Gemini {
        private String model = "gemini-3.1-flash-lite";
        private String complexModel = "gemini-2.5-flash";
        private int complexQueryLength = 300;
        private String apiKey;

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public String getComplexModel() { return complexModel; }
        public void setComplexModel(String complexModel) { this.complexModel = complexModel; }
        public int getComplexQueryLength() { return complexQueryLength; }
        public void setComplexQueryLength(int complexQueryLength) { this.complexQueryLength = complexQueryLength; }

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
        private String schemaVersion = "v7";
        private int cacheTtlSeconds = 3600;
        private int sessionTtlSeconds = 1800;

        public String getSchemaVersion() {
            return schemaVersion;
        }

        public void setSchemaVersion(String schemaVersion) {
            this.schemaVersion = schemaVersion;
        }

        public int getCacheTtlSeconds() {
            return cacheTtlSeconds;
        }

        public void setCacheTtlSeconds(int cacheTtlSeconds) {
            this.cacheTtlSeconds = cacheTtlSeconds;
        }

        public int getSessionTtlSeconds() { return sessionTtlSeconds; }
        public void setSessionTtlSeconds(int sessionTtlSeconds) { this.sessionTtlSeconds = sessionTtlSeconds; }
    }

    public static class Gpt {
        private String model = "gpt-4.1-mini";
        private String apiKey;
        private String baseUrl = "https://api.openai.com/v1/chat/completions";

        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }
        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    }

    public static class OpenRouter {
        private String model = "openrouter/free";
        private String apiKey;
        private String baseUrl = "https://openrouter.ai/api/v1/chat/completions";
        private Path credentialsFile = Path.of(".claude", "settings.json");

        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }
        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
        public Path getCredentialsFile() { return credentialsFile; }
        public void setCredentialsFile(Path credentialsFile) { this.credentialsFile = credentialsFile; }
    }
}
