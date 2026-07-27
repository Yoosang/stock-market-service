package com.usang.stockmarket.infra.kis;

import java.time.Instant;

public record KisTokenCacheData(
        String accessToken,
        Instant accessTokenExpiresAt,
        String approvalKey,
        Instant approvalKeyExpiresAt
) {
}
