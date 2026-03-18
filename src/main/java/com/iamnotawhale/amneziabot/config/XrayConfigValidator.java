package com.iamnotawhale.amneziabot.config;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

@Component
public class XrayConfigValidator {

    private final XrayProperties xrayProperties;

    public XrayConfigValidator(XrayProperties xrayProperties) {
        this.xrayProperties = xrayProperties;
    }

    @PostConstruct
    public void validate() {
        requireRealValue("XRAY_HOST", xrayProperties.getHost());
        requireRealValue("XRAY_PUBLIC_KEY", xrayProperties.getPublicKey());
        requireRealValue("XRAY_SNI", xrayProperties.getSni());
        requireRealValue("XRAY_SHORT_ID", xrayProperties.getShortId());
    }

    private void requireRealValue(String key, String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(key + " is empty in deploy/.env");
        }
        String normalized = value.trim();
        if (normalized.contains("YOUR_") || normalized.contains("PUT_YOUR") || normalized.contains("CHANGE_ME")) {
            throw new IllegalStateException(key + " still contains placeholder value: " + normalized);
        }
    }
}
