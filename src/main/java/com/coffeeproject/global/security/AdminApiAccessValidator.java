package com.coffeeproject.global.security;

import com.coffeeproject.global.exception.BusinessException;
import com.coffeeproject.global.exception.ErrorCode;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class AdminApiAccessValidator {

    public static final String ADMIN_TOKEN_HEADER = "X-Admin-Token";

    private final AdminApiProperties adminApiProperties;

    public void validate(String token) {
        String configuredToken = adminApiProperties.getToken();
        if (!StringUtils.hasText(configuredToken) || !Objects.equals(configuredToken, token)) {
            throw new BusinessException(ErrorCode.ADMIN_API_FORBIDDEN);
        }
    }
}
