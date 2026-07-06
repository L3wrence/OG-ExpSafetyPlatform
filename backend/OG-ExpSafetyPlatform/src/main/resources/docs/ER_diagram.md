# 油气工程实验教学与安全考核平台 — ER 图

> 共 24 张表，分为 6 个模块。每个模块单独一张 ER 图。

---

## 1. 🔐 用户权限模块（6张表）

```mermaid
erDiagram
    t_user {
        bigint 用户ID PK
        varchar 用户名 UK
        varchar 密码
        varchar 真实姓名
        varchar 手机号
        tinyint 状态
    }

    t_role {
        bigint 角色ID PK
        varchar 角色名称
        varchar 角色编码 UK
        varchar 描述
    }

    t_permission {
        bigint 权限ID PK
        varchar 权限名称
        varchar 权限编码 UK
        tinyint 类型
        bigint 父权限ID FK
        varchar 路由路径
        varchar 图标
        int 排序
    }

    t_user_role {
        bigint 用户ID PK_FK
        bigint 角色ID PK_FK
    }

    t_role_permission {
        bigint 角色ID PK_FK
        bigint 权限ID PK_FK
    }

    t_token {
        bigint token_ID PK
        bigint 用户ID FK
        varchar 令牌 UK
        datetime 过期时间
    }

    t_user ||--o{ t_user_role : 拥有
    t_role ||--o{ t_user_role : 分配
    t_role ||--o{ t_role_permission : 拥有
    t_permission ||--o{ t_role_permission : 被分配
    t_permission ||--o{ t_permission : 父子菜单
    t_user ||--o{ t_token : 登录凭证
```

| 表 | 说明 |
|----|------|
| `t_user` | 用户账号，密码MD5加密，status=1启用 |
| `t_role` | 角色：ADMIN / TEACHER / STUDENT / LAB_ADMIN |
| `t_permission` | 权限：type=1菜单 type=2按钮，parent_id构建树 |
| `t_user_role` | 用户↔角色 多对多 |
| `t_role_permission` | 角色↔权限 多对多 |
| `t_token` | 登录Token，UUID去横线，默认1天过期 |

---

## 2. 📚 实验教学模块（7张表）

```mermaid
erDiagram
    t_lab_course {
        bigint 课程ID PK
        varchar 课程编码 UK
        varchar 课程名称
        varchar 专业方向
        bigint 授课教师ID FK
        varchar 学期
        text 课程描述
        tinyint 状态
        int 排序
        tinyint 逻辑删除
    }

    t_course_student {
        bigint 选课ID PK
        bigint 课程ID FK
        bigint 学生ID FK
        varchar 学期
        tinyint 状态
        datetime 加入时间
        tinyint 逻辑删除
    }

    t_experiment {
        bigint 实验ID PK
        bigint 课程ID FK
        varchar 实验编码 UK
        varchar 实验名称
        text 实验目的
        text 实验原理
        text 实验设备
        varchar 风险等级
        int 时长分钟
        int 安全准入及格分
        tinyint 开放预约
        tinyint 状态
        int 排序
        tinyint 逻辑删除
    }

    t_experiment_step {
        bigint 步骤ID PK
        bigint 实验ID FK
        int 步骤序号
        varchar 步骤标题
        text 步骤内容
        text 安全提示
        tinyint 是否必做
        int 预计分钟
        tinyint 逻辑删除
    }

    t_resource {
        bigint 资源ID PK
        bigint 实验ID FK
        varchar 资源标题
        varchar 资源类型
        varchar URL
        bigint 文件大小
        tinyint 是否必修
        int 查看次数
        bigint 上传者ID FK
        tinyint 逻辑删除
    }

    t_safety_knowledge {
        bigint 知识ID PK
        bigint 实验ID FK
        varchar 知识点
        varchar 风险类型
        text 知识内容
        bigint 关联步骤ID FK
        tinyint 逻辑删除
    }

    t_learning_record {
        bigint 记录ID PK
        bigint 学生ID FK
        bigint 资源ID FK
        bigint 实验ID FK
        decimal 学习进度
        int 学习时长秒
        tinyint 是否完成
        datetime 首次学习
        datetime 最近学习
        tinyint 逻辑删除
    }

    t_user ||--o{ t_lab_course : 授课
    t_user ||--o{ t_course_student : 选课
    t_lab_course ||--o{ t_course_student : 包含
    t_lab_course ||--o{ t_experiment : 包含
    t_experiment ||--o{ t_experiment_step : 包含步骤
    t_experiment ||--o{ t_resource : 包含资源
    t_user ||--o{ t_resource : 上传
    t_experiment ||--o{ t_safety_knowledge : 关联安全知识
    t_experiment_step ||--o{ t_safety_knowledge : 关联
    t_user ||--o{ t_learning_record : 学习记录
    t_resource ||--o{ t_learning_record : 学习记录
    t_experiment ||--o{ t_learning_record : 学习记录
```

| 表 | 说明 |
|----|------|
| `t_lab_course` | 实验课程，teacher_id关联授课教师 |
| `t_course_student` | 学生选课关系表 |
| `t_experiment` | 实验项目，risk_level: LOW/MEDIUM/HIGH |
| `t_experiment_step` | 实验操作步骤，含安全提示 |
| `t_resource` | 教学资源(DOCUMENT/VIDEO)，view_count累计点击 |
| `t_safety_knowledge` | 安全知识库，可按实验/步骤关联 |
| `t_learning_record` | 学生学习进度追踪，progress 0-100 |

---

## 3. 📝 考试模块（5张表）

```mermaid
erDiagram
    t_question {
        bigint 题目ID PK
        varchar 题型
        text 题目内容
        json 选项JSON
        varchar 正确答案
        int 分值
        varchar 解析
        varchar 知识点
        varchar 难度
        bigint 课程ID FK
        bigint 出题人ID FK
        tinyint 逻辑删除
    }

    t_exam_paper {
        bigint 试卷ID PK
        varchar 试卷标题
        varchar 描述
        bigint 课程ID FK
        bigint 实验ID FK
        int 总分
        int 及格分
        int 考试时长
        bigint 出卷教师ID FK
        varchar 状态
        datetime 开始时间
        datetime 结束时间
        tinyint 逻辑删除
    }

    t_exam_paper_question {
        bigint 关联ID PK
        bigint 试卷ID FK
        bigint 题目ID FK
        int 分值
        int 排序
    }

    t_exam_record {
        bigint 考试记录ID PK
        bigint 学生ID FK
        bigint 试卷ID FK
        bigint 实验ID FK
        int 总分
        int 客观题得分
        int 主观题得分
        varchar 状态
        tinyint 是否通过
        datetime 开始时间
        datetime 提交时间
        tinyint 逻辑删除
    }

    t_exam_answer {
        bigint 作答ID PK
        bigint 考试记录ID FK
        bigint 题目ID FK
        varchar 学生答案
        tinyint 是否正确
        tinyint 批改标记
        int 得分
    }

    t_lab_course ||--o{ t_question : 题库
    t_user ||--o{ t_question : 出题
    t_lab_course ||--o{ t_exam_paper : 试卷
    t_experiment ||--o{ t_exam_paper : 试卷
    t_user ||--o{ t_exam_paper : 出卷
    t_exam_paper ||--o{ t_exam_paper_question : 组卷
    t_question ||--o{ t_exam_paper_question : 选题
    t_user ||--o{ t_exam_record : 考试
    t_exam_paper ||--o{ t_exam_record : 答卷
    t_exam_record ||--o{ t_exam_answer : 作答明细
    t_question ||--o{ t_exam_answer : 作答题目
```

| 表 | 说明 |
|----|------|
| `t_question` | 题库：SINGLE/MULTIPLE/JUDGE/SHORT |
| `t_exam_paper` | 试卷：DRAFT→PUBLISHED→CLOSED |
| `t_exam_paper_question` | 试卷↔题目 多对多，含每题分值 |
| `t_exam_record` | 考试记录：IN_PROGRESS→SUBMITTED→GRADED |
| `t_exam_answer` | 每道题的作答详情 |

---

## 4. 📅 预约模块（2张表）

```mermaid
erDiagram
    t_lab_time_slot {
        bigint 时间段ID PK
        bigint 实验室编号
        bigint 实验ID FK
        date 日期
        time 开始时间
        time 结束时间
        int 容量
        int 已预约数
        varchar 状态
        bigint 创建者ID FK
    }

    t_reservation {
        bigint 预约ID PK
        bigint 学生ID FK
        bigint 时间段ID FK
        bigint 实验室编号
        bigint 实验ID FK
        varchar 预约目的
        varchar 状态
        bigint 审核教师ID FK
        varchar 审核意见
        datetime 审核时间
        tinyint 逻辑删除
    }

    t_experiment ||--o{ t_lab_time_slot : 可预约时段
    t_user ||--o{ t_lab_time_slot : 创建时段
    t_user ||--o{ t_reservation : 学生预约
    t_lab_time_slot ||--o{ t_reservation : 预约时段
    t_user ||--o{ t_reservation : 教师审核
```

| 表 | 说明 |
|----|------|
| `t_lab_time_slot` | 实验室开放时段，booked_count随预约更新 |
| `t_reservation` | 预约记录：PENDING→APPROVED/REJECTED/CANCELLED |

---

## 5. 📄 报告模块（2张表）

```mermaid
erDiagram
    t_report {
        bigint 报告ID PK
        bigint 学生ID FK
        bigint 实验ID FK
        varchar 报告标题
        text 报告内容
        varchar 附件URL
        varchar 状态
        datetime 提交时间
        datetime 最近提交
        tinyint 逻辑删除
    }

    t_report_score {
        bigint 评分ID PK
        bigint 报告ID FK
        bigint 教师ID FK
        int 分数
        varchar 评语
        tinyint 是否最新评分
        datetime 评分时间
    }

    t_user ||--o{ t_report : 撰写报告
    t_experiment ||--o{ t_report : 实验报告
    t_report ||--o{ t_report_score : 评分记录
    t_user ||--o{ t_report_score : 教师评分
```

| 表 | 说明 |
|----|------|
| `t_report` | 实验报告：DRAFT→SUBMITTED→GRADED |
| `t_report_score` | 多次评分保留历史，is_latest=1为最新 |

---

## 6. 🤖 推荐与AI模块（2张表）

```mermaid
erDiagram
    t_recommend_record {
        bigint 推荐ID PK
        bigint 学生ID FK
        bigint 实验ID FK
        bigint 资源ID FK
        decimal 推荐总分
        json 评分明细JSON
        varchar 推荐理由
        tinyint 是否点击
        datetime 推荐时间
    }

    t_ai_chat_record {
        bigint 对话ID PK
        bigint 用户ID FK
        varchar 场景
        text 用户问题
        text AI回答
        varchar 工具名称
        bigint 实验ID FK
        text 人工修订
        datetime 对话时间
    }

    t_user ||--o{ t_recommend_record : 推荐给
    t_experiment ||--o{ t_recommend_record : 关联实验
    t_resource ||--o{ t_recommend_record : 推荐资源
    t_user ||--o{ t_ai_chat_record : AI对话
    t_experiment ||--o{ t_ai_chat_record : 关联实验
```

| 表 | 说明 |
|----|------|
| `t_recommend_record` | 资源推荐记录，score_breakdown存各维度评分 |
| `t_ai_chat_record` | AI问答记录，scene: SAFETY_QA / ERROR_EXPLAIN / REPORT_SUGGEST |

---

## 模块关系总览

```
 t_user ────→ t_user_role ←──── t_role ────→ t_role_permission ←──── t_permission
   │  │                                          
   │  └──→ t_token (登录)                          
   │                                               
   ├──→ t_lab_course ──→ t_experiment ──→ t_experiment_step
   │        │                 │
   │        │                 ├──→ t_resource ←── t_learning_record ── t_user
   │        │                 ├──→ t_safety_knowledge
   │        │                 ├──→ t_exam_paper ──→ t_question ──→ t_exam_record
   │        │                 ├──→ t_lab_time_slot ──→ t_reservation
   │        │                 ├──→ t_report ──→ t_report_score
   │        │                 ├──→ t_recommend_record
   │        │                 └──→ t_ai_chat_record
   │        │
   │        └──→ t_course_student ←── t_user (选课)
   │
   └──→ (所有表的 student_id / teacher_id / create_by 都指向 t_user)
```
