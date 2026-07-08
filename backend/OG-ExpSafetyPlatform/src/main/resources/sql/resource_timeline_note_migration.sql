CREATE TABLE IF NOT EXISTS t_resource_timeline_note (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  resource_id BIGINT NOT NULL COMMENT '资源ID',
  experiment_id BIGINT NULL COMMENT '实验ID',
  user_id BIGINT NOT NULL COMMENT '用户ID',
  position_seconds INT NOT NULL DEFAULT 0 COMMENT '资源时间点秒数',
  note_type VARCHAR(20) NOT NULL DEFAULT 'NOTE' COMMENT 'NOTE/QUESTION/RISK',
  content VARCHAR(1000) NOT NULL COMMENT '笔记或问题内容',
  visibility VARCHAR(20) NOT NULL DEFAULT 'PRIVATE' COMMENT 'PRIVATE/COURSE',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '状态',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  INDEX idx_resource_position (resource_id, position_seconds, deleted),
  INDEX idx_experiment_type (experiment_id, note_type, deleted),
  INDEX idx_user_resource (user_id, resource_id, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='资源时间点笔记与问题';
