# 后端系统检查、功能优化与模块说明

## 1. 检查结论

当前后端已经整合为一个 Spring Boot 应用，核心包名为 `com.cupk`，统一使用 MyBatis-Plus 访问 MySQL，统一返回结构为 `Result<T>` 和 `PageResult<T>`。系统功能可以分为三层：

- 基础支撑层：统一异常、分页返回、自动填充、登录拦截、权限切面、当前用户上下文。
- 用户与权限管理层：登录、Token、用户、角色、权限、菜单和按钮权限。
- 实验教学与数据统计层：课程、实验、实验步骤、教学资源、学习记录、安全知识、Dashboard 聚合统计。

本次检查后实施了四项低风险优化：

- 用户分页列表的角色信息改为批量装配，减少用户管理页的重复查询。
- 用户分页参数在服务层兜底限制，避免异常页码或超大分页影响查询。
- 学习进度统计去除必修资源的重复查询，计算逻辑保持不变。
- Dashboard 统计统一校验日期范围，并在服务层限制 `limit` 为 1 到 50。
- 全局未知异常改为服务端记录日志、前端返回通用错误文案，避免泄露内部异常细节。

## 2. 公共基础模块

### 2.1 统一返回

涉及文件：

- `com.cupk.common.Result`
- `com.cupk.common.PageResult`

说明：

- 普通接口返回 `Result<T>`，包含 `code`、`message`、`data`。
- 分页接口返回 `PageResult<T>`，包含 `records`、`total`、`pageNum`、`pageSize`。
- 业务成功统一返回 `code=200`，业务失败由 `BusinessException` 指定错误码。

### 2.2 统一异常

涉及文件：

- `com.cupk.exception.BusinessException`
- `com.cupk.exception.GlobalExceptionHandler`

说明：

- `BusinessException` 用于主动抛出业务错误，例如无权限、数据不存在、状态非法。
- 参数校验异常统一返回 `400`。
- 未知异常记录服务端日志，前端只返回“系统内部异常，请稍后重试”。

后续建议：

- 为不同异常增加统一错误码枚举，例如 `USER_NOT_FOUND`、`COURSE_CODE_EXISTS`。
- 接入日志链路字段，例如 `traceId`，方便定位线上问题。

### 2.3 登录拦截与权限切面

涉及文件：

- `com.cupk.interceptor.LoginInterceptor`
- `com.cupk.interceptor.PermissionAspect`
- `com.cupk.interceptor.UserContext`
- `com.cupk.common.RequirePermission`

说明：

- 除 `/api/auth/login` 外，所有 `/api/**` 接口都需要请求头 `Authorization`。
- 登录拦截器从 `t_token` 校验 Token，再读取用户角色和按钮权限写入 `UserContext`。
- Controller 方法通过 `@RequirePermission("permission:code")` 声明接口权限。
- 权限切面从 `UserContext.permissions()` 校验当前用户是否拥有对应权限码。

后续建议：

- 增加退出登录接口，主动删除当前 Token。
- 增加 Token 刷新或过期续期策略。
- 如果并发访问量提升，可把用户权限缓存到 Redis，并在角色权限变更后主动失效。

## 3. 用户与权限管理模块

### 3.1 登录认证

主要接口：

- `POST /api/auth/login`

核心流程：

1. 根据 `username` 查询 `t_user`。
2. 使用与用户权限子系统一致的 MD5 方式校验密码。
3. 校验用户启用状态。
4. 写入 `t_token` 并返回 Token。
5. 返回用户信息、菜单树和按钮权限码列表。

返回内容：

- `token`：后续接口请求头认证凭据。
- `userInfo`：当前用户基础信息和角色码。
- `menus`：当前用户拥有的菜单权限树。
- `permissions`：当前用户拥有的按钮/接口权限码。

### 3.2 用户管理

主要接口：

- `GET /api/users`
- `POST /api/users`
- `PUT /api/users`
- `DELETE /api/users`
- `DELETE /api/users/{id}`

权限要求：

- `user:view`
- `user:create`
- `user:update`
- `user:delete`

功能说明：

- 支持按用户名和真实姓名关键字分页查询。
- 新增用户时校验用户名唯一性。
- 用户密码保存为 MD5 摘要，保持与用户权限管理子系统一致。
- 用户列表和登录返回均使用 `UserInfoVO`，不会返回密码字段。
- 删除用户时同步删除用户角色关系和 Token。

本次优化：

- 用户分页列表批量查询角色关系和角色码，避免每个用户单独查询角色。
- `pageNum` 最小兜底为 1，`pageSize` 限制在 1 到 100。

后续建议：

- 增加重置密码、修改本人密码、批量启用/停用用户。
- 增加防止删除当前登录用户或最后一个管理员的保护。
- 增加用户导入导出，便于批量维护学生和教师账号。

### 3.3 角色与权限

主要接口：

- `GET /api/roles`
- `GET /api/roles/{roleId}/permissions`
- `PUT /api/roles/{roleId}/permissions`
- `GET /api/permissions/tree`

权限要求：

- `role:view`
- `role:permission:update`
- `permission:view`

功能说明：

- 角色来自 `t_role`，通过 `role_code` 标识 `ADMIN`、`TEACHER`、`STUDENT`、`LAB_ADMIN`。
- 权限来自 `t_permission`，`type=1` 表示菜单，`type=2` 表示按钮或接口权限。
- 角色权限关系由 `t_role_permission` 维护。
- 用户角色关系由 `t_user_role` 维护。

后续建议：

- 增加角色新增、编辑、停用和删除。
- 增加权限变更审计，记录谁在什么时间调整了角色权限。
- 增加菜单权限和按钮权限的前端路由联动说明。

## 4. 实验教学模块

### 4.1 课程管理

主要接口：

- `GET /api/courses`
- `GET /api/courses/{id}`
- `POST /api/courses`
- `PUT /api/courses/{id}`
- `DELETE /api/courses/{id}`
- `PUT /api/courses/{id}/status`

权限要求：

- `course:view`
- `course:create`
- `course:update`
- `course:delete`

核心数据表：

- `t_lab_course`
- `t_course_student`

功能说明：

- 课程包含课程编号、名称、方向、负责教师、学期、封面、简介、状态和排序。
- 创建和修改课程时会校验负责教师存在、启用，并拥有 `TEACHER` 角色。
- 课程编号全局唯一。
- 教师只能创建和维护自己负责的课程。
- 管理员可以维护全部课程。
- 学生查询课程时只能看到开放状态的课程。
- 删除课程前会检查课程下是否存在实验项目；存在实验时拒绝删除。

后续建议：

- 增加课程选课/退课/批量导入学生接口。
- 课程列表中的实验数量、资源数量、平均进度可以进一步改为批量聚合 SQL，减少课程列表统计查询。
- 增加课程归档状态，用于保留历史数据但不再开放教学。

### 4.2 实验项目管理

主要接口：

- `GET /api/experiments`
- `GET /api/experiments/{id}`
- `GET /api/experiments/{id}/overview`
- `POST /api/experiments`
- `PUT /api/experiments/{id}`
- `DELETE /api/experiments/{id}`
- `PUT /api/experiments/{id}/status`
- `POST /api/experiments/{id}/steps`

权限要求：

- `experiment:view`
- `experiment:create`
- `experiment:update`
- `experiment:delete`

核心数据表：

- `t_experiment`
- `t_experiment_step`

功能说明：

- 实验归属于课程，包含实验编号、名称、目标、原理、设备、风险等级、安全考试通过分、是否允许预约、状态和排序。
- 同一课程下实验编号不能重复。
- 风险等级限制为 `LOW`、`MEDIUM`、`HIGH`。
- 教师只能维护自己课程下的实验。
- 学生只能查看开放课程下开放状态的实验。
- 实验详情会返回实验步骤、教学资源、安全知识、当前学生学习进度、考试通过状态和是否允许预约。
- 保存实验步骤时会校验步骤序号不能重复，并整体替换该实验的步骤列表。
- 删除实验前会检查考试、预约、报告等历史业务引用；存在历史时拒绝删除，建议改为关闭状态。

后续建议：

- 增加实验复制功能，便于跨学期复用实验设计。
- 增加实验版本管理，避免步骤和资源修改影响历史学习记录。
- 增加实验风险审批流程，高风险实验发布前由实验室管理员审核。

### 4.3 教学资源管理

主要接口：

- `GET /api/resources`
- `POST /api/resources`
- `PUT /api/resources/{id}`
- `DELETE /api/resources/{id}`
- `PUT /api/resources/{id}/status`
- `POST /api/resources/{id}/view`

权限要求：

- `resource:view`
- `resource:create`
- `resource:update`
- `resource:delete`

核心数据表：

- `t_resource`

功能说明：

- 资源归属于实验，支持资源标题、类型、URL、文件路径、文件大小、是否必修、浏览次数、状态和排序。
- 创建资源时会写入上传用户 ID。
- 教师只能维护自己课程下实验的资源。
- 学生只能查看开放状态资源。
- 学生打开资源时会增加 `view_count`，并创建或更新学习记录。

后续建议：

- 增加真实文件上传、断点续传、文件类型和大小校验。
- 增加资源预览能力，例如 PDF、视频、图片、Office 文档在线预览。
- 增加资源引用检测，删除资源前提示是否影响学生学习进度。

### 4.4 学习记录

主要接口：

- `PUT /api/learning-records/progress`
- `GET /api/learning-records/my`
- `GET /api/learning-records/experiments/{experimentId}/progress`

权限要求：

- `learning:update:self`

核心数据表：

- `t_learning_record`

功能说明：

- 只有学生可以创建和更新自己的学习记录。
- 学习记录记录学生、资源、实验、进度、累计学习时长、是否完成、首次学习时间和最后学习时间。
- 更新进度时会自动累加学习时长。
- 如果未显式传入完成标记，进度达到 100 时自动标记完成。
- 实验学习进度按开放且必修的资源计算完成率。

本次优化：

- 实验学习进度计算只查询一次必修资源列表，减少一次重复统计查询。

后续建议：

- 增加学习记录明细导出。
- 增加学习时长异常检测，例如短时间内刷完大量资源。
- 增加学习进度与安全考试、实验预约之间的强制联动规则配置。

### 4.5 安全知识库

主要接口：

- `GET /api/safety-knowledge`
- `POST /api/safety-knowledge`
- `PUT /api/safety-knowledge/{id}`
- `DELETE /api/safety-knowledge/{id}`

权限要求：

- `safety:view`
- `safety:create`
- `safety:update`
- `safety:delete`

核心数据表：

- `t_safety_knowledge`

功能说明：

- 安全知识可以关联具体实验，也可以作为全局知识点存在。
- 支持按关键字、实验、风险类型和状态查询。
- 教师只能维护自己课程实验下关联的安全知识。
- 管理员可以维护全局安全知识。
- 学生只能查看启用状态的安全知识。

后续建议：

- 增加知识点标签、难度、适用专业和适用设备。
- 增加知识点与题库题目的双向关联。
- 增加错题高频知识点自动推荐。

## 5. 数据统计模块

### 5.1 Dashboard 概览

主要接口：

- `GET /api/dashboard/overview`

权限要求：

- `dashboard:view`

统计内容：

- 课程数量
- 实验数量
- 资源数量
- 学生数量
- 本月预约数量
- 待审核预约数量
- 待批改报告数量
- 安全考试通过率

数据范围：

- 管理员查看全量数据。
- 教师查看自己负责课程下的数据。
- 学生查看自己的学习、考试、预约和报告相关统计。
- 实验室管理员可以查看具备 `dashboard:view` 权限的数据视图。

### 5.2 教学完成度统计

主要接口：

- `GET /api/dashboard/course-completion`
- `GET /api/dashboard/resources/completion`

功能说明：

- 课程完成度按课程、学生和必修资源计算。
- 资源完成度按资源统计学生完成比例。
- 统计口径依赖 `t_course_student`、`t_resource` 和 `t_learning_record`。

后续建议：

- 增加按班级、专业、学期维度统计。
- 增加低完成率预警和学生未完成名单。

### 5.3 资源统计

主要接口：

- `GET /api/dashboard/resources/type-distribution`
- `GET /api/dashboard/resources/hot-ranking`

功能说明：

- 资源类型分布按 `resource_type` 聚合。
- 热门资源排行按 `view_count` 排序。

后续建议：

- 增加资源学习时长排行。
- 增加资源完成率和考试通过率的相关性分析。

### 5.4 考试与错题统计

主要接口：

- `GET /api/dashboard/exams/pass-rate`
- `GET /api/dashboard/exams/wrong-knowledge-ranking`

功能说明：

- 考试通过率按实验聚合，使用学生最佳成绩或通过标记判断是否通过。
- 错题知识点排行按错误次数和错误率排序。
- 统计数据来自考试子系统相关表，只读聚合，不修改考试数据。

后续建议：

- 增加考试成绩区间分布。
- 增加连续未通过学生清单。
- 增加知识点薄弱项自动生成复习资源推荐。

### 5.5 预约、容量与报告统计

主要接口：

- `GET /api/dashboard/reservations/trend`
- `GET /api/dashboard/reservations/status-distribution`
- `GET /api/dashboard/reservations/capacity-usage`
- `GET /api/dashboard/reports/score-distribution`

功能说明：

- 预约趋势按日期统计预约创建数量。
- 预约状态分布按预约状态聚合。
- 容量使用率按实验时段统计容量、已预约人数和使用率。
- 报告成绩分布按 0-59、60-69、70-79、80-89、90-100 分桶。

后续建议：

- 增加实验室设备占用率和冲突检测。
- 增加预约爽约率、取消率和审批耗时统计。
- 增加报告批改耗时和教师工作量统计。

### 5.6 学习活跃趋势

主要接口：

- `GET /api/dashboard/learning/activity-trend`

功能说明：

- 综合学习记录、考试提交、预约创建、报告提交、推荐点击等行为统计每日活跃学生数。
- 支持按课程、实验、日期范围和当前用户角色范围过滤。

本次优化：

- 所有 Dashboard 接口都会统一校验 `startDate <= endDate`。
- `limit` 在服务层限制为 1 到 50，避免绕过 Controller 校验时传入过大值。

后续建议：

- 增加按小时活跃趋势。
- 增加学生个人学习画像。
- 增加课程、实验、资源、考试之间的漏斗分析。

## 6. 数据库脚本模块

涉及文件：

- `src/main/resources/sql/user_permission_schema.sql`
- `src/main/resources/sql/experiment_teaching_schema.sql`

说明：

- `user_permission_schema.sql` 创建用户、角色、权限、角色权限、用户角色、Token 表，并初始化基础角色和权限。
- `experiment_teaching_schema.sql` 创建课程、课程学生、实验、步骤、资源、安全知识和学习记录表，并补充实验教学相关权限。

后续建议：

- 引入 Flyway 或 Liquibase 管理数据库版本，避免手工执行 SQL 时顺序和重复执行问题。
- 为外部统计依赖表补充单独的建表说明，例如预约、报告、考试、推荐记录表。
- 对高频查询字段补充索引评估，尤其是 `teacher_id`、`course_id`、`experiment_id`、`student_id`、`create_time`。

## 7. 后续可添加功能建议

### 7.1 权限与安全

- 退出登录、Token 刷新、Token 黑名单。
- 登录失败次数限制和验证码。
- 密码强度校验与密码过期策略。
- 关键操作审计日志，例如删除课程、修改角色权限、批量导入用户。

### 7.2 教学业务

- 课程学生管理：选课、退课、批量导入、班级分组。
- 实验预约全流程：时段发布、预约申请、教师/实验室管理员审核、取消和爽约记录。
- 实验报告全流程：模板、提交、批改、退回、重新提交、成绩统计。
- 安全考试全流程：题库、试卷、组卷、考试记录、错题本。

### 7.3 数据统计

- 教学质量看板：课程完成率、考试通过率、报告平均分、学生活跃度综合评分。
- 学生风险预警：长期未学习、考试多次未通过、预约后未完成报告。
- 教师工作台：待审批预约、待批改报告、低完成率课程提醒。
- 实验室运行看板：容量利用率、设备利用率、预约峰谷分布。

### 7.4 平台工程化

- OpenAPI/Swagger 接口文档。
- 单元测试和集成测试覆盖核心权限、课程、实验、学习进度和统计查询。
- 环境配置拆分：开发、测试、生产数据库连接分离。
- CI 流水线：编译、测试、静态检查、打包。
- 监控告警：接口耗时、错误率、慢 SQL 和数据库连接池状态。

## 8. 当前已知注意点

- `t_user_role` 和 `t_role_permission` 是复合主键关系表，MyBatis-Plus 在启动时可能提示找不到单列主键；当前代码通过条件构造器维护关系，不依赖 `xxById` 操作。
- Dashboard 中考试、预约、报告、推荐相关统计依赖其他业务子系统表；如果只初始化当前两个 SQL 文件，调用这些统计接口前需要确保相关表已经存在。
- 课程列表详情目前会实时计算实验数、资源数和平均进度；数据量增大后建议改为批量聚合或统计快照。

## 9. 安全考试、预约、报告、推荐与 AI 子系统说明

### 9.1 安全考试模块

安全考试模块包含题库、试卷、考试记录、答题明细、错题本和考试统计。教师通过题库维护题型、答案、解析、知识点和难度，再组装试卷并发布；学生只看到已发布且自己未参加过的试卷，开始考试后系统生成 `t_exam_record`，提交后写入 `t_exam_answer` 并自动批改客观题。短答题由教师在统计模块批改，批改后重新计算总分和通过状态。

本轮优化补充了学生端考试权限 `exam:take`、试卷查看权限 `exam-paper:view`、考试记录归属校验和重复开考拦截，避免学生重复参加同一试卷或通过记录 ID 查看/提交他人考试。

### 9.2 实验预约模块

实验预约模块围绕 `t_lab_time_slot` 和 `t_reservation` 工作。教师或实验室管理员维护实验室可预约时间段；学生查看剩余名额并提交预约申请；系统校验学生是否已通过对应实验安全考试，并在预约成功时占用名额；教师或实验室管理员审核通过或拒绝，拒绝会释放名额。

本轮优化补充了时间段日期筛选、时间段删除保护、预约记录归属校验、审核状态白名单、名额非负保护，并将预约的实验室和实验项目从时间段派生，减少前端传错字段导致的数据不一致。

### 9.3 实验报告模块

实验报告模块支持草稿保存、修改、提交、教师评分、退回修改和评分历史。报告主体存储在 `t_report`，评分记录存储在 `t_report_score`，通过 `is_latest` 标记最新评分，`grade_time` 用于统计分析。

本轮优化补充了 `report:view`、`report:submit`、`report:grade`、`report:review` 的接口边界，并在服务层限制学生只能修改/提交自己的草稿或退回报告、只能查看自己的报告；教师只能评分或退回已提交报告。

### 9.4 推荐与 AI 辅助模块

推荐模块基于实验匹配、错题相关度、学习新鲜度、资源热度和难度匹配进行打分，生成推荐资源和可解释推荐理由，并记录到 `t_recommend_record`。AI 辅助模块当前采用本地题库检索和模板回答，覆盖安全问答、错题解释和报告建议，问答及人工修订记录写入 `t_ai_chat_record`。

本轮优化补充了推荐与 AI 接口权限，修复推荐理由文案，并限制 AI 人工修订只能修改本人问答记录。AI 输出仍明确标注“仅供参考”，不替代教师评分、预约审核或正式安全规范。

### 9.5 数据库与权限脚本

完整初始化顺序为：

1. `user_permission_schema.sql`
2. `experiment_teaching_schema.sql`
3. `exam_reservation_report_ai_schema.sql`

三份脚本均采用幂等写法创建表和补充权限。角色权限建议保持当前默认分工：管理员全量授权，教师负责教学、题库、试卷、预约审核、报告批改和统计，学生负责学习、考试、预约、报告和 AI 辅助，实验室管理员负责预约和运行统计。

