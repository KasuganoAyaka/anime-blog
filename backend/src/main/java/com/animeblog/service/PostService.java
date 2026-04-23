package com.animeblog.service;

import com.animeblog.dto.PublicCategorySummaryItem;
import com.animeblog.dto.PublicPostListItem;
import com.animeblog.dto.PublicPostSearchIndexItem;
import com.animeblog.dto.PublicTagSummaryItem;
import com.animeblog.entity.Post;
import com.animeblog.mapper.PostMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 文章服务
 */
@Service
public class PostService {

    private static final long VIEW_DEDUP_WINDOW_MILLIS = TimeUnit.MINUTES.toMillis(30);
    private static final long VIEW_CACHE_CLEANUP_INTERVAL_MILLIS = TimeUnit.MINUTES.toMillis(10);
    private static final long PUBLIC_METADATA_CACHE_TTL_MILLIS = TimeUnit.MINUTES.toMillis(15);
    private static final int MAX_PUBLIC_PAGE_SIZE = 24;

    @Autowired
    private PostMapper postMapper;

    private final ConcurrentHashMap<String, Long> recentPostViews = new ConcurrentHashMap<>();
    private final AtomicLong lastViewCacheCleanupAt = new AtomicLong(System.currentTimeMillis());
    private final Object publicMetadataCacheLock = new Object();
    private volatile List<PublicTagSummaryItem> cachedTagSummaries;
    private volatile long cachedTagSummariesAt;
    private volatile List<PublicCategorySummaryItem> cachedCategorySummaries;
    private volatile long cachedCategorySummariesAt;
    private volatile List<PublicPostSearchIndexItem> cachedSearchIndexItems;
    private volatile long cachedSearchIndexItemsAt;

    public List<Post> getPublishedPosts() {
        QueryWrapper<Post> wrapper = new QueryWrapper<>();
        wrapper.eq("status", "published").orderByDesc("create_time");
        return postMapper.selectList(wrapper);
    }

    public Page<PublicPostListItem> getPublishedPostListPage(int page, int size, String category, String tag) {
        long current = Math.max(1, page);
        long pageSize = Math.min(Math.max(1, size), MAX_PUBLIC_PAGE_SIZE);

        QueryWrapper<Post> wrapper = buildPublicPostListWrapper(category, tag);
        Page<Post> sourcePage = postMapper.selectPage(new Page<>(current, pageSize), wrapper);

        Page<PublicPostListItem> resultPage = new Page<>(sourcePage.getCurrent(), sourcePage.getSize(), sourcePage.getTotal());
        resultPage.setRecords(sourcePage.getRecords().stream()
                .map(this::toPublicPostListItem)
                .toList());
        return resultPage;
    }

    public List<PublicTagSummaryItem> getPublishedTagSummaries() {
        List<PublicTagSummaryItem> cached = cachedTagSummaries;
        if (isPublicMetadataCacheValid(cached, cachedTagSummariesAt)) {
            return cached;
        }

        synchronized (publicMetadataCacheLock) {
            cached = cachedTagSummaries;
            if (isPublicMetadataCacheValid(cached, cachedTagSummariesAt)) {
                return cached;
            }

            List<PublicTagSummaryItem> items = loadPublishedTagSummaries();
            cachedTagSummaries = items;
            cachedTagSummariesAt = System.currentTimeMillis();
            return items;
        }
    }

    public List<PublicCategorySummaryItem> getPublishedCategorySummaries() {
        List<PublicCategorySummaryItem> cached = cachedCategorySummaries;
        if (isPublicMetadataCacheValid(cached, cachedCategorySummariesAt)) {
            return cached;
        }

        synchronized (publicMetadataCacheLock) {
            cached = cachedCategorySummaries;
            if (isPublicMetadataCacheValid(cached, cachedCategorySummariesAt)) {
                return cached;
            }

            List<PublicCategorySummaryItem> items = loadPublishedCategorySummaries();
            cachedCategorySummaries = items;
            cachedCategorySummariesAt = System.currentTimeMillis();
            return items;
        }
    }

    public List<PublicPostSearchIndexItem> getPublishedPostSearchIndexItems() {
        List<PublicPostSearchIndexItem> cached = cachedSearchIndexItems;
        if (isPublicMetadataCacheValid(cached, cachedSearchIndexItemsAt)) {
            return cached;
        }

        synchronized (publicMetadataCacheLock) {
            cached = cachedSearchIndexItems;
            if (isPublicMetadataCacheValid(cached, cachedSearchIndexItemsAt)) {
                return cached;
            }

            List<PublicPostSearchIndexItem> items = loadPublishedPostSearchIndexItems();
            cachedSearchIndexItems = items;
            cachedSearchIndexItemsAt = System.currentTimeMillis();
            return items;
        }
    }

    public void invalidatePublicCaches() {
        cachedTagSummaries = null;
        cachedTagSummariesAt = 0L;
        cachedCategorySummaries = null;
        cachedCategorySummariesAt = 0L;
        cachedSearchIndexItems = null;
        cachedSearchIndexItemsAt = 0L;
    }

    public Post getPostById(Long id) {
        return postMapper.selectById(id);
    }

    public Post getPublishedPostById(Long id) {
        QueryWrapper<Post> wrapper = new QueryWrapper<>();
        wrapper.eq("id", id).eq("status", "published");
        return postMapper.selectOne(wrapper);
    }

    public Post getPublishedPostByIdAndIncreaseView(Long id, String visitorKey) {
        Post post = getPublishedPostById(id);
        if (post == null) {
            return null;
        }

        if (shouldIncreaseView(id, visitorKey)) {
            long currentViewCount = post.getViewCount() == null ? 0L : post.getViewCount();
            post.setViewCount(currentViewCount + 1);
            postMapper.updateById(post);
        }
        return post;
    }

    private QueryWrapper<Post> buildPublicPostListWrapper(String category, String tag) {
        QueryWrapper<Post> wrapper = new QueryWrapper<>();
        wrapper.select(
                        "id",
                        "title",
                        "slug",
                        "summary",
                        "excerpt",
                        "category",
                        "tags",
                        "cover_image",
                        "user_id",
                        "status",
                        "view_count",
                        "like_count",
                        "comment_count",
                        "is_featured",
                        "create_time",
                        "update_time"
                )
                .eq("status", "published");

        if (StringUtils.hasText(category)) {
            wrapper.eq("category", category.trim());
        }
        if (StringUtils.hasText(tag)) {
            wrapper.apply(
                    "REPLACE(COALESCE(tags, ''), '，', ',') REGEXP {0}",
                    buildTagMatchRegex(tag)
            );
        }

        wrapper.orderByDesc("create_time");
        return wrapper;
    }

    private boolean shouldIncreaseView(Long postId, String visitorKey) {
        if (!StringUtils.hasText(visitorKey)) {
            return true;
        }

        long now = System.currentTimeMillis();
        cleanupExpiredViewCache(now);

        String cacheKey = postId + ":" + visitorKey;
        AtomicBoolean shouldIncrease = new AtomicBoolean(false);
        recentPostViews.compute(cacheKey, (key, lastViewedAt) -> {
            if (lastViewedAt == null || now - lastViewedAt >= VIEW_DEDUP_WINDOW_MILLIS) {
                shouldIncrease.set(true);
                return now;
            }
            return lastViewedAt;
        });
        return shouldIncrease.get();
    }

    private void cleanupExpiredViewCache(long now) {
        long lastCleanupAt = lastViewCacheCleanupAt.get();
        if (now - lastCleanupAt < VIEW_CACHE_CLEANUP_INTERVAL_MILLIS) {
            return;
        }
        if (!lastViewCacheCleanupAt.compareAndSet(lastCleanupAt, now)) {
            return;
        }
        recentPostViews.entrySet().removeIf(entry -> now - entry.getValue() >= VIEW_DEDUP_WINDOW_MILLIS);
    }

    private boolean isPublicMetadataCacheValid(List<?> cached, long cachedAt) {
        return cached != null && (System.currentTimeMillis() - cachedAt) < PUBLIC_METADATA_CACHE_TTL_MILLIS;
    }

    private List<PublicTagSummaryItem> loadPublishedTagSummaries() {
        QueryWrapper<Post> wrapper = new QueryWrapper<>();
        wrapper.select("tags")
                .eq("status", "published")
                .orderByDesc("create_time");

        Map<String, Long> tagCountMap = new LinkedHashMap<>();
        for (Post post : postMapper.selectList(wrapper)) {
            for (String tag : parseTags(post.getTags())) {
                tagCountMap.merge(tag, 1L, Long::sum);
            }
        }

        return tagCountMap.entrySet().stream()
                .map(entry -> {
                    PublicTagSummaryItem item = new PublicTagSummaryItem();
                    item.setName(entry.getKey());
                    item.setCount(entry.getValue());
                    return item;
                })
                .sorted((left, right) -> {
                    int countCompare = Long.compare(right.getCount(), left.getCount());
                    if (countCompare != 0) {
                        return countCompare;
                    }
                    return left.getName().compareToIgnoreCase(right.getName());
                })
                .toList();
    }

    private List<PublicCategorySummaryItem> loadPublishedCategorySummaries() {
        QueryWrapper<Post> wrapper = new QueryWrapper<>();
        wrapper.select("category")
                .eq("status", "published")
                .orderByDesc("create_time");

        Map<String, Long> categoryCountMap = new LinkedHashMap<>();
        for (Post post : postMapper.selectList(wrapper)) {
            if (!StringUtils.hasText(post.getCategory())) {
                continue;
            }
            String category = post.getCategory().trim();
            categoryCountMap.merge(category, 1L, Long::sum);
        }

        return categoryCountMap.entrySet().stream()
                .map(entry -> {
                    PublicCategorySummaryItem item = new PublicCategorySummaryItem();
                    item.setName(entry.getKey());
                    item.setCount(entry.getValue());
                    return item;
                })
                .sorted((left, right) -> {
                    int countCompare = Long.compare(right.getCount(), left.getCount());
                    if (countCompare != 0) {
                        return countCompare;
                    }
                    return left.getName().compareToIgnoreCase(right.getName());
                })
                .toList();
    }

    private List<PublicPostSearchIndexItem> loadPublishedPostSearchIndexItems() {
        QueryWrapper<Post> wrapper = new QueryWrapper<>();
        wrapper.select(
                        "id",
                        "title",
                        "category",
                        "summary",
                        "excerpt",
                        "content",
                        "create_time"
                )
                .eq("status", "published")
                .orderByDesc("create_time");
        return postMapper.selectList(wrapper).stream()
                .map(this::toPublicPostSearchIndexItem)
                .toList();
    }

    private List<String> parseTags(String tagsValue) {
        if (!StringUtils.hasText(tagsValue)) {
            return List.of();
        }

        List<String> tags = new ArrayList<>();
        for (String segment : tagsValue.split("[,，]")) {
            if (!StringUtils.hasText(segment)) {
                continue;
            }
            tags.add(segment.trim());
        }
        return tags;
    }

    private String buildTagMatchRegex(String tag) {
        return "(^|,)[[:space:]]*" + escapeMysqlRegexLiteral(tag.trim()) + "[[:space:]]*(,|$)";
    }

    private String escapeMysqlRegexLiteral(String value) {
        StringBuilder builder = new StringBuilder(value.length() * 2);
        for (char current : value.toCharArray()) {
            if ("\\.^$|?*+()[]{}".indexOf(current) >= 0) {
                builder.append('\\');
            }
            builder.append(current);
        }
        return builder.toString();
    }

    private PublicPostListItem toPublicPostListItem(Post post) {
        PublicPostListItem item = new PublicPostListItem();
        item.setId(post.getId());
        item.setTitle(post.getTitle());
        item.setSlug(post.getSlug());
        item.setSummary(post.getSummary());
        item.setExcerpt(post.getExcerpt());
        item.setCategory(post.getCategory());
        item.setTags(post.getTags());
        item.setCoverImage(post.getCoverImage());
        item.setUserId(post.getUserId());
        item.setStatus(post.getStatus());
        item.setViewCount(post.getViewCount());
        item.setLikeCount(post.getLikeCount());
        item.setCommentCount(post.getCommentCount());
        item.setIsFeatured(post.getIsFeatured());
        item.setCreateTime(post.getCreateTime());
        item.setUpdateTime(post.getUpdateTime());
        return item;
    }

    private PublicPostSearchIndexItem toPublicPostSearchIndexItem(Post post) {
        PublicPostSearchIndexItem item = new PublicPostSearchIndexItem();
        item.setId(post.getId());
        item.setTitle(post.getTitle());
        item.setCategory(post.getCategory());
        item.setSummary(post.getSummary());
        item.setExcerpt(post.getExcerpt());
        item.setContent(post.getContent());
        item.setCreateTime(post.getCreateTime());
        return item;
    }
}
