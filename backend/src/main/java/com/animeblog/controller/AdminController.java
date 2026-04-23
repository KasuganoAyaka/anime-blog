package com.animeblog.controller;

import com.animeblog.dto.ApiResponse;
import com.animeblog.dto.AdminPageResult;
import com.animeblog.dto.BatchModerationRequest;
import com.animeblog.dto.ContactReplyRequest;
import com.animeblog.dto.MusicUploadResponse;
import com.animeblog.dto.PostReviewDecisionRequest;
import com.animeblog.entity.ContactMessage;
import com.animeblog.entity.FriendLink;
import com.animeblog.entity.Music;
import com.animeblog.entity.Post;
import com.animeblog.entity.PostComment;
import com.animeblog.entity.PostReview;
import com.animeblog.entity.User;
import com.animeblog.exception.BusinessException;
import com.animeblog.exception.ForbiddenException;
import com.animeblog.exception.AuthException;
import com.animeblog.mapper.ContactMessageMapper;
import com.animeblog.mapper.FriendLinkMapper;
import com.animeblog.mapper.MusicMapper;
import com.animeblog.mapper.PostMapper;
import com.animeblog.mapper.PostCommentMapper;
import com.animeblog.mapper.PostReviewMapper;
import com.animeblog.mapper.UserMapper;
import com.animeblog.service.ContactMessageService;
import com.animeblog.service.MusicUploadService;
import com.animeblog.service.ModerationTaskService;
import com.animeblog.service.PostImageUploadService;
import com.animeblog.service.PostReviewService;
import com.animeblog.service.PostService;
import com.animeblog.util.AuthHelper;
import com.animeblog.util.InputValidationUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 管理员控制器
 * 提供后台管理功能,包括用户管理、文章管理、投稿审核、音乐管理、友链管理、留言管理等
 * URL前缀: /api/admin
 * 访问权限: 需要管理员(admin/manager)或站长权限
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    // 角色常量定义
    private static final String ROLE_STATION_MASTER = AuthHelper.ROLE_STATION_MASTER;
    private static final String ROLE_MANAGER = AuthHelper.ROLE_MANAGER;
    private static final String ROLE_USER = "user";

    // 站长账号固定信息
    private static final String STATIONMASTER_USERNAME = "admin";
    private static final String STATIONMASTER_NICKNAME = "站长";

    // 权限校验错误提示信息
    private static final String ADMIN_FORBIDDEN_MESSAGE = "无权限访问管理员接口";
    private static final String MUSIC_FORBIDDEN_MESSAGE = "只有站长可以管理音乐";
    private static final String LOGIN_REQUIRED_MESSAGE = "请先登录";

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PostMapper postMapper;

    @Autowired
    private PostReviewMapper postReviewMapper;

    @Autowired
    private PostCommentMapper postCommentMapper;

    @Autowired
    private MusicMapper musicMapper;

    @Autowired
    private FriendLinkMapper friendLinkMapper;

    @Autowired
    private ContactMessageMapper contactMessageMapper;

    @Autowired
    private ContactMessageService contactMessageService;

    @Autowired
    private AuthHelper authHelper;

    @Autowired
    private PostReviewService postReviewService;

    @Autowired
    private PostService postService;

    @Autowired
    private MusicUploadService musicUploadService;

    @Autowired
    private PostImageUploadService postImageUploadService;

    @Autowired
    private ModerationTaskService moderationTaskService;

    /**
     * 检查当前用户是否非管理员(站长或管理员)
     * @param authorization 认证令牌
     * @return true表示无权限, false表示有权限
     */
    private boolean isForbidden(String authorization) {
        User currentUser = authHelper.getCurrentUser(authorization);
        return !authHelper.isAdmin(currentUser);
    }

    /**
     * 检查当前用户是否非站长
     * @param authorization 认证令牌
     * @return true表示非站长, false表示是站长
     */
    private boolean isStationMasterForbidden(String authorization) {
        User currentUser = authHelper.getCurrentUser(authorization);
        return !isStationMaster(currentUser);
    }

    /**
     * 判断用户是否为站长
     */
    private boolean isStationMaster(User user) {
        return authHelper.isStationMaster(user);
    }

    /**
     * 判断是否为受保护的站长账号(用户名必须为admin且角色为站长)
     */
    private boolean isProtectedStationMaster(User user) {
        return user != null
                && ROLE_STATION_MASTER.equals(user.getRole())
                && STATIONMASTER_USERNAME.equals(user.getUsername());
    }

    /**
     * 判断是否为管理员账号
     */
    private boolean isManagerAccount(User user) {
        return user != null && ROLE_MANAGER.equals(user.getRole());
    }

    /**
     * 规范化角色名称,将空值或非法角色转换为用户角色
     */
    private String normalizeRole(String role) {
        if (role == null || role.isBlank()) {
            return ROLE_USER;
        }
        if (ROLE_STATION_MASTER.equals(role) || ROLE_MANAGER.equals(role) || ROLE_USER.equals(role)) {
            return role;
        }
        return ROLE_USER;
    }

    /**
     * 清除用户对象中的密码,防止敏感信息泄露
     */
    private User sanitizeUser(User user) {
        if (user != null) {
            user.setPassword(null);
        }
        return user;
    }

    /**
     * 获取所有用户列表(仅管理员可访问)
     * URL: GET /api/admin/users
     * 权限: 管理员/站长
     * @return 用户列表(已脱敏)
     */
    @GetMapping("/users")
    public ApiResponse<?> getUsers(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        User currentUser = authHelper.getCurrentUser(authorization);
        if (!authHelper.isAdmin(currentUser)) {
            throw new ForbiddenException(ADMIN_FORBIDDEN_MESSAGE);
        }

        Page<User> pageObj = new Page<>(page, size);
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.orderByDesc("create_time", "id");
        Page<User> users = userMapper.selectPage(pageObj, wrapper);
        users.getRecords().forEach(this::sanitizeUser);
        return ApiResponse.success(toPageResult(users));
    }

    /**
     * 创建新用户(仅管理员可访问)
     * URL: POST /api/admin/users
     * 权限: 管理员/站长(创建管理员账号仅站长可操作)
     * 参数: User对象(包含username, password, email, role等)
     * @return 创建成功的用户信息(已脱敏)
     */
    @PostMapping("/users")
    public ApiResponse<?> createUser(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody User user) {
        User currentUser = authHelper.getCurrentUser(authorization);
        if (!authHelper.isAdmin(currentUser)) {
            throw new ForbiddenException(ADMIN_FORBIDDEN_MESSAGE);
        }
        if (userMapper.selectByUsername(user.getUsername()) != null) {
            throw new BusinessException("用户名已存在");
        }
        if (userMapper.selectByEmail(user.getEmail()) != null) {
            throw new BusinessException("邮箱已被使用");
        }

        if (!InputValidationUtil.isPasswordValid(user.getPassword())) {
            throw new BusinessException(InputValidationUtil.PASSWORD_RULE_MESSAGE);
        }
        String targetRole = normalizeRole(user.getRole());
        if (ROLE_STATION_MASTER.equals(targetRole)) {
            throw new BusinessException("站长账号唯一，不能新建");
        }
        if (ROLE_MANAGER.equals(targetRole) && !isStationMaster(currentUser)) {
            throw new ForbiddenException("只有站长可以创建管理员账号");
        }
        user.setRole(targetRole);
        user.setPassword(new BCryptPasswordEncoder().encode(user.getPassword()));
        if (isProtectedStationMaster(user)) {
            user.setNickname(STATIONMASTER_NICKNAME);
            user.setRole(ROLE_STATION_MASTER);
            user.setStatus(1);
        }
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        userMapper.insert(user);
        return ApiResponse.success(sanitizeUser(user));
    }

    /**
     * 更新用户信息(仅管理员可访问)
     * URL: PUT /api/admin/users/{id}
     * 权限: 管理员/站长(编辑管理员/站长账号仅站长可操作)
     * 参数: id-用户ID, User对象-更新的用户信息
     * @return 更新后的用户信息(已脱敏)
     */
    @PutMapping("/users/{id}")
    public ApiResponse<?> updateUser(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long id,
            @RequestBody User user) {
        User currentUser = authHelper.getCurrentUser(authorization);
        if (!authHelper.isAdmin(currentUser)) {
            throw new ForbiddenException(ADMIN_FORBIDDEN_MESSAGE);
        }

        User existing = userMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException("用户不存在");
        }

        if (isProtectedStationMaster(existing) && !isStationMaster(currentUser)) {
            throw new ForbiddenException("管理员不可编辑站长账号");
        }
        if (isManagerAccount(existing) && !isStationMaster(currentUser)) {
            throw new ForbiddenException("只有站长可以编辑管理员账号");
        }

        User usernameOwner = userMapper.selectByUsername(user.getUsername());
        if (user.getUsername() != null && usernameOwner != null && !usernameOwner.getId().equals(id)) {
            throw new BusinessException("用户名已存在");
        }
        User emailOwner = userMapper.selectByEmail(user.getEmail());
        if (user.getEmail() != null && emailOwner != null && !emailOwner.getId().equals(id)) {
            throw new BusinessException("邮箱已被使用");
        }

        if (user.getUsername() != null && !user.getUsername().isBlank()) {
            existing.setUsername(user.getUsername());
        }
        String targetRole = normalizeRole(user.getRole());
        if (ROLE_STATION_MASTER.equals(targetRole) && !isProtectedStationMaster(existing)) {
            throw new BusinessException("站长角色唯一，不可分配给其他账号");
        }
        if (ROLE_MANAGER.equals(targetRole) && !ROLE_MANAGER.equals(existing.getRole()) && !isStationMaster(currentUser)) {
            throw new ForbiddenException("只有站长可以将用户设为管理员");
        }
        existing.setNickname(user.getNickname());
        existing.setEmail(user.getEmail());
        existing.setRole(targetRole);
        existing.setStatus(user.getStatus());
        if (user.getPassword() != null && !user.getPassword().isBlank()) {
            existing.setPassword(new BCryptPasswordEncoder().encode(user.getPassword()));
        }
        if (isProtectedStationMaster(existing)) {
            existing.setUsername(STATIONMASTER_USERNAME);
            existing.setNickname(STATIONMASTER_NICKNAME);
            existing.setRole(ROLE_STATION_MASTER);
            existing.setStatus(1);
        }
        existing.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(existing);
        return ApiResponse.success(sanitizeUser(existing));
    }

    /**
     * 删除用户(仅管理员可访问)
     * URL: DELETE /api/admin/users/{id}
     * 权限: 管理员/站长(删除管理员/站长账号仅站长可操作)
     * 参数: id-用户ID
     * @return 删除结果
     */
    @DeleteMapping("/users/{id}")
    public ApiResponse<?> deleteUser(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long id) {
        User currentUser = authHelper.getCurrentUser(authorization);
        if (!authHelper.isAdmin(currentUser)) {
            throw new ForbiddenException(ADMIN_FORBIDDEN_MESSAGE);
        }

        User user = userMapper.selectById(id);
        if (isProtectedStationMaster(user)) {
            if (!isStationMaster(currentUser)) {
                throw new ForbiddenException("管理员不可删除站长账号");
            }
            throw new BusinessException("不能删除站长账号");
        }
        if (isManagerAccount(user) && !isStationMaster(currentUser)) {
            throw new ForbiddenException("只有站长可以删除管理员账号");
        }
        userMapper.deleteById(id);
        return ApiResponse.success();
    }

    /**
     * 获取文章列表(支持分页和按作者筛选)
     * URL: GET /api/admin/posts
     * 权限: 管理员/站长/普通用户(普通用户只能看自己的文章)
     * 参数: creatorId-作者ID(可选), page-页码(默认1), size-每页数量(默认20)
     * @return 分页文章数据
     */
    @GetMapping("/posts")
    public ApiResponse<?> getPosts(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam(required = false) Long creatorId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        if (isForbidden(authorization)) {
            throw new ForbiddenException(ADMIN_FORBIDDEN_MESSAGE);
        }

        Page<Post> pageObj = new Page<>(page, size);
        QueryWrapper<Post> wrapper = new QueryWrapper<>();
        if (creatorId != null) {
            wrapper.eq("user_id", creatorId);
        }
        wrapper.orderByDesc("create_time");
        return ApiResponse.success(toPageResult(postMapper.selectPage(pageObj, wrapper)));
    }

    /**
     * 获取文章详情
     * URL: GET /api/admin/posts/{id}
     * 权限: 登录用户(管理员可看所有,普通用户只能看自己的)
     * 参数: id-文章ID
     * @return 文章详情
     */
    @GetMapping("/posts/{id}")
    public ApiResponse<?> getPostDetail(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long id) {
        User currentUser = authHelper.getCurrentUser(authorization);
        if (currentUser == null) {
            throw new AuthException(LOGIN_REQUIRED_MESSAGE);
        }

        Post post = postMapper.selectById(id);
        if (post == null) {
            throw new BusinessException("文章不存在");
        }

        if (!authHelper.isAdmin(currentUser) && !currentUser.getId().equals(post.getUserId())) {
            throw new ForbiddenException("无权查看该文章");
        }

        return ApiResponse.success(post);
    }

    /**
     * 获取投稿工作区数据(用于普通用户查看自己的投稿状态)
     * URL: GET /api/admin/post-workspace
     * 权限: 登录用户
     * @return 工作区数据(包含投稿列表和统计信息)
     */
    @GetMapping("/post-workspace")
    public ApiResponse<?> getPostWorkspace(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "12") int size) {
        User currentUser = authHelper.getCurrentUser(authorization);
        if (currentUser == null) {
            throw new AuthException(LOGIN_REQUIRED_MESSAGE);
        }
        return ApiResponse.success(postReviewService.getWorkspacePage(currentUser, page, size));
    }

    /**
     * 创建新文章(仅管理员可访问)
     * URL: POST /api/admin/posts
     * 权限: 管理员/站长
     * 参数: Post对象(包含title, content, summary, category, tags等)
     * @return 创建成功的文章信息
     */
    @PostMapping("/posts")
    public ApiResponse<?> createPost(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody Post post) {
        User currentUser = authHelper.getCurrentUser(authorization);
        if (!authHelper.isAdmin(currentUser)) {
            throw new ForbiddenException(ADMIN_FORBIDDEN_MESSAGE);
        }

        post.setUserId(currentUser.getId());
        post.setCreateTime(LocalDateTime.now());
        post.setUpdateTime(LocalDateTime.now());
        postMapper.insert(post);
        postService.invalidatePublicCaches();
        return ApiResponse.success(post);
    }

    /**
     * 更新文章(管理员和普通用户都可访问,用户只能修改自己的文章)
     * URL: PUT /api/admin/posts/{id}
     * 权限: 管理员/站长/文章作者
     * 参数: id-文章ID, Post对象-更新的文章信息
     * @return 更新后的文章信息
     */
    @PutMapping("/posts/{id}")
    public ApiResponse<?> updatePost(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long id,
            @RequestBody Post post) {
        if (isForbidden(authorization)) {
            throw new ForbiddenException(ADMIN_FORBIDDEN_MESSAGE);
        }

        Post existing = postMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException("文章不存在");
        }

        existing.setTitle(post.getTitle());
        existing.setContent(post.getContent());
        existing.setSummary(post.getSummary());
        existing.setExcerpt(post.getExcerpt());
        existing.setCategory(post.getCategory());
        existing.setTags(post.getTags());
        existing.setCoverImage(post.getCoverImage());
        existing.setStatus(post.getStatus());
        existing.setUpdateTime(LocalDateTime.now());
        postMapper.updateById(existing);
        postService.invalidatePublicCaches();
        return ApiResponse.success(existing);
    }

    /**
     * 提交文章修改审核(普通用户提交修改申请给管理员审核)
     * URL: POST /api/admin/posts/{id}/review
     * 权限: 登录用户(管理员不可使用,管理员直接编辑文章)
     * 参数: id-文章ID, Post对象-修改后的文章内容
     * @return 审核记录信息
     */
    @PostMapping("/posts/{id}/review")
    public ApiResponse<?> submitPostReview(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long id,
            @RequestBody Post post) {
        User currentUser = authHelper.getCurrentUser(authorization);
        if (currentUser == null) {
            throw new AuthException(LOGIN_REQUIRED_MESSAGE);
        }
        if (authHelper.isAdmin(currentUser)) {
            throw new ForbiddenException("管理员请直接编辑文章");
        }
        return ApiResponse.success(postReviewService.submitPostUpdateReview(id, currentUser, post));
    }

    /**
     * 创建新文章审核稿(普通用户创建新文章草稿)
     * URL: POST /api/admin/post-reviews
     * 权限: 登录用户(管理员不可使用,管理员直接发布文章)
     * 参数: Post对象-文章内容
     * @return 创建的草稿信息
     */
    @PostMapping("/post-reviews")
    public ApiResponse<?> createPostReview(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody Post post) {
        User currentUser = authHelper.getCurrentUser(authorization);
        if (currentUser == null) {
            throw new AuthException(LOGIN_REQUIRED_MESSAGE);
        }
        if (authHelper.isAdmin(currentUser)) {
            throw new ForbiddenException("管理员请直接发布文章");
        }
        return ApiResponse.success(postReviewService.createDraftForNewPost(currentUser, post));
    }

    /**
     * 更新审核稿草稿(用户编辑自己提交的审核稿)
     * URL: PUT /api/admin/post-reviews/{id}
     * 权限: 登录用户(管理员不可使用)
     * 参数: id-审核稿ID, Post对象-更新后的文章内容
     * @return 更新后的草稿信息
     */
    @PutMapping("/post-reviews/{id}")
    public ApiResponse<?> updatePostReview(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long id,
            @RequestBody Post post) {
        User currentUser = authHelper.getCurrentUser(authorization);
        if (currentUser == null) {
            throw new AuthException(LOGIN_REQUIRED_MESSAGE);
        }
        if (authHelper.isAdmin(currentUser)) {
            throw new ForbiddenException("管理员无需编辑审核稿");
        }
        return ApiResponse.success(postReviewService.saveOwnReviewDraft(id, currentUser, post));
    }

    /**
     * 提交已保存的审核稿(将草稿提交给管理员审核)
     * URL: PUT /api/admin/post-reviews/{id}/submit
     * 权限: 登录用户(管理员不可使用)
     * 参数: id-审核稿ID
     * @return 提交结果
     */
    @PutMapping("/post-reviews/{id}/submit")
    public ApiResponse<?> submitSavedPostReview(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long id) {
        User currentUser = authHelper.getCurrentUser(authorization);
        if (currentUser == null) {
            throw new AuthException(LOGIN_REQUIRED_MESSAGE);
        }
        if (authHelper.isAdmin(currentUser)) {
            throw new ForbiddenException("管理员无需提交审核稿");
        }
        return ApiResponse.success(postReviewService.submitOwnDraftReview(id, currentUser));
    }

    /**
     * 保存文章修改为草稿(不提交审核,仅保存)
     * URL: PUT /api/admin/posts/{id}/draft
     * 权限: 登录用户(管理员不可使用)
     * 参数: id-文章ID, Post对象-文章内容
     * @return 保存结果
     */
    @PutMapping("/posts/{id}/draft")
    public ApiResponse<?> savePostDraft(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long id,
            @RequestBody Post post) {
        User currentUser = authHelper.getCurrentUser(authorization);
        if (currentUser == null) {
            throw new AuthException(LOGIN_REQUIRED_MESSAGE);
        }
        if (authHelper.isAdmin(currentUser)) {
            throw new ForbiddenException("管理员请直接编辑文章");
        }
        return ApiResponse.success(postReviewService.savePostUpdateDraft(id, currentUser, post));
    }

    /**
     * 下线已发布文章(将文章从已发布状态撤下)
     * URL: PUT /api/admin/posts/{id}/offline
     * 权限: 登录用户(仅文章作者本人,管理员不可使用)
     * 参数: id-文章ID
     * @return 下线结果
     */
    @PutMapping("/posts/{id}/offline")
    public ApiResponse<?> offlinePost(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long id) {
        User currentUser = authHelper.getCurrentUser(authorization);
        if (currentUser == null) {
            throw new AuthException(LOGIN_REQUIRED_MESSAGE);
        }
        if (authHelper.isAdmin(currentUser)) {
            throw new ForbiddenException("管理员请直接编辑文章");
        }
        return ApiResponse.success(postReviewService.offlineOwnPost(id, currentUser));
    }

    /**
     * 发布文章(将审核通过的文章发布)
     * URL: PUT /api/admin/posts/{id}/publish
     * 权限: 登录用户(仅文章作者本人,管理员不可使用)
     * 参数: id-文章ID
     * @return 发布结果
     */
    @PutMapping("/posts/{id}/publish")
    public ApiResponse<?> publishPost(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long id) {
        User currentUser = authHelper.getCurrentUser(authorization);
        if (currentUser == null) {
            throw new AuthException(LOGIN_REQUIRED_MESSAGE);
        }
        if (authHelper.isAdmin(currentUser)) {
            throw new ForbiddenException("管理员请直接编辑文章");
        }
        return ApiResponse.success(postReviewService.publishOwnPost(id, currentUser));
    }

    /**
     * 获取所有投稿审核列表(仅管理员可访问)
     * URL: GET /api/admin/post-reviews
     * 权限: 管理员/站长
     * @return 投稿审核列表
     */
    @GetMapping("/post-reviews")
    public ApiResponse<?> getPostReviews(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam(required = false, defaultValue = "all") String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        if (isForbidden(authorization)) {
            throw new ForbiddenException(ADMIN_FORBIDDEN_MESSAGE);
        }
        return ApiResponse.success(postReviewService.getReviewsForAdmin(status, page, size));
    }

    /**
     * 获取投稿审核详情
     * URL: GET /api/admin/post-reviews/{id}
     * 权限: 登录用户(管理员可看所有,普通用户只能看自己的)
     * 参数: id-审核ID
     * @return 投稿审核详情
     */
    @GetMapping("/post-reviews/{id}")
    public ApiResponse<?> getPostReviewDetail(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long id) {
        User currentUser = authHelper.getCurrentUser(authorization);
        if (currentUser == null) {
            throw new AuthException(LOGIN_REQUIRED_MESSAGE);
        }

        PostReview review = postReviewMapper.selectById(id);
        if (review == null) {
            throw new BusinessException("投稿记录不存在");
        }

        if (!authHelper.isAdmin(currentUser) && !currentUser.getId().equals(review.getUserId())) {
            throw new ForbiddenException("无权查看该投稿");
        }

        return ApiResponse.success(review);
    }

    /**
     * 通过投稿审核(管理员批准投稿并发布文章)
     * URL: PUT /api/admin/post-reviews/{id}/approve
     * 权限: 管理员/站长
     * 参数: id-审核ID
     * @return 审核结果
     */
    @PutMapping("/post-reviews/{id}/approve")
    public ApiResponse<?> approvePostReview(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long id) {
        User currentUser = authHelper.getCurrentUser(authorization);
        if (!authHelper.isAdmin(currentUser)) {
            throw new ForbiddenException(ADMIN_FORBIDDEN_MESSAGE);
        }
        return ApiResponse.success(postReviewService.approveReview(id, currentUser));
    }

    /**
     * 拒绝投稿审核(管理员驳回投稿)
     * URL: PUT /api/admin/post-reviews/{id}/reject
     * 权限: 管理员/站长
     * 参数: id-审核ID, PostReviewDecisionRequest对象(包含reviewNote审核意见)
     * @return 审核结果
     */
    @PutMapping("/post-reviews/{id}/reject")
    public ApiResponse<?> rejectPostReview(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long id,
            @RequestBody PostReviewDecisionRequest request) {
        User currentUser = authHelper.getCurrentUser(authorization);
        if (!authHelper.isAdmin(currentUser)) {
            throw new ForbiddenException(ADMIN_FORBIDDEN_MESSAGE);
        }
        return ApiResponse.success(postReviewService.rejectReview(id, currentUser, request.getReviewNote()));
    }

    @PostMapping("/post-reviews/batch/{operation}")
    public ApiResponse<?> submitPostReviewBatchTask(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable String operation,
            @RequestBody @jakarta.validation.Valid BatchModerationRequest request) {
        User currentUser = authHelper.getCurrentUser(authorization);
        if (!authHelper.isAdmin(currentUser)) {
            throw new ForbiddenException(ADMIN_FORBIDDEN_MESSAGE);
        }
        return ApiResponse.success(moderationTaskService.submitReviewBatch(operation, request, currentUser));
    }

    /**
     * 删除文章(管理员可删除所有,普通用户只能删除自己的)
     * URL: DELETE /api/admin/posts/{id}
     * 权限: 登录用户(管理员/文章作者)
     * 参数: id-文章ID
     * @return 删除结果
     */
    @DeleteMapping("/posts/{id}")
    public ApiResponse<?> deletePost(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long id) {
        User currentUser = authHelper.getCurrentUser(authorization);
        if (currentUser == null) {
            throw new AuthException(LOGIN_REQUIRED_MESSAGE);
        }
        Post existing = postMapper.selectById(id);
        if (existing == null) {
            return ApiResponse.success();
        }
        if (!authHelper.isAdmin(currentUser) && !currentUser.getId().equals(existing.getUserId())) {
            throw new ForbiddenException("无权删除该文章");
        }
        postMapper.deleteById(id);
        postReviewService.deleteReviewsByPostId(id);
        postService.invalidatePublicCaches();
        return ApiResponse.success();
    }

    /**
     * 删除投稿审核稿(用户删除自己创建的审核稿)
     * URL: DELETE /api/admin/post-reviews/{id}
     * 权限: 登录用户(管理员不可删除)
     * 参数: id-审核ID
     * @return 删除结果
     */
    @DeleteMapping("/post-reviews/{id}")
    public ApiResponse<?> deletePostReview(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long id) {
        User currentUser = authHelper.getCurrentUser(authorization);
        if (currentUser == null) {
            throw new AuthException(LOGIN_REQUIRED_MESSAGE);
        }
        if (authHelper.isAdmin(currentUser)) {
            throw new ForbiddenException("管理员不可删除投稿审核稿");
        }
        postReviewService.deleteOwnCreateReview(id, currentUser);
        return ApiResponse.success();
    }

    /**
     * 获取音乐列表(仅站长可访问)
     * URL: GET /api/admin/music
     * 权限: 站长
     * @return 音乐列表(按排序字段升序)
     */
    @GetMapping("/music")
    public ApiResponse<?> getMusic(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        if (isStationMasterForbidden(authorization)) {
            throw new ForbiddenException(MUSIC_FORBIDDEN_MESSAGE);
        }

        Page<Music> pageObj = new Page<>(page, size);
        QueryWrapper<Music> wrapper = new QueryWrapper<>();
        wrapper.orderByAsc("sort").orderByAsc("id");
        return ApiResponse.success(toPageResult(musicMapper.selectPage(pageObj, wrapper)));
    }

    /**
     * 创建音乐条目(仅站长可访问)
     * URL: POST /api/admin/music
     * 权限: 站长
     * 参数: Music对象(包含title, artist, album, url, coverUrl等)
     * @return 创建成功的音乐信息
     */
    @PostMapping("/music")
    public ApiResponse<?> createMusic(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody Music music) {
        if (isStationMasterForbidden(authorization)) {
            throw new ForbiddenException(MUSIC_FORBIDDEN_MESSAGE);
        }

        if (music.getStatus() == null) {
            music.setStatus(1);
        }
        if (music.getSort() == null) {
            music.setSort(0);
        }
        musicMapper.insert(music);
        return ApiResponse.success(music);
    }

    /**
     * 上传音乐文件及封面图片(仅站长可访问)
     * URL: POST /api/admin/music/upload
     * 权限: 站长
     * 参数: audio-音频文件(可选), cover-封面图片(可选)
     * @return 上传结果(包含文件URL)
     */
    @PostMapping("/music/upload")
    public ApiResponse<?> uploadMusicAssets(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam(value = "audio", required = false) MultipartFile audio,
            @RequestParam(value = "cover", required = false) MultipartFile cover) {
        if (isStationMasterForbidden(authorization)) {
            throw new ForbiddenException(MUSIC_FORBIDDEN_MESSAGE);
        }

        try {
            MusicUploadResponse response = musicUploadService.upload(audio, cover);
            return ApiResponse.success(response);
        } catch (java.io.IOException e) {
            throw new BusinessException("音乐文件上传失败", e);
        }
    }

    /**
     * 上传文章配图(登录用户可使用)
     * URL: POST /api/admin/posts/images
     * 权限: 登录用户
     * 参数: images-图片文件列表
     * @return 上传成功后的图片URL列表
     */
    @PostMapping("/posts/images")
    public ApiResponse<?> uploadPostImages(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam("images") List<MultipartFile> images) {
        User currentUser = authHelper.getCurrentUser(authorization);
        if (currentUser == null) {
            throw new AuthException(LOGIN_REQUIRED_MESSAGE);
        }

        try {
            List<String> urls = postImageUploadService.upload(images);
            return ApiResponse.success(Map.of("urls", urls));
        } catch (java.io.IOException e) {
            throw new BusinessException("图片上传失败", e);
        }
    }

    /**
     * 更新音乐信息(仅站长可访问)
     * URL: PUT /api/admin/music/{id}
     * 权限: 站长
     * 参数: id-音乐ID, Music对象-更新的音乐信息
     * 注意: 如果更换了文件URL,会自动删除旧的管理文件
     * @return 更新后的音乐信息
     */
    @PutMapping("/music/{id}")
    public ApiResponse<?> updateMusic(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long id,
            @RequestBody Music music) {
        if (isStationMasterForbidden(authorization)) {
            throw new ForbiddenException(MUSIC_FORBIDDEN_MESSAGE);
        }

        Music existing = musicMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException("音乐不存在");
        }

        if (shouldReplaceManagedFile(existing.getUrl(), music.getUrl())) {
            musicUploadService.deleteManagedFile(existing.getUrl());
        }
        if (shouldReplaceManagedFile(existing.getCoverUrl(), music.getCoverUrl())) {
            musicUploadService.deleteManagedFile(existing.getCoverUrl());
        }

        existing.setTitle(music.getTitle());
        existing.setArtist(music.getArtist());
        existing.setAlbum(music.getAlbum());
        existing.setUrl(music.getUrl());
        existing.setCoverUrl(music.getCoverUrl());
        existing.setDuration(music.getDuration());
        existing.setSort(music.getSort());
        existing.setStatus(music.getStatus());
        musicMapper.updateById(existing);
        return ApiResponse.success(existing);
    }

    /**
     * 删除音乐(仅站长可访问)
     * URL: DELETE /api/admin/music/{id}
     * 权限: 站长
     * 参数: id-音乐ID
     * 注意: 删除音乐时会同时删除关联的管理文件(音频和封面)
     * @return 删除结果
     */
    @DeleteMapping("/music/{id}")
    public ApiResponse<?> deleteMusic(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long id) {
        if (isStationMasterForbidden(authorization)) {
            throw new ForbiddenException(MUSIC_FORBIDDEN_MESSAGE);
        }

        Music existing = musicMapper.selectById(id);
        if (existing != null) {
            musicUploadService.deleteManagedFile(existing.getUrl());
            musicUploadService.deleteManagedFile(existing.getCoverUrl());
        }
        musicMapper.deleteById(id);
        return ApiResponse.success();
    }

    /**
     * 判断是否需要替换已管理的文件(用于判断是否需要删除旧文件)
     */
    private boolean shouldReplaceManagedFile(String existingUrl, String nextUrl) {
        return existingUrl != null && !existingUrl.equals(nextUrl);
    }

    /**
     * 获取友链列表(仅管理员可访问)
     * URL: GET /api/admin/friend-links
     * 权限: 管理员/站长
     * @return 友链列表(按排序字段升序)
     */
    @GetMapping("/friend-links")
    public ApiResponse<?> getFriendLinks(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        if (isForbidden(authorization)) {
            throw new ForbiddenException(ADMIN_FORBIDDEN_MESSAGE);
        }

        Page<FriendLink> pageObj = new Page<>(page, size);
        QueryWrapper<FriendLink> wrapper = new QueryWrapper<>();
        wrapper.orderByAsc("sort").orderByAsc("id");
        return ApiResponse.success(toPageResult(friendLinkMapper.selectPage(pageObj, wrapper)));
    }

    /**
     * 创建友链(仅管理员可访问)
     * URL: POST /api/admin/friend-links
     * 权限: 管理员/站长
     * 参数: FriendLink对象
     * @return 创建的友链信息
     */
    @PostMapping("/friend-links")
    public ApiResponse<?> createFriendLink(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody FriendLink link) {
        if (isForbidden(authorization)) {
            throw new ForbiddenException(ADMIN_FORBIDDEN_MESSAGE);
        }

        friendLinkMapper.insert(link);
        return ApiResponse.success(link);
    }

    /**
     * 更新友链(仅管理员可访问)
     * URL: PUT /api/admin/friend-links/{id}
     * 权限: 管理员/站长
     * 参数: id-友链ID, FriendLink对象-更新的友链信息
     * @return 更新后的友链信息
     */
    @PutMapping("/friend-links/{id}")
    public ApiResponse<?> updateFriendLink(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long id,
            @RequestBody FriendLink link) {
        if (isForbidden(authorization)) {
            throw new ForbiddenException(ADMIN_FORBIDDEN_MESSAGE);
        }

        FriendLink existing = friendLinkMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException("友链不存在");
        }

        existing.setName(link.getName());
        existing.setUrl(link.getUrl());
        existing.setLogo(link.getLogo());
        existing.setDescription(link.getDescription());
        existing.setStatus(link.getStatus());
        existing.setSort(link.getSort());
        friendLinkMapper.updateById(existing);
        return ApiResponse.success(existing);
    }

    /**
     * 删除友链(仅管理员可访问)
     * URL: DELETE /api/admin/friend-links/{id}
     * 权限: 管理员/站长
     * 参数: id-友链ID
     * @return 删除结果
     */
    @DeleteMapping("/friend-links/{id}")
    public ApiResponse<?> deleteFriendLink(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long id) {
        if (isForbidden(authorization)) {
            throw new ForbiddenException(ADMIN_FORBIDDEN_MESSAGE);
        }
        friendLinkMapper.deleteById(id);
        return ApiResponse.success();
    }

    /**
     * 审批通过友链申请(仅管理员可访问)
     * URL: PUT /api/admin/friend-links/{id}/approve
     * 权限: 管理员/站长
     * 参数: id-友链ID
     * @return 更新后的友链信息
     */
    @PutMapping("/friend-links/{id}/approve")
    public ApiResponse<?> approveFriendLink(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long id) {
        if (isForbidden(authorization)) {
            throw new ForbiddenException(ADMIN_FORBIDDEN_MESSAGE);
        }

        FriendLink existing = friendLinkMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException("友链不存在");
        }

        existing.setStatus(1);
        friendLinkMapper.updateById(existing);
        return ApiResponse.success(existing);
    }

    /**
     * 获取留言列表(仅管理员可访问)
     * URL: GET /api/admin/contact-messages
     * 权限: 管理员/站长
     * @return 留言列表(按创建时间倒序)
     */
    @GetMapping("/contact-messages")
    public ApiResponse<?> getContactMessages(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        if (isForbidden(authorization)) {
            throw new ForbiddenException(ADMIN_FORBIDDEN_MESSAGE);
        }

        Page<ContactMessage> pageObj = new Page<>(page, size);
        QueryWrapper<ContactMessage> wrapper = new QueryWrapper<>();
        wrapper.orderByDesc("create_time", "id");
        Page<ContactMessage> messagePage = contactMessageMapper.selectPage(pageObj, wrapper);
        return ApiResponse.success(toPageResult(messagePage, Map.of(
                "total", messagePage.getTotal(),
                "pending", contactMessageMapper.selectCount(new QueryWrapper<ContactMessage>().ne("status", 1)),
                "resolved", contactMessageMapper.selectCount(new QueryWrapper<ContactMessage>().eq("status", 1))
        )));
    }

    /**
     * 更新留言状态(仅管理员可访问)
     * URL: PUT /api/admin/contact-messages/{id}/status
     * 权限: 管理员/站长
     * 参数: id-留言ID, payload对象(包含status字段, 0-未处理, 1-已处理)
     * @return 更新后的留言信息
     */
    @PutMapping("/contact-messages/{id}/status")
    public ApiResponse<?> updateContactMessageStatus(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long id,
            @RequestBody Map<String, Integer> payload) {
        if (isForbidden(authorization)) {
            throw new ForbiddenException(ADMIN_FORBIDDEN_MESSAGE);
        }

        ContactMessage existing = contactMessageMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException("留言不存在");
        }

        Integer status = payload == null ? null : payload.get("status");
        if (status == null || (status != 0 && status != 1)) {
            throw new BusinessException("状态参数不正确");
        }

        existing.setStatus(status);
        existing.setUpdateTime(LocalDateTime.now());
        contactMessageMapper.updateById(existing);
        return ApiResponse.success(existing);
    }

    /**
     * 回复留言(仅管理员可访问)
     * URL: POST /api/admin/contact-messages/{id}/reply
     * 权限: 管理员/站长
     * 参数: id-留言ID, ContactReplyRequest对象(包含replyContent回复内容)
     * @return 回复结果
     */
    @PostMapping("/contact-messages/{id}/reply")
    public ApiResponse<?> replyContactMessage(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long id,
            @RequestBody @jakarta.validation.Valid ContactReplyRequest request) {
        if (isForbidden(authorization)) {
            throw new ForbiddenException(ADMIN_FORBIDDEN_MESSAGE);
        }

        return ApiResponse.success(contactMessageService.replyMessage(id, request.getReplyContent()));
    }

    /**
     * 删除留言(仅管理员可访问)
     * URL: DELETE /api/admin/contact-messages/{id}
     * 权限: 管理员/站长
     * 参数: id-留言ID
     * @return 删除结果
     */
    @DeleteMapping("/contact-messages/{id}")
    public ApiResponse<?> deleteContactMessage(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long id) {
        if (isForbidden(authorization)) {
            throw new ForbiddenException(ADMIN_FORBIDDEN_MESSAGE);
        }

        contactMessageMapper.deleteById(id);
        return ApiResponse.success();
    }

    @GetMapping("/overview")
    public ApiResponse<?> getOverview(@RequestHeader(value = "Authorization", required = false) String authorization) {
        User currentUser = authHelper.getCurrentUser(authorization);
        if (currentUser == null) {
            throw new AuthException(LOGIN_REQUIRED_MESSAGE);
        }

        if (!authHelper.isAdmin(currentUser)) {
            return ApiResponse.success(postReviewService.getWorkspaceSummary(currentUser));
        }

        return ApiResponse.success(Map.of(
                "posts", postMapper.selectCount(null),
                "users", userMapper.selectCount(null),
                "links", friendLinkMapper.selectCount(null),
                "music", isStationMaster(currentUser) ? musicMapper.selectCount(null) : 0L,
                "pendingReviews", postReviewMapper.selectCount(new QueryWrapper<PostReview>().eq("review_status", "pending")),
                "pendingComments", postCommentMapper.selectCount(new QueryWrapper<PostComment>().eq("status", "pending")),
                "pendingContacts", contactMessageMapper.selectCount(new QueryWrapper<ContactMessage>().ne("status", 1))
        ));
    }

    /**
     * 获取网站统计数据(用户数、文章数、音乐数、友链数)
     * URL: GET /api/admin/stats
     * 权限: 管理员/站长
     * @return 统计数据数组 [users, posts, music, links]
     */
    @GetMapping("/stats")
    public ApiResponse<?> getStats(@RequestHeader(value = "Authorization", required = false) String authorization) {
        if (isForbidden(authorization)) {
            throw new ForbiddenException(ADMIN_FORBIDDEN_MESSAGE);
        }

        long users = userMapper.selectCount(null);
        long posts = postMapper.selectCount(null);
        long music = musicMapper.selectCount(null);
        long links = friendLinkMapper.selectCount(null);
        return ApiResponse.success(new Object[]{users, posts, music, links});
    }

    private <T> AdminPageResult<T> toPageResult(Page<T> page) {
        return toPageResult(page, Map.of("total", page.getTotal()));
    }

    private <T> AdminPageResult<T> toPageResult(Page<T> page, Map<String, Long> summary) {
        return AdminPageResult.of(
                page.getRecords(),
                page.getCurrent(),
                page.getSize(),
                page.getTotal(),
                summary
        );
    }
}
