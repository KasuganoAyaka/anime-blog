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
 * 为公开接口下发缓存策略。
 * 首页列表、标签、分类、友链等允许短缓存,文章详情和评论仍禁缓存以保持访问量与互动数据实时。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class PublicApiCacheControlFilter extends OncePerRequestFilter {

    private static final List<Pattern> NO_STORE_PATH_PATTERNS = List.of(
            Pattern.compile("^/api/posts/\\d+/?$"),
            Pattern.compile("^/api/posts/\\d+/comments/?$")
    );

    private static final List<Pattern> SHORT_CACHE_PATH_PATTERNS = List.of(
            Pattern.compile("^/api/posts/?$"),
            Pattern.compile("^/api/posts/tags/?$"),
            Pattern.compile("^/api/posts/categories/?$"),
            Pattern.compile("^/api/posts/search-index/?$"),
            Pattern.compile("^/api/public/stats/?$"),
            Pattern.compile("^/api/public/friend-links/?$"),
            Pattern.compile("^/api/public/music/list/?$"),
            Pattern.compile("^/api/friend-links/?$"),
            Pattern.compile("^/api/music/?$")
    );

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        boolean cacheableGet = "GET".equalsIgnoreCase(request.getMethod());
        boolean disableCache = cacheableGet && matchesAny(request, NO_STORE_PATH_PATTERNS);
        boolean shortCache = cacheableGet && !disableCache && matchesAny(request, SHORT_CACHE_PATH_PATTERNS);
        if (disableCache) {
            applyNoStoreHeaders(response);
        } else if (shortCache) {
            applyShortCacheHeaders(response);
        }

        filterChain.doFilter(request, response);

        if (disableCache) {
            applyNoStoreHeaders(response);
        } else if (shortCache) {
            applyShortCacheHeaders(response);
        }
    }

    private boolean matchesAny(HttpServletRequest request, List<Pattern> patterns) {
        String path = normalizePath(request.getRequestURI());
        if (path == null || path.isBlank()) {
            return false;
        }

        for (Pattern pattern : patterns) {
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

    private void applyShortCacheHeaders(HttpServletResponse response) {
        response.setHeader("Cache-Control", "public, max-age=60, s-maxage=300, stale-while-revalidate=600");
        response.setHeader("CDN-Cache-Control", "public, max-age=300, stale-while-revalidate=600");
        response.setHeader("Cloudflare-CDN-Cache-Control", "public, max-age=300, stale-while-revalidate=600");
    }
}
