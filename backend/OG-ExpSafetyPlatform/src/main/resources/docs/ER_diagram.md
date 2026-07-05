# 油气工程实验教学与安全考核平台 — ER 图

> 基于 3 个建表 SQL 生成，共 24 张表，分为 6 个模块。

```mermaid
erDiagram
    %% ==================== 用户权限模块 ====================
    t_user {
        bigint id PK "主键"
        varchar username UK "用户名/学工号"
        varchar password "密码(MD5)"
        varchar real_name "真实姓名"
        varchar phone "手机号"
        tinyint status "状态 1启用"
        datetime create_time "创建时间"
        datetime update_time "更新时间"
    }

    t_role {
        bigint id PK "主键"
        varchar role_name "角色名称"
        varchar role_code UK "角色编码 ADMIN/TEACHER/STUDENT/LAB_ADMIN"
        varchar description "描述"
    }

    t_permission {
        bigint id PK "主键"
        varchar name "权限名称"
        varchar code UK "权限编码"
        tinyint type "类型 1菜单 2按钮"
        bigint parent_id "父权限ID(自关联)"
        varchar path "前端路由"
        varchar icon "图标"
        int sort "排序"
        datetime create_time "创建时间"
    }

    t_user_role {
        bigint user_id PK_FK "用户ID"
        bigint role_id PK_FK "角色ID"
    }

    t_role_permission {
        bigint role_id PK_FK "角色ID"
        bigint permission_id PK_FK "权限ID"
    }

    t_token {
        bigint id PK "主键"
        bigint user_id FK "用户ID"
        varchar token UK "令牌值(UUID)"
        datetime expire_time "过期时间"
        datetime create_time "创建时间"
    }

    %% ==================== 实验教学模块 ====================
    t_lab_course {
        bigint id PK "主键"
        varchar course_code UK "课程编码"
        varchar course_name "课程名称"
        varchar direction "专业方向"
        bigint teacher_id FK "授课教师ID"
        varchar semester "学期"
        text description "课程描述"
        varchar cover_url "封面图"
        tinyint status "状态"
        int sort "排序"
        datetime create_time "创建时间"
        datetime update_time "更新时间"
        tinyint deleted "逻辑删除"
    }

    t_course_student {
        bigint id PK "主键"
        bigint course_id FK "课程ID"
        bigint student_id FK "学生ID"
        varchar semester "学期"
        tinyint status "状态"
        datetime join_time "加入时间"
        datetime create_time "创建时间"
        datetime update_time "更新时间"
        tinyint deleted "逻辑删除"
    }

    t_experiment {
        bigint id PK "主键"
        bigint course_id FK "所属课程ID"
        varchar exp_code UK "实验编码"
        varchar exp_name "实验名称"
        text objective "实验目的"
        text principle "实验原理"
        text equipment "实验设备"
        varchar risk_level "风险等级 LOW/MEDIUM/HIGH"
        int duration_minutes "时长(分钟)"
        int safety_pass_score "安全准入及格分"
        tinyint reservation_enabled "是否开放预约"
        tinyint status "状态"
        int sort "排序"
        datetime create_time "创建时间"
        datetime update_time "更新时间"
        tinyint deleted "逻辑删除"
    }

    t_experiment_step {
        bigint id PK "主键"
        bigint experiment_id FK "实验ID"
        int step_no "步骤序号"
        varchar title "步骤标题"
        text content "步骤内容"
        text safety_tip "安全提示"
        tinyint required_flag "是否必做"
        int estimated_minutes "预计时长"
        datetime create_time "创建时间"
        datetime update_time "更新时间"
        tinyint deleted "逻辑删除"
    }

    t_resource {
        bigint id PK "主键"
        bigint experiment_id FK "实验ID"
        varchar title "资源标题"
        varchar resource_type "类型 DOCUMENT/VIDEO"
        varchar url "资源URL"
        varchar file_path "文件路径"
        bigint file_size "文件大小"
        tinyint required_flag "是否必修"
        int view_count "查看次数"
        tinyint status "上下架"
        int sort "排序"
        bigint upload_user_id FK "上传者ID"
        datetime create_time "创建时间"
        datetime update_time "更新时间"
        tinyint deleted "逻辑删除"
    }

    t_safety_knowledge {
        bigint id PK "主键"
        bigint experiment_id FK "关联实验ID(可空)"
        varchar knowledge_point "知识点"
        varchar risk_type "风险类型"
        text content "知识内容"
        bigint related_step_id FK "关联步骤ID"
        tinyint status "状态"
        datetime create_time "创建时间"
        datetime update_time "更新时间"
        tinyint deleted "逻辑删除"
    }

    t_learning_record {
        bigint id PK "主键"
        bigint student_id FK "学生ID"
        bigint resource_id FK "资源ID"
        bigint experiment_id FK "实验ID"
        decimal progress "学习进度 0-100"
        int duration_seconds "学习时长(秒)"
        tinyint finish_flag "是否完成"
        datetime first_time "首次学习时间"
        datetime last_time "最近学习时间"
        datetime create_time "创建时间"
        datetime update_time "更新时间"
        tinyint deleted "逻辑删除"
    }

    %% ==================== 考试模块 ====================
    t_question {
        bigint id PK "主键"
        varchar type "题型 SINGLE/MULTIPLE/JUDGE/SHORT"
        text content "题目内容"
        json options "选项(JSON)"
        varchar answer "正确答案"
        int score "分值"
        varchar analysis "解析"
        varchar knowledge_point "知识点"
        varchar difficulty "难度 EASY/MEDIUM/HARD"
        bigint course_id FK "关联课程ID"
        bigint create_by FK "创建者ID"
        datetime create_time "创建时间"
        datetime update_time "更新时间"
        tinyint is_deleted "逻辑删除"
    }

    t_exam_paper {
        bigint id PK "主键"
        varchar title "试卷标题"
        varchar description "描述"
        bigint course_id FK "关联课程ID"
        bigint experiment_id FK "关联实验ID"
        int total_score "总分"
        int pass_score "及格分"
        int duration "考试时长(分钟)"
        bigint teacher_id FK "出卷教师ID"
        varchar status "状态 DRAFT/PUBLISHED/CLOSED"
        datetime start_time "开始时间"
        datetime end_time "结束时间"
        datetime create_time "创建时间"
        datetime update_time "更新时间"
        tinyint is_deleted "逻辑删除"
    }

    t_exam_paper_question {
        bigint id PK "主键"
        bigint paper_id FK "试卷ID"
        bigint question_id FK "题目ID"
        int score "分值"
        int order_num "排序"
    }

    t_exam_record {
        bigint id PK "主键"
        bigint student_id FK "学生ID"
        bigint paper_id FK "试卷ID"
        bigint experiment_id FK "实验ID"
        int total_score "总分"
        int objective_score "客观题得分"
        int subjective_score "主观题得分"
        varchar status "状态 IN_PROGRESS/SUBMITTED/GRADED"
        tinyint passed "是否通过"
        datetime start_time "开始时间"
        datetime submit_time "提交时间"
        datetime create_time "创建时间"
        datetime update_time "更新时间"
        tinyint deleted "逻辑删除"
    }

    t_exam_answer {
        bigint id PK "主键"
        bigint record_id FK "考试记录ID"
        bigint question_id FK "题目ID"
        bigint knowledge_id FK "关联知识点ID"
        varchar student_answer "学生答案"
        tinyint is_correct "是否正确"
        tinyint correct_flag "批改标记"
        int score "得分"
    }

    %% ==================== 预约模块 ====================
    t_lab_time_slot {
        bigint id PK "主键"
        bigint lab_id "实验室ID"
        bigint experiment_id FK "实验ID"
        date date "日期"
        time start_time "开始时间"
        time end_time "结束时间"
        int capacity "容量"
        int booked_count "已预约数"
        varchar status "状态 AVAILABLE/FULL"
        bigint create_by FK "创建者ID"
        datetime create_time "创建时间"
        datetime update_time "更新时间"
    }

    t_reservation {
        bigint id PK "主键"
        bigint student_id FK "学生ID"
        bigint time_slot_id FK "时间段ID"
        bigint lab_id "实验室ID"
        bigint experiment_id FK "实验ID"
        varchar purpose "预约目的"
        varchar status "状态 PENDING/APPROVED/REJECTED/CANCELLED"
        bigint teacher_id FK "审核教师ID"
        varchar review_comment "审核意见"
        datetime review_time "审核时间"
        datetime create_time "创建时间"
        datetime update_time "更新时间"
        tinyint deleted "逻辑删除"
    }

    %% ==================== 报告模块 ====================
    t_report {
        bigint id PK "主键"
        bigint student_id FK "学生ID"
        bigint experiment_id FK "实验ID"
        varchar title "报告标题"
        text content "报告内容"
        varchar file_url "附件URL"
        varchar status "状态 DRAFT/SUBMITTED/GRADED"
        datetime submit_time "提交时间"
        datetime latest_submit_time "最近提交时间"
        datetime create_time "创建时间"
        datetime update_time "更新时间"
        tinyint is_deleted "逻辑删除"
    }

    t_report_score {
        bigint id PK "主键"
        bigint report_id FK "报告ID"
        bigint teacher_id FK "评分教师ID"
        int score "分数"
        varchar comment "评语"
        tinyint is_latest "是否最新评分"
        datetime create_time "创建时间"
        datetime grade_time "评分时间"
    }

    %% ==================== 推荐与AI模块 ====================
    t_recommend_record {
        bigint id PK "主键"
        bigint student_id FK "学生ID"
        bigint experiment_id FK "实验ID"
        bigint resource_id FK "推荐资源ID"
        decimal total_score "推荐总分"
        json score_breakdown "评分明细(JSON)"
        varchar reason "推荐理由"
        tinyint clicked "是否点击"
        datetime create_time "创建时间"
    }

    t_ai_chat_record {
        bigint id PK "主键"
        bigint user_id FK "用户ID"
        varchar scene "场景 SAFETY_QA/ERROR_EXPLAIN/REPORT_SUGGEST"
        text question "用户问题"
        text answer "AI回答"
        varchar tool_name "工具名称"
        bigint experiment_id FK "关联实验ID"
        text manual_revision "人工修订"
        datetime create_time "创建时间"
    }

    %% ==================== 关系 ====================

    %% --- 用户权限 ---
    t_user ||--o{ t_user_role : "用户ID"
    t_role ||--o{ t_user_role : "角色ID"
    t_role ||--o{ t_role_permission : "角色ID"
    t_permission ||--o{ t_role_permission : "权限ID"
    t_permission ||--o{ t_permission : "parent_id(菜单树)"
    t_user ||--o{ t_token : "用户ID"

    %% --- 实验教学 ---
    t_user ||--o{ t_lab_course : "teacher_id(授课教师)"
    t_lab_course ||--o{ t_course_student : "课程ID"
    t_user ||--o{ t_course_student : "student_id(选课学生)"
    t_lab_course ||--o{ t_experiment : "课程ID"
    t_experiment ||--o{ t_experiment_step : "实验ID"
    t_experiment ||--o{ t_resource : "实验ID"
    t_user ||--o{ t_resource : "upload_user_id(上传者)"
    t_experiment ||--o{ t_safety_knowledge : "实验ID"
    t_experiment_step ||--o{ t_safety_knowledge : "related_step_id"
    t_user ||--o{ t_learning_record : "student_id"
    t_resource ||--o{ t_learning_record : "资源ID"
    t_experiment ||--o{ t_learning_record : "实验ID"

    %% --- 考试 ---
    t_lab_course ||--o{ t_question : "课程ID"
    t_user ||--o{ t_question : "create_by(出题人)"
    t_lab_course ||--o{ t_exam_paper : "课程ID"
    t_experiment ||--o{ t_exam_paper : "实验ID"
    t_user ||--o{ t_exam_paper : "teacher_id(出卷人)"
    t_exam_paper ||--o{ t_exam_paper_question : "试卷ID"
    t_question ||--o{ t_exam_paper_question : "题目ID"
    t_user ||--o{ t_exam_record : "student_id"
    t_exam_paper ||--o{ t_exam_record : "试卷ID"
    t_experiment ||--o{ t_exam_record : "实验ID"
    t_exam_record ||--o{ t_exam_answer : "考试记录ID"
    t_question ||--o{ t_exam_answer : "题目ID"

    %% --- 预约 ---
    t_experiment ||--o{ t_lab_time_slot : "实验ID"
    t_user ||--o{ t_lab_time_slot : "create_by(创建者)"
    t_user ||--o{ t_reservation : "student_id"
    t_lab_time_slot ||--o{ t_reservation : "时间段ID"
    t_experiment ||--o{ t_reservation : "实验ID"
    t_user ||--o{ t_reservation : "teacher_id(审核人)"

    %% --- 报告 ---
    t_user ||--o{ t_report : "student_id"
    t_experiment ||--o{ t_report : "实验ID"
    t_report ||--o{ t_report_score : "报告ID"
    t_user ||--o{ t_report_score : "teacher_id(评分人)"

    %% --- 推荐与AI ---
    t_user ||--o{ t_recommend_record : "student_id"
    t_experiment ||--o{ t_recommend_record : "实验ID"
    t_resource ||--o{ t_recommend_record : "资源ID"
    t_user ||--o{ t_ai_chat_record : "用户ID"
    t_experiment ||--o{ t_ai_chat_record : "实验ID"
```

## 模块总览

| 模块 | 表数 | 核心表 |
|------|------|--------|
| 🔐 用户权限 | 6 | `t_user` · `t_role` · `t_permission` · `t_user_role` · `t_role_permission` · `t_token` |
| 📚 实验教学 | 7 | `t_lab_course` · `t_course_student` · `t_experiment` · `t_experiment_step` · `t_resource` · `t_safety_knowledge` · `t_learning_record` |
| 📝 考试 | 5 | `t_question` · `t_exam_paper` · `t_exam_paper_question` · `t_exam_record` · `t_exam_answer` |
| 📅 预约 | 2 | `t_lab_time_slot` · `t_reservation` |
| 📄 报告 | 2 | `t_report` · `t_report_score` |
| 🤖 推荐与AI | 2 | `t_recommend_record` · `t_ai_chat_record` |
| **合计** | **24** | |
