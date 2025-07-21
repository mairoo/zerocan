/*
 Navicat Premium Data Transfer

 Source Server         : 신규-ZeroCan
 Source Server Type    : MariaDB
 Source Server Version : 110402 (11.4.2-MariaDB-ubu2404)
 Source Host           : 218.145.71.213:3306
 Source Schema         : zerocan

 Target Server Type    : MariaDB
 Target Server Version : 110402 (11.4.2-MariaDB-ubu2404)
 File Encoding         : 65001

 Date: 21/07/2025 09:05:18
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for audit_log
-- ----------------------------
DROP TABLE IF EXISTS `audit_log`;
CREATE TABLE `audit_log` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `created_at` datetime NOT NULL,
  `modified_at` datetime NOT NULL,
  `created_by` bigint(20) DEFAULT NULL,
  `origin` text NOT NULL,
  `changed` text DEFAULT NULL,
  `type` varchar(32) NOT NULL,
  `entity_type` varchar(64) NOT NULL,
  `entity_id` varchar(64) NOT NULL,
  `modified_by` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  KEY `ix_audit_log_created` (`created_at`) USING BTREE,
  KEY `ix_audit_log_created_by` (`created_by`) USING BTREE,
  KEY `ix_audit_log_type` (`type`) USING BTREE,
  KEY `ix_audit_log_aggregate` (`entity_type`,`entity_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------
-- Table structure for auth_user
-- ----------------------------
DROP TABLE IF EXISTS `auth_user`;
CREATE TABLE `auth_user` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `created_at` datetime NOT NULL,
  `modified_at` datetime NOT NULL,
  `is_removed` tinyint(1) NOT NULL,
  `keycloak_id` varchar(255) NOT NULL,
  `is_active` tinyint(1) NOT NULL,
  `name` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `email` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_auth_user_email` (`email`) USING BTREE,
  UNIQUE KEY `ix_auth_user_keycloak_id` (`keycloak_id`) USING BTREE,
  KEY `ix_auth_user_active_email` (`is_active`,`email`) USING BTREE,
  KEY `ix_auth_user_active_username` (`is_active`) USING BTREE,
  KEY `ix_auth_user_name` (`name`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ----------------------------
-- Table structure for auth_user_role
-- ----------------------------
DROP TABLE IF EXISTS `auth_user_role`;
CREATE TABLE `auth_user_role` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `created_at` datetime NOT NULL,
  `modified_at` datetime NOT NULL,
  `is_removed` tinyint(1) NOT NULL,
  `user_id` bigint(20) NOT NULL,
  `role` varchar(32) NOT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_auth_user_role_all` (`user_id`,`role`,`is_removed`) USING BTREE,
  KEY `ix_auth_user_role_role` (`role`) USING BTREE,
  KEY `ix_auth_user_role_user_id` (`user_id`) USING BTREE,
  KEY `ix_auth_user_role_user_role` (`user_id`,`role`) USING BTREE,
  CONSTRAINT `fk_auth_user_role_user` FOREIGN KEY (`user_id`) REFERENCES `auth_user` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SET FOREIGN_KEY_CHECKS = 1;
