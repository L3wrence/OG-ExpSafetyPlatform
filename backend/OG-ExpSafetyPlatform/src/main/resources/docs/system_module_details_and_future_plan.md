# 油气工程实验教学与安全考核平台后端系统说明

## 1. 系统定位

本后端面向油气工程实验教学与安全考核场景，提供统一的用户权限、实验教学、安全考试、实验预约、实验报告、推荐与 AI 辅助、数据统计能力。系统采用 Spring Boot + MyBatis-Plus + MySQL 实现，所有业务接口统一使用 `/api/**` 前缀、`Authorization` Token 鉴权、`@RequirePermission` 权限控制和 `Result<T>` 响应结构。

## 2. 整体架构

- 接入层：Controller 负责参数接收、DTO 校验和权限注解。
- 业务层：Service 负责业务规则、状态流转、记录归属校验和事务控制。
- 数据层：Mapper 继承 MyBatis-Plus BaseMapper，少量统计查询使用自定义 Mapper。
- 权限层：`LoginInterceptor` 解析 Token，写入 `UserContext`；`PermissionAspect` 校验接口权限码。
- 数据库层：脚本位于 `src/main/resources/sql`，推荐按用户权限、实验教学、考试预约报告 AI 的顺序初始化。

## 3. 功能模块说明

### 3.1 用户与权限管理

提供用户、角色、权限、登录 Token 和菜单权限能力。管理员可管理用户、角色权限分配；教师、学生、实验室管理员通过角色获得各自业务权限。其他业务模块全部沿用该权限体系，不再维护独立用户上下文。

核心表：`t_user`、`t_role`、`t_permission`、`t_user_role`、`t_role_permission`、`t_token`。

核心接口：`/api/auth/login`、`/api/users`、`/api/roles`、`/api/permissions/tree`。

### 3.2 实验教学管理

提供课程、实验项目、实验步骤、教学资源、安全知识和学习记录。教师负责课程与实验内容维护，学生查看开放课程、学习资源并更新学习进度，统计模块可基于学习记录生成完成率与活跃趋势。

核心表：`t_lab_course`、`t_experiment`、`t_experiment_step`、`t_resource`、`t_safety_knowledge`、`t_learning_record`。

核心接口：`/api/courses`、`/api/experiments`、`/api/resources`、`/api/safety-knowledge`、`/api/learning-records`。

### 3.3 安全考试

提供题库维护、试卷组卷、发布考试、学生考试、自动评分、短答题批改、错题本和考试统计。学生可参加已发布且未参加过的考试；系统在开始考试时创建记录，提交时批改客观题并写入答题明细；教师可对短答题补充评分。

核心表：`t_question`、`t_exam_paper`、`t_exam_paper_question`、`t_exam_record`、`t_exam_answer`。

核心接口：`/api/questions`、`/api/exams/papers`、`/api/exams`、`/api/exams/statistics`。

### 3.4 实验预约

提供实验室时间段维护、可预约时段查询、预约申请、取消和审核。预约前会校验学生是否通过对应实验安全考试；预约成功后占用时间段名额；拒绝或取消会释放名额。

核心表：`t_lab_time_slot`、`t_reservation`。

核心接口：`/api/reservations/time-slots`、`/api/reservations/available-slots`、`/api/reservations/my`、`/api/reservations/pending`。

### 3.5 实验报告

提供报告草稿、修改、提交、教师评分、退回和最新评分查询。学生只能操作自己的报告；教师只能处理已提交报告；评分历史通过最新标记保留。

核心表：`t_report`、`t_report_score`。

核心接口：`/api/reports`、`/api/reports/my`、`/api/reports/pending`、`/api/reports/{id}/grade`、`/api/reports/{id}/return`。

### 3.6 推荐与 AI 辅助

推荐模块基于实验匹配、错题相关度、学习新鲜度、资源热度、难度匹配生成资源推荐，并记录推荐结果。AI 辅助模块基于本地题库检索和模板回答，为安全问答、错题解释、报告改进提供参考，并保存问答和人工修订。

核心表：`t_recommend_record`、`t_ai_chat_record`。

核心接口：`/api/recommendations/resources`、`/api/ai/ask`、`/api/ai/records`。

### 3.7 数据统计

提供课程完成率、资源分布与热度、考试通过率、错题知识点、预约趋势、容量利用率、报告分数分布和学习活跃趋势。统计范围由角色控制：管理员全量，教师查看自己负责课程，学生查看个人数据，实验室管理员查看运行统计。

核心接口：`/api/dashboard/**`。

## 4. 本轮检查与优化结果

- 补齐考试、题库、试卷、预约、报告、推荐、AI 接口的权限注解。
- 增加考试记录、预约记录、报告记录、AI 问答记录的本人归属校验。
- 修复学生重复开考、预约字段不一致、预约审核状态未限制、时间段删除未保护等流程问题。
- 修复推荐理由和业务异常中的不可读文案。
- 修复数据统计考试通过率字段映射，统一使用 `t_exam_record.total_score`。
- 错题知识点统计已关联题库 `t_question.knowledge_point`，安全考试结果可直接进入数据统计看板。
- 数据库脚本改为清晰中文权限名称，并和接口权限码保持一致。
- 数据库连接配置改为环境变量可覆盖，保留本地默认值。
- 补充整合说明、模块检查和本说明文档。

## 5. 最终整合确认

- 用户与权限管理：所有业务接口统一通过 `Authorization` Token、`UserContext` 和 `@RequirePermission` 控制访问。
- 实验教学：课程、实验、资源、安全知识、学习记录为考试、预约、报告、推荐和统计提供基础数据。
- 安全考试：考试记录写入实验维度和题目维度，结果可用于预约资格校验、错题本、推荐和统计。
- 实验预约：预约绑定时间段、实验室、实验项目，并依赖对应实验安全考试通过记录。
- 实验报告：报告绑定学生和实验项目，评分结果进入报告统计。
- 推荐与 AI：推荐使用学习记录、错题和实验资源；AI 问答记录保留人工修订，不替代教师审核。
- 数据统计：看板已覆盖教学、学习、考试、预约、报告、推荐行为数据。
- 验证结果：`mvn -q -DskipTests compile` 和 `mvn -q test` 均已通过。

## 6. 后续可新增功能建议

- OpenAPI/Swagger：生成接口文档，便于前后端联调和验收。
- 文件上传：支持报告附件、实验图片、资源文件统一上传、预览和权限访问。
- 考试增强：随机组卷、题目乱序、限时防重复提交、考试恢复、主观题评分标准。
- 预约增强：设备维度预约、预约签到、爽约记录、预约日历视图、容量预警。
- 报告增强：报告模板、查重、评分 Rubric、批注、重新提交版本对比。
- AI 增强：接入正式大模型服务、知识库向量检索、教师审核 AI 建议、敏感操作拦截。
- 推荐增强：记录点击/完成反馈，形成可迭代推荐模型。
- 统计增强：教师工作台、学生风险预警、实验室利用率报表、导出 Excel。
- 工程增强：单元测试、集成测试、CI、日志审计、操作追踪、生产配置分离。
