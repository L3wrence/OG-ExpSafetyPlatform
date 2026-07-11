# OG-ExpSafetyPlatform 数据库 E-R 图

> 基于《系统详细设计说明》43 张数据表，覆盖全部 12 个业务模块。
> 使用 Mermaid ERD 语法，GitHub 原生渲染。

---

## 1. 系统总览（核心实体关系）

```mermaid
erDiagram
    t_user ||--o{ t_user_role : "拥有"
    t_role ||--o{ t_user_role : "被分配"
    t_role ||--o{ t_role_permission : "包含"
    t_permission ||--o{ t_role_permission : "被授予"
    t_user ||--o{ t_token : "持有"
    t_user ||--o{ t_operation_log : "产生"
    t_user ||--o{ t_teacher_certification : "申请"
    t_user ||--o{ t_course_student : "加入"
    t_lab_course ||--o{ t_course_student : "包含"
    t_lab_course ||--o{ t_teaching_class : "下设"
    t_lab_course ||--o{ t_class_invite : "生成"
    t_lab_course ||--o{ t_experiment : "组织"
    t_experiment ||--o{ t_experiment_step : "包含"
    t_experiment ||--o{ t_learning_task : "配置"
    t_experiment ||--o{ t_experiment_admission : "形成"
    t_user ||--o{ t_learning_record : "学习"
    t_user ||--o{ t_step_learning_record : "完成步骤"
    t_experiment_step ||--o{ t_step_learning_record : "被完成"
    t_learning_task ||--o{ t_learning_task_record : "被完成"
    t_user ||--o{ t_learning_task_record : "完成"
    t_question ||--o{ t_exam_paper_question : "被引用"
    t_exam_paper ||--o{ t_exam_paper_question : "组成"
    t_exam_paper ||--o{ t_exam_record : "产生"
    t_user ||--o{ t_exam_record : "参加"
    t_exam_record ||--o{ t_exam_answer : "包含"
    t_question ||--o{ t_exam_answer : "被作答"
    t_experiment_admission ||--o{ t_reservation : "作为前提"
    t_lab_time_slot ||--o{ t_reservation : "占用"
    t_user ||--o{ t_reservation : "预约"
    t_user ||--o{ t_report : "撰写"
    t_experiment ||--o{ t_report_template : "定义"
    t_experiment ||--o{ t_report_rubric_item : "定义"
    t_report ||--o{ t_report_score : "获得"
    t_report_score ||--o{ t_report_score_item : "包含"
    t_report_rubric_item ||--o{ t_report_score_item : "被评分"
    t_lab_course ||--o{ t_discussion_topic : "属于"
    t_user ||--o{ t_discussion_topic : "发布"
    t_discussion_topic ||--o{ t_discussion_reply : "收到"
    t_user ||--o{ t_discussion_reply : "回复"
    t_user ||--o{ t_resource : "创建"
    t_resource ||--o{ t_resource_interaction : "收到"
    t_user ||--o{ t_resource_interaction : "互动"
    t_user ||--o{ t_resource_timeline_note : "记录"
    t_resource ||--o{ t_resource_timeline_note : "被标注"
    t_user ||--o{ t_resource_submission : "投稿"
    t_user ||--o{ t_portal_message : "接收"
    t_user ||--o{ t_recent_visit : "记录"
    t_user ||--o{ t_user_shortcut : "配置"
    t_user ||--o{ t_ai_chat_record : "提问"
    t_user ||--o{ t_recommend_record : "获得推荐"
    t_resource ||--o{ t_recommend_record : "被推荐"

    t_user {
        bigint id PK "用户ID"
        varchar username UK "学号/工号"
        varchar password "MD5密码"
        varchar real_name "真实姓名"
        varchar phone "手机号"
        int status "1=启用 0=禁用"
        datetime create_time
        datetime update_time
    }

    t_role {
        bigint id PK "角色ID"
        varchar role_name "角色名称"
        varchar role_code UK "角色编码"
        varchar description "角色描述"
    }

    t_user_role {
        bigint user_id PK,FK "用户ID"
        bigint role_id PK,FK "角色ID"
    }

    t_permission {
        bigint id PK "权限ID"
        varchar perm_code UK "权限码"
        varchar perm_name "权限名称"
        bigint parent_id FK "父节点ID"
    }

    t_role_permission {
        bigint role_id PK,FK "角色ID"
        bigint perm_id PK,FK "权限ID"
    }

    t_token {
        bigint id PK
        bigint user_id FK "用户ID"
        varchar token UK "令牌值"
        datetime expire_time "过期时间"
        datetime create_time
    }

    t_lab_course {
        bigint id PK "课堂ID"
        bigint teacher_id FK "教师ID"
        varchar course_name "课程名称"
        varchar description "课程描述"
        varchar status "DRAFT/PUBLISHED/ARCHIVED"
        datetime create_time
    }
```

---

## 2. 用户认证与权限模块（RBAC）

```mermaid
erDiagram
    t_user ||--o{ t_token : "登录产生"
    t_user ||--o{ t_user_role : "拥有"
    t_role ||--o{ t_user_role : "被分配"
    t_role ||--o{ t_role_permission : "包含"
    t_permission ||--o{ t_role_permission : "被授予"
    t_permission ||--o{ t_permission : "父子树"
    t_user ||--o{ t_teacher_certification : "申请"
    t_user ||--o{ t_operation_log : "操作"
    t_user ||--o{ t_user_shortcut : "配置"

    t_user {
        bigint id PK
        varchar username UK
        varchar password
        varchar real_name
        varchar phone
        int status
        datetime create_time
        datetime update_time
    }

    t_role {
        bigint id PK
        varchar role_name
        varchar role_code UK
        varchar description
    }

    t_user_role {
        bigint user_id PK_FK
        bigint role_id PK_FK
    }

    t_permission {
        bigint id PK
        varchar perm_code UK
        varchar perm_name
        bigint parent_id FK
        int sort_order
    }

    t_role_permission {
        bigint role_id PK_FK
        bigint perm_id PK_FK
    }

    t_token {
        bigint id PK
        bigint user_id FK
        varchar token UK
        datetime expire_time
        datetime create_time
    }

    t_teacher_certification {
        bigint id PK
        bigint user_id FK
        varchar real_name
        varchar id_card
        varchar material_url
        varchar status "PENDING/APPROVED/REJECTED"
        varchar reject_reason
        datetime create_time
    }

    t_operation_log {
        bigint id PK
        bigint user_id FK
        varchar action "操作类型"
        varchar target "操作目标"
        varchar result "操作结果"
        varchar ip "IP地址"
        datetime create_time
    }

    t_user_shortcut {
        bigint id PK
        bigint user_id FK
        varchar name "快捷入口名"
        varchar path "路由路径"
        int sort_order "排序"
    }
```

---

## 3. 公共门户与消息模块

```mermaid
erDiagram
    t_user ||--o{ t_portal_message : "接收"
    t_user ||--o{ t_recent_visit : "访问记录"
    t_user ||--o{ t_user_shortcut : "快捷入口"
    t_portal_notice ||--o{ t_portal_notice : "公告管理"

    t_portal_notice {
        bigint id PK
        varchar title "公告标题"
        text content "公告内容"
        varchar status "DRAFT/PUBLISHED/OFFLINE"
        bigint publisher_id FK "发布者"
        datetime publish_time
        datetime create_time
    }

    t_portal_message {
        bigint id PK
        bigint user_id FK "接收者"
        varchar title "消息标题"
        text content "消息内容"
        varchar type "消息类型"
        varchar source_type "来源业务"
        bigint source_id "来源ID"
        tinyint is_read "已读标记"
        datetime create_time
    }

    t_recent_visit {
        bigint id PK
        bigint user_id FK
        varchar target_type "访问类型"
        bigint target_id "访问目标ID"
        varchar target_name "目标名称"
        datetime visit_time
    }

    t_user_shortcut {
        bigint id PK
        bigint user_id FK
        varchar name
        varchar path
        int sort_order
    }
```

---

## 4. 课堂管理与成员模块

```mermaid
erDiagram
    t_lab_course ||--o{ t_teaching_class : "下设"
    t_lab_course ||--o{ t_course_student : "包含"
    t_lab_course ||--o{ t_class_invite : "生成"
    t_teaching_class ||--o{ t_course_student : "归属"
    t_user ||--o{ t_course_student : "加入"
    t_user ||--o{ t_lab_course : "负责"

    t_lab_course {
        bigint id PK "课堂ID"
        bigint teacher_id FK "负责教师"
        varchar course_name "课程名称"
        varchar course_code UK "课程编码"
        varchar description "课程描述"
        varchar cover_image "封面图"
        varchar status "DRAFT/PUBLISHED/ARCHIVED"
        datetime start_date "开课日期"
        datetime end_date "结课日期"
        datetime create_time
        datetime update_time
    }

    t_teaching_class {
        bigint id PK "教学班ID"
        bigint course_id FK "所属课堂"
        varchar class_name "班级名称"
        varchar description "班级描述"
        datetime create_time
    }

    t_course_student {
        bigint id PK
        bigint course_id FK "课堂ID"
        bigint class_id FK "教学班ID"
        bigint student_id FK "学生ID"
        varchar status "加入状态"
        datetime join_time "加入时间"
    }

    t_class_invite {
        bigint id PK
        bigint course_id FK "课堂ID"
        bigint class_id FK "教学班ID"
        varchar invite_code UK "邀请码"
        int max_uses "最大使用次数"
        int used_count "已使用次数"
        datetime expire_time "过期时间"
        tinyint is_active "启用状态"
        datetime create_time
    }
```

---

## 5. 实验与学习路径模块

```mermaid
erDiagram
    t_experiment ||--o{ t_experiment_step : "包含步骤"
    t_experiment ||--o{ t_learning_task : "配置任务"
    t_experiment ||--o{ t_learning_record : "产生记录"
    t_user ||--o{ t_learning_record : "学习"
    t_user ||--o{ t_step_learning_record : "完成步骤"
    t_experiment_step ||--o{ t_step_learning_record : "被完成"
    t_learning_task ||--o{ t_learning_task_record : "被完成"
    t_user ||--o{ t_learning_task_record : "完成"
    t_lab_course ||--o{ t_experiment : "包含"

    t_experiment {
        bigint id PK "实验ID"
        bigint course_id FK "所属课堂"
        varchar title "实验名称"
        varchar risk_level "风险等级"
        text safety_requirement "安全要求"
        text admission_rule "准入规则"
        varchar status "DRAFT/PUBLISHED"
        datetime create_time
    }

    t_experiment_step {
        bigint id PK "步骤ID"
        bigint experiment_id FK "所属实验"
        int step_order "步骤序号"
        varchar title "步骤标题"
        text content "步骤内容"
        text safety_tip "安全提示"
        varchar resource_url "关联资源URL"
    }

    t_step_learning_record {
        bigint id PK
        bigint user_id FK "学生"
        bigint step_id FK "步骤ID"
        tinyint is_completed "完成标记"
        datetime completed_at "完成时间"
    }

    t_learning_task {
        bigint id PK "任务ID"
        bigint experiment_id FK "所属实验"
        varchar title "任务标题"
        text description "任务描述"
        varchar task_type "任务类型"
        tinyint is_required "是否必需"
        int sort_order "排序"
    }

    t_learning_task_record {
        bigint id PK
        bigint user_id FK "学生"
        bigint task_id FK "任务ID"
        tinyint is_completed "完成标记"
        datetime completed_at
    }

    t_learning_record {
        bigint id PK
        bigint user_id FK "学生"
        bigint experiment_id FK "实验ID"
        bigint course_id FK "课堂ID"
        varchar target_type "学习目标类型"
        bigint target_id "学习目标ID"
        int progress "进度百分比"
        int duration "学习时长(秒)"
        varchar status "完成状态"
        datetime start_time
        datetime end_time
    }
```

---

## 6. 教学资源与互动模块

```mermaid
erDiagram
    t_user ||--o{ t_resource : "创建/上传"
    t_resource ||--o{ t_resource_interaction : "收到"
    t_user ||--o{ t_resource_interaction : "互动"
    t_resource ||--o{ t_resource_timeline_note : "被标注"
    t_user ||--o{ t_resource_timeline_note : "记录"
    t_user ||--o{ t_resource_submission : "投稿"
    t_lab_course ||--o{ t_resource : "关联"
    t_experiment ||--o{ t_resource : "关联"
    t_resource ||--o{ t_recommend_record : "被推荐"
    t_user ||--o{ t_recommend_record : "获得推荐"

    t_resource {
        bigint id PK
        varchar title "资源名称"
        varchar description "资源描述"
        varchar file_url "文件路径"
        varchar file_type "文件类型VIDEO/DOC/IMG/LINK"
        bigint file_size "文件大小"
        varchar resource_type "资源分类"
        bigint course_id FK "关联课堂"
        bigint experiment_id FK "关联实验"
        bigint creator_id FK "创建者"
        varchar status "DRAFT/PUBLISHED/HIDDEN"
        int view_count "查看次数"
        int download_count "下载次数"
        datetime create_time
    }

    t_resource_interaction {
        bigint id PK
        bigint user_id FK "用户"
        bigint resource_id FK "资源"
        varchar action "收藏/评分/点赞"
        int score "评分值(1-5)"
        datetime create_time
    }

    t_resource_timeline_note {
        bigint id PK
        bigint user_id FK "用户"
        bigint resource_id FK "资源(视频)"
        int time_point "时间点(秒)"
        varchar content "笔记内容"
        varchar status "ACTIVE/RESOLVED"
        datetime create_time
    }

    t_resource_submission {
        bigint id PK
        bigint user_id FK "投稿人"
        varchar title "标题"
        varchar description "描述"
        varchar file_url "文件路径"
        varchar status "PENDING/APPROVED/REJECTED"
        bigint reviewer_id FK "审核人"
        varchar review_comment "审核意见"
        datetime review_time
        datetime create_time
    }

    t_recommend_record {
        bigint id PK
        bigint user_id FK "用户"
        bigint resource_id FK "资源"
        varchar reason "推荐理由"
        varchar feedback "用户反馈"
        datetime create_time
    }
```

---

## 7. 安全考试模块（题库 + 试卷 + 考试）

```mermaid
erDiagram
    t_question ||--o{ t_exam_paper_question : "被引用"
    t_exam_paper ||--o{ t_exam_paper_question : "组成"
    t_exam_paper ||--o{ t_exam_record : "产生"
    t_user ||--o{ t_exam_record : "参加"
    t_exam_record ||--o{ t_exam_answer : "包含"
    t_question ||--o{ t_exam_answer : "被作答"
    t_exam_record ||--o{ t_experiment_admission : "形成准入"
    t_lab_course ||--o{ t_exam_paper : "关联"

    t_question {
        bigint id PK "题目ID"
        varchar content "题干"
        varchar type "SINGLE/MULTIPLE/JUDGE/SHORT_ANSWER"
        varchar options "选项JSON(选择题)"
        varchar answer "标准答案"
        varchar analysis "解析"
        varchar difficulty "EASY/MEDIUM/HARD"
        varchar knowledge_tag "知识点标签"
        varchar risk_type "风险类型"
        bigint course_id FK "关联课堂"
        bigint creator_id FK "创建者"
        bigint usage_count "被引用次数"
        datetime create_time
    }

    t_exam_paper {
        bigint id PK "试卷ID"
        bigint course_id FK "所属课堂"
        bigint experiment_id FK "关联实验"
        varchar title "试卷名称"
        varchar description "试卷描述"
        int total_score "总分"
        int pass_score "及格线"
        int duration "考试时长(分钟)"
        int max_attempts "最大考试次数"
        varchar status "DRAFT/PUBLISHED/CLOSED"
        datetime create_time
    }

    t_exam_paper_question {
        bigint id PK
        bigint paper_id FK "试卷ID"
        bigint question_id FK "题目ID"
        int score "分值"
        int sort_order "排序"
    }

    t_exam_record {
        bigint id PK "考试记录ID"
        bigint paper_id FK "试卷ID"
        bigint student_id FK "考生"
        int score "得分"
        varchar status "IN_PROGRESS/SUBMITTED/GRADED/EXPIRED"
        tinyint is_passed "是否通过"
        datetime start_time "开始时间"
        datetime submit_time "提交时间"
        datetime expire_time "过期时间"
    }

    t_exam_answer {
        bigint id PK
        bigint record_id FK "考试记录ID"
        bigint question_id FK "题目ID"
        varchar answer "考生答案"
        int score "得分"
        tinyint is_correct "是否正确"
        varchar grader_comment "批改评语"
        varchar grading_status "AUTO_GRADED/MANUAL_GRADED/PENDING"
        datetime create_time
    }

    t_experiment_admission {
        bigint id PK "准入ID"
        bigint student_id FK "学生"
        bigint experiment_id FK "实验"
        bigint exam_record_id FK "来源考试记录"
        varchar status "VALID/EXPIRED/REVOKED"
        datetime valid_from "生效时间"
        datetime valid_until "失效时间"
        datetime create_time
    }
```

---

## 8. 实验预约模块

```mermaid
erDiagram
    t_experiment ||--o{ t_lab_time_slot : "配置时段"
    t_lab_time_slot ||--o{ t_reservation : "被预约"
    t_user ||--o{ t_reservation : "提交"
    t_experiment_admission ||--o{ t_reservation : "作为前提"
    t_experiment ||--o{ t_experiment_admission : "准入记录"

    t_lab_time_slot {
        bigint id PK "时段ID"
        bigint experiment_id FK "实验ID"
        bigint lab_id "实验室ID"
        date slot_date "日期"
        time start_time "开始时间"
        time end_time "结束时间"
        int capacity "最大容量"
        int booked_count "已预约数"
        varchar status "AVAILABLE/FULL/CLOSED"
        datetime create_time
    }

    t_reservation {
        bigint id PK "预约ID"
        bigint student_id FK "学生"
        bigint experiment_id FK "实验"
        bigint slot_id FK "时段"
        varchar purpose "实验目的"
        varchar status "PENDING/APPROVED/REJECTED/CANCELED"
        bigint reviewer_id FK "审核教师"
        varchar review_comment "审核意见"
        datetime review_time
        datetime create_time
    }
```

---

## 9. 实验报告与评分模块

```mermaid
erDiagram
    t_user ||--o{ t_report : "撰写"
    t_experiment ||--o{ t_report : "所属"
    t_experiment ||--o{ t_report_template : "定义模板"
    t_experiment ||--o{ t_report_rubric_item : "定义量规"
    t_report ||--o{ t_report_score : "获得评分"
    t_report_score ||--o{ t_report_score_item : "包含明细"
    t_report_rubric_item ||--o{ t_report_score_item : "被评分"
    t_user ||--o{ t_report_score : "评阅"

    t_report {
        bigint id PK "报告ID"
        bigint student_id FK "学生"
        bigint experiment_id FK "实验"
        bigint course_id FK "课堂"
        varchar title "报告标题"
        text content "报告内容(Markdown)"
        varchar attachment_url "附件路径"
        varchar status "DRAFT/SUBMITTED/GRADED/RETURNED"
        varchar teacher_comment "教师意见"
        datetime submit_time "提交时间"
        datetime grade_time "评分时间"
        datetime create_time
        datetime update_time
    }

    t_report_template {
        bigint id PK
        bigint experiment_id FK "实验ID"
        varchar title "模板名称"
        text structure "报告结构定义(JSON)"
        datetime create_time
    }

    t_report_rubric_item {
        bigint id PK
        bigint experiment_id FK "实验ID"
        varchar name "评分维度"
        int max_score "满分"
        varchar description "评分说明"
        int sort_order "排序"
    }

    t_report_score {
        bigint id PK
        bigint report_id FK "报告ID"
        bigint teacher_id FK "评分教师"
        int total_score "总分"
        varchar comment "总评"
        datetime create_time
    }

    t_report_score_item {
        bigint id PK
        bigint score_id FK "评分ID"
        bigint rubric_item_id FK "量规项ID"
        int score "得分"
        varchar comment "评语"
    }
```

---

## 10. 讨论交流模块

```mermaid
erDiagram
    t_lab_course ||--o{ t_discussion_topic : "属于"
    t_user ||--o{ t_discussion_topic : "发布"
    t_discussion_topic ||--o{ t_discussion_reply : "收到回复"
    t_user ||--o{ t_discussion_reply : "回复"

    t_discussion_topic {
        bigint id PK "主题ID"
        bigint course_id FK "所属课堂"
        bigint author_id FK "作者"
        varchar title "标题"
        text content "内容"
        varchar status "OPEN/CLOSED"
        tinyint is_featured "精华标记"
        int reply_count "回复数"
        datetime last_reply_at "最后回复时间"
        datetime create_time
    }

    t_discussion_reply {
        bigint id PK
        bigint topic_id FK "主题ID"
        bigint author_id FK "作者"
        text content "回复内容"
        datetime create_time
    }
```

---

## 11. AI 问答模块

```mermaid
erDiagram
    t_user ||--o{ t_ai_chat_record : "提问"

    t_ai_chat_record {
        bigint id PK "记录ID"
        bigint user_id FK "提问用户"
        varchar scene "场景:SAFETY_QA/ERROR_EXPLAIN/REPORT_SUGGEST"
        text question "用户问题"
        text answer "AI回答"
        varchar feedback "用户反馈"
        bigint experiment_id FK "关联实验"
        datetime create_time
    }
```

---

## 12. 统计看板模块（跨表聚合读取）

```mermaid
erDiagram
    t_lab_course ||--o{ t_course_student : "统计范围"
    t_lab_course ||--o{ t_learning_record : "学习数据"
    t_exam_paper ||--o{ t_exam_record : "考试数据"
    t_exam_record ||--o{ t_exam_answer : "错题数据"
    t_lab_time_slot ||--o{ t_reservation : "预约数据"
    t_report ||--o{ t_report_score : "成绩数据"
    t_resource ||--o{ t_resource_interaction : "互动数据"

    t_learning_record {
        bigint id PK
        bigint user_id FK
        bigint course_id FK
        int progress
        int duration
        varchar status
    }

    t_exam_record {
        bigint id PK
        bigint paper_id FK
        bigint student_id FK
        int score
        tinyint is_passed
        datetime submit_time
    }

    t_exam_answer {
        bigint id PK
        bigint record_id FK
        bigint question_id FK
        tinyint is_correct
    }

    t_reservation {
        bigint id PK
        bigint slot_id FK
        varchar status
    }

    t_report_score {
        bigint id PK
        bigint report_id FK
        int total_score
    }

    t_resource_interaction {
        bigint id PK
        bigint resource_id FK
        int score
    }
```

---

## 13. 全系统 43 表汇总

| # | 表名 | 所属模块 | 核心外键 |
|:-:|---|--------|---------|
| 1 | `t_user` | 认证与账户 | — |
| 2 | `t_role` | 角色权限 | — |
| 3 | `t_user_role` | 角色权限 | user_id, role_id |
| 4 | `t_permission` | 角色权限 | parent_id(自引用) |
| 5 | `t_role_permission` | 角色权限 | role_id, perm_id |
| 6 | `t_token` | 认证 | user_id |
| 7 | `t_teacher_certification` | 认证 | user_id |
| 8 | `t_operation_log` | 审计 | user_id |
| 9 | `t_portal_notice` | 门户公告 | publisher_id |
| 10 | `t_portal_message` | 门户消息 | user_id |
| 11 | `t_recent_visit` | 门户 | user_id |
| 12 | `t_user_shortcut` | 门户 | user_id |
| 13 | `t_lab_course` | 课堂管理 | teacher_id |
| 14 | `t_teaching_class` | 课堂管理 | course_id |
| 15 | `t_course_student` | 课堂管理 | course_id, class_id, student_id |
| 16 | `t_class_invite` | 课堂管理 | course_id, class_id |
| 17 | `t_experiment` | 实验 | course_id |
| 18 | `t_experiment_step` | 实验 | experiment_id |
| 19 | `t_step_learning_record` | 学习记录 | user_id, step_id |
| 20 | `t_learning_record` | 学习记录 | user_id, course_id, experiment_id |
| 21 | `t_learning_task` | 学习任务 | experiment_id |
| 22 | `t_learning_task_record` | 学习记录 | user_id, task_id |
| 23 | `t_resource` | 教学资源 | course_id, experiment_id, creator_id |
| 24 | `t_resource_interaction` | 资源互动 | user_id, resource_id |
| 25 | `t_resource_timeline_note` | 资源笔记 | user_id, resource_id |
| 26 | `t_resource_submission` | 资源投稿 | user_id, reviewer_id |
| 27 | `t_recommend_record` | 智能推荐 | user_id, resource_id |
| 28 | `t_question` | 题库 | course_id, creator_id |
| 29 | `t_exam_paper` | 考试 | course_id, experiment_id |
| 30 | `t_exam_paper_question` | 考试 | paper_id, question_id |
| 31 | `t_exam_record` | 考试 | paper_id, student_id |
| 32 | `t_exam_answer` | 考试 | record_id, question_id |
| 33 | `t_experiment_admission` | 实验准入 | student_id, experiment_id, exam_record_id |
| 34 | `t_lab_time_slot` | 实验预约 | experiment_id |
| 35 | `t_reservation` | 实验预约 | student_id, experiment_id, slot_id, reviewer_id |
| 36 | `t_report` | 实验报告 | student_id, experiment_id, course_id |
| 37 | `t_report_template` | 实验报告 | experiment_id |
| 38 | `t_report_rubric_item` | 实验报告 | experiment_id |
| 39 | `t_report_score` | 实验报告 | report_id, teacher_id |
| 40 | `t_report_score_item` | 实验报告 | score_id, rubric_item_id |
| 41 | `t_discussion_topic` | 讨论交流 | course_id, author_id |
| 42 | `t_discussion_reply` | 讨论交流 | topic_id, author_id |
| 43 | `t_ai_chat_record` | AI问答 | user_id, experiment_id |

---

> **说明**：以上 E-R 图使用 Mermaid ERD 语法编写，在 GitHub 上直接渲染为可视化实体关系图。
> 打开 `docs/ER-Diagram.md` 即可在 GitHub 仓库页面查看完整图表。
