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
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 为公开文章接口显式下发禁缓存响应头,避免浏览器或 CDN 继续复用旧数据。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class PublicApiCacheControlFilter extends OncePerRequestFilter {

    private static final Set<String> EXACT_PATHS = Set.of(
            "/api/posts",
            "/api/posts/tags",
            "/api/posts/categories",
            "/api/posts/search-index",
            "/api/public/stats"
    );

    private static final Pattern POST_DETAIL_PATH = Pattern.compile("^/api/posts/\\d+$");
    private static final Pattern POST_COMMENTS_PATH = Pattern.compile("^/api/posts/\\d+/comments$");

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        if ("GET".equalsIgnoreCase(request.getMethod()) && shouldDisableCache(request)) {
            applyNoStoreHeaders(response);
        }

        filterChain.doFilter(request, response);
    }

    private boolean shouldDisableCache(HttpServletRequest request) {
        String path = request.getRequestURI();
        if (path == null || path.isBlank()) {
            return false;
        }

        if (EXACT_PATHS.contains(path)) {
            return true;
        }

        return POST_DETAIL_PATH.matcher(path).matches()
                || POST_COMMENTS_PATH.matcher(path).matches();
    }

    private void applyNoStoreHeaders(HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0, s-maxage=0");
        response.setHeader("Pragma", "no-cache");
        response.setDateHeader("Expires", 0);
        response.setHeader("Surrogate-Control", "no-store");
        response.setHeader("CDN-Cache-Control", "no-store");
    }
}
