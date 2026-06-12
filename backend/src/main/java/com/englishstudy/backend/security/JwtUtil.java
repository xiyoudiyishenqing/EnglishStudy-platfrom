package com.englishstudy.backend.security;

import com.englishstudy.backend.common.BusinessException;
import com.englishstudy.backend.context.CurrentUser;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtUtil {

    private static final String HEADER = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";

    @Value("${app.jwt-secret}")
    private String secret;

    @Value("${app.jwt-expire-minutes}")
    private Long expireMinutes;

    public String generate(CurrentUser user) {
        long expireAt = expireAtMillis();
        String payload = "{"
                + "\"userId\":" + user.getUserId() + ","
                + "\"username\":\"" + escape(user.getUsername()) + "\","
                + "\"realName\":\"" + escape(user.getRealName()) + "\","
                + "\"role\":\"" + escape(user.getRole()) + "\","
                + "\"className\":\"" + escape(user.getClassName()) + "\","
                + "\"exp\":" + expireAt
                + "}";
        String headerPart = base64Url(HEADER.getBytes(StandardCharsets.UTF_8));
        String payloadPart = base64Url(payload.getBytes(StandardCharsets.UTF_8));
        return headerPart + "." + payloadPart + "." + sign(headerPart + "." + payloadPart);
    }

    public CurrentUser parse(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                return null;
            }
            String unsigned = parts[0] + "." + parts[1];
            if (!sign(unsigned).equals(parts[2])) {
                return null;
            }
            String payload = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
            long exp = Long.parseLong(readJsonValue(payload, "exp"));
            if (System.currentTimeMillis() > exp) {
                return null;
            }
            return new CurrentUser(
                    Long.parseLong(readJsonValue(payload, "userId")),
                    readJsonValue(payload, "username"),
                    readJsonValue(payload, "realName"),
                    readJsonValue(payload, "role"),
                    readJsonValue(payload, "className")
            );
        } catch (Exception exception) {
            return null;
        }
    }

    public long expireAtMillis() {
        return Instant.now().plusSeconds(expireMinutes * 60).toEpochMilli();
    }

    private String sign(String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return base64Url(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new BusinessException("JWT签名失败");
        }
    }

    private String base64Url(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String readJsonValue(String json, String key) {
        String keyExpr = "\"" + key + "\":";
        int start = json.indexOf(keyExpr);
        if (start < 0) {
            return "";
        }
        int valueStart = start + keyExpr.length();
        if (json.charAt(valueStart) == '"') {
            int end = json.indexOf('"', valueStart + 1);
            return unescape(json.substring(valueStart + 1, end));
        }
        int comma = json.indexOf(',', valueStart);
        int brace = json.indexOf('}', valueStart);
        int end = comma < 0 ? brace : Math.min(comma, brace);
        return json.substring(valueStart, end).trim();
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private String unescape(String value) {
        return value.replace("\\\"", "\"").replace("\\\\", "\\");
    }
}
