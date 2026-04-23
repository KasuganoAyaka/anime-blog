package com.animeblog.util;

import java.util.regex.Pattern;

/**
 * 输入验证工具类
 * 提供用户名和密码的格式验证功能,确保用户输入符合安全规范
 * 使用final修饰且私有化构造函数,防止实例化
 */
public final class InputValidationUtil {

    /** 用户名最小长度 */
    public static final int USERNAME_MIN_LENGTH = 3;
    /** 用户名最大长度 */
    public static final int USERNAME_MAX_LENGTH = 12;
    /** 密码最小长度 */
    public static final int PASSWORD_MIN_LENGTH = 8;
    /** 密码最大长度 */
    public static final int PASSWORD_MAX_LENGTH = 20;
    /** 密码规则错误提示信息 */
    public static final String PASSWORD_RULE_MESSAGE = "密码需为 8-20 位，仅支持英文、数字和常见符号，且字母、数字、特殊字符至少满足两类";

    /** 英文字母匹配模式 */
    private static final Pattern LETTER_PATTERN = Pattern.compile("[A-Za-z]");
    /** 数字匹配模式 */
    private static final Pattern DIGIT_PATTERN = Pattern.compile("\\d");
    /** 特殊字符匹配模式(包含常见符号) */
    private static final Pattern SPECIAL_PATTERN = Pattern.compile("[~!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?]");
    /** 密码允许字符范围匹配模式(仅允许字母、数字和特殊符号) */
    private static final Pattern PASSWORD_ALLOWED_PATTERN = Pattern.compile("^[A-Za-z\\d~!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?]+$");

    /** 私有构造函数,防止实例化工具类 */
    private InputValidationUtil() {
    }

    /**
     * 验证用户名格式是否合法
     * 规则:去除首尾空格后长度在3-12个字符之间
     *
     * @param username 待验证的用户名
     * @return 如果用户名非null且长度符合规则则返回true,否则返回false
     */
    public static boolean isUsernameValid(String username) {
        if (username == null) {
            return false;
        }

        // 去除首尾空格后计算长度
        String trimmedUsername = username.trim();
        int length = trimmedUsername.length();
        return length >= USERNAME_MIN_LENGTH && length <= USERNAME_MAX_LENGTH;
    }

    /**
     * 验证密码格式是否合法
     * 规则:
     * 1. 长度在8-20个字符之间
     * 2. 仅允许英文字母、数字和常见特殊符号
     * 3. 字母、数字、特殊字符三类中至少包含两类
     *
     * @param password 待验证的密码
     * @return 如果密码符合所有规则则返回true,否则返回false
     */
    public static boolean isPasswordValid(String password) {
        if (password == null) {
            return false;
        }

        // 检查密码长度是否在8-20位之间
        int length = password.length();
        if (length < PASSWORD_MIN_LENGTH || length > PASSWORD_MAX_LENGTH) {
            return false;
        }
        // 检查是否仅包含允许的字符(字母、数字、特殊符号)
        if (!PASSWORD_ALLOWED_PATTERN.matcher(password).matches()) {
            return false;
        }

        // 统计密码中包含的字符类型数量(字母、数字、特殊字符)
        int categoryCount = 0;
        if (LETTER_PATTERN.matcher(password).find()) {
            categoryCount++;
        }
        if (DIGIT_PATTERN.matcher(password).find()) {
            categoryCount++;
        }
        if (SPECIAL_PATTERN.matcher(password).find()) {
            categoryCount++;
        }

        // 至少包含两种字符类型才认为合法
        return categoryCount >= 2;
    }
}
