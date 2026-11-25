package com.javaweb.config;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.javaweb.model.response.BuildingSearchResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer;
import org.springframework.cache.Cache;
import org.springframework.cache.annotation.CachingConfigurerSupport;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.List;

@Configuration
@EnableCaching
public class RedisConfig extends CachingConfigurerSupport {

    public static final String BUILDINGS_CACHE = "buildings";
    private static final Logger log = LoggerFactory.getLogger(RedisConfig.class);

    /** Cấu hình mặc định: TTL 10 phút, key là chuỗi, không cache giá trị null. */
    @Bean
    public RedisCacheConfiguration redisCacheConfiguration() {
        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10))
                .disableCachingNullValues()
                .serializeKeysWith(SerializationPair.fromSerializer(new StringRedisSerializer()));
    }

    /** Cache "buildings": value lưu dạng JSON của List<BuildingSearchResponse> (đọc được bằng redis-cli). */
    @Bean
    public RedisCacheManagerBuilderCustomizer buildingsCacheCustomizer(RedisCacheConfiguration defaultConfig) {
        ObjectMapper mapper = new ObjectMapper();
        JavaType listType = mapper.getTypeFactory()
                .constructCollectionType(List.class, BuildingSearchResponse.class);
        Jackson2JsonRedisSerializer<Object> jsonSerializer = new Jackson2JsonRedisSerializer<>(listType);
        jsonSerializer.setObjectMapper(mapper);

        return builder -> builder.withCacheConfiguration(BUILDINGS_CACHE,
                defaultConfig.serializeValuesWith(SerializationPair.fromSerializer(jsonSerializer)));
    }

    /** Redis chết -> chỉ log cảnh báo; @Cacheable coi như "miss" và vẫn chạy truy vấn MySQL. */
    @Override
    public CacheErrorHandler errorHandler() {
        return new CacheErrorHandler() {
            @Override
            public void handleCacheGetError(RuntimeException e, Cache cache, Object key) {
                log.warn("Redis GET lỗi ({}), chuyển sang đọc MySQL", e.getMessage());
            }

            @Override
            public void handleCachePutError(RuntimeException e, Cache cache, Object key, Object value) {
                log.warn("Redis PUT lỗi: {}", e.getMessage());
            }

            @Override
            public void handleCacheEvictError(RuntimeException e, Cache cache, Object key) {
                log.warn("Redis EVICT lỗi: {}", e.getMessage());
            }

            @Override
            public void handleCacheClearError(RuntimeException e, Cache cache) {
                log.warn("Redis CLEAR lỗi: {}", e.getMessage());
            }
        };
    }
}
