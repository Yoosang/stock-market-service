package com.usang.stockmarket.infra.kis;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

@Component
@Slf4j
public class KisAuthClient {
    private static final long APPROVAL_KEY_VALID_SECONDS = 24 * 60 * 60;
    private static final long EXPIRY_BUFFER_SECONDS = 60;

    private final KisConfiguration kisConfiguration;
    private final RestClient restClient;
    private final KisTokenCache kisTokenCache;

    public KisAuthClient(KisConfiguration kisConfiguration, RestClient.Builder restClientBuilder, KisTokenCache kisTokenCache) {
        this.kisConfiguration = kisConfiguration;
        this.restClient = restClientBuilder.baseUrl(kisConfiguration.restUri()).build();
        this.kisTokenCache = kisTokenCache;
    }

    public String getNewApprovalKey() {
        Optional<KisTokenCacheData> cached = kisTokenCache.load();
        if (cached.isPresent() && isStillValid(cached.get().approvalKey(), cached.get().approvalKeyExpiresAt())) {
            log.info("Reusing cached KIS approval key.");
            return cached.get().approvalKey();
        }

        String newApprovalKey = fetchApprovalKey();
        saveApprovalKey(cached, newApprovalKey);
        return newApprovalKey;
    }

    private boolean isStillValid(String value, Instant expiresAt) {
        return value != null && expiresAt != null && expiresAt.isAfter(Instant.now().plusSeconds(EXPIRY_BUFFER_SECONDS));
    }

    private String fetchApprovalKey() {
        try {
            return restClient.post()
                    .uri(kisConfiguration.restPath())
                    .header("Content-Type", "application/json")
                    .body(Map.of(
                            "grant_type", "client_credentials",
                            "appkey", kisConfiguration.appKey(),
                            "secretkey", kisConfiguration.appSecret()
                    ))
                    .retrieve()
                    .body(Map.class).get("approval_key").toString();

        }
        catch (RestClientException e) {
            log.error("Failed to obtain kis approval key: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "한국 투자증권 API 호출 중 오류가 발생하였습니다.", e);
        }
    }

    private void saveApprovalKey(Optional<KisTokenCacheData> existing, String newApprovalKey) {
        String accessToken = existing.map(KisTokenCacheData::accessToken).orElse(null);
        Instant accessTokenExpiresAt = existing.map(KisTokenCacheData::accessTokenExpiresAt).orElse(null);

        kisTokenCache.save(new KisTokenCacheData(
                accessToken, accessTokenExpiresAt,
                newApprovalKey, Instant.now().plusSeconds(APPROVAL_KEY_VALID_SECONDS)));
    }

}
