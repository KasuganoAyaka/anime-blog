package com.animeblog.controller;

import com.animeblog.dto.ApiResponse;
import com.animeblog.entity.EmailCode;
import com.animeblog.entity.User;
import com.animeblog.mapper.EmailCodeMapper;
import com.animeblog.mapper.UserMapper;
import com.animeblog.service.PostImageUploadService;
import com.animeblog.util.AuthHelper;
import com.animeblog.util.InputValidationUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/admin/profile")
public class ProfileController {

    private static final Pattern URL_PATTERN = Pattern.compile(
            "^https?://[a-zA-Z0-9._~:/?#\\[\\]@!$&'()*+,;=%-]+$"
    );

    private static final Pattern DATA_IMAGE_PATTERN = Pattern.compile(
            "^data:image/(png|jpeg|jpg|webp|gif);base64,[A-Za-z0-9+/=\\r\\n]+$"
    );

    private static final int MAX_AVATAR_LENGTH = 60000;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private EmailCodeMapper emailCodeMapper;

    @Autowired
    private AuthHelper authHelper;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Autowired
    private PostImageUploadService postImageUploadService;

    private User getCurrentUser(String authorization) {
        return authHelper.getCurrentUser(authorization);
    }

    private User sanitizeUser(User user) {
        if (user != null) {
            user.setPassword(null);
        }
        return user;
    }

    @GetMapping
    public ApiResponse<?> getProfile(@RequestHeader(value = "Authorization", required = false) String authorization) {
        User currentUser = getCurrentUser(authorization);
        if (currentUser == null) {
            return ApiResponse.unauthorized("未登录");
        }
        return ApiResponse.success(sanitizeUser(currentUser));
    }

    @PutMapping("/avatar")
    public ApiResponse<?> updateAvatar(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody Map<String, String> body) {
        User currentUser = getCurrentUser(authorization);
        if (currentUser == null) {
            return ApiResponse.unauthorized("未登录，请重新登录");
        }

        String avatar = body.get("avatar");
        if (avatar == null || avatar.isBlank()) {
            return ApiResponse.error("头像地址不能为空");
        }
        if (avatar.length() > MAX_AVATAR_LENGTH) {
            return ApiResponse.error("头像图片过大，请换一张更小的图片");
        }
        if (!URL_PATTERN.matcher(avatar).matches() && !DATA_IMAGE_PATTERN.matcher(avatar).matches()) {
            return ApiResponse.error("头像地址格式不正确，仅支持 http/https 或本地上传图片");
        }

        if (shouldReplaceManagedAvatar(currentUser.getAvatar(), avatar)) {
            postImageUploadService.deleteManagedFile(currentUser.getAvatar());
        }
        currentUser.setAvatar(avatar);
        currentUser.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(currentUser);
        return ApiResponse.success("头像更新成功");
    }

    @PostMapping("/avatar/upload")
    public ApiResponse<?> uploadAvatar(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam("avatar") MultipartFile avatarFile) {
        User currentUser = getCurrentUser(authorization);
        if (currentUser == null) {
            return ApiResponse.unauthorized("未登录，请重新登录");
        }

        try {
            String avatarUrl = postImageUploadService.uploadAvatar(avatarFile);
            if (shouldReplaceManagedAvatar(currentUser.getAvatar(), avatarUrl)) {
                postImageUploadService.deleteManagedFile(currentUser.getAvatar());
            }
            currentUser.setAvatar(avatarUrl);
            currentUser.setUpdateTime(LocalDateTime.now());
            userMapper.updateById(currentUser);
            return ApiResponse.success(Map.of("url", avatarUrl));
        } catch (IOException | IllegalArgumentException e) {
            return ApiResponse.error(e.getMessage() == null ? "头像上传失败" : e.getMessage());
        }
    }

    @PutMapping("/basic")
    public ApiResponse<?> updateBasic(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody Map<String, String> body) {
        User currentUser = getCurrentUser(authorization);
        if (currentUser == null) {
            return ApiResponse.unauthorized("未登录，请重新登录");
        }

        currentUser.setNickname(body.getOrDefault("nickname", currentUser.getNickname()));
        currentUser.setBio(body.getOrDefault("bio", currentUser.getBio()));
        currentUser.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(currentUser);
        return ApiResponse.success("保存成功");
    }

    @PutMapping("/nickname")
    public ApiResponse<?> updateNickname(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody Map<String, String> body) {
        User currentUser = getCurrentUser(authorization);
        if (currentUser == null) {
            return ApiResponse.unauthorized("未登录，请重新登录");
        }

        String nickname = body.get("nickname");
        if (nickname == null || nickname.trim().isEmpty()) {
            return ApiResponse.error("昵称不能为空");
        }

        currentUser.setNickname(nickname.trim());
        currentUser.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(currentUser);
        return ApiResponse.success("昵称更新成功");
    }

    @PutMapping("/bio")
    public ApiResponse<?> updateBio(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody Map<String, String> body) {
        User currentUser = getCurrentUser(authorization);
        if (currentUser == null) {
            return ApiResponse.unauthorized("未登录，请重新登录");
        }

        currentUser.setBio(body.get("bio"));
        currentUser.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(currentUser);
        return ApiResponse.success("简介更新成功");
    }

    @PutMapping("/email")
    public ApiResponse<?> changeEmail(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody Map<String, String> body) {
        User currentUser = getCurrentUser(authorization);
        if (currentUser == null) {
            return ApiResponse.unauthorized("未登录，请重新登录");
        }

        String newEmail = body.get("newEmail");
        String code = body.get("code");
        if (newEmail == null || newEmail.trim().isEmpty()) {
            return ApiResponse.error("邮箱不能为空");
        }
        if (code == null || code.trim().isEmpty()) {
            return ApiResponse.error("验证码不能为空");
        }

        User existingUser = userMapper.selectByEmail(newEmail);
        if (existingUser != null && !existingUser.getId().equals(currentUser.getId())) {
            return ApiResponse.error("该邮箱已被其他用户使用");
        }

        EmailCode emailCode = emailCodeMapper.selectLatestCode(newEmail, "change_email");
        if (emailCode == null || !emailCode.getCode().equals(code)) {
            return ApiResponse.error("验证码错误");
        }

        currentUser.setEmail(newEmail);
        currentUser.setEmailVerified(1);
        currentUser.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(currentUser);

        emailCode.setUsed(1);
        emailCodeMapper.updateById(emailCode);
        return ApiResponse.success("邮箱换绑成功");
    }

    @PutMapping("/password")
    public ApiResponse<?> changePassword(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody Map<String, String> body) {
        User currentUser = getCurrentUser(authorization);
        if (currentUser == null) {
            return ApiResponse.unauthorized("未登录，请重新登录");
        }

        String oldPassword = body.get("oldPassword");
        String newPassword = body.get("newPassword");
        if (oldPassword == null || oldPassword.isBlank()) {
            return ApiResponse.error("请输入当前密码");
        }
        if (newPassword == null || newPassword.isBlank()) {
            return ApiResponse.error("请输入新密码");
        }
        if (!InputValidationUtil.isPasswordValid(newPassword)) {
            return ApiResponse.error("密码需为 8-20 位，且字母、数字、特殊字符至少满足两类");
        }
        if (!passwordEncoder.matches(oldPassword, currentUser.getPassword())) {
            return ApiResponse.error("当前密码错误");
        }

        currentUser.setPassword(passwordEncoder.encode(newPassword));
        currentUser.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(currentUser);
        return ApiResponse.success("密码修改成功");
    }

    @DeleteMapping
    public ApiResponse<?> deleteAccount(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        User currentUser = getCurrentUser(authorization);
        if (currentUser == null) {
            return ApiResponse.unauthorized("未登录，请重新登录");
        }
        if ("admin".equals(currentUser.getRole())) {
            return ApiResponse.error("管理员账户不能注销");
        }

        if (currentUser.getAvatar() != null && currentUser.getAvatar().startsWith("/uploads/")) {
            postImageUploadService.deleteManagedFile(currentUser.getAvatar());
        }
        userMapper.deleteById(currentUser.getId());
        return ApiResponse.success("账号已注销");
    }

    private boolean shouldReplaceManagedAvatar(String existingAvatar, String nextAvatar) {
        return existingAvatar != null
                && existingAvatar.startsWith("/uploads/")
                && !existingAvatar.equals(nextAvatar);
    }
}
