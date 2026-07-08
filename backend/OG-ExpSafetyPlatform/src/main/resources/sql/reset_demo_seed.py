import hashlib
import json
import os
from datetime import datetime, time, timedelta

import pymysql

DB = dict(
    host=os.getenv("MYSQL_HOST", "localhost"),
    port=int(os.getenv("MYSQL_PORT", "3306")),
    user=os.getenv("MYSQL_USERNAME", "root"),
    password=os.getenv("MYSQL_PASSWORD", "gnah060725"),
    database=os.getenv("MYSQL_DATABASE", "ogexpsafetyplatform"),
    charset="utf8mb4",
    autocommit=False,
)


def main():
    conn = pymysql.connect(**DB)
    cur = conn.cursor()
    now = datetime.now().replace(microsecond=0)

    def q(sql, params=None):
        cur.execute(sql, params or ())
        return cur

    def table_exists(name):
        q(
            "select count(*) from information_schema.tables "
            "where table_schema=database() and table_name=%s",
            (name,),
        )
        return cur.fetchone()[0] > 0

    def index_exists(table, index):
        q(
            "select count(*) from information_schema.statistics "
            "where table_schema=database() and table_name=%s and index_name=%s",
            (table, index),
        )
        return cur.fetchone()[0] > 0

    def add_index(table, index, ddl):
        if table_exists(table) and not index_exists(table, index):
            q(f"alter table {table} add index {index} {ddl}")

    def insert(table, data):
        cols = list(data)
        placeholders = ", ".join(["%s"] * len(cols))
        q(
            f"insert into {table} ({', '.join(cols)}) values ({placeholders})",
            [data[c] for c in cols],
        )
        return cur.lastrowid

    try:
        create_phase4_tables(q)
        add_index("t_resource", "idx_resource_public_course", "(status, invalid_flag, open_scope, course_id)")
        add_index("t_course_student", "idx_course_student_user_course", "(student_id, status, course_id)")
        add_index("t_exam_record", "idx_exam_record_student_exp_status", "(student_id, experiment_id, status)")
        add_index("t_reservation", "idx_reservation_student_exp_status", "(student_id, experiment_id, status)")
        add_index("t_learning_task_record", "idx_task_record_student_task", "(student_id, task_id, status)")

        q("show tables")
        tables = [row[0] for row in cur.fetchall()]
        q("set foreign_key_checks=0")
        for table in tables:
            q(f"truncate table {table}")
        q("set foreign_key_checks=1")

        roles = seed_roles(insert, now)
        perms = seed_permissions(insert, now)
        seed_role_permissions(insert, roles, perms)
        users = seed_users(insert, roles, now)

        seed_teacher_certifications(insert, users, now)
        courses, classes = seed_courses(insert, users, now)
        experiments = seed_experiments(insert, courses, users, now)
        seed_course_members(insert, courses, classes, users, now)
        resources = seed_resources(insert, courses, experiments, users, now)
        seed_learning_assets(insert, courses, experiments, resources, users, now)
        papers, questions = seed_exams(insert, courses, experiments, resources, users, now)
        seed_reports_and_reservations(insert, courses, experiments, papers, users, now)
        seed_public_and_social(insert, courses, classes, experiments, resources, users, now)

        conn.commit()
        print("RESET_AND_SEED_OK")
        for table in [
            "t_user",
            "t_role",
            "t_permission",
            "t_lab_course",
            "t_teaching_class",
            "t_course_student",
            "t_experiment",
            "t_resource",
            "t_exam_paper",
            "t_reservation",
            "t_report",
            "t_discussion_topic",
            "t_teacher_certification",
            "t_class_invite",
            "t_resource_submission",
        ]:
            q(f"select count(*) from {table}")
            print(f"{table}: {cur.fetchone()[0]}")
        print("accounts:", ", ".join(sorted(users)))
    except Exception:
        conn.rollback()
        raise
    finally:
        cur.close()
        conn.close()


def create_phase4_tables(q):
    q(
        """
        create table if not exists t_teacher_certification (
          id bigint primary key auto_increment,
          user_id bigint not null,
          school varchar(120) not null,
          employee_no varchar(80) not null,
          education_email varchar(120) not null,
          status varchar(30) not null default 'PENDING',
          reviewer_id bigint null,
          review_comment varchar(500) null,
          review_time datetime null,
          create_time datetime default current_timestamp,
          update_time datetime default current_timestamp on update current_timestamp,
          deleted tinyint not null default 0,
          key idx_teacher_cert_user_status (user_id, status, deleted),
          key idx_teacher_cert_status_time (status, create_time, deleted)
        ) engine=InnoDB default charset=utf8mb4 comment='教师认证申请'
        """
    )
    q(
        """
        create table if not exists t_class_invite (
          id bigint primary key auto_increment,
          course_id bigint not null,
          teaching_class_id bigint null,
          invite_code varchar(32) not null,
          expire_time datetime null,
          max_uses int null,
          used_count int not null default 0,
          status tinyint not null default 1,
          created_by bigint not null,
          create_time datetime default current_timestamp,
          update_time datetime default current_timestamp on update current_timestamp,
          deleted tinyint not null default 0,
          unique key uk_class_invite_code (invite_code, deleted),
          key idx_class_invite_course_status (course_id, status, deleted)
        ) engine=InnoDB default charset=utf8mb4 comment='课堂邀请码'
        """
    )
    q(
        """
        create table if not exists t_resource_submission (
          id bigint primary key auto_increment,
          submitter_id bigint not null,
          title varchar(180) not null,
          resource_type varchar(40) not null,
          knowledge_point varchar(200) null,
          risk_type varchar(120) null,
          tags varchar(255) null,
          description varchar(1000) null,
          url varchar(500) null,
          file_path varchar(500) null,
          original_filename varchar(255) null,
          content_type varchar(120) null,
          status varchar(30) not null default 'PENDING',
          reviewer_id bigint null,
          review_comment varchar(500) null,
          review_time datetime null,
          public_resource_id bigint null,
          create_time datetime default current_timestamp,
          update_time datetime default current_timestamp on update current_timestamp,
          deleted tinyint not null default 0,
          key idx_submission_status_time (status, create_time, deleted),
          key idx_submission_submitter (submitter_id, status, deleted)
        ) engine=InnoDB default charset=utf8mb4 comment='公共资源投稿'
        """
    )


def seed_roles(insert, now):
    roles = {}
    rows = [
        ("普通用户", "USER", "公共资源学习、交流、投稿和加入课堂"),
        ("系统管理员", "ADMIN", "平台治理、权限、审核与日志"),
    ]
    for name, code, desc in rows:
        roles[code] = insert("t_role", dict(role_name=name, role_code=code, description=desc, create_time=now))
    return roles


def seed_permissions(insert, now):
    rows = [
        ("门户访问", "portal:view"), ("门户搜索", "portal:search"), ("消息日程", "portal:message"),
        ("公告管理", "portal:notice:manage"), ("个人资料", "profile:update"), ("修改密码", "profile:password"),
        ("仪表盘", "dashboard:view"), ("AI问答", "ai:ask"),
        ("课程查看", "course:view"), ("课程创建", "course:create"), ("课程更新", "course:update"),
        ("课程删除", "course:delete"), ("课程发布", "course:publish"), ("课程归档", "course:archive"),
        ("课程复制", "course:copy"), ("教学班管理", "course:class:manage"),
        ("课堂成员管理", "course:student:manage"), ("课堂加入", "course:join"),
        ("课堂邀请码管理", "course:invite:manage"),
        ("实验查看", "experiment:view"), ("实验创建", "experiment:create"), ("实验更新", "experiment:update"),
        ("实验删除", "experiment:delete"),
        ("资源查看", "resource:view"), ("资源创建", "resource:create"), ("资源更新", "resource:update"),
        ("资源删除", "resource:delete"), ("资源投稿", "resource-submission:create"),
        ("资源投稿审核", "resource-submission:review"), ("学习记录", "learning:update:self"),
        ("考试参加", "exam:take"), ("试卷查看", "exam-paper:view"), ("试卷创建", "exam-paper:create"),
        ("试卷更新", "exam-paper:update"), ("试卷删除", "exam-paper:delete"), ("主观题评分", "exam:grade"),
        ("预约查看", "reservation:view"), ("预约管理", "reservation:manage"), ("预约审核", "reservation:review"),
        ("报告查看", "report:view"), ("报告批改", "report:review"), ("报告评分", "report:grade"),
        ("用户查看", "user:view"), ("用户创建", "user:create"), ("用户更新", "user:update"),
        ("用户删除", "user:delete"), ("角色查看", "role:view"), ("角色权限", "role:permission:update"),
        ("权限查看", "permission:view"), ("操作日志", "operation-log:view"),
        ("安全知识查看", "safety:view"), ("安全知识创建", "safety:create"),
        ("安全知识更新", "safety:update"), ("安全知识删除", "safety:delete"),
        ("教师认证申请", "teacher-certification:apply"), ("教师认证审核", "teacher-certification:review"),
    ]
    ids = {}
    for idx, (name, code) in enumerate(rows, 1):
        ids[code] = insert("t_permission", dict(
            name=name, code=code, type=2, parent_id=0, path=None, icon=None, sort=idx * 10, create_time=now
        ))
    return ids


def seed_role_permissions(insert, roles, perms):
    role_codes = {
        "USER": [
            "portal:view", "portal:search", "portal:message", "profile:update", "profile:password",
            "course:view", "course:join", "experiment:view", "resource:view", "learning:update:self", "exam:take",
            "reservation:view", "report:view", "report:submit", "safety:view", "ai:ask", "teacher-certification:apply",
            "resource-submission:create",
        ],
        "ADMIN": list(perms),
    }
    for role, codes in role_codes.items():
        for code in codes:
            insert("t_role_permission", dict(role_id=roles[role], permission_id=perms[code]))


def seed_users(insert, roles, now):
    password = hashlib.md5("123456".encode("utf-8")).hexdigest()
    rows = [
        ("admin", "系统管理员", "ADMIN", "admin@cupk.edu.cn", "平台治理中心", None),
        ("teacher_wang", "王海峰", "USER", "wanghf@cupk.edu.cn", "石油工程学院", None),
        ("teacher_li", "李晨曦", "USER", "licx@cupk.edu.cn", "安全工程学院", None),
        ("student_zhang", "张雨辰", "USER", "zhangyc@example.com", "石油工程", "油工2301"),
        ("student_li", "李思源", "USER", "lisy@example.com", "油气储运", "储运2302"),
        ("student_chen", "陈若冰", "USER", "chenrb@example.com", "安全工程", "安工2301"),
        ("user_oilfan", "赵一鸣", "USER", "oilfan@example.com", "油气工程兴趣用户", "公开学习者"),
        ("user_guest", "周晓禾", "USER", "guest@example.com", "跨专业学习", "公开学习者"),
    ]
    users = {}
    for username, real, role, email, major, klass in rows:
        uid = insert("t_user", dict(
            username=username, password=password, real_name=real, phone=f"1380000{len(users)+1:04d}",
            avatar_url=None, major=major, class_name=klass, email=email, status=1,
            create_time=now, update_time=now,
        ))
        users[username] = uid
        insert("t_user_role", dict(user_id=uid, role_id=roles[role]))
    return users


def seed_teacher_certifications(insert, users, now):
    rows = [
        ("teacher_wang", "中国石油大学", "T2026001", "wanghf@cupk.edu.cn", "APPROVED", "教师身份核验通过", 20),
        ("teacher_li", "中国石油大学", "T2026002", "licx@cupk.edu.cn", "APPROVED", "教师身份核验通过", 18),
        ("user_guest", "西部能源学院", "EN202612", "guest@edu.cn", "PENDING", None, 0),
    ]
    for username, school, employee_no, email, status, comment, days in rows:
        insert("t_teacher_certification", dict(
            user_id=users[username], school=school, employee_no=employee_no, education_email=email,
            status=status, reviewer_id=users["admin"] if status != "PENDING" else None,
            review_comment=comment, review_time=None if status == "PENDING" else now - timedelta(days=days),
            create_time=now - timedelta(days=days + 1 if days else 0), update_time=now, deleted=0,
        ))


def seed_courses(insert, users, now):
    courses = {}
    course_rows = [
        ("mud", "OG-LAB-101", "钻井液性能测试实验课堂", "石油工程", "teacher_wang", "钻井液,井控,安全准入"),
        ("pipe", "OG-LAB-202", "油气集输与管输压降实验课堂", "油气储运", "teacher_wang", "管输,压降,阀门"),
        ("hse", "OG-LAB-303", "HSE风险识别与应急处置课堂", "安全工程", "teacher_li", "HSE,应急,风险识别"),
    ]
    for idx, (key, code, name, direction, teacher, tags) in enumerate(course_rows, 1):
        courses[key] = insert("t_lab_course", dict(
            course_code=code, course_name=name, direction=direction, teacher_id=users[teacher], semester="2026秋",
            description=f"{name}演示课堂，覆盖资源预习、风险认知、准入考核、预约、报告和答疑。",
            cover_url="/src/assets/amazing/lab-hero.png", tagline="让油气实验知识看得懂、能操作、可考核。",
            highlight_tags=tags, visual_theme=key, status=1, sort=idx, credit=1.5, hours=24,
            assessment_method="资源预习30% + 安全准入30% + 报告40%",
            learning_requirement="完成预习资源、风险识别与准入任务后进入实验预约。",
            allow_empty_publish=1, archive_time=None, create_time=now - timedelta(days=30 - idx), update_time=now, deleted=0,
        ))
    classes = {
        "mud": insert("t_teaching_class", dict(course_id=courses["mud"], class_name="钻井液实验1班", teacher_id=users["teacher_wang"], assistant_id=users["teacher_li"], admin_class="油工2301", semester="2026秋", status=1, create_time=now, update_time=now, deleted=0)),
        "pipe": insert("t_teaching_class", dict(course_id=courses["pipe"], class_name="集输实验2班", teacher_id=users["teacher_wang"], assistant_id=None, admin_class="储运2302", semester="2026秋", status=1, create_time=now, update_time=now, deleted=0)),
        "hse": insert("t_teaching_class", dict(course_id=courses["hse"], class_name="HSE案例研讨班", teacher_id=users["teacher_li"], assistant_id=None, admin_class="安工2301", semester="2026秋", status=1, create_time=now, update_time=now, deleted=0)),
    }
    return courses, classes


def seed_experiments(insert, courses, users, now):
    exp_specs = [
        ("mud_density", courses["mud"], "MUD-DENSITY", "钻井液密度与流变性能测试", "MEDIUM", "钻井液密度、黏度与滤失量测试。"),
        ("well_control", courses["mud"], "WELL-CONTROL", "井控风险识别与关井流程演示", "HIGH", "溢流信号识别与关井流程模拟。"),
        ("pipe_drop", courses["pipe"], "PIPE-DROP", "管输流量与压降关系实验", "MEDIUM", "测量不同流量下管路压降。"),
        ("hse_case", courses["hse"], "HSE-CASE", "油气实验室事故案例复盘", "LOW", "事故链分析与应急卡片制作。"),
    ]
    exps = {}
    for idx, (key, course_id, code, name, risk, desc) in enumerate(exp_specs, 1):
        exps[key] = insert("t_experiment", dict(
            course_id=course_id, exp_code=code, direction="油气工程", cover_url="/src/assets/amazing/procedure-safety.png",
            scenario_intro=f"{name}工程情境导入。", visual_theme=key, description=desc, exp_name=name,
            objective="理解工程参数、设备操作与安全风险之间的关系。", principle="通过实验数据和流程节点建立工程判断。",
            equipment="实验台、传感器、记录表、个人防护用品", materials="模拟样品与案例数据", location=f"油气工程实验楼 {idx}01",
            applicable_classes="2026秋课堂", risk_level=risk, hazard_sources="高压、旋转设备、样品飞溅或误操作",
            risk_types="高压,机械,飞溅,HSE", ppe_requirements="实验服、护目镜、手套、防滑鞋",
            prerequisite_knowledge="基础实验安全、设备认知、风险识别", safety_requirement="完成准入学习与考试后操作。",
            exam_required=0 if key == "hse_case" else 1, duration_minutes=90, safety_pass_score=70,
            data_record_requirement="完整记录实验数据和异常现象。", abnormal_handling="设备异常立即停机并报告教师。",
            emergency_procedure="按应急预案撤离或处置。", report_template_url=None,
            grading_criteria="数据完整30，分析40，安全复盘30", reservation_enabled=0 if key == "hse_case" else 1,
            status=1, sort=idx, create_time=now, update_time=now, deleted=0,
        ))
        insert("t_experiment_step", dict(
            experiment_id=exps[key], step_no=1, title="工程情境导入", content=f"进入{name}的现场问题。",
            safety_tip="先读风险提示，再进入操作。", media_type="VIDEO", media_url=f"/demo/{key}.mp4",
            flowchart_data=None, required_flag=1, estimated_minutes=12, create_time=now, update_time=now, deleted=0,
        ))
        insert("t_experiment_step", dict(
            experiment_id=exps[key], step_no=2, title="设备与风险识别", content="识别关键设备、危险源和个人防护要求。",
            safety_tip="未经教师确认不得启动设备。", media_type="IMAGE", media_url=f"/demo/{key}.png",
            flowchart_data=None, required_flag=1, estimated_minutes=15, create_time=now, update_time=now, deleted=0,
        ))
    return exps


def seed_course_members(insert, courses, classes, users, now):
    rows = [
        ("mud", "student_zhang", "A组"), ("mud", "student_chen", "B组"),
        ("pipe", "student_li", "管输小组"), ("pipe", "user_oilfan", "旁听项目组"),
        ("hse", "student_chen", "HSE复盘组"), ("hse", "user_guest", "公开加入组"),
    ]
    for course_key, username, group in rows:
        insert("t_course_student", dict(
            course_id=courses[course_key], teaching_class_id=classes[course_key], student_id=users[username],
            semester="2026秋", group_name=group, remark="演示数据", status=1,
            join_time=now - timedelta(days=10), create_time=now, update_time=now, deleted=0,
        ))


def seed_resources(insert, courses, exps, users, now):
    specs = [
        ("mud_video", courses["mud"], exps["mud_density"], "钻井液性能测试沉浸视频", "TEACHING_VIDEO", "钻井液密度", "飞溅,滑倒", "COURSE", 1),
        ("well_video", courses["mud"], exps["well_control"], "井控关井流程动画", "TEACHING_VIDEO", "井控关井", "高压,误操作", "COURSE", 1),
        ("pipe_doc", courses["pipe"], exps["pipe_drop"], "管输压降实验指导书", "DOCUMENT", "管输压降", "压力,机械", "COURSE", 1),
        ("hse_open", courses["hse"], exps["hse_case"], "油气实验室事故案例开放课", "LINK", "事故链分析", "HSE", "PUBLIC", 0),
        ("mud_image", courses["mud"], exps["mud_density"], "马氏漏斗操作图解", "IMAGE", "漏斗黏度", "玻璃器皿", "PUBLIC", 0),
    ]
    resources = {}
    for idx, (key, course_id, exp_id, title, rtype, point, risk, scope, required) in enumerate(specs, 1):
        resources[key] = insert("t_resource", dict(
            course_id=course_id, experiment_id=exp_id, title=title, resource_type=rtype, knowledge_point=point,
            risk_type=risk, tags=f"{point},{risk},油气实验", category="PREVIEW" if required else "EXTENSION",
            description=f"{title}，用于理解实验目标、设备和风险节点。", url=f"https://example.com/{key}",
            file_path=None, original_filename=None, content_type="text/html" if rtype == "LINK" else "video/mp4",
            file_size=0, required_flag=required, completion_rule="PROGRESS_TIME" if required else "CONFIRM",
            min_study_seconds=300 if required else 0, min_progress=85 if required else 100,
            open_time=None, close_time=None, open_scope=scope, invalid_flag=0, invalid_check_time=None,
            view_count=20 + idx, download_count=idx, favorite_count=idx + 1, like_count=idx + 3,
            comment_count=0, rating_avg=4.6, rating_count=3, status=1, sort=idx,
            upload_user_id=users["teacher_wang"], create_time=now, update_time=now, deleted=0,
        ))
    return resources


def seed_learning_assets(insert, courses, exps, resources, users, now):
    safety = [
        (exps["mud_density"], "密度计校准", "飞溅", "密度测试前应校准水平泡并排除气泡。"),
        (exps["well_control"], "井控异常信号", "高压", "返出流量异常和立压变化是井控风险信号。"),
        (exps["pipe_drop"], "泵启动前检查", "机械", "启动泵前确认阀门开度和管路无泄漏。"),
        (exps["hse_case"], "事故链分析", "HSE", "从人、机、环、管四类因素复盘控制点。"),
    ]
    for exp_id, point, risk, content in safety:
        insert("t_safety_knowledge", dict(
            experiment_id=exp_id, category="HSE_BASIC", knowledge_point=point, risk_type=risk, content=content,
            related_step_id=None, reference_resource_id=None, emergency_flag=1 if risk in ("高压", "HSE") else 0,
            status=1, create_time=now, update_time=now, deleted=0,
        ))
    task_specs = [
        (courses["mud"], exps["mud_density"], "观看钻井液预习视频", "RESOURCE", resources["mud_video"], None, 1),
        (courses["mud"], exps["mud_density"], "提交钻井液实验报告", "REPORT", None, None, 2),
        (courses["pipe"], exps["pipe_drop"], "阅读管输压降指导书", "RESOURCE", resources["pipe_doc"], None, 1),
        (courses["hse"], exps["hse_case"], "学习HSE事故案例", "RESOURCE", resources["hse_open"], None, 1),
    ]
    task_ids = []
    for course_id, exp_id, name, kind, rid, pid, sort in task_specs:
        task_ids.append(insert("t_learning_task", dict(
            course_id=course_id, experiment_id=exp_id, task_name=name, task_type=kind,
            target_resource_id=rid, target_knowledge_id=None, target_paper_id=pid,
            prerequisite_task_id=None, required_flag=1, sort=sort,
            open_time=now - timedelta(days=5), deadline=now + timedelta(days=20),
            completion_rule="AUTO", status=1, create_time=now, update_time=now, deleted=0,
        )))
    insert("t_learning_record", dict(
        student_id=users["student_zhang"], resource_id=resources["mud_video"], experiment_id=exps["mud_density"],
        progress=96, duration_seconds=620, last_position_seconds=600, note="密度计读数前要排气泡。",
        finish_flag=1, first_time=now - timedelta(days=3), last_time=now - timedelta(days=1),
        create_time=now, update_time=now, deleted=0,
    ))
    insert("t_learning_record", dict(
        student_id=users["student_li"], resource_id=resources["pipe_doc"], experiment_id=exps["pipe_drop"],
        progress=55, duration_seconds=260, last_position_seconds=120, note="压降曲线还需复习。",
        finish_flag=0, first_time=now - timedelta(days=2), last_time=now, create_time=now, update_time=now, deleted=0,
    ))
    for task_id, username, status in [
        (task_ids[0], "student_zhang", "COMPLETED"),
        (task_ids[2], "student_li", "IN_PROGRESS"),
        (task_ids[3], "student_chen", "COMPLETED"),
    ]:
        insert("t_learning_task_record", dict(
            task_id=task_id, student_id=users[username], status=status, start_time=now - timedelta(days=3),
            complete_time=now - timedelta(days=1) if status == "COMPLETED" else None, source_type="DEMO",
            create_time=now, update_time=now, deleted=0,
        ))
    insert("t_resource_timeline_note", dict(
        resource_id=resources["mud_video"], experiment_id=exps["mud_density"], user_id=users["student_zhang"],
        position_seconds=180, note_type="QUESTION", content="密度计读数前气泡如何快速排除？",
        visibility="COURSE", status=1, create_time=now, update_time=now, deleted=0,
    ))


def seed_exams(insert, courses, exps, resources, users, now):
    q1 = insert("t_question", dict(
        type="SINGLE", content="钻井液密度测试时首先应确认什么？",
        options=json.dumps([{"key": "A", "label": "密度计已校准"}, {"key": "B", "label": "随意取样"}], ensure_ascii=False),
        answer="A", score=20, analysis="密度计校准是读数可靠的前提。", knowledge_point="密度计校准",
        knowledge_id=None, experiment_id=exps["mud_density"], risk_type="飞溅", related_resource_id=resources["mud_video"],
        difficulty="EASY", course_id=courses["mud"], create_by=users["teacher_wang"],
        create_time=now, update_time=now, is_deleted=0,
    ))
    q2 = insert("t_question", dict(
        type="MULTIPLE", content="井控风险识别应关注哪些信号？",
        options=json.dumps([{"key": "A", "label": "立压变化"}, {"key": "B", "label": "返出流量异常"}, {"key": "D", "label": "泥浆池液面变化"}], ensure_ascii=False),
        answer="A,B,D", score=30, analysis="压力、流量和液面变化是关键判断依据。", knowledge_point="井控异常信号",
        knowledge_id=None, experiment_id=exps["well_control"], risk_type="高压", related_resource_id=resources["well_video"],
        difficulty="MEDIUM", course_id=courses["mud"], create_by=users["teacher_wang"],
        create_time=now, update_time=now, is_deleted=0,
    ))
    paper = insert("t_exam_paper", dict(
        title="钻井液实验安全准入考试", description="完成后可进入实验预约。",
        course_id=courses["mud"], experiment_id=exps["mud_density"], total_score=100, pass_score=70,
        duration=30, attempt_limit=3, show_answer_after_submit=1, admission_validity_days=90,
        multiple_score_policy="ALL_OR_NOTHING", random_enabled=0, random_count=0, teacher_id=users["teacher_wang"],
        status="PUBLISHED", start_time=now - timedelta(days=5), end_time=now + timedelta(days=90),
        create_time=now, update_time=now, is_deleted=0,
    ))
    insert("t_exam_paper_question", dict(paper_id=paper, question_id=q1, score=50, order_num=1))
    insert("t_exam_paper_question", dict(paper_id=paper, question_id=q2, score=50, order_num=2))
    record = insert("t_exam_record", dict(
        student_id=users["student_zhang"], paper_id=paper, experiment_id=exps["mud_density"],
        total_score=90, objective_score=90, subjective_score=0, status="GRADED",
        question_snapshot_json=json.dumps([
            {"id": q1, "type": "SINGLE", "content": "钻井液密度测试时首先应确认什么？", "answer": "A", "score": 50},
            {"id": q2, "type": "MULTIPLE", "content": "井控风险识别应关注哪些信号？", "answer": "A,B,D", "score": 50},
        ], ensure_ascii=False),
        auto_submit_flag=0, admission_id=None, passed=1,
        start_time=now - timedelta(days=2, hours=1), submit_time=now - timedelta(days=2),
        end_time=now - timedelta(days=2, hours=1), last_save_time=now - timedelta(days=2),
        final_grade_time=now - timedelta(days=2), create_time=now, update_time=now, deleted=0,
    ))
    insert("t_exam_answer", dict(record_id=record, question_id=q1, knowledge_id=None, student_answer="A", is_correct=1, correct_flag=1, score=50))
    insert("t_exam_answer", dict(record_id=record, question_id=q2, knowledge_id=None, student_answer="A,B,D", is_correct=1, correct_flag=1, score=40))
    admission = insert("t_experiment_admission", dict(
        student_id=users["student_zhang"], experiment_id=exps["mud_density"], paper_id=paper, record_id=record,
        status="VALID", issued_time=now - timedelta(days=2), valid_until=now + timedelta(days=88),
        revoke_time=None, revoked_by=None, revoke_reason=None, create_time=now, update_time=now, deleted=0,
    ))
    return {"mud": paper}, {"q1": q1, "q2": q2, "record": record, "admission": admission}


def seed_reports_and_reservations(insert, courses, exps, papers, users, now):
    for exp_id, title in [
        (exps["mud_density"], "钻井液性能测试实验报告模板"),
        (exps["pipe_drop"], "管输压降实验报告模板"),
        (exps["hse_case"], "HSE事故案例复盘报告模板"),
    ]:
        insert("t_report_template", dict(
            experiment_id=exp_id, title=title,
            schema_json=json.dumps({"sections": ["实验目的", "数据记录", "结果分析", "风险复盘"]}, ensure_ascii=False),
            status=1, create_time=now, update_time=now, deleted=0,
        ))
        insert("t_report_rubric_item", dict(
            experiment_id=exp_id, item_name="数据与证据", description="数据完整、单位清晰、证据充分",
            max_score=40, order_no=1, create_time=now, update_time=now, deleted=0,
        ))
        insert("t_report_rubric_item", dict(
            experiment_id=exp_id, item_name="分析与复盘", description="能解释现象并提出安全改进",
            max_score=60, order_no=2, create_time=now, update_time=now, deleted=0,
        ))
    slot1 = insert("t_lab_time_slot", dict(
        lab_id=101, experiment_id=exps["mud_density"], date=(now + timedelta(days=3)).date(),
        start_time=time(9, 0), end_time=time(11, 0), capacity=24, booked_count=1,
        status="AVAILABLE", create_by=users["teacher_wang"], create_time=now, update_time=now,
    ))
    slot2 = insert("t_lab_time_slot", dict(
        lab_id=102, experiment_id=exps["pipe_drop"], date=(now + timedelta(days=4)).date(),
        start_time=time(14, 0), end_time=time(16, 0), capacity=20, booked_count=1,
        status="AVAILABLE", create_by=users["teacher_wang"], create_time=now, update_time=now,
    ))
    insert("t_reservation", dict(
        student_id=users["student_zhang"], time_slot_id=slot1, lab_id=101, experiment_id=exps["mud_density"],
        purpose="完成钻井液性能测试实验", status="APPROVED", teacher_id=users["teacher_wang"],
        review_comment="准入已通过，按时到场。", review_time=now - timedelta(days=1),
        create_time=now, update_time=now, deleted=0,
    ))
    insert("t_reservation", dict(
        student_id=users["student_li"], time_slot_id=slot2, lab_id=102, experiment_id=exps["pipe_drop"],
        purpose="管输压降实验预约", status="PENDING", teacher_id=users["teacher_wang"],
        review_comment=None, review_time=None, create_time=now, update_time=now, deleted=0,
    ))
    report = insert("t_report", dict(
        student_id=users["student_zhang"], experiment_id=exps["mud_density"],
        title="钻井液密度与流变性能测试报告", content="数据完整，结果满足目标窗口，需进一步关注读数误差。",
        file_url=None, status="GRADED", submit_time=now - timedelta(days=1),
        latest_submit_time=now - timedelta(days=1), create_time=now, update_time=now, is_deleted=0,
    ))
    insert("t_report_score", dict(
        report_id=report, teacher_id=users["teacher_wang"], score=88, comment="数据完整，风险复盘可以再具体。",
        is_latest=1, create_time=now, grade_time=now,
    ))
    insert("t_report", dict(
        student_id=users["student_li"], experiment_id=exps["pipe_drop"], title="管输压降实验预报告",
        content="已完成指导书阅读，等待实验数据。", file_url=None, status="DRAFT",
        submit_time=None, latest_submit_time=None, create_time=now, update_time=now, is_deleted=0,
    ))


def seed_public_and_social(insert, courses, classes, exps, resources, users, now):
    topic = insert("t_discussion_topic", dict(
        course_id=courses["mud"], experiment_id=exps["mud_density"], user_id=users["student_zhang"],
        title="密度计读数为什么会波动？", content="同一杯样品重复读数有小幅变化，是气泡还是温度影响？",
        status="OPEN", is_anonymous=0, is_featured=1, reply_count=1,
        create_time=now - timedelta(days=1), update_time=now, deleted=0,
    ))
    insert("t_discussion_reply", dict(
        topic_id=topic, user_id=users["teacher_wang"],
        content="优先检查样品气泡和密度计刀口是否清洁，再记录环境温度。",
        is_teacher_reply=1, create_time=now, update_time=now, deleted=0,
    ))
    insert("t_discussion_topic", dict(
        course_id=courses["pipe"], experiment_id=exps["pipe_drop"], user_id=users["user_oilfan"],
        title="公共学习用户加入课堂后可以提交报告吗？", content="已通过邀请码加入，想确认是否能完整参与课堂任务。",
        status="OPEN", is_anonymous=0, is_featured=0, reply_count=0, create_time=now, update_time=now, deleted=0,
    ))
    for key, code, used, creator in [
        ("mud", "MUD2026", 2, "teacher_wang"), ("pipe", "PIPE2026", 1, "teacher_wang"), ("hse", "HSE2026", 1, "teacher_li"),
    ]:
        insert("t_class_invite", dict(
            course_id=courses[key], teaching_class_id=classes[key], invite_code=code,
            expire_time=now + timedelta(days=60), max_uses=100, used_count=used, status=1,
            created_by=users[creator], create_time=now, update_time=now, deleted=0,
        ))
    for username, title, status, reviewer, comment in [
        ("user_oilfan", "井筒压力平衡公开动画", "APPROVED", "teacher_wang", "资源适合公开学习，审核通过"),
        ("user_guest", "海上平台HSE短视频合集", "PENDING", None, None),
        ("student_li", "无关娱乐网站链接", "REJECTED", "admin", "与油气工程学习无关"),
    ]:
        insert("t_resource_submission", dict(
            submitter_id=users[username], title=title, resource_type="LINK", knowledge_point="油气工程",
            risk_type="HSE", tags="油气,HSE,公开资源", description=f"{title}演示投稿。",
            url=f"https://example.com/{username}-{status.lower()}", file_path=None, original_filename=None,
            content_type="text/html", status=status, reviewer_id=users[reviewer] if reviewer else None,
            review_comment=comment, review_time=now if reviewer else None, public_resource_id=None,
            create_time=now, update_time=now, deleted=0,
        ))
    insert("t_resource_interaction", dict(
        resource_id=resources["mud_video"], user_id=users["student_zhang"], favorite_flag=1, like_flag=1,
        rating=4.5, comment="视频步骤清楚。", create_time=now, update_time=now, deleted=0,
    ))
    insert("t_ai_chat_record", dict(
        user_id=users["student_zhang"], scene="RESOURCE_EXPLAIN",
        question="用一句话解释钻井液密度为什么影响井控？",
        answer="钻井液密度决定井底压力窗口，过低会诱发溢流，过高可能压漏地层。",
        tool_name="AI解释", experiment_id=exps["mud_density"], manual_revision=None, create_time=now,
    ))
    insert("t_portal_notice", dict(
        title="平台演示数据已重置", content="所有演示账号密码均为 123456。",
        target_role="ALL", priority="HIGH", status=1, publish_time=now, expire_time=now + timedelta(days=30),
        create_by=users["admin"], create_time=now, update_time=now, deleted=0,
    ))
    messages = [
        ("student_zhang", "钻井液实验预约已通过", "请按预约时间到 A203 实验室。", "/student/reserve"),
        ("teacher_wang", "有新的课堂问题", "学生提出密度计读数波动问题。", "/discussions"),
        ("admin", "有待审核教师认证", "请审核普通用户提交的教师认证。", "/admin/teacher-certifications"),
        ("user_guest", "教师认证申请已提交", "管理员审核后将获得创建课堂权限。", "/profile"),
    ]
    for username, title, content, path in messages:
        insert("t_portal_message", dict(
            user_id=users[username], title=title, content=content, biz_type="DEMO", biz_id=None,
            path=path, read_flag=0, read_time=None, create_time=now, deleted=0,
        ))
    for username, title, path, module in [
        ("student_zhang", "我的课堂", "/classrooms", "classroom"),
        ("user_oilfan", "资源学习", "/resources", "resource"),
        ("teacher_wang", "课堂管理", "/teacher/courses", "course"),
        ("admin", "教师认证审核", "/admin/teacher-certifications", "admin"),
    ]:
        insert("t_recent_visit", dict(
            user_id=users[username], title=title, path=path, module=module, visit_count=1,
            last_visit_time=now, create_time=now, update_time=now,
        ))
        insert("t_user_shortcut", dict(
            user_id=users[username], title=title, path=path, icon=module, sort=1, create_time=now, update_time=now,
        ))


if __name__ == "__main__":
    main()
