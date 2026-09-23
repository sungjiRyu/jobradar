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
            @Value("${spring.data.redis.ssl.enabled:false}") boolean sslEnabled,
            @Value("${spring.data.redis.password:}") String password,
            @Value("${spring.data.redis.username:}") String username,
            @Value("${app.redisson.threads:16}") int threads,
            @Value("${app.redisson.netty-threads:32}") int nettyThreads) {

        Config config = new Config();
        config.setThreads(threads);
        config.setNettyThreads(nettyThreads);
        String protocol = sslEnabled ? "rediss://" : "redis://";
        var serverConfig = config.useSingleServer()
                .setAddress(protocol + host + ":" + port);

        if (username != null && !username.isBlank()) {
            serverConfig.setUsername(username);
        }

        if (password != null && !password.isBlank()) {
            serverConfig.setPassword(password);
        }

        return Redisson.create(config);
    }
}
