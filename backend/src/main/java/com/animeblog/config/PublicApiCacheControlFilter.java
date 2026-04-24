package com.animeblog.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 为公开文章接口显式下发禁缓存响应头,避免浏览器或 CDN 继续复用旧数据。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class PublicApiCacheControlFilter extends OncePerRequestFilter {

    private static final List<Pattern> NO_STORE_PATH_PATTERNS = List.of(
            Pattern.compile("^/api/posts(?:/.*)?$"),
            Pattern.compile("^/api/public(?:/.*)?$"),
            Pattern.compile("^/api/friend-links/?$"),
            Pattern.compile("^/api/music/?$")
    );

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        boolean disableCache = "GET".equalsIgnoreCase(request.getMethod()) && shouldDisableCache(request);
        if (disableCache) {
            applyNoStoreHeaders(response);
        }

        filterChain.doFilter(request, response);

        if (disableCache) {
            applyNoStoreHeaders(response);
        }
    }

    private boolean shouldDisableCache(HttpServletRequest request) {
        String path = normalizePath(request.getRequestURI());
        if (path == null || path.isBlank()) {
            return false;
        }

        for (Pattern pattern : NO_STORE_PATH_PATTERNS) {
            if (pattern.matcher(path).matches()) {
                return true;
            }
        }
        return false;
    }

    private String normalizePath(String path) {
        if (path == null) {
            return null;
        }
        if (path.length() > 1 && path.endsWith("/")) {
            return path.substring(0, path.length() - 1);
        }
        return path;
    }

    private void applyNoStoreHeaders(HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0, s-maxage=0");
        response.setHeader("Pragma", "no-cache");
        response.setDateHeader("Expires", 0);
        response.setHeader("Surrogate-Control", "no-store");
        response.setHeader("CDN-Cache-Control", "no-store");
        response.setHeader("Cloudflare-CDN-Cache-Control", "no-store");
    }
}
