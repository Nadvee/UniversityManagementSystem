package com.mirpur.advising.shared.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.*;
import org.springframework.cache.*;
import org.springframework.cache.annotation.*;
import org.springframework.cache.support.NoOpCacheManager;
import org.springframework.context.annotation.*;
import org.springframework.data.redis.cache.*;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.*;

@Configuration
@EnableCaching public class CacheConfig implements CachingConfigurer {
    private final RedisConnectionFactory factory;
    private final ObjectMapper mapper;
    private final boolean enabled;
    private final int listMinutes, routineMinutes;
    private final String namespace;
    public CacheConfig(RedisConnectionFactory f, ObjectMapper m, RuntimeSettings settings) {
        this.namespace = settings.app().namespace();
        factory = f;
        mapper = m;
        enabled = settings.app().cacheEnabled();
        listMinutes = settings.app().cacheListMinutes();
        routineMinutes = settings.app().cacheRoutineMinutes();
    }
    public RedisCacheConfiguration settings() {
        var json = mapper.copy();
        var validator = com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator.builder().allowIfSubType("java.util.").allowIfSubType("java.lang.").allowIfSubType("java.math.").allowIfSubType("java.time.").build();
        json.activateDefaultTyping(validator, ObjectMapper.DefaultTyping.NON_FINAL);
        return RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofMinutes(listMinutes)).disableCachingNullValues().computePrefixWith(n -> namespace + ":cache:" + n + "::").serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer(json)));
    }
    @Bean public CacheManager cacheManager() {
        if ( !enabled) return new NoOpCacheManager();
        var config = settings();
        var writer = RedisCacheWriter.lockingRedisCacheWriter(factory, Duration.ofMillis(10),(key, value) -> Duration.ofSeconds(10), BatchStrategies.scan(1000));
        var redis = RedisCacheManager.builder(writer).cacheDefaults(config).withInitialCacheConfigurations(Map.of("routine", config.entryTtl(Duration.ofMinutes(routineMinutes)), "advisingCourses", config)).build();
        return new RecoveringCacheManager(redis);
    }
}
