package com.iamnotawhale.amneziabot.service;

import com.iamnotawhale.amneziabot.config.XrayProperties;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
public class VlessLinkService {

    private final XrayProperties xrayProperties;

    public VlessLinkService(XrayProperties xrayProperties) {
        this.xrayProperties = xrayProperties;
    }

    public String buildLink(String uuid, String label) {
        String safeLabel = URLEncoder.encode(label, StandardCharsets.UTF_8);
        return "vless://" + uuid + "@" + xrayProperties.getHost() + ":" + xrayProperties.getPort()
                + "?type=tcp"
                + "&security=reality"
                + "&sni=" + xrayProperties.getSni()
                + "&fp=" + xrayProperties.getFingerprint()
                + "&pbk=" + xrayProperties.getPublicKey()
                + "&sid=" + xrayProperties.getShortId()
                + "&flow=" + xrayProperties.getFlow()
                + "&encryption=none"
                + "#" + safeLabel;
    }
}
