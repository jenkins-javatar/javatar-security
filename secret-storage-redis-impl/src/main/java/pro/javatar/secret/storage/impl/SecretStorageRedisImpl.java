/*
 * Copyright (c) 2018 Javatar LLC
 * All rights reserved.
 */
package pro.javatar.secret.storage.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.StringUtils;
import pro.javatar.secret.storage.api.SecretStorageService;
import pro.javatar.secret.storage.api.model.SecretTokenDetails;

import java.util.concurrent.TimeUnit;

public class SecretStorageRedisImpl implements SecretStorageService {

    private static final Logger logger = LoggerFactory.getLogger(SecretStorageRedisImpl.class);


    private RedisTemplate<String, String> redisTemplate;

    private ObjectMapper mapper;

    private long keyExpirationHours;

    @Value("${secret.storage.name:pro.javatar.secret.storage}")
    private String storageName;

    public SecretStorageRedisImpl(RedisTemplate<String, String> redisTemplate, long keyExpirationHours) {
        this.redisTemplate = redisTemplate;
        mapper = new ObjectMapper();
        this.keyExpirationHours = keyExpirationHours;
    }

    @Override
    public void put(String secretKey, SecretTokenDetails secretTokenDetails) {
        logger.info("Storing secret details for secretKey `{}`", secretKey);
        try {
            String json = mapper.writeValueAsString(secretTokenDetails);
            String fullSecretKey = getFullSecretKey(secretKey);
            redisTemplate.opsForValue().set(fullSecretKey, json, keyExpirationHours, TimeUnit.HOURS);
        } catch (Exception e) {
            logger.error("Can't write token details as json. Token details is {}", secretTokenDetails, e);
        }
    }

    @Override
    public SecretTokenDetails get(String secretKey) {
        String fullSecretKey = getFullSecretKey(secretKey);
        logger.debug("Trying to get SecretTokenDetails by fullSecretKey `{}`", fullSecretKey);
        String json = redisTemplate.opsForValue().get(fullSecretKey);
        if (StringUtils.isEmpty(json)) {
            logger.info("Token details not found for fullSecretKey: {}", fullSecretKey);
            return null;
        }
        try {
            return mapper.readValue(json, SecretTokenDetails.class);
        } catch (Exception e) {
            logger.error("Can't parse token details {}", json, e);
            return null;
        }
    }

    @Override
    public void delete(String secretKey) {
        String fullSecretKey = getFullSecretKey(secretKey);
        logger.info("removing secretKey: {} from redis, fullSecretKey: {}", secretKey, fullSecretKey);
        redisTemplate.delete(fullSecretKey);
    }

    @Override
    public String getStorageName() {
        return storageName;
    }

    String getFullSecretKey(String secretKey) {
        return getFullSecretKey(getStorageName(), secretKey);
    }

    String getFullSecretKey(String storageName, String secretKey) {
        return storageName + "." + secretKey;
    }

}
