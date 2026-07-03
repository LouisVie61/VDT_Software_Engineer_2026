package vdt.se.demo.adapter.out.redis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;
import vdt.se.demo.adapter.config.AppProperties;
import vdt.se.demo.application.port.outboundPort.cache.DslCachePort;
import vdt.se.demo.application.port.outboundPort.cache.IntentCachePort;
import vdt.se.demo.domain.model.CachedIntent;
import vdt.se.demo.domain.model.PendingConfirmation;
import vdt.se.demo.domain.model.SearchIntent;

import java.time.Duration;
import java.util.Optional;

@Component
public class RedisSearchStateAdapter implements DslCachePort, IntentCachePort {
    private static final Logger log = LoggerFactory.getLogger(RedisSearchStateAdapter.class);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final AppProperties properties;

    public RedisSearchStateAdapter(StringRedisTemplate redisTemplate, ObjectMapper objectMapper, AppProperties properties) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    @Override
    public Optional<String> findFinalizedDsl(String schemaVersion, String sessionId, String queryHash) {
        return get(dslKey(schemaVersion, sessionId, queryHash));
    }

    @Override
    public void saveFinalizedDsl(String schemaVersion, String sessionId, String queryHash, String dsl) {
        set(dslKey(schemaVersion, sessionId, queryHash), dsl, properties.getSearch().getCacheTtlSeconds());
    }

    @Override
    public Optional<CachedIntent> findLastClassifiedIntent(String sessionId) {
        return get(lastIntentKey(sessionId)).flatMap(value -> read(value, CachedIntent.class));
    }

    @Override
    public void saveLastClassifiedIntent(String sessionId, String schemaVersion, SearchIntent intent) {
        write(lastIntentKey(sessionId), CachedIntent.builder()
                .schemaVersion(schemaVersion)
                .intent(intent)
                .build(), properties.getSearch().getCacheTtlSeconds());
    }

    @Override
    public void savePendingConfirmation(PendingConfirmation confirmation) {
        write(pendingKey(confirmation.confirmationId()), confirmation, properties.getSearch().getConfirmationTtlSeconds());
    }

    @Override
    public Optional<PendingConfirmation> findPendingConfirmation(String confirmationId) {
        return get(pendingKey(confirmationId)).flatMap(value -> read(value, PendingConfirmation.class));
    }

    private Optional<String> get(String key) {
        try {
            return Optional.ofNullable(redisTemplate.opsForValue().get(key));
        } catch (RuntimeException e) {
            log.debug("Redis read failed for key {}: {}", key, e.getMessage());
            return Optional.empty();
        }
    }

    private void set(String key, String value, int ttlSeconds) {
        try {
            redisTemplate.opsForValue().set(key, value, Duration.ofSeconds(Math.max(1, ttlSeconds)));
        } catch (RuntimeException e) {
            log.debug("Redis write failed for key {}: {}", key, e.getMessage());
        }
    }

    private void write(String key, Object value, int ttlSeconds) {
        try {
            set(key, objectMapper.writeValueAsString(value), ttlSeconds);
        } catch (Exception e) {
            log.debug("Cannot serialize Redis value for key {}: {}", key, e.getMessage());
        }
    }

    private <T> Optional<T> read(String value, Class<T> type) {
        try {
            return Optional.of(objectMapper.readValue(value, type));
        } catch (Exception e) {
            log.debug("Cannot deserialize Redis value as {}: {}", type.getSimpleName(), e.getMessage());
            return Optional.empty();
        }
    }

    private String dslKey(String schemaVersion, String sessionId, String queryHash) {
        return "dsl:%s:%s:%s".formatted(schemaVersion, sessionId, queryHash);
    }

    private String lastIntentKey(String sessionId) {
        return "last_classified_intent:" + sessionId;
    }

    private String pendingKey(String confirmationId) {
        return "confirmation:" + confirmationId;
    }
}
