SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
--  Table structure for `robot_config`
-- ----------------------------
DROP TABLE IF EXISTS `robot_config`;
CREATE TABLE `robot_config` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL DEFAULT '' COMMENT '机器人名称',
  `symbol` varchar(255) NOT NULL DEFAULT '' COMMENT '市场',
  `user_id` int(11) NOT NULL COMMENT '刷量账号',
  `max_price` decimal(25,8) NOT NULL COMMENT '委托最高价',
  `min_price` decimal(25,8) NOT NULL COMMENT '委托最低价',
  `max_num` decimal(25,8) NOT NULL COMMENT '委托最大数量',
  `min_num` decimal(25,8) NOT NULL COMMENT '委托最小数量',
  `frequence` bigint(20) NOT NULL COMMENT '频率（毫秒）',
  `status` int(2) NOT NULL DEFAULT '0' COMMENT '状态：0-停止；1-启动',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8 COMMENT='机器人配置';

-- ----------------------------
--  Table structure for `user`
-- ----------------------------
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `account` varchar(255) NOT NULL DEFAULT '' COMMENT '用户账号',
  `pwd` varchar(255) DEFAULT '' COMMENT '密码',
  `trans_pwd` varchar(255) DEFAULT '' COMMENT '交易密码',
  `access_key` varchar(255) NOT NULL,
  `secret_key` varchar(255) NOT NULL,
  `create_time` datetime NOT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8 COMMENT='用户信息';

SET FOREIGN_KEY_CHECKS = 1;
