package com.envoil.admin.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {

    /** HS256 密钥（utf-8 字节长度建议 ≥ 32） */
    private String secret = "change-me-in-production-min-32-chars";

    private int expireMinutes = 10080;

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public int getExpireMinutes() {
        return expireMinutes;
    }

    public void setExpireMinutes(int expireMinutes) {
        this.expireMinutes = expireMinutes;
    }
}
