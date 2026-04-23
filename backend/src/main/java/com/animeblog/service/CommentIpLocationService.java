package com.animeblog.service;

import com.animeblog.entity.IpRegionCache;
import com.animeblog.mapper.IpRegionCacheMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class CommentIpLocationService {

    public static final String REGION_UNKNOWN = "unknown";
    public static final String REGION_PUBLIC = "public-network";
    private static final Map<String, String> GLOBAL_REGION_LABELS = Map.ofEntries(
            Map.entry("hong kong", "香港"),
            Map.entry("hong kong sar", "香港"),
            Map.entry("macau", "澳门"),
            Map.entry("macao", "澳门"),
            Map.entry("tokyo", "东京"),
            Map.entry("osaka", "大阪"),
            Map.entry("kyoto", "京都"),
            Map.entry("yokohama", "横滨"),
            Map.entry("sapporo", "札幌"),
            Map.entry("nagoya", "名古屋"),
            Map.entry("fukuoka", "福冈"),
            Map.entry("taipei", "台北"),
            Map.entry("taichung", "台中"),
            Map.entry("kaohsiung", "高雄"),
            Map.entry("seoul", "首尔"),
            Map.entry("busan", "釜山"),
            Map.entry("incheon", "仁川"),
            Map.entry("singapore", "新加坡"),
            Map.entry("bangkok", "曼谷"),
            Map.entry("phuket", "普吉"),
            Map.entry("kuala lumpur", "吉隆坡"),
            Map.entry("jakarta", "雅加达"),
            Map.entry("bali", "巴厘岛"),
            Map.entry("manila", "马尼拉"),
            Map.entry("ho chi minh city", "胡志明市"),
            Map.entry("hanoi", "河内"),
            Map.entry("dubai", "迪拜"),
            Map.entry("abu dhabi", "阿布扎比"),
            Map.entry("new york", "纽约"),
            Map.entry("los angeles", "洛杉矶"),
            Map.entry("san francisco", "旧金山"),
            Map.entry("seattle", "西雅图"),
            Map.entry("chicago", "芝加哥"),
            Map.entry("toronto", "多伦多"),
            Map.entry("vancouver", "温哥华"),
            Map.entry("london", "伦敦"),
            Map.entry("manchester", "曼彻斯特"),
            Map.entry("paris", "巴黎"),
            Map.entry("berlin", "柏林"),
            Map.entry("munich", "慕尼黑"),
            Map.entry("frankfurt", "法兰克福"),
            Map.entry("rome", "罗马"),
            Map.entry("milan", "米兰"),
            Map.entry("madrid", "马德里"),
            Map.entry("barcelona", "巴塞罗那"),
            Map.entry("amsterdam", "阿姆斯特丹"),
            Map.entry("moscow", "莫斯科"),
            Map.entry("saint petersburg", "圣彼得堡"),
            Map.entry("sydney", "悉尼"),
            Map.entry("melbourne", "墨尔本"),
            Map.entry("brisbane", "布里斯班"),
            Map.entry("auckland", "奥克兰"),
            Map.entry("japan", "日本"),
            Map.entry("south korea", "韩国"),
            Map.entry("korea republic of", "韩国"),
            Map.entry("republic of korea", "韩国"),
            Map.entry("united states", "美国"),
            Map.entry("united states of america", "美国"),
            Map.entry("usa", "美国"),
            Map.entry("united kingdom", "英国"),
            Map.entry("great britain", "英国"),
            Map.entry("uk", "英国"),
            Map.entry("france", "法国"),
            Map.entry("germany", "德国"),
            Map.entry("italy", "意大利"),
            Map.entry("spain", "西班牙"),
            Map.entry("russia", "俄罗斯"),
            Map.entry("australia", "澳大利亚"),
            Map.entry("new zealand", "新西兰"),
            Map.entry("canada", "加拿大"),
            Map.entry("singapore republic", "新加坡"),
            Map.entry("hong kong sar china", "香港"),
            Map.entry("taiwan", "中国台湾"),
            Map.entry("taiwan province of china", "中国台湾"),
            Map.entry("macao sar china", "澳门"),
            Map.entry("united arab emirates", "阿联酋")
    );
    private static final String[] GLOBAL_REGION_SUFFIXES = {
            " special administrative region",
            " autonomous region",
            " metropolitan city",
            " municipality",
            " prefecture",
            " province",
            " district",
            " county",
            " region",
            " state",
            " city"
    };

    private final ObjectMapper objectMapper;
    private final IpRegionCacheMapper ipRegionCacheMapper;
    private final Map<String, CachedRegion> regionCache = new ConcurrentHashMap<>();

    @Value("${app.ip-location.enabled:true}")
    private boolean enabled;

    @Value("${app.ip-location.amap-key:}")
    private String amapKey;

    @Value("${app.ip-location.amap-endpoint:https://restapi.amap.com/v3/ip}")
    private String amapEndpoint;

    @Value("${app.ip-location.global-fallback-enabled:true}")
    private boolean globalFallbackEnabled;

    @Value("${app.ip-location.global-fallback-endpoint:https://api.ip2location.io/}")
    private String globalFallbackEndpoint;

    @Value("${app.ip-location.global-fallback-api-key:}")
    private String globalFallbackApiKey;

    @Value("${app.ip-location.cache-ttl-seconds:31536000}")
    private long cacheTtlSeconds;

    @Value("${app.ip-location.failure-cache-ttl-seconds:21600}")
    private long failureCacheTtlSeconds;

    @Value("${app.ip-location.connect-timeout-millis:2500}")
    private long connectTimeoutMillis;

    @Value("${app.ip-location.read-timeout-millis:2500}")
    private long readTimeoutMillis;

    public CommentIpLocationService(ObjectMapper objectMapper, IpRegionCacheMapper ipRegionCacheMapper) {
        this.objectMapper = objectMapper;
        this.ipRegionCacheMapper = ipRegionCacheMapper;
    }

    public String resolveRegion(String clientIp) {
        String normalizedIp = normalizeClientIp(clientIp);
        if (!StringUtils.hasText(normalizedIp)) {
            return REGION_UNKNOWN;
        }

        String lowerIp = normalizedIp.toLowerCase(Locale.ROOT);
        long now = System.currentTimeMillis();

        CachedRegion memoryCache = readMemoryCache(lowerIp, now);
        if (memoryCache != null && !shouldRefreshCachedRegion(lowerIp, memoryCache.region())) {
            return memoryCache.region();
        }

        CachedRegion databaseCache = readDatabaseCache(lowerIp, now);
        if (databaseCache != null && !shouldRefreshCachedRegion(lowerIp, databaseCache.region())) {
            regionCache.put(lowerIp, databaseCache);
            return databaseCache.region();
        }

        return queryAndCacheRegion(lowerIp, now);
    }

    private String queryAndCacheRegion(String ip, long now) {
        String region = REGION_UNKNOWN;
        long ttlMillis = Math.max(failureCacheTtlSeconds, 300L) * 1000L;

        if (enabled && StringUtils.hasText(amapKey)) {
            region = queryAmapRegion(ip);
        }

        if (shouldUseGlobalFallback(ip, region)) {
            String globalRegion = queryGlobalFallbackRegion(ip);
            if (isResolvedRegion(globalRegion)) {
                region = globalRegion;
            }
        }

        if (!isResolvedRegion(region)) {
            region = fallbackRegion(ip);
        }

        if (isResolvedRegion(region)) {
            ttlMillis = Math.max(cacheTtlSeconds, 3600L) * 1000L;
        }

        region = beautifyResolvedRegion(region);
        writeCaches(ip, region, now + ttlMillis);
        return region;
    }

    private String queryAmapRegion(String ip) {
        try {
            String query = "key=" + URLEncoder.encode(amapKey.trim(), StandardCharsets.UTF_8);
            if (isIpv4(ip)) {
                query += "&ip=" + URLEncoder.encode(ip, StandardCharsets.UTF_8);
            }

            URI uri = URI.create(amapEndpoint + (amapEndpoint.contains("?") ? "&" : "?") + query);
            HttpResponse<String> response = sendHttpRequest(uri);
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.debug("Amap IP lookup returned status {} for {}", response.statusCode(), ip);
                return REGION_UNKNOWN;
            }

            JsonNode root = objectMapper.readTree(response.body());
            if (!"1".equals(root.path("status").asText())) {
                log.debug("Amap IP lookup rejected for {} with info: {}", ip, root.path("info").asText(""));
                return REGION_UNKNOWN;
            }

            return formatAmapRegion(
                    sanitizeRegionValue(root.path("province").asText("")),
                    sanitizeRegionValue(root.path("city").asText(""))
            );
        } catch (Exception exception) {
            log.debug("Amap IP lookup failed for {}", ip, exception);
            return REGION_UNKNOWN;
        }
    }

    private String queryGlobalFallbackRegion(String ip) {
        try {
            if (!StringUtils.hasText(globalFallbackApiKey)) {
                return REGION_UNKNOWN;
            }

            String query = "key=" + URLEncoder.encode(globalFallbackApiKey.trim(), StandardCharsets.UTF_8)
                    + "&ip=" + URLEncoder.encode(ip, StandardCharsets.UTF_8)
                    + "&format=json";
            URI uri = URI.create(globalFallbackEndpoint + (globalFallbackEndpoint.contains("?") ? "&" : "?") + query);
            HttpResponse<String> response = sendHttpRequest(uri);
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.debug("Global IP fallback returned status {} for {}", response.statusCode(), ip);
                return REGION_UNKNOWN;
            }

            JsonNode root = objectMapper.readTree(response.body());
            if (!StringUtils.hasText(root.path("country_code").asText(""))) {
                log.debug("Global IP fallback rejected for {} with payload: {}", ip, response.body());
                return REGION_UNKNOWN;
            }

            return formatGlobalRegion(
                    sanitizeRegionValue(root.path("city_name").asText("")),
                    sanitizeRegionValue(root.path("region_name").asText("")),
                    sanitizeRegionValue(root.path("country_name").asText(""))
            );
        } catch (Exception exception) {
            log.debug("Global IP fallback failed for {}", ip, exception);
            return REGION_UNKNOWN;
        }
    }

    private HttpResponse<String> sendHttpRequest(URI uri) throws Exception {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(Math.max(connectTimeoutMillis, 500L)))
                .build();
        HttpRequest request = HttpRequest.newBuilder(uri)
                .GET()
                .header("Accept", "application/json")
                .timeout(Duration.ofMillis(Math.max(readTimeoutMillis, 500L)))
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private CachedRegion readMemoryCache(String ip, long now) {
        CachedRegion cachedRegion = regionCache.get(ip);
        if (cachedRegion == null) {
            return null;
        }
        if (cachedRegion.expiresAt() > now) {
            CachedRegion normalizedCache = normalizeCachedRegion(cachedRegion);
            if (!normalizedCache.region().equals(cachedRegion.region())) {
                regionCache.put(ip, normalizedCache);
            }
            return normalizedCache;
        }
        regionCache.remove(ip, cachedRegion);
        return null;
    }

    private CachedRegion readDatabaseCache(String ip, long now) {
        try {
            IpRegionCache cacheEntity = ipRegionCacheMapper.selectById(ip);
            if (cacheEntity == null || !StringUtils.hasText(cacheEntity.getRegion()) || cacheEntity.getExpiresAt() == null) {
                return null;
            }

            long expiresAtMillis = cacheEntity.getExpiresAt()
                    .atZone(ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli();
            if (expiresAtMillis <= now) {
                return null;
            }

            String normalizedRegion = beautifyResolvedRegion(cacheEntity.getRegion());
            if (!normalizedRegion.equals(cacheEntity.getRegion())) {
                cacheEntity.setRegion(normalizedRegion);
                cacheEntity.setUpdateTime(LocalDateTime.now());
                ipRegionCacheMapper.updateById(cacheEntity);
            }

            return new CachedRegion(normalizedRegion, expiresAtMillis);
        } catch (Exception exception) {
            log.debug("Read IP region cache failed for {}", ip, exception);
            return null;
        }
    }

    private void writeCaches(String ip, String region, long expiresAtMillis) {
        regionCache.put(ip, new CachedRegion(region, expiresAtMillis));
        writeDatabaseCache(ip, region, expiresAtMillis);
    }

    private void writeDatabaseCache(String ip, String region, long expiresAtMillis) {
        try {
            LocalDateTime expiresAt = LocalDateTime.ofInstant(
                    java.time.Instant.ofEpochMilli(expiresAtMillis),
                    ZoneId.systemDefault()
            );
            LocalDateTime now = LocalDateTime.now();

            IpRegionCache existing = ipRegionCacheMapper.selectById(ip);
            if (existing == null) {
                IpRegionCache created = new IpRegionCache();
                created.setIp(ip);
                created.setRegion(region);
                created.setExpiresAt(expiresAt);
                created.setCreateTime(now);
                created.setUpdateTime(now);
                ipRegionCacheMapper.insert(created);
                return;
            }

            existing.setRegion(region);
            existing.setExpiresAt(expiresAt);
            existing.setUpdateTime(now);
            ipRegionCacheMapper.updateById(existing);
        } catch (Exception exception) {
            log.debug("Write IP region cache failed for {}", ip, exception);
        }
    }

    private boolean shouldRefreshCachedRegion(String ip, String region) {
        return shouldUseGlobalFallback(ip, region);
    }

    private boolean shouldUseGlobalFallback(String ip, String region) {
        if (!globalFallbackEnabled
                || !StringUtils.hasText(globalFallbackEndpoint)
                || !StringUtils.hasText(globalFallbackApiKey)) {
            return false;
        }
        if (!StringUtils.hasText(ip) || !isIpv4(ip)) {
            return false;
        }
        return !StringUtils.hasText(region)
                || REGION_UNKNOWN.equals(region)
                || REGION_PUBLIC.equals(region);
    }

    private boolean isResolvedRegion(String region) {
        return StringUtils.hasText(region)
                && !REGION_UNKNOWN.equals(region)
                && !REGION_PUBLIC.equals(region);
    }

    private String formatAmapRegion(String province, String city) {
        if ("局域网".equals(province) || "局域网".equals(city) || "内网IP".equals(province) || "内网IP".equals(city)) {
            return REGION_UNKNOWN;
        }
        if (StringUtils.hasText(city) && !"市辖区".equals(city) && !"县".equals(city)) {
            return city;
        }
        if (StringUtils.hasText(province)) {
            return province;
        }
        if (StringUtils.hasText(city)) {
            return city;
        }
        return REGION_UNKNOWN;
    }

    private String formatGlobalRegion(String city, String region, String country) {
        String normalizedCity = beautifyResolvedRegion(simplifyGlobalRegionValue(city));
        if (StringUtils.hasText(normalizedCity)) {
            return normalizedCity;
        }

        String normalizedRegion = beautifyResolvedRegion(simplifyGlobalRegionValue(region));
        if (StringUtils.hasText(normalizedRegion)) {
            return normalizedRegion;
        }

        String normalizedCountry = beautifyResolvedRegion(simplifyGlobalRegionValue(country));
        return StringUtils.hasText(normalizedCountry) ? normalizedCountry : REGION_UNKNOWN;
    }

    private String simplifyGlobalRegionValue(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }

        String normalized = value.trim();
        if (normalized.contains(",")) {
            String[] parts = normalized.split(",");
            for (String part : parts) {
                String trimmed = part.trim();
                if (StringUtils.hasText(trimmed)) {
                    return trimmed;
                }
            }
        }
        return normalized;
    }

    private CachedRegion normalizeCachedRegion(CachedRegion cachedRegion) {
        String normalizedRegion = beautifyResolvedRegion(cachedRegion.region());
        if (normalizedRegion.equals(cachedRegion.region())) {
            return cachedRegion;
        }
        return new CachedRegion(normalizedRegion, cachedRegion.expiresAt());
    }

    private String beautifyResolvedRegion(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }

        String normalized = simplifyGlobalRegionValue(value);
        if (!StringUtils.hasText(normalized)
                || REGION_UNKNOWN.equals(normalized)
                || REGION_PUBLIC.equals(normalized)) {
            return normalized;
        }

        String translated = lookupBeautifiedGlobalRegion(normalized);
        return StringUtils.hasText(translated) ? translated : normalized;
    }

    private String lookupBeautifiedGlobalRegion(String value) {
        String key = normalizeGlobalRegionLookupKey(value);
        if (!StringUtils.hasText(key)) {
            return "";
        }

        String translated = GLOBAL_REGION_LABELS.get(key);
        if (StringUtils.hasText(translated)) {
            return translated;
        }

        String strippedKey = stripGlobalRegionSuffix(key);
        if (!strippedKey.equals(key)) {
            translated = GLOBAL_REGION_LABELS.get(strippedKey);
            if (StringUtils.hasText(translated)) {
                return translated;
            }
        }

        return "";
    }

    private String normalizeGlobalRegionLookupKey(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT)
                .replace('&', ' ')
                .replaceAll("[^a-z0-9\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String stripGlobalRegionSuffix(String key) {
        String normalized = key;
        boolean changed = true;
        while (changed) {
            changed = false;
            for (String suffix : GLOBAL_REGION_SUFFIXES) {
                if (normalized.endsWith(suffix) && normalized.length() > suffix.length()) {
                    normalized = normalized.substring(0, normalized.length() - suffix.length()).trim();
                    changed = true;
                    break;
                }
            }
        }
        return normalized;
    }

    private String sanitizeRegionValue(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        String normalized = value.trim();
        return "[]".equals(normalized) ? "" : normalized;
    }

    private String normalizeClientIp(String clientIp) {
        if (!StringUtils.hasText(clientIp)) {
            return "";
        }

        String normalized = clientIp.trim();
        int commaIndex = normalized.indexOf(',');
        if (commaIndex >= 0) {
            normalized = normalized.substring(0, commaIndex).trim();
        }
        if (normalized.toLowerCase(Locale.ROOT).startsWith("::ffff:")) {
            normalized = normalized.substring(7);
        }
        return normalized;
    }

    private boolean isLoopbackIp(String value) {
        return "localhost".equals(value) || "127.0.0.1".equals(value) || "::1".equals(value);
    }

    private boolean isPrivateIpv4(String value) {
        if (!isIpv4(value)) {
            return false;
        }
        if (value.startsWith("10.") || value.startsWith("192.168.") || value.startsWith("169.254.")) {
            return true;
        }
        if (value.startsWith("172.")) {
            String[] segments = value.split("\\.");
            if (segments.length > 1) {
                try {
                    int second = Integer.parseInt(segments[1]);
                    return second >= 16 && second <= 31;
                } catch (NumberFormatException ignored) {
                    return false;
                }
            }
        }
        return false;
    }

    private boolean isPrivateIpv6(String value) {
        String normalized = value.toLowerCase(Locale.ROOT);
        return normalized.startsWith("fc")
                || normalized.startsWith("fd")
                || normalized.startsWith("fe80:");
    }

    private boolean isIpv4(String value) {
        String[] segments = value.split("\\.");
        if (segments.length != 4) {
            return false;
        }

        for (String segment : segments) {
            if (!StringUtils.hasText(segment) || segment.length() > 3) {
                return false;
            }
            try {
                int number = Integer.parseInt(segment);
                if (number < 0 || number > 255) {
                    return false;
                }
            } catch (NumberFormatException ignored) {
                return false;
            }
        }
        return true;
    }

    private String fallbackRegion(String value) {
        if (!StringUtils.hasText(value)) {
            return REGION_UNKNOWN;
        }
        String normalized = value.toLowerCase(Locale.ROOT);
        if (isLoopbackIp(normalized) || isPrivateIpv4(normalized) || isPrivateIpv6(normalized)) {
            return REGION_UNKNOWN;
        }
        if (normalized.contains(":")) {
            return REGION_UNKNOWN;
        }
        if (isIpv4(normalized)) {
            return REGION_PUBLIC;
        }
        return REGION_UNKNOWN;
    }

    private record CachedRegion(String region, long expiresAt) {
    }
}
