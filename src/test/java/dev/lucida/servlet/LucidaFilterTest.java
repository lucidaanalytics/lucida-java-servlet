package dev.lucida.servlet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import dev.lucida.LucidaRequestContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.Map;
import org.junit.jupiter.api.Test;

class LucidaFilterTest {
    /** A request answering the few methods the filter reads; everything else is unsupported. */
    private static HttpServletRequest request(Map<String, String> headers) {
        return (HttpServletRequest) Proxy.newProxyInstance(
                LucidaFilterTest.class.getClassLoader(), new Class<?>[] {HttpServletRequest.class}, (p, m, a) -> switch (m.getName()) {
                    case "getHeaderNames" -> Collections.enumeration(headers.keySet());
                    case "getHeader" -> headers.get((String) a[0]);
                    case "getRequestURL" -> new StringBuffer("https://app.example/checkout");
                    case "getMethod" -> "POST";
                    case "getRequestURI" -> "/checkout";
                    case "getRemoteAddr" -> "10.0.0.7";
                    default -> throw new UnsupportedOperationException(m.getName());
                });
    }

    @Test
    void contextIsOpenInsideTheChainAndRestoredAfter() throws Exception {
        LucidaRequestContext.Data[] seen = new LucidaRequestContext.Data[1];
        FilterChain chain = (rq, rs) -> seen[0] = LucidaRequestContext.current();
        Map<String, String> h = Map.of("X-Lucida-Distinct-Id", "u1", "x-lucida-session-id", "s1", "User-Agent", "UA/1");
        new LucidaFilter().doFilter(request(h), null, chain);
        assertEquals("u1", seen[0].distinctId());
        assertEquals("s1", seen[0].sessionId());
        assertEquals("/checkout", seen[0].props().get("$request_path"));
        assertEquals("UA/1", seen[0].props().get("$user_agent"));
        assertEquals("10.0.0.7", seen[0].props().get("$ip"));
        assertNull(LucidaRequestContext.current());
    }

    @Test
    void forwardedForIsHonouredOnlyWhenTrusted() throws Exception {
        Map<String, String> h = Map.of("X-Forwarded-For", "203.0.113.9, 10.0.0.1");
        assertEquals("10.0.0.7", new LucidaFilter(false).context(request(h)).props().get("$ip"));
        assertEquals("203.0.113.9", new LucidaFilter(true).context(request(h)).props().get("$ip"));
    }
}
