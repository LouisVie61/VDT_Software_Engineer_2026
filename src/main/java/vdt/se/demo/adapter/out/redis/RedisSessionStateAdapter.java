package vdt.se.demo.adapter.out.redis;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;
import vdt.se.demo.adapter.config.AppProperties;
import vdt.se.demo.application.port.outboundPort.session.SessionStateStore;
import vdt.se.demo.domain.iql.SessionState;

import java.time.Duration;
import java.util.Optional;

@Component
public final class RedisSessionStateAdapter implements SessionStateStore {
    private final StringRedisTemplate redis; private final ObjectMapper mapper; private final AppProperties properties;
    public RedisSessionStateAdapter(StringRedisTemplate redis, ObjectMapper mapper, AppProperties properties) {
        this.redis=redis; this.mapper=mapper; this.properties=properties;
    }
    @Override public Optional<SessionState> load(String sessionId) {
        try { String value=redis.opsForValue().get(key(sessionId)); return value==null?Optional.empty():Optional.of(mapper.readValue(value, SessionState.class)); }
        catch (RuntimeException ignored) { return Optional.empty(); }
    }
    @Override public void save(String sessionId, SessionState state) {
        try { redis.opsForValue().set(key(sessionId), mapper.writeValueAsString(state), Duration.ofSeconds(properties.getSearch().getCacheTtlSeconds())); }
        catch (RuntimeException ignored) { }
    }
    private String key(String sessionId) { return "iql:session:" + sessionId; }
}
