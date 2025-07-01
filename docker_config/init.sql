# 创建数据库并设置字符集
CREATE DATABASE IF NOT EXISTS grid_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE grid_db;

# 创建用user表
CREATE TABLE IF NOT EXISTS `t_user` (
 `user_id` INT NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '用户ID',
 `username` VARCHAR(20) NOT NULL COMMENT '用户名',
 `password` VARCHAR(100) NOT NULL COMMENT '密码',
 `email` VARCHAR(100) NOT NULL COMMENT '邮箱',
 `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
 `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
 `is_deleted` TINYINT(1) DEFAULT 0 COMMENT '是否删除, 0-否, 1-是',
 INDEX idx_user_name (username) COMMENT '用户名索引',
 INDEX idx_email (email) COMMENT '邮箱索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT '用户表';

# 创键worker表
CREATE TABLE IF NOT EXISTS `t_worker` (
 `worker_id` INT NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '维修人员ID',
 `user_id` INT NOT NULL COMMENT '用户ID',
 `name` VARCHAR(20) NOT NULL COMMENT '维修人员姓名',
 `gender` TINYINT(1) NOT NULL COMMENT '维修人员性别, 0-男, 1-女',
 `email` VARCHAR(100) NOT NULL COMMENT '维修人员邮箱',
 `status` TINYINT(1) DEFAULT 0 COMMENT '维修人员状态, 0-空闲, 1-忙碌',
 `version` BIGINT DEFAULT 0 COMMENT '版本号，乐观锁',
 `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
 `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
 `is_deleted` TINYINT(1) DEFAULT 0 COMMENT '是否删除, 0-否, 1-是',
 INDEX idx_user_id (user_id) COMMENT '用户ID索引'
)DEFAULT CHARSET=utf8mb4 COMMENT '维修人员表';

# 创建用device表
CREATE TABLE IF NOT EXISTS `t_device` (
--  `id` INT NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '设备ID',
 `device_id` INT NOT NULL COMMENT '设备唯一标识' PRIMARY KEY,
 `user_id` INT NOT NULL COMMENT '用户ID',
 `api_key` VARCHAR(100) NOT NULL COMMENT 'API密钥',
 `latitude` DECIMAL(10, 6) NOT NULL COMMENT '纬度',
 `longitude` DECIMAL(10, 6) NOT NULL COMMENT '经度',
 `city` VARCHAR(20) NOT NULL COMMENT '城市',
 `full_address` VARCHAR(100) NOT NULL COMMENT '详细地址',
 `description` VARCHAR(255) NOT NULL COMMENT '设备描述',
 `version` BIGINT DEFAULT 0 COMMENT '版本号，乐观锁',
 `status` TINYINT(1) DEFAULT 0 COMMENT '设备状态, 0-在线,1-离线,2-异常',
 `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
 `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
 `is_deleted` TINYINT(1) DEFAULT 0 COMMENT '是否删除, 0-否, 1-是',
 INDEX idx_user_id (user_id) COMMENT '用户ID索引'
)DEFAULT CHARSET=utf8mb4 COMMENT '设备表';

# 创建维修记录表
CREATE TABLE IF NOT EXISTS `t_repair_record` (
 `id` INT(11) NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '维修记录ID',
 `user_id` INT NOT NULL COMMENT '用户ID',
 `device_id` INT NOT NULL COMMENT '设备ID',
 `worker_id` INT NOT NULL COMMENT '维修人员ID',
 `status` TINYINT(1) DEFAULT 0 COMMENT '维修状态, 0-未完成, 1-已完成',
 `description` VARCHAR(255) DEFAULT NULL COMMENT '故障描述',
 `process` VARCHAR(255) DEFAULT NULL COMMENT '维修过程',
 `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
 `finished_at` TIMESTAMP DEFAULT NULL COMMENT '完成时间',
 `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
 `is_deleted` TINYINT(1) DEFAULT 0 COMMENT '是否删除, 0-否, 1-是'
)DEFAULT CHARSET=utf8mb4 COMMENT '维修记录表';

# 消息提醒表
CREATE TABLE IF NOT EXISTS `t_message` (
 `message_id` INT NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '消息ID',
 `user_id` INT NOT NULL COMMENT '用户ID',
 `title` VARCHAR(100) NOT NULL COMMENT '消息标题',
 `type` TINYINT(1) DEFAULT 0 COMMENT '消息类型, 0-通知,2-警告,3-错误',
 `content` VARCHAR(255) NOT NULL COMMENT '消息内容',
 `has_read` TINYINT(1) DEFAULT 0 COMMENT '是否已读, 0-未读, 1-已读',
 `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
 `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
 `is_deleted` TINYINT(1) DEFAULT 0 COMMENT '是否删除, 0-否, 1-是',
 INDEX idx_user_id (user_id) COMMENT '用户ID索引'
)DEFAULT CHARSET=utf8mb4 COMMENT '消息提醒表';

# 初始化数据
INSERT INTO `t_user` ( `username`, `email`, `password`)
VALUES ('fish','dreamfishyx@qq.com','$2a$10$3fYWL/n9Nd0KGjLDmPTx4.pmdUCyOPzCx1A.h0lFZOYnHvPvqnUca');

# 插入多个设备
INSERT INTO `t_device` ( `device_id`, `user_id`, `api_key`, `latitude`, `longitude`, `city`, `full_address`, `status`,`description`)
VALUES
 (1, 1, '1234567890',39.855264,116.437619,  '北京市', '北京市海淀区', 0, '这是一个测试设备'),
 (2, 1, '1234567891', 30.123457, 105.123457, '北京市', '北京市朝阳区', 1, '这是另一个测试设备'),
 (3, 1, '1234567892', 39.123458, 116.123458, '北京市', '北京市丰台区', 2, '这是第三个测试设备'),
 (4, 1, '1234567893', 39.123459, 116.123459, '天津市', '天津市和平区', 2, '这是第四个测试设备'),
 (5, 1, '1234567894', 39.123460, 116.123460, '天津市', '天津市河东区', 0, '这是第五个测试设备'),
 (6, 1, '1234567895', 39.123461, 116.123461, '天津市', '天津市河西区', 1, '这是第六个测试设备'),
 (7, 1, '1234567896', 39.123462, 116.123462, '天津市', '天津市南开区', 2, '这是第七个测试设备'),
 (8, 1, '1234567897', 39.123463, 116.123463, '天津市', '天津市红桥区', 0, '这是第八个测试设备'),
 (9, 1, '1234567898', 39.123464, 116.123464, '天津市', '天津市滨海区', 0, '这是第九个测试设备'),
 (10, 1, '1234567899', 39.123465, 116.123465, '天津市', '天津市东丽区', 1, '这是第十个测试设备'),
 (1001, 1, '1234567800', 39.123466, 116.123466, '天津市', '天津市西青区', 2, '这是第十一个测试设备');

# 插入多个worker
INSERT INTO `t_worker` ( `user_id`, `name`, `gender`, `email`, `status`)
VALUES
 (1, '梦鱼', 0, 'worker1@qq.com', 0),
 (1, 'worker2', 1, 'worker2@qq.com', 0);
--  (1, '梦鱼', 0, 'worker1@qq.com', 0),
--  (1, 'worker2', 1, 'worker2@qq.com', 0),
--  (1, 'worker3', 0, 'worker3@qq.com', 0),
--  (1, 'worker4', 1, 'worker4@qq.com', 0),
--  (1, 'worker5', 0, 'worker5@qq.com', 0),
--  (1, 'worker6', 1, 'worker6@qq.com', 0),
--  (1, 'worker7', 0, 'worker7@qq.com', 0),
--  (1, 'worker8', 1, 'worker8@qq.com', 0),
--  (1, 'worker9', 0, 'worker9@qq.com', 0),
--  (1, 'worker10', 1, 'worker10@qq.com', 0);

# 插入多个维修记录
INSERT INTO `t_repair_record` ( `user_id`, `device_id`, `worker_id`, `status`, `description`, `process`,finished_at)
VALUES
 (1, 1, 1, 0, NULL, NULL, NULL),
 (1, 2, 2, 1, '这是另一个测试维修记录', '这是另一个测试维修过程', '2026-02-01 10:00:00'),
 (1, 3, 3, 0, NULL, NULL,  NULL),
 (1, 5, 5, 1, '这是第四个测试维修记录', '这是第四个测试维修过程', '2026-02-01 10:00:00'),
 (1, 6, 5, 0, NULL, NULL, NULL),
 (1, 6, 6, 1, '这是第六个测试维修记录', '这是第六个测试维修过程', '2026-02-01 10:00:00'),
 (1, 7, 7, 0, NULL, NULL, NULL),
 (1, 8, 8, 1, '这是第八个测试维修记录', '这是第八个测试维修过程', '2026-02-01 10:00:00'),
 (1, 9, 9, 0, NULL, NULL, NULL),
 (1, 9, 9, 1, '这是第十个测试维修记录', '这是第十个测试维修过程', '2026-02-01 10:00:00');

# 插入多个消息提醒
INSERT INTO `t_message` ( `user_id`, `title`, `content`, `type`, `created_at`)
VALUES
 (1, '这是第一个测试消息', '这是第一个测试消息内容', 0, '2026-02-01 10:00:00'),
 (1, '这是第二个测试消息', '这是第二个测试消息内容', 1, '2026-02-01 10:00:00'),
 (1, '这是第三个测试消息', '这是第三个测试消息内容', 2, '2026-02-01 10:00:00'),
 (1, '这是第四个测试消息', '这是第四个测试消息内容', 1, '2026-02-01 10:00:00');