# 用户权限与实验教学统计后端整合说明

## 1. 整体结构

当前后端已经整合为一个 Spring Boot 应用，统一包名为 `com.cupk`，统一使用：

- `Result<T>` 作为接口返回结构。
- `PageResult<T>` 作为分页返回结构。
- `BusinessException` 和 `GlobalExceptionHandler` 统一处理业务异常。
- MyBatis-Plus 负责基础 CRUD 和分页。

整合后的后端包含两个业务域：

- 用户与权限管理：登录、Token、用户、角色、权限、菜单和按钮权限。
- 实验教学与数据统计：课程、实验、步骤、资源、安全知识、学习记录和 Dashboard 统计。

## 2. 用户权限管理模块

### 2.1 数据表

用户权限模块使用以下表：

- `t_user`：用户账号、密码、姓名、手机号、状态。
- `t_role`：角色，使用 `role_code` 区分 `ADMIN`、`TEACHER`、`STUDENT`、`LAB_ADMIN`。
- `t_permission`：菜单和按钮权限，`type=1` 表示菜单，`type=2` 表示按钮或接口权限。
- `t_user_role`：用户和角色关系。
- `t_role_permission`：角色和权限关系。
- `t_token`：登录 Token 和过期时间。

初始化脚本：

```text
src/main/resources/sql/user_permission_schema.sql
```

### 2.2 登录与认证

登录接口：

```http
POST /api/auth/login
```

请求体：

```json
{
  "username": "admin",
  "password": "123456"
}
```

登录成功后返回：

- `token`
- `userInfo`
- `menus`
- `permissions`

后续所有 `/api/**` 接口，除 `/api/auth/login` 外，都需要携带：

```http
Authorization: <token>
```

`LoginInterceptor` 会读取 `t_token`，校验 Token 有效性，再读取用户角色和按钮权限写入 `UserContext`。

### 2.3 权限校验

Controller 方法使用：

```java
@RequirePermission("course:create")
```

`PermissionAspect` 会检查当前登录用户是否拥有该权限码。权限来源是：

```text
t_user_role -> t_role_permission -> t_permission
```

### 2.4 用户接口

基础路径：`/api/users`

- `GET /api/users`：用户分页查询，权限 `user:view`。
- `POST /api/users`：新增用户，权限 `user:create`。
- `PUT /api/users`：修改用户，权限 `user:update`。
- `DELETE /api/users`：按请求体删除用户，权限 `user:delete`。
- `DELETE /api/users/{id}`：按路径删除用户，权限 `user:delete`。

用户列表和登录返回均使用 `UserInfoVO`，不会返回密码字段。

### 2.5 角色与权限接口

基础路径：

- `/api/roles`
- `/api/permissions`

接口：

- `GET /api/roles`：角色列表，权限 `role:view`。
- `GET /api/roles/{roleId}/permissions`：查询角色权限 ID 列表，权限 `role:view`。
- `PUT /api/roles/{roleId}/permissions`：保存角色权限，权限 `role:permission:update`。
- `GET /api/permissions/tree`：权限树，权限 `permission:view`。

## 3. 实验教学模块

### 3.1 数据表

实验教学模块使用以下表：

- `t_lab_course`：实验课程。
- `t_course_student`：课程学生关联，用作完成度统计分母。
- `t_experiment`：实验项目。
- `t_experiment_step`：实验步骤。
- `t_resource`：教学资源。
- `t_safety_knowledge`：安全知识。
- `t_learning_record`：学习记录。

初始化脚本：

```text
src/main/resources/sql/experiment_teaching_schema.sql
```

### 3.2 课程接口

基础路径：`/api/courses`

- `GET /api/courses`：课程分页，权限 `course:view`。
- `GET /api/courses/{id}`：课程详情，权限 `course:view`。
- `POST /api/courses`：新增课程，权限 `course:create`。
- `PUT /api/courses/{id}`：修改课程，权限 `course:update`。
- `DELETE /api/courses/{id}`：删除课程，权限 `course:delete`。
- `PUT /api/courses/{id}/status`：修改课程状态，权限 `course:update`。

教师只能管理 `teacher_id` 为自己的课程；管理员可以管理全部课程。

### 3.3 实验接口

基础路径：`/api/experiments`

- `GET /api/experiments`：实验分页，权限 `experiment:view`。
- `GET /api/experiments/{id}`：实验详情，权限 `experiment:view`。
- `GET /api/experiments/{id}/overview`：实验概览，权限 `experiment:view`。
- `POST /api/experiments`：新增实验，权限 `experiment:create`。
- `PUT /api/experiments/{id}`：修改实验，权限 `experiment:update`。
- `DELETE /api/experiments/{id}`：删除实验，权限 `experiment:delete`。
- `PUT /api/experiments/{id}/status`：修改实验状态，权限 `experiment:update`。
- `POST /api/experiments/{id}/steps`：批量保存实验步骤，权限 `experiment:update`。

实验风险等级使用 `LOW`、`MEDIUM`、`HIGH`。

### 3.4 资源接口

基础路径：`/api/resources`

- `GET /api/resources`：资源分页，权限 `resource:view`。
- `POST /api/resources`：新增资源，权限 `resource:create`。
- `PUT /api/resources/{id}`：修改资源，权限 `resource:update`。
- `DELETE /api/resources/{id}`：删除资源，权限 `resource:delete`。
- `PUT /api/resources/{id}/status`：上下架资源，权限 `resource:update`。
- `POST /api/resources/{id}/view`：打开资源并记录学习，权限 `resource:view`。

打开资源时会原子增加 `view_count`，并创建或更新 `t_learning_record`。

### 3.5 学习记录接口

基础路径：`/api/learning-records`

- `PUT /api/learning-records/progress`：更新当前学生学习进度，权限 `learning:update:self`。
- `GET /api/learning-records/my`：当前学生学习记录，权限 `learning:update:self`。
- `GET /api/learning-records/experiments/{experimentId}/progress`：当前学生实验完成度，权限 `learning:update:self`。

### 3.6 安全知识接口

基础路径：`/api/safety-knowledge`

- `GET /api/safety-knowledge`：分页查询，权限 `safety:view`。
- `POST /api/safety-knowledge`：新增，权限 `safety:create`。
- `PUT /api/safety-knowledge/{id}`：修改，权限 `safety:update`。
- `DELETE /api/safety-knowledge/{id}`：删除，权限 `safety:delete`。

教师只能维护自己课程下实验关联的安全知识。

## 4. 数据统计模块

基础路径：`/api/dashboard`

统一权限：`dashboard:view`。

通用参数：

- `startDate`
- `endDate`
- `courseId`
- `experimentId`
- `limit`

接口：

- `GET /api/dashboard/overview`
- `GET /api/dashboard/course-completion`
- `GET /api/dashboard/resources/type-distribution`
- `GET /api/dashboard/resources/hot-ranking`
- `GET /api/dashboard/resources/completion`
- `GET /api/dashboard/exams/pass-rate`
- `GET /api/dashboard/exams/wrong-knowledge-ranking`
- `GET /api/dashboard/reservations/trend`
- `GET /api/dashboard/reservations/status-distribution`
- `GET /api/dashboard/reservations/capacity-usage`
- `GET /api/dashboard/reports/score-distribution`
- `GET /api/dashboard/learning/activity-trend`

统计模块只读聚合业务表，不修改考试、预约、报告和推荐 AI 数据。

## 5. 数据范围

数据范围由 `UserContext` 中的角色决定：

- `ADMIN`：全量。
- `TEACHER`：只看和管理自己负责的课程及其下属数据。
- `STUDENT`：只看开放内容和自己的学习、考试、预约、报告统计。
- `LAB_ADMIN`：可看实验室运行统计，默认不具备教学内容写权限。

## 6. 推荐联调顺序

1. 执行 `user_permission_schema.sql`。
2. 执行 `experiment_teaching_schema.sql`。
3. 创建管理员账号或导入已有用户数据。
4. 给管理员角色分配全部权限。
5. 调用 `/api/auth/login` 获取 Token。
6. 携带 `Authorization` 调用用户、课程、实验、资源和统计接口。

## 7. 模块检查与后续规划

更详细的系统检查、已完成优化、各功能模块说明和后续可添加功能建议见：

```text
src/main/resources/docs/backend_module_review_and_roadmap.md
```


## 9. ?????????????? AI ????

?????? C ???????????????????????????????????????????????? AI ??????????????

- `/api/questions`????????
- `/api/exam-papers`??????
- `/api/exams`????????????????
- `/api/reservations`?????????????????
- `/api/reports`???????????????????
- `/api/recommendations`?????????????
- `/api/ai`?AI ?????????????????

???????????

```text
src/main/resources/sql/exam_reservation_report_ai_schema.sql
```

???????????

1. `user_permission_schema.sql`
2. `experiment_teaching_schema.sql`
3. `exam_reservation_report_ai_schema.sql`

C ?????????? `Authorization` Token?`UserContext`?`@RequirePermission`?`Result<T>` ????????
