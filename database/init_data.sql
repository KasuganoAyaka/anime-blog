-- Anime Blog seed data
USE `anime_blog`;

INSERT INTO `site_stats` (`stat_key`, `stat_value`, `description`)
VALUES
    ('total_users', 0, 'Total registered users'),
    ('active_users', 0, 'Active users'),
    ('total_posts', 0, 'Total published posts'),
    ('today_visitors', 0, 'Visitors today')
ON DUPLICATE KEY UPDATE
    `description` = VALUES(`description`);

-- Default admin account for a fresh database only.
-- Username: admin
-- Password: admin123
INSERT INTO `user` (
    `username`,
    `password`,
    `email`,
    `email_verified`,
    `nickname`,
    `role`,
    `status`
)
SELECT
    'admin',
    '$2a$10$aOU2l53iNaDoTup6AX9ynuITO7Xm0Q26AYUJ4.PcLT5i/2TlUxoBe',
    'blog@ayakacloud.cn',
    1,
    CONVERT(0xE7AB99E995BF USING utf8mb4),
    'admin',
    1
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1
    FROM `user`
    WHERE `username` = 'admin'
);

UPDATE `user`
SET `nickname` = CONVERT(0xE7AB99E995BF USING utf8mb4),
    `role` = 'admin',
    `status` = 1
WHERE `username` = 'admin';

UPDATE `user`
SET `role` = 'manager'
WHERE `role` = 'admin'
  AND `username` <> 'admin';

INSERT INTO `friend_link` (
    `name`,
    `url`,
    `logo`,
    `description`,
    `status`,
    `sort`
)
SELECT
    'GitHub',
    'https://github.com',
    'https://github.githubassets.com/favicons/favicon.svg',
    'Code hosting platform',
    1,
    1
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1
    FROM `friend_link`
    WHERE `url` = 'https://github.com'
);

INSERT INTO `friend_link` (
    `name`,
    `url`,
    `logo`,
    `description`,
    `status`,
    `sort`
)
SELECT
    'Bilibili',
    'https://www.bilibili.com',
    'https://www.bilibili.com/favicon.ico',
    'Video community',
    1,
    2
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1
    FROM `friend_link`
    WHERE `url` = 'https://www.bilibili.com'
);

INSERT INTO `post` (
    `title`,
    `slug`,
    `content`,
    `summary`,
    `excerpt`,
    `category`,
    `tags`,
    `user_id`,
    `status`,
    `view_count`,
    `like_count`,
    `comment_count`,
    `is_featured`
)
SELECT
    'Welcome to Ayaka Blog',
    'welcome',
    '<p>Welcome to Ayaka Blog. This is the first post created by the initialization script.</p>',
    'Welcome post',
    'Welcome to Ayaka Blog.',
    'Default',
    'welcome,intro',
    `id`,
    'published',
    0,
    0,
    0,
    1
FROM `user`
WHERE `username` = 'admin'
  AND NOT EXISTS (
      SELECT 1
      FROM `post`
      WHERE `slug` = 'welcome'
  );
