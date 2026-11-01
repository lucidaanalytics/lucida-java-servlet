package dev.lucida.servlet;

import dev.lucida.LucidaRequestContext;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Opens a {@link LucidaRequestContext} for the duration of each HTTP request, so {@code capture} calls made while
 * handling it pick up the browser's distinct and session ids (from {@code X-Lucida-Distinct-Id} /
 * {@code X-Lucida-Session-Id}), the URL, method, path, IP and user agent. Those headers are client-controlled:
 * never use them for authentication. Register it once in front of your handlers ({@code FilterRegistrationBean},
 * {@code web.xml} or {@code @WebFilter}). Set {@code trustProxy} only behind a proxy that overwrites
 * {@code X-Forwarded-For}.
 */
public final class LucidaFilter implements Filter {
    private final boolean trustProxy;

    public LucidaFilter() {
        this(false);
    }

    public LucidaFilter(boolean trustProxy) {
        this.trustProxy = trustProxy;
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
        if (!(req instanceof HttpServletRequest r)) {
            chain.doFilter(req, res);
            return;
        }
        try (LucidaRequestContext.Scope scope = LucidaRequestContext.with(context(r))) {
            chain.doFilter(req, res);
        }
    }

    LucidaRequestContext.Data context(HttpServletRequest r) {
        Map<String, Object> headers = new HashMap<>();
        for (String n : Collections.list(r.getHeaderNames())) headers.put(n, r.getHeader(n));
        Map<String, Object> props = new HashMap<>();
        put(props, "$current_url", r.getRequestURL() == null ? null : r.getRequestURL().toString());
        put(props, "$request_method", r.getMethod());
        put(props, "$request_path", r.getRequestURI());
        put(props, "$ip", r.getRemoteAddr());
        return LucidaRequestContext.fromHeaders(headers, trustProxy, props);
    }

    private static void put(Map<String, Object> m, String k, String v) {
        if (v != null) m.put(k, v); // Map.copyOf in the context rejects nulls
    }
}
