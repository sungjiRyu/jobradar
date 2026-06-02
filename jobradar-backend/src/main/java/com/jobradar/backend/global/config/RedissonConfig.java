package com.jobradar.backend.global.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedissonConfig {

    @Bean(destroyMethod = "shutdown")
    public RedissonClient redissonClient(
            @Value("${spring.data.redis.host}") String host,
            @Value("${spring.data.redis.port}") int port,
            @Value("${spring.data.redis.password:}") String password) {

        Config config = new Config();
        var serverConfig = config.useSingleServer()
                .setAddress("redis://" + host + ":" + port);

        if (password != null && !password.isBlank()) {
            serverConfig.setPassword(password);
        }

        return Redisson.create(config);
    }
}
