package com.dreamfish.backend.config;

import com.dreamfish.backend.common.HeartbeatExpirationListener;
import com.dreamfish.backend.component.DeviceHeartbeatDispose;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * @author Dream fish
 * @version 1.0
 * @description: Redis配置类
 * @date 2025/4/5 18:14
 */
@Configuration
public class RedisConfig {

    @Bean
    RedisTemplate<String, Object> redisTemplate(LettuceConnectionFactory lettuceConnectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        // 设置连接工厂
        template.setConnectionFactory(lettuceConnectionFactory);
        // 创建序列化器
        StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();
        GenericJackson2JsonRedisSerializer jsonRedisSerializer = new GenericJackson2JsonRedisSerializer();
        // 使用 StringRedisSerializer 来序列化和反序列化 key 值
        template.setKeySerializer(stringRedisSerializer);
        //使用 GenericJackson2JsonRedisSerializer 来序列化和反序列化 value 值
        template.setValueSerializer(jsonRedisSerializer);
        // 使用 StringRedisSerializer 来序列化和反序列化 hash key 值
        template.setHashKeySerializer(stringRedisSerializer);
        // 使用 GenericJackson2JsonRedisSerializer 来序列化和反序列化 hash value 值
        template.setHashValueSerializer(jsonRedisSerializer);
        // 初始化 RedisTemplate
        template.afterPropertiesSet();
        return template;
    }

    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(RedisConnectionFactory connectionFactory) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        return container;
    }

    @Bean
    public HeartbeatExpirationListener heartbeatExpirationListener(
            RedisMessageListenerContainer container,
            DeviceHeartbeatDispose deviceHeartbeatDispose
    ) {

        HeartbeatExpirationListener listener = new HeartbeatExpirationListener(container, deviceHeartbeatDispose);
        container.addMessageListener(listener, new ChannelTopic("__keyevent@0__:expired"));

        return listener;
    }
}