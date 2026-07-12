package com.example.praxis.apiquickstart.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Centraliza a fronteira de confianca para headers adicionados por proxy.
 *
 * <p>Headers como {@code X-Forwarded-For}, {@code X-Forwarded-Host} e
 * {@code X-Forwarded-Proto} sao triviais de forjar quando a aplicacao e acessada diretamente.
 * O quickstart so os usa quando o par imediato da conexao esta em uma lista explicita de proxies
 * confiaveis. A mesma decisao governa origem de config e identidade de rate limit.</p>
 */
@Component
public class TrustedProxyPolicy {
    private final boolean enabled;
    private final Set<String> trustedProxyAddresses;

    public TrustedProxyPolicy(
            @Value("${app.security.trusted-proxy.enabled:false}") boolean enabled,
            @Value("${app.security.trusted-proxy.addresses:}") String trustedProxyAddresses
    ) {
        this.enabled = enabled;
        this.trustedProxyAddresses = Arrays.stream(trustedProxyAddresses.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .collect(Collectors.toUnmodifiableSet());
    }

    public boolean isTrustedProxy(HttpServletRequest request) {
        return request != null && isTrustedProxyAddress(request.getRemoteAddr());
    }

    public boolean isTrustedProxyAddress(String remoteAddress) {
        if (!enabled || !StringUtils.hasText(remoteAddress) || trustedProxyAddresses.isEmpty()) {
            return false;
        }
        return trustedProxyAddresses.stream().anyMatch(rule -> matches(remoteAddress, rule));
    }

    public String resolveClientAddress(HttpServletRequest request) {
        if (isTrustedProxy(request)) {
            String forwardedFor = request.getHeader("X-Forwarded-For");
            if (StringUtils.hasText(forwardedFor)) {
                String first = forwardedFor.split(",")[0].trim();
                if (StringUtils.hasText(first)) {
                    return first;
                }
            }
        }
        return request.getRemoteAddr();
    }

    private boolean matches(String remoteAddress, String rule) {
        if (remoteAddress.equals(rule)) {
            return true;
        }
        int slash = rule.indexOf('/');
        if (slash < 0) {
            return false;
        }
        try {
            InetAddress remote = InetAddress.getByName(remoteAddress);
            InetAddress network = InetAddress.getByName(rule.substring(0, slash));
            int prefixLength = Integer.parseInt(rule.substring(slash + 1));
            return matchesCidr(remote.getAddress(), network.getAddress(), prefixLength);
        } catch (RuntimeException | UnknownHostException ignored) {
            return false;
        }
    }

    private boolean matchesCidr(byte[] remote, byte[] network, int prefixLength) {
        if (remote.length != network.length || prefixLength < 0 || prefixLength > remote.length * 8) {
            return false;
        }
        int fullBytes = prefixLength / 8;
        int remainingBits = prefixLength % 8;
        for (int i = 0; i < fullBytes; i++) {
            if (remote[i] != network[i]) {
                return false;
            }
        }
        if (remainingBits == 0) {
            return true;
        }
        int mask = 0xFF << (8 - remainingBits);
        return ((remote[fullBytes] & 0xFF) & mask) == ((network[fullBytes] & 0xFF) & mask);
    }
}
