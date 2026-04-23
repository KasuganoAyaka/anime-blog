package com.animeblog.controller;

import com.animeblog.dto.ApiResponse;
import com.animeblog.dto.CommentReportCreateRequest;
import com.animeblog.dto.ContactMessageRequest;
import com.animeblog.dto.FriendLinkApplyRequest;
import com.animeblog.dto.PostCommentCreateRequest;
import com.animeblog.dto.PostCommentSubmitResponse;
import com.animeblog.dto.PostCommentView;
import com.animeblog.dto.PublicCategorySummaryItem;
import com.animeblog.dto.PublicPostListItem;
import com.animeblog.dto.PublicPostSearchIndexItem;
import com.animeblog.dto.PublicTagSummaryItem;
import com.animeblog.entity.FriendLink;
import com.animeblog.entity.Music;
import com.animeblog.entity.Post;
import com.animeblog.entity.User;
import com.animeblog.exception.BusinessException;
import com.animeblog.mapper.MusicMapper;
import com.animeblog.mapper.UserMapper;
import com.animeblog.service.ContactMessageService;
import com.animeblog.service.CommentReportService;
import com.animeblog.service.FriendLinkService;
import com.animeblog.service.MusicService;
import com.animeblog.service.PostCommentService;
import com.animeblog.service.PostImageUploadService;
import com.animeblog.service.PostService;
import com.animeblog.service.TurnstileService;
import com.animeblog.util.SecurityHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class PublicController {

    @Autowired
    private MusicService musicService;

    @Autowired
    private FriendLinkService friendLinkService;

    @Autowired
    private PostService postService;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private MusicMapper musicMapper;

    @Autowired
    private ContactMessageService contactMessageService;

    @Autowired
    private PostCommentService postCommentService;

    @Autowired
    private PostImageUploadService postImageUploadService;

    @Autowired
    private SecurityHelper securityHelper;

    @Autowired
    private CommentReportService commentReportService;

    @Autowired
    private TurnstileService turnstileService;

    @GetMapping("/public/music/list")
    public ApiResponse<List<Music>> getPublicMusicList() {
        return ApiResponse.success(musicService.getAllMusic());
    }

    @GetMapping("/public/music/next/{id}")
    public ApiResponse<Music> getNextMusic(@PathVariable Long id) {
        Music nextMusic = musicService.getNextMusic(id);
        if (nextMusic == null) {
            List<Music> allMusic = musicService.getAllMusic();
            if (!allMusic.isEmpty()) {
                return ApiResponse.success(allMusic.get(0));
            }
            throw new BusinessException("没有音乐数据");
        }
        return ApiResponse.success(nextMusic);
    }

    @GetMapping("/public/music/prev/{id}")
    public ApiResponse<Music> getPrevMusic(@PathVariable Long id) {
        Music prevMusic = musicService.getPrevMusic(id);
        if (prevMusic == null) {
            List<Music> allMusic = musicService.getAllMusic();
            if (!allMusic.isEmpty()) {
                return ApiResponse.success(allMusic.get(allMusic.size() - 1));
            }
            throw new BusinessException("没有音乐数据");
        }
        return ApiResponse.success(prevMusic);
    }

    @GetMapping("/public/stats")
    public ApiResponse<Map<String, Object>> getPublicStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("users", userMapper.selectCount(null));
        stats.put("posts", postService.getPublishedPosts().size());
        stats.put("music", musicMapper.selectCount(null));
        stats.put("links", friendLinkService.getApprovedLinks().size());
        return ApiResponse.success(stats);
    }

    @GetMapping("/public/friend-links")
    public ApiResponse<List<FriendLink>> getPublicFriendLinks() {
        return ApiResponse.success(friendLinkService.getApprovedLinks());
    }

    @PostMapping("/public/contact")
    public ApiResponse<?> submitContact(@Valid @RequestBody ContactMessageRequest request) {
        contactMessageService.createMessage(request);
        return ApiResponse.success("消息已提交");
    }

    @PostMapping("/public/friend-links/apply")
    public ApiResponse<?> submitFriendLink(@Valid @RequestBody FriendLinkApplyRequest request) {
        return ApiResponse.success(friendLinkService.submitLinkApplication(
                request.getSiteName(),
                request.getSiteUrl(),
                request.getSiteDesc(),
                request.getContactEmail()
        ));
    }

    @GetMapping("/music")
    public ApiResponse<List<Music>> getMusic() {
        return ApiResponse.success(musicService.getAllMusic());
    }

    @GetMapping("/friend-links")
    public ApiResponse<List<FriendLink>> getFriendLinks() {
        return ApiResponse.success(friendLinkService.getApprovedLinks());
    }

    @GetMapping("/posts")
    public ApiResponse<Page<PublicPostListItem>> getPosts(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "6") int size,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String tag) {
        return ApiResponse.success(postService.getPublishedPostListPage(page, size, category, tag));
    }

    @GetMapping("/posts/tags")
    public ApiResponse<List<PublicTagSummaryItem>> getPostTags() {
        return ApiResponse.success(postService.getPublishedTagSummaries());
    }

    @GetMapping("/posts/categories")
    public ApiResponse<List<PublicCategorySummaryItem>> getPostCategories() {
        return ApiResponse.success(postService.getPublishedCategorySummaries());
    }

    @GetMapping("/posts/search-index")
    public ApiResponse<List<PublicPostSearchIndexItem>> getPostSearchIndex() {
        return ApiResponse.success(postService.getPublishedPostSearchIndexItems());
    }

    @GetMapping("/posts/{id}")
    public ApiResponse<Post> getPost(@PathVariable Long id, HttpServletRequest request) {
        Post post = postService.getPublishedPostByIdAndIncreaseView(id, buildVisitorKey(request));
        if (post == null) {
            throw new BusinessException("文章不存在或未发布");
        }
        return ApiResponse.success(post);
    }

    @GetMapping("/posts/{id}/comments")
    public ApiResponse<List<PostCommentView>> getPostComments(@PathVariable Long id) {
        return ApiResponse.success(postCommentService.getVisibleCommentsByPostId(id));
    }

    @PostMapping("/posts/{id}/comments")
    public ApiResponse<PostCommentSubmitResponse> createPostComment(
            @PathVariable Long id,
            @Valid @RequestBody PostCommentCreateRequest request,
            HttpServletRequest httpServletRequest
    ) {
        User currentUser = securityHelper.getCurrentUser();
        boolean guestComment = currentUser == null
                || Boolean.TRUE.equals(request.getGuestMode())
                || Boolean.TRUE.equals(request.getAnonymous());
        if (guestComment && turnstileService.isGuestCommentProtectionEnabled()) {
            turnstileService.validateOrThrow(
                    request.getTurnstileToken(),
                    resolveClientIp(httpServletRequest),
                    "guest_comment",
                    "请先完成人机验证再发表评论"
            );
        }
        PostCommentSubmitResponse comment = postCommentService.createComment(
                id,
                request,
                currentUser,
                resolveClientIp(httpServletRequest),
                httpServletRequest.getHeader("User-Agent")
        );
        return ApiResponse.success(comment);
    }

    @PostMapping("/posts/{postId}/comments/{commentId}/reports")
    public ApiResponse<?> createCommentReport(
            @PathVariable Long postId,
            @PathVariable Long commentId,
            @Valid @RequestBody CommentReportCreateRequest request
    ) {
        User currentUser = securityHelper.getCurrentUser();
        commentReportService.createReport(postId, commentId, request, currentUser);
        return ApiResponse.success("举报已提交，我们会尽快处理");
    }

    @PostMapping("/public/comments/images")
    public ApiResponse<Map<String, Object>> uploadCommentImages(
            @RequestParam("images") List<MultipartFile> images
    ) {
        try {
            List<String> urls = postImageUploadService.upload(images);
            return ApiResponse.success(Map.of("urls", urls));
        } catch (java.io.IOException e) {
            throw new BusinessException("评论图片上传失败", e);
        }
    }

    private String buildVisitorKey(HttpServletRequest request) {
        String clientIp = resolveClientIp(request);
        String userAgent = request.getHeader("User-Agent");
        if (userAgent == null) {
            userAgent = "";
        }
        userAgent = userAgent.trim();
        if (userAgent.length() > 200) {
            userAgent = userAgent.substring(0, 200);
        }
        return clientIp + "|" + userAgent;
    }

    private String resolveClientIp(HttpServletRequest request) {
        String[] headerNames = {
                "X-Forwarded-For",
                "Forwarded",
                "X-Real-IP",
                "X-Client-IP",
                "X-Original-Forwarded-For",
                "X-Original-Real-IP",
                "CF-Connecting-IP",
                "True-Client-IP",
                "Proxy-Client-IP",
                "WL-Proxy-Client-IP",
                "HTTP_CLIENT_IP",
                "HTTP_X_FORWARDED_FOR"
        };

        List<String> candidates = new ArrayList<>();
        for (String headerName : headerNames) {
            String headerValue = request.getHeader(headerName);
            if (!StringUtils.hasText(headerValue) || "unknown".equalsIgnoreCase(headerValue.trim())) {
                continue;
            }
            candidates.addAll(extractClientIpCandidates(headerName, headerValue));
        }

        String preferredIp = selectPreferredClientIp(candidates);
        if (preferredIp != null) {
            return preferredIp;
        }

        String remoteAddr = normalizeIpCandidate(request.getRemoteAddr());
        return remoteAddr == null ? request.getRemoteAddr() : remoteAddr;
    }

    private List<String> extractClientIpCandidates(String headerName, String headerValue) {
        List<String> candidates = new ArrayList<>();
        if ("Forwarded".equalsIgnoreCase(headerName)) {
            for (String forwardedEntry : headerValue.split(",")) {
                for (String part : forwardedEntry.split(";")) {
                    String trimmedPart = part.trim();
                    if (trimmedPart.regionMatches(true, 0, "for=", 0, 4)) {
                        String candidate = normalizeIpCandidate(trimmedPart.substring(4));
                        if (candidate != null) {
                            candidates.add(candidate);
                        }
                    }
                }
            }
            return candidates;
        }

        for (String part : headerValue.split(",")) {
            String candidate = normalizeIpCandidate(part);
            if (candidate != null) {
                candidates.add(candidate);
            }
        }
        return candidates;
    }

    private String selectPreferredClientIp(List<String> candidates) {
        String fallbackIp = null;
        for (String candidate : candidates) {
            if (!isValidClientIp(candidate)) {
                continue;
            }
            if (fallbackIp == null) {
                fallbackIp = candidate;
            }
            if (isPublicClientIp(candidate)) {
                return candidate;
            }
        }
        return fallbackIp;
    }

    private String normalizeIpCandidate(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim();
        if (normalized.isEmpty() || "unknown".equalsIgnoreCase(normalized)) {
            return null;
        }
        if (normalized.startsWith("\"") && normalized.endsWith("\"") && normalized.length() > 1) {
            normalized = normalized.substring(1, normalized.length() - 1).trim();
        }
        if (normalized.regionMatches(true, 0, "for=", 0, 4)) {
            normalized = normalized.substring(4).trim();
        }
        if (normalized.startsWith("[") && normalized.contains("]")) {
            normalized = normalized.substring(1, normalized.indexOf(']')).trim();
        } else if (normalized.indexOf(':') >= 0
                && normalized.indexOf(':') == normalized.lastIndexOf(':')
                && normalized.contains(".")) {
            normalized = normalized.substring(0, normalized.lastIndexOf(':')).trim();
        }
        if (normalized.startsWith("::ffff:")) {
            normalized = normalized.substring(7);
        }
        int zoneIndex = normalized.indexOf('%');
        if (zoneIndex >= 0) {
            normalized = normalized.substring(0, zoneIndex);
        }
        return normalized.isEmpty() ? null : normalized;
    }

    private boolean isValidClientIp(String candidate) {
        if (candidate == null) {
            return false;
        }
        try {
            InetAddress.getByName(candidate);
            return true;
        } catch (Exception exception) {
            return false;
        }
    }

    private boolean isPublicClientIp(String candidate) {
        try {
            InetAddress address = InetAddress.getByName(candidate);
            if (address.isAnyLocalAddress()
                    || address.isLoopbackAddress()
                    || address.isLinkLocalAddress()
                    || address.isSiteLocalAddress()
                    || address.isMulticastAddress()) {
                return false;
            }

            String normalized = candidate.toLowerCase(Locale.ROOT);
            if ("localhost".equals(normalized)) {
                return false;
            }
            if (address instanceof Inet6Address
                    && (normalized.startsWith("fc") || normalized.startsWith("fd") || normalized.startsWith("fe80:"))) {
                return false;
            }
            return true;
        } catch (Exception exception) {
            return false;
        }
    }
}
