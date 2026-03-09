-- Schema aligned with JPA entities (MySQL 5.7+)

CREATE DATABASE IF NOT EXISTS `emoque`
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE `emoque`;

CREATE TABLE IF NOT EXISTS `user_profiles` (
  `id` varchar(255) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `name` varchar(255) NOT NULL,
  `email` varchar(255) NOT NULL,
  `gender` varchar(64) NULL,
  `job` varchar(128) NULL,
  `mbti` varchar(16) NULL,
  `intro` text NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_profiles_email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `user_personality_keywords` (
  `user_id` varchar(255) NOT NULL,
  `idx` int NOT NULL DEFAULT 0,
  `keyword` varchar(255) NULL,
  PRIMARY KEY (`user_id`, `idx`),
  CONSTRAINT `fk_upk_user` FOREIGN KEY (`user_id`) REFERENCES `user_profiles` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `user_emoticon_samples` (
  `user_id` varchar(255) NOT NULL,
  `idx` int NOT NULL DEFAULT 0,
  `url` varchar(1024) NULL,
  PRIMARY KEY (`user_id`, `idx`),
  CONSTRAINT `fk_ues_user` FOREIGN KEY (`user_id`) REFERENCES `user_profiles` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `chat_conversations` (
  `user_id` varchar(255) NOT NULL,
  `imported_at` datetime(6) NULL,
  PRIMARY KEY (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `chat_messages` (
  `user_id` varchar(255) NOT NULL,
  `idx` int NOT NULL DEFAULT 0,
  `message` text NULL,
  PRIMARY KEY (`user_id`, `idx`),
  CONSTRAINT `fk_cm_user` FOREIGN KEY (`user_id`) REFERENCES `chat_conversations` (`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `generation_tasks` (
  `id` varchar(255) NOT NULL,
  `user_id` varchar(255) NOT NULL,
  `status` varchar(32) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `completed_at` datetime(6) NULL,
  `bio` text NULL,
  `failure_reason` text NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `generation_task_emotions` (
  `task_id` varchar(255) NOT NULL,
  `emotion` varchar(255) NOT NULL,
  PRIMARY KEY (`task_id`, `emotion`),
  CONSTRAINT `fk_gte_task` FOREIGN KEY (`task_id`) REFERENCES `generation_tasks` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `generation_task_images` (
  `task_id` varchar(255) NOT NULL,
  `emotion` varchar(255) NOT NULL,
  `image_url` varchar(1024) NULL,
  PRIMARY KEY (`task_id`, `emotion`),
  CONSTRAINT `fk_gti_task` FOREIGN KEY (`task_id`) REFERENCES `generation_tasks` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
