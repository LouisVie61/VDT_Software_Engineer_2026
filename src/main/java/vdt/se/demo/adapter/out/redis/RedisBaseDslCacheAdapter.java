package vdt.se.demo.adapter.out.redis;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import vdt.se.demo.adapter.config.AppProperties;
import vdt.se.demo.application.port.outboundPort.cache.BaseDslCachePort;
import java.time.Duration;
import java.util.Optional;

@Component
public final class RedisBaseDslCacheAdapter implements BaseDslCachePort {
    private final StringRedisTemplate redis; private final ObjectMapper mapper; private final AppProperties properties;
    public RedisBaseDslCacheAdapter(StringRedisTemplate redis,ObjectMapper mapper,AppProperties properties){this.redis=redis;this.mapper=mapper;this.properties=properties;}
    public Optional<JsonNode> findBaseDsl(String key){try{String value=redis.opsForValue().get(redisKey(key));return value==null?Optional.empty():Optional.of(mapper.readTree(value));}catch(RuntimeException e){return Optional.empty();}}
    public void saveBaseDsl(String key,JsonNode dsl){try{redis.opsForValue().set(redisKey(key),mapper.writeValueAsString(dsl),Duration.ofSeconds(properties.getSearch().getCacheTtlSeconds()));}catch(RuntimeException ignored){}}
    private String redisKey(String key){return "iql:dsl:"+key;}
}
