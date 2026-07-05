-- ============================================
-- 完整种子数据
-- 密码统一为 123456
-- MD5('123456') = e10adc3949ba59abbe56e057f20f883e
-- ============================================

USE `og-expsafetyplatform`;

-- ==================== 1. 用户 ====================
INSERT IGNORE INTO t_user (username, password, real_name, phone, status) VALUES
('admin',      'e10adc3949ba59abbe56e057f20f883e', '系统管理员', '13800000001', 1),
('teacher01',  'e10adc3949ba59abbe56e057f20f883e', '张教授',     '13800000002', 1),
('teacher02',  'e10adc3949ba59abbe56e057f20f883e', '李副教授',   '13800000003', 1),
('student01',  'e10adc3949ba59abbe56e057f20f883e', '王同学',     '13800000004', 1),
('student02',  'e10adc3949ba59abbe56e057f20f883e', '赵同学',     '13800000005', 1),
('student03',  'e10adc3949ba59abbe56e057f20f883e', '刘同学',     '13800000006', 1),
('lab_admin',  'e10adc3949ba59abbe56e057f20f883e', '陈实验员',   '13800000007', 1);

-- ==================== 2. 角色分配 ====================
INSERT IGNORE INTO t_user_role (user_id, role_id)
SELECT u.id, r.id FROM t_user u, t_role r WHERE u.username = 'admin'     AND r.role_code = 'ADMIN';
INSERT IGNORE INTO t_user_role (user_id, role_id)
SELECT u.id, r.id FROM t_user u, t_role r WHERE u.username = 'teacher01' AND r.role_code = 'TEACHER';
INSERT IGNORE INTO t_user_role (user_id, role_id)
SELECT u.id, r.id FROM t_user u, t_role r WHERE u.username = 'teacher02' AND r.role_code = 'TEACHER';
INSERT IGNORE INTO t_user_role (user_id, role_id)
SELECT u.id, r.id FROM t_user u, t_role r WHERE u.username = 'student01' AND r.role_code = 'STUDENT';
INSERT IGNORE INTO t_user_role (user_id, role_id)
SELECT u.id, r.id FROM t_user u, t_role r WHERE u.username = 'student02' AND r.role_code = 'STUDENT';
INSERT IGNORE INTO t_user_role (user_id, role_id)
SELECT u.id, r.id FROM t_user u, t_role r WHERE u.username = 'student03' AND r.role_code = 'STUDENT';
INSERT IGNORE INTO t_user_role (user_id, role_id)
SELECT u.id, r.id FROM t_user u, t_role r WHERE u.username = 'lab_admin' AND r.role_code = 'LAB_ADMIN';

-- ==================== 3. 课程 ====================
INSERT IGNORE INTO t_lab_course (id, course_code, course_name, direction, teacher_id, semester, description, status, sort) VALUES
(1, 'PE2026-01', '油气井钻井工程实验', '钻井工程', (SELECT id FROM t_user WHERE username='teacher01'), '2025-2026-2', '本课程涵盖钻井液性能测试、固井水泥浆体系设计、钻井压力控制等核心实验项目，培养学生掌握钻井工程基本实验技能。', 1, 1),
(2, 'PE2026-02', '油气藏开发实验',     '油藏工程', (SELECT id FROM t_user WHERE username='teacher01'), '2025-2026-2', '包含岩石物性测定、相对渗透率实验、驱油效率评价等内容，使学生理解油气藏开发的基本实验方法。', 1, 2),
(3, 'PE2026-03', '采油工程实验',       '采油工程', (SELECT id FROM t_user WHERE username='teacher02'), '2025-2026-2', '涵盖抽油机系统效率测试、注水井吸水剖面测试、压裂液性能评价等采油工程核心实验。', 1, 3);

-- ==================== 4. 学生选课 ====================
INSERT IGNORE INTO t_course_student (id, course_id, student_id, semester, status, join_time)
SELECT 1, 1, s.id, '2025-2026-2', 1, '2026-02-20 09:00:00' FROM t_user s WHERE s.username='student01';
INSERT IGNORE INTO t_course_student (id, course_id, student_id, semester, status, join_time)
SELECT 2, 1, s.id, '2025-2026-2', 1, '2026-02-20 09:00:00' FROM t_user s WHERE s.username='student02';
INSERT IGNORE INTO t_course_student (id, course_id, student_id, semester, status, join_time)
SELECT 3, 1, s.id, '2025-2026-2', 1, '2026-02-21 10:00:00' FROM t_user s WHERE s.username='student03';
INSERT IGNORE INTO t_course_student (id, course_id, student_id, semester, status, join_time)
SELECT 4, 2, s.id, '2025-2026-2', 1, '2026-02-20 09:00:00' FROM t_user s WHERE s.username='student01';
INSERT IGNORE INTO t_course_student (id, course_id, student_id, semester, status, join_time)
SELECT 5, 2, s.id, '2025-2026-2', 1, '2026-02-20 09:00:00' FROM t_user s WHERE s.username='student02';
INSERT IGNORE INTO t_course_student (id, course_id, student_id, semester, status, join_time)
SELECT 6, 3, s.id, '2025-2026-2', 1, '2026-02-21 10:00:00' FROM t_user s WHERE s.username='student03';

-- ==================== 5. 实验项目 ====================
INSERT IGNORE INTO t_experiment (id, course_id, exp_code, exp_name, objective, risk_level, duration_minutes, safety_pass_score, status, sort) VALUES
(1, 1, 'EXP-01-001', '钻井液密度与流变性测定',
   '掌握钻井液密度计和旋转粘度计的使用方法；理解钻井液流变参数（表观粘度、塑性粘度、动切力）对钻井安全的影响；能够根据实验数据判断钻井液性能是否符合工程要求。',
   '中风险', 90, 70, 1, 1),
(2, 1, 'EXP-01-002', '地层破裂压力试验',
   '通过模拟地层条件掌握地层破裂压力测试原理及安全操作流程；学习高压实验设备的使用规范；理解地层破裂压力对钻井液密度窗口设计的指导意义。',
   '高风险', 120, 80, 1, 2),
(3, 2, 'EXP-02-001', '岩石孔隙度与渗透率测定',
   '掌握氦气孔隙度测定仪和渗透率测定仪的操作方法；理解孔隙度和渗透率作为储层核心物性参数的地质意义；学习实验数据的误差分析与处理方法。',
   '低风险', 90, 60, 1, 1),
(4, 2, 'EXP-02-002', '油水相对渗透率测定',
   '掌握非稳态法测定油水相对渗透率的实验原理与操作流程；理解相对渗透率曲线在油藏开发方案设计中的应用；学习JBN方法进行数据处理。',
   '中风险', 120, 70, 1, 2),
(5, 3, 'EXP-03-001', '抽油机工况测试与诊断',
   '掌握示功图测试方法和井下泵工作状态判断技术；分析抽油系统效率及其影响因素；学习利用示功图进行故障诊断的基本方法。',
   '中风险', 90, 65, 1, 1);

-- ==================== 6. 实验步骤 ====================

-- EXP-01-001: 钻井液密度与流变性测定 (4步)
INSERT IGNORE INTO t_experiment_step (id, experiment_id, step_no, title, content, safety_tip, required_flag, estimated_minutes) VALUES
(1, 1, 1, '实验前准备与安全检查',
   '1. 穿戴实验服、安全帽、护目镜、防滑手套\n2. 检查密度计、粘度计校准有效期\n3. 检查电源线路是否完好\n4. 准备待测钻井液样品，充分搅拌使其均匀',
   '必须穿戴完整PPE，未穿戴不得进入实验区域；检查设备接地是否良好', 1, 10),
(2, 1, 2, '钻井液密度测定',
   '1. 将钻井液样品缓慢倒入密度计杯中，避免产生气泡\n2. 盖上杯盖并旋紧，擦净溢出液体\n3. 将密度计置于支架上，移动游码至平衡位置\n4. 读取密度值并记录，精确到0.01g/cm³\n5. 重复测量3次，取平均值',
   '防止钻井液溅入眼睛；若发生接触，立即用洗眼器冲洗15分钟并报告指导教师', 1, 20),
(3, 1, 3, '钻井液流变性测定',
   '1. 将旋转粘度计调整至水平状态\n2. 依次选择600rpm和300rpm档位\n3. 将转子浸入钻井液至刻度线\n4. 启动电机，待读数稳定后记录Φ600和Φ300\n5. 计算表观粘度AV=Φ600/2、塑性粘度PV=Φ600-Φ300、动切力YP=Φ300-PV',
   '旋转部件运转时严禁用手触碰；换档必须先停机；注意高温杯体防烫', 1, 30),
(4, 1, 4, '数据整理与实验报告',
   '1. 整理实验数据，填写数据记录表\n2. 计算流变参数并与标准值对比\n3. 判断钻井液性能是否符合工程要求\n4. 分析实验中可能存在的误差来源\n5. 撰写实验报告并提交',
   '实验数据必须如实记录，不得篡改或伪造', 1, 30);

-- EXP-01-002: 地层破裂压力试验 (5步)
INSERT IGNORE INTO t_experiment_step (id, experiment_id, step_no, title, content, safety_tip, required_flag, estimated_minutes) VALUES
(5, 2, 1, '高压实验安全培训',
   '1. 学习高压容器操作规程\n2. 了解紧急泄压装置位置与使用方法\n3. 检查防护屏是否完好\n4. 确认通讯设备畅通',
   '高压实验存在严重安全隐患，必须全程在指导教师监督下进行；严禁单独操作高压设备', 1, 15),
(6, 2, 2, '实验装置搭建与检查',
   '1. 安装岩心夹持器并检查密封性\n2. 连接液压泵与压力传感器\n3. 校准压力表和数据采集系统\n4. 低压试运行，检查各连接处是否泄漏',
   '加压前必须确认所有接头已紧固；试运行压力不得超过预设值的50%', 1, 25),
(7, 2, 3, '逐步加压与数据采集',
   '1. 以恒定速率逐步增加围压和孔压\n2. 实时监测压力变化曲线\n3. 记录岩心发生破裂时的压力值\n4. 注意观察压力曲线的异常波动',
   '加压过程中人员必须站在防护屏后；如听到异常声响立即停止加压并泄压', 1, 30),
(8, 2, 4, '泄压与设备拆卸',
   '1. 按操作规程逐步泄压\n2. 确认压力归零后再拆卸管线\n3. 取出岩心样品并观察破裂形态\n4. 清理实验台面',
   '严禁在带压状态下拆卸任何连接件；泄压速度不得过快以免损坏传感器', 1, 20),
(9, 2, 5, '数据处理与分析',
   '1. 绘制压力-时间曲线\n2. 确定破裂压力值和裂缝延伸压力\n3. 计算地层破裂压力梯度\n4. 分析影响破裂压力的因素\n5. 撰写实验报告',
   '数据分析时注意单位换算和有效应力概念', 1, 30);

-- EXP-02-001: 岩石孔隙度与渗透率测定 (4步)
INSERT IGNORE INTO t_experiment_step (id, experiment_id, step_no, title, content, safety_tip, required_flag, estimated_minutes) VALUES
(10, 3, 1, '岩心样品制备',
    '1. 选取标准岩心柱塞样品\n2. 使用游标卡尺测量岩心直径和长度\n3. 清洗岩心表面并用烘箱干燥\n4. 称量干岩心质量并记录',
    '烘箱操作注意防烫；岩心样品轻拿轻放避免碎裂', 1, 15),
(11, 3, 2, '氦气孔隙度测定',
    '1. 将干燥岩心放入氦气孔隙度仪样品室\n2. 检查气路密封性\n3. 通入氦气，记录平衡压力\n4. 根据波义耳定律计算孔隙体积和孔隙度\n5. 重复测量3次取平均值',
    '氦气为惰性气体但仍需保持通风；压力传感器属精密仪器，轻拿轻放', 1, 25),
(12, 3, 3, '气体渗透率测定',
    '1. 将岩心装入渗透率仪夹持器\n2. 施加围压确保密封\n3. 通入氮气并记录进出口压力和流量\n4. 根据达西定律计算渗透率\n5. 在不同围压下重复测量',
    '加压时注意围压不得超过设备额定值；氮气瓶使用后及时关闭主阀', 1, 25),
(13, 3, 4, '数据整理与报告',
    '1. 整理孔隙度和渗透率数据\n2. 绘制孔隙度-渗透率关系图\n3. 分析岩石物性特征\n4. 撰写实验报告',
    '注意克氏渗透率校正；对比不同围压下的测量结果', 1, 25);

-- EXP-02-002: 油水相对渗透率测定 (4步)
INSERT IGNORE INTO t_experiment_step (id, experiment_id, step_no, title, content, safety_tip, required_flag, estimated_minutes) VALUES
(14, 4, 1, '岩心准备与饱和',
    '1. 测量岩心基本物性参数\n2. 将岩心抽真空饱和地层水\n3. 称量饱和后岩心质量，计算孔隙体积\n4. 将饱和岩心装入夹持器',
    '抽真空时注意玻璃器皿防爆；饱和过程需充分，确保100%饱和', 1, 20),
(15, 4, 2, '油驱水建立束缚水饱和度',
    '1. 以恒定流速注入模拟油\n2. 记录产出水量和驱替压差\n3. 待不再出水后继续驱替2PV\n4. 计算束缚水饱和度',
    '使用模拟油时注意防火；废液收集到指定容器', 1, 35),
(16, 4, 3, '水驱油实验',
    '1. 以恒定流速注入地层水进行驱替\n2. 定时记录产油量、产液量和驱替压差\n3. 持续驱替至含水率达到98%以上\n4. 记录残余油饱和度',
    '实验过程中保持流速恒定；准确记录每个时间点的数据', 1, 35),
(17, 4, 4, 'JBN法数据处理',
    '1. 整理驱替实验数据\n2. 使用JBN方法计算油水相对渗透率\n3. 绘制相对渗透率曲线\n4. 分析曲线的特征点（等渗点、端点值）\n5. 撰写实验报告',
    'JBN方法需要数值微分，注意数据平滑处理', 1, 30);

-- EXP-03-001: 抽油机工况测试与诊断 (4步)
INSERT IGNORE INTO t_experiment_step (id, experiment_id, step_no, title, content, safety_tip, required_flag, estimated_minutes) VALUES
(18, 5, 1, '抽油机模型认知',
    '1. 识别抽油机各组成部分（驴头、游梁、连杆、曲柄、减速箱、电机）\n2. 了解冲程、冲次调节方法\n3. 检查各润滑点\n4. 确认安全防护装置完好',
    '抽油机运转时保持安全距离；长发需束起防止卷入', 1, 15),
(19, 5, 2, '示功图测试',
    '1. 安装载荷传感器和位移传感器\n2. 连接数据采集系统\n3. 启动抽油机，待运行稳定后开始采集\n4. 记录至少5个完整冲程的数据',
    '传感器安装时抽油机必须停机；测试过程中注意电缆走向避免缠绕', 1, 25),
(20, 5, 3, '工况分析与故障诊断',
    '1. 绘制实测示功图\n2. 与理论示功图进行对比\n3. 判断泵的工作状态（正常、供液不足、漏失、碰泵等）\n4. 分析抽油系统效率',
    '示功图形状异常时需复查数据确保不是传感器故障', 1, 25),
(21, 5, 4, '实验总结与报告',
    '1. 汇总测试数据\n2. 对发现的问题提出改进建议\n3. 计算系统效率并分析损耗环节\n4. 撰写实验报告',
    '对比不同冲次下的系统效率变化趋势', 1, 25);

-- ==================== 7. 教学资源 ====================
INSERT IGNORE INTO t_resource (id, experiment_id, title, resource_type, url, required_flag, status, sort, upload_user_id) VALUES
(1,  1, '钻井液实验安全操作规程',      'DOCUMENT', '/resources/safety-manual-drilling.pdf',     1, 1, 1, (SELECT id FROM t_user WHERE username='teacher01')),
(2,  1, '旋转粘度计操作演示视频',      'VIDEO',    '/resources/viscometer-tutorial.mp4',         1, 1, 2, (SELECT id FROM t_user WHERE username='teacher01')),
(3,  1, 'API RP 13B-1 钻井液测试标准', 'DOCUMENT', '/resources/api-rp-13b-1.pdf',                 0, 1, 3, (SELECT id FROM t_user WHERE username='teacher01')),
(4,  2, '高压容器安全操作手册',        'DOCUMENT', '/resources/high-pressure-safety.pdf',         1, 1, 1, (SELECT id FROM t_user WHERE username='teacher01')),
(5,  2, '地层破裂压力试验教学视频',    'VIDEO',    '/resources/frac-test-tutorial.mp4',           1, 1, 2, (SELECT id FROM t_user WHERE username='teacher01')),
(6,  3, '岩石物性测定实验指导书',      'DOCUMENT', '/resources/petrophysics-lab-manual.pdf',      1, 1, 1, (SELECT id FROM t_user WHERE username='teacher01')),
(7,  3, '孔隙度渗透率仪操作指南',      'DOCUMENT', '/resources/poroperm-instrument-guide.pdf',    0, 1, 2, (SELECT id FROM t_user WHERE username='teacher01')),
(8,  4, '相对渗透率实验原理与方法',    'DOCUMENT', '/resources/relative-perm-theory.pdf',         1, 1, 1, (SELECT id FROM t_user WHERE username='teacher01')),
(9,  5, '抽油机结构与工作原理',        'VIDEO',    '/resources/pumping-unit-structure.mp4',        1, 1, 1, (SELECT id FROM t_user WHERE username='teacher02')),
(10, 5, '示功图诊断方法详解',          'DOCUMENT', '/resources/dynamometer-diagnosis.pdf',        0, 1, 2, (SELECT id FROM t_user WHERE username='teacher02'));

-- ==================== 8. 安全知识 ====================
INSERT IGNORE INTO t_safety_knowledge (id, experiment_id, knowledge_point, risk_type, content, status) VALUES
(1,  1, '硫化氢（H₂S）防护',     '中毒风险', 'H₂S是无色、剧毒、易燃气体，低浓度有臭鸡蛋气味，高浓度可致嗅觉疲劳。吸入后可导致呼吸道刺激、肺水肿甚至瞬间死亡。进入含H₂S区域必须佩戴便携式检测仪和正压式空气呼吸器。', 1),
(2,  1, '高压设备操作安全',       '机械伤害', '钻井液实验涉及高压管路和旋转设备。实验前必须检查接头紧固和压力表状态。加压时人员应站在防护屏后，严禁超压运行。设备检修前必须泄压、断电并挂牌。', 1),
(3,  1, '化学品安全防护',         '化学灼伤', '钻井液含多种化学添加剂（烧碱、聚合物、缓蚀剂等），具有腐蚀性和刺激性。配制和测试时避免皮肤直接接触，戴防化手套。废液分类收集，不得随意倾倒。', 1),
(4,  2, '超压防护与应急响应',     '爆炸风险', '高压实验存在容器破裂和爆炸风险。严格按照设计压力操作，不得超过额定压力的80%。熟悉紧急泄压按钮位置，实验前检查安全阀和爆破片是否完好。', 1),
(5,  2, '噪声防护',               '听力损伤', '高压泵和增压设备运行时产生高强度噪声（>85dB）。进入高噪声区域必须佩戴耳塞或耳罩，连续暴露时间不超过2小时。', 1),
(6,  3, '压缩气体安全',           '物理伤害', '氦气和氮气钢瓶属高压容器，使用和搬运时必须固定防止倾倒。开关阀门应缓慢，禁止用油润滑阀门。气瓶远离热源和火源，存储区保持通风。', 1),
(7,  4, '有机溶剂安全',           '火灾中毒', '实验使用的模拟油和有机溶剂易燃易爆。操作在通风橱内进行，远离火源。废液桶保持密闭，定期清理。实验室内禁止吸烟和使用明火。', 1),
(8,  5, '机械运转安全',           '机械伤害', '抽油机运转时旋转部件外露，必须保持1米以上安全距离。长发需束起并戴工作帽，禁止佩戴手套操作旋转设备。检查或维修前必须断电并悬挂警示牌。', 1),
(9,  NULL, '实验室通用安全守则',  '综合安全', '进入实验室前必须通过安全准入考试。熟悉逃生路线、灭火器位置、洗眼器和紧急喷淋装置使用方法。严禁在实验室内饮食、吸烟、追逐打闹。紧急情况拨打校园安保电话。', 1),
(10, NULL, '用电安全',            '触电风险', '实验室内所有电气设备必须有可靠接地。使用前检查线路是否老化破损。湿手不得操作电气开关。大功率设备需专人值守，实验结束及时切断电源。', 1),
(11, NULL, '消防安全',            '火灾风险', '实验室配备干粉灭火器和CO₂灭火器。熟悉灭火器"提、拔、握、压"操作步骤。电气火灾使用CO₂灭火器，油类火灾使用干粉灭火器。火势无法控制时立即撤离并报警。', 1);

-- ==================== 9. 题库 ====================
-- question type: SINGLE(单选) / MULTIPLE(多选) / JUDGE(判断) / SHORT(简答)
INSERT IGNORE INTO t_question (id, type, content, options, answer, score, analysis, knowledge_point, difficulty, course_id, create_by) VALUES
-- 单选题 (id 1-10)
(1,  'SINGLE', '钻井液密度通常采用什么仪器进行测定？',
     '["A.旋转粘度计","B.密度计","C.pH计","D.电导率仪"]',
     'B', 5, '钻井液密度使用密度计（泥浆比重秤）测定，是钻井液最基本也是最重要的性能参数之一。', '钻井液性能测试', 'EASY', 1, (SELECT id FROM t_user WHERE username='teacher01')),
(2,  'SINGLE', 'API标准中，旋转粘度计的标准转速为多少？',
     '["A.100rpm和200rpm","B.300rpm和600rpm","C.500rpm和1000rpm","D.60rpm和120rpm"]',
     'B', 5, '根据API RP 13B-1标准，旋转粘度计的标准转速为600rpm和300rpm，分别用于模拟钻头喷嘴处和环空中的剪切速率。', '钻井液性能测试', 'EASY', 1, (SELECT id FROM t_user WHERE username='teacher01')),
(3,  'SINGLE', '表观粘度（AV）的计算公式是什么？',
     '["A.AV = Φ600 + Φ300","B.AV = Φ600 / 2","C.AV = Φ600 - Φ300","D.AV = (Φ600 + Φ300) / 2"]',
     'B', 5, '表观粘度 AV = Φ600 / 2，单位为mPa·s。它表示钻井液在600rpm剪切速率下的表观粘度值。', '钻井液性能测试', 'MEDIUM', 1, (SELECT id FROM t_user WHERE username='teacher01')),
(4,  'SINGLE', '塑性粘度（PV）反映的是钻井液中什么因素对粘度的影响？',
     '["A.固相颗粒间的机械摩擦","B.化学添加剂的浓度","C.温度变化","D.压力变化"]',
     'A', 5, '塑性粘度PV = Φ600 - Φ300，主要反映钻井液中固相颗粒之间的机械摩擦阻力，与固相含量、颗粒大小和形状有关。', '钻井液性能测试', 'MEDIUM', 1, (SELECT id FROM t_user WHERE username='teacher01')),
(5,  'SINGLE', '地层破裂压力试验中，破裂压力的判断依据是什么？',
     '["A.压力表指针停止上升","B.压力曲线出现明显拐点或下降","C.听到异常声响","D.泵注量达到预设值"]',
     'B', 5, '当井筒压力超过地层破裂强度时，压力曲线会出现明显的拐点或突然下降，此时对应的井底压力即为地层破裂压力。', '地层破裂压力', 'MEDIUM', 1, (SELECT id FROM t_user WHERE username='teacher01')),
(6,  'SINGLE', '氦气孔隙度仪测定孔隙度的原理基于什么定律？',
     '["A.达西定律","B.波义耳定律","C.亨利定律","D.牛顿定律"]',
     'B', 5, '氦气孔隙度仪基于波义耳定律（等温条件下气体压力与体积成反比），通过测量气体在已知体积和岩心孔隙中的压力变化来计算孔隙体积。', '岩石物性测定', 'MEDIUM', 2, (SELECT id FROM t_user WHERE username='teacher01')),
(7,  'SINGLE', '气体渗透率测定的理论基础是什么？',
     '["A.泊肃叶定律","B.达西定律","C.菲克定律","D.傅里叶定律"]',
     'B', 5, '渗透率测定基于达西定律：流体通过多孔介质的流量与压差和截面积成正比，与流体粘度和介质长度成反比。', '岩石物性测定', 'EASY', 2, (SELECT id FROM t_user WHERE username='teacher01')),
(8,  'SINGLE', '抽油机示功图的横坐标和纵坐标分别代表什么？',
     '["A.时间和载荷","B.位移和载荷","C.载荷和位移","D.冲程和时间"]',
     'B', 5, '示功图的横坐标为光杆位移（冲程位置），纵坐标为光杆载荷，反映了一个冲程内载荷随位移的变化关系。', '抽油机工况诊断', 'MEDIUM', 3, (SELECT id FROM t_user WHERE username='teacher02')),
(9,  'SINGLE', 'H₂S在空气中的致死浓度大约是多少？',
     '["A.10ppm","B.100ppm","C.500ppm","D.1000ppm"]',
     'D', 5, 'H₂S浓度达到1000ppm以上时，可在数分钟内导致人员死亡。安全临界浓度为10ppm，报警浓度为15ppm。', '安全知识', 'MEDIUM', NULL, (SELECT id FROM t_user WHERE username='teacher01')),
(10, 'SINGLE', '实验室发生化学品溅入眼睛的事故时，首先应该做什么？',
     '["A.打电话给120","B.立即用洗眼器冲洗至少15分钟","C.用手揉搓眼睛","D.使用眼药水"]',
     'B', 5, '化学品溅入眼睛后，黄金处理时间只有10-15秒，必须立即使用洗眼器持续冲洗至少15分钟，同时撑开眼皮确保冲洗充分。', '安全知识', 'EASY', NULL, (SELECT id FROM t_user WHERE username='teacher01')),

-- 多选题 (id 11-15)
(11, 'MULTIPLE', '以下哪些属于钻井液的基本性能参数？',
     '["A.密度","B.粘度","C.滤失量","D.pH值","E.含砂量","F.电导率"]',
     '["A","B","C","D","E"]', 5, '钻井液基本性能参数包括密度、粘度（表观粘度、塑性粘度）、滤失量（API滤失和HTTP滤失）、pH值、含砂量等。电导率主要用于油基钻井液。', '钻井液性能测试', 'MEDIUM', 1, (SELECT id FROM t_user WHERE username='teacher01')),
(12, 'MULTIPLE', '影响地层破裂压力的主要因素有哪些？',
     '["A.地应力状态","B.岩石抗张强度","C.孔隙压力","D.井眼尺寸","E.钻井液密度","F.地层温度"]',
     '["A","B","C","E"]', 5, '地层破裂压力主要受地应力状态、岩石抗张强度、孔隙压力和钻井液密度影响。井眼尺寸和温度的影响相对较小。', '地层破裂压力', 'HARD', 1, (SELECT id FROM t_user WHERE username='teacher01')),
(13, 'MULTIPLE', '以下哪些是正确的实验室安全操作规范？',
     '["A.实验前了解逃生路线","B.穿拖鞋进入实验室","C.实验废物分类收集","D.独自进行高压实验","E.熟悉灭火器使用方法","F.实验室内使用明火加热"]',
     '["A","C","E"]', 5, '实验前必须了解逃生路线和安全设施位置；实验废物应分类收集处理；必须熟悉灭火器使用方法。B、D、F均为违规行为。', '安全知识', 'EASY', NULL, (SELECT id FROM t_user WHERE username='teacher01')),
(14, 'MULTIPLE', '岩石孔隙度测定中，可能影响测量精度的因素包括哪些？',
     '["A.岩心干燥程度","B.气路密封性","C.室温波动","D.氦气纯度","E.岩心形状规则度","F.大气压变化"]',
     '["A","B","C","E"]', 5, '岩心干燥不充分会导致孔隙被水占据；气路泄漏直接影响压力读数；温度波动改变气体状态；岩心形状不规则影响体积计算。', '岩石物性测定', 'MEDIUM', 2, (SELECT id FROM t_user WHERE username='teacher01')),
(15, 'MULTIPLE', '示功图可以诊断以下哪些井下泵故障？',
     '["A.供液不足","B.游动阀漏失","C.固定阀漏失","D.抽油杆断脱","E.泵筒磨损","F.电机过载"]',
     '["A","B","C","D","E"]', 5, '示功图通过分析载荷-位移曲线的形状变化，可以诊断供液不足、游动阀/固定阀漏失、抽油杆断脱、泵筒磨损等多种故障。电机过载需通过电流检测。', '抽油机工况诊断', 'HARD', 3, (SELECT id FROM t_user WHERE username='teacher02')),

-- 判断题 (id 16-20)
(16, 'JUDGE', '钻井液密度越高，机械钻速越快。',
     '["正确","错误"]', '错误', 5, '钻井液密度过高会产生压持效应，增加井底压差，反而降低机械钻速。需要在平衡地层压力和保证钻井效率之间找到最优密度。', '钻井工程基础', 'EASY', 1, (SELECT id FROM t_user WHERE username='teacher01')),
(17, 'JUDGE', '在地层破裂压力试验中，通常以地层产生明显破裂时的井底压力作为破裂压力。',
     '["正确","错误"]', '正确', 5, '地层破裂压力定义为使地层产生水力裂缝时的井底压力，在压力-时间曲线上表现为明显的拐点或压力突降。', '地层破裂压力', 'EASY', 1, (SELECT id FROM t_user WHERE username='teacher01')),
(18, 'JUDGE', '渗透率是岩石本身的属性，与通过的流体性质无关。',
     '["正确","错误"]', '正确', 5, '绝对渗透率是岩石的固有属性，理论上与通过的流体无关。但实际测量中，气体渗透率需要进行克氏校正（Klinkenberg校正）以消除滑脱效应。', '岩石物性测定', 'MEDIUM', 2, (SELECT id FROM t_user WHERE username='teacher01')),
(19, 'JUDGE', '抽油机的冲程越大，产量一定越高。',
     '["正确","错误"]', '错误', 5, '抽油机产量不仅取决于冲程，还与冲次、泵径、泵效等多个因素有关。单纯增大冲程而不考虑其他因素优化，不一定能提高产量。', '抽油机工况诊断', 'MEDIUM', 3, (SELECT id FROM t_user WHERE username='teacher02')),
(20, 'JUDGE', '化学品的废液可以直接倒入下水道。',
     '["正确","错误"]', '错误', 5, '实验室化学废液必须分类收集在专用容器中，交由有资质的单位处理，严禁直接倒入下水道，以免污染环境和腐蚀管道。', '安全知识', 'EASY', NULL, (SELECT id FROM t_user WHERE username='teacher01')),

-- 简答题 (id 21-25)
(21, 'SHORT', '简述钻井液的主要功能（至少列出5项）。',
     NULL,
     '1.携带和悬浮岩屑；2.冷却和润滑钻头及钻柱；3.平衡地层压力，防止井涌和井喷；4.形成泥饼，稳定井壁；5.传递水功率给钻头；6.获取地层信息（岩屑和录井）', 10,
     '钻井液是钻井工程的"血液"，其核心功能包括：清洗井底携带岩屑、冷却润滑钻具、平衡地层压力、稳定井壁、传递水力能量、提供地质信息等。', '钻井工程基础', 'MEDIUM', 1, (SELECT id FROM t_user WHERE username='teacher01')),
(22, 'SHORT', '什么是塑性粘度（PV）和动切力（YP）？它们分别反映了钻井液的什么特性？',
     NULL,
     '塑性粘度PV = Φ600 - Φ300，反映钻井液中固相颗粒间的机械摩擦阻力；动切力YP = Φ300 - PV = 2×Φ300 - Φ600，反映钻井液中粘土颗粒形成网架结构的能力，即携岩能力。', 10,
     'PV表征内摩擦力，与固相含量和分散度有关；YP表征结构力，是钻井液在动态条件下悬浮岩屑的能力指标。两者共同决定了钻井液的流变特性。', '钻井液性能测试', 'MEDIUM', 1, (SELECT id FROM t_user WHERE username='teacher01')),
(23, 'SHORT', '简述地层破裂压力试验的目的和基本步骤。',
     NULL,
     '目的：确定地层破裂压力，为钻井液密度窗口设计和固井施工提供依据。基本步骤：1.下钻至试验井深，循环清洗井底；2.关闭防喷器；3.以恒定低排量向井内泵入钻井液；4.记录泵压随时间的变化曲线；5.当压力出现拐点或突降时判断地层破裂，记录破裂压力值；6.泄压并分析数据。', 10,
     '地层破裂压力试验（LOT）是钻井工程中确定安全钻井液密度范围的关键试验，必须在套管鞋处进行以确保下一开次钻井安全。', '地层破裂压力', 'MEDIUM', 1, (SELECT id FROM t_user WHERE username='teacher01')),
(24, 'SHORT', '什么是相对渗透率？它有什么工程意义？',
     NULL,
     '相对渗透率是指多相流体共存时，某一相的有效渗透率与绝对渗透率的比值。工程意义：1.预测油井产量和含水率变化；2.设计注水开发方案；3.评价储层开发效果；4.指导提高采收率措施。', 10,
     '相对渗透率曲线是油藏工程中最基础的输入参数之一，直接决定了油水两相流动规律和开发动态预测的准确性。', '岩石物性测定', 'HARD', 2, (SELECT id FROM t_user WHERE username='teacher01')),
(25, 'SHORT', '简述实验室应急救援的基本流程。',
     NULL,
     '1.立即停止实验；2.判断事故类型（火灾、化学品泄漏、人员受伤等）；3.采取初步应急措施（灭火、洗消、断电等）；4.拨打校园紧急电话和120；5.组织人员疏散；6.向上级报告事故情况；7.保护事故现场，配合调查。', 10,
     '实验室安全应急的核心是"先救人、后救物"。任何事故处理中，人员安全始终是第一位的。掌握基本的急救技能是每个实验人员的必备素质。', '安全知识', 'EASY', NULL, (SELECT id FROM t_user WHERE username='teacher01'));

-- ==================== 10. 试卷 ====================
INSERT IGNORE INTO t_exam_paper (id, title, description, course_id, experiment_id, total_score, pass_score, duration, teacher_id, status, start_time, end_time, is_deleted) VALUES
(1, '钻井液实验安全准入考核',
   '考查学生对钻井液实验基础知识、操作规范和安全要点的掌握程度，通过后方可进入实验室。',
   1, 1, 100, 70, 30,
   (SELECT id FROM t_user WHERE username='teacher01'),
   'PUBLISHED', '2026-07-01 00:00:00', '2026-12-31 23:59:59', 0),
(2, '地层破裂压力试验考核',
   '针对高压实验的特殊安全要求，考查学生对破裂压力试验原理、操作流程和应急处置的掌握程度。',
   1, 2, 100, 80, 30,
   (SELECT id FROM t_user WHERE username='teacher01'),
   'PUBLISHED', '2026-07-01 00:00:00', '2026-12-31 23:59:59', 0),
(3, '岩石物性实验基础考核',
   '考查学生对孔隙度、渗透率基本概念和测定方法的理解。',
   2, 3, 100, 60, 25,
   (SELECT id FROM t_user WHERE username='teacher01'),
   'PUBLISHED', '2026-07-01 00:00:00', '2026-12-31 23:59:59', 0),
(4, '采油工程实验安全与基础考核',
   '考查学生对抽油机工作原理、示功图分析和实验安全的掌握程度。',
   3, 5, 100, 65, 25,
   (SELECT id FROM t_user WHERE username='teacher02'),
   'PUBLISHED', '2026-07-01 00:00:00', '2026-12-31 23:59:59', 0),
(5, '实验室安全通识考核',
   '面向所有进入实验室的学生，考查实验室基础安全知识和应急处理能力。',
   NULL, NULL, 100, 80, 20,
   (SELECT id FROM t_user WHERE username='teacher01'),
   'PUBLISHED', '2026-07-01 00:00:00', '2026-12-31 23:59:59', 0);

-- ==================== 11. 试卷-题目关联 ====================
INSERT IGNORE INTO t_exam_paper_question (id, paper_id, question_id, score, order_num) VALUES
-- 试卷1：钻井液实验安全准入 (questions 1,2,3,4,11,16,21,22 + 安全题)
(1,  1, 1,  5, 1),
(2,  1, 2,  5, 2),
(3,  1, 3,  5, 3),
(4,  1, 4,  5, 4),
(5,  1, 11, 5, 5),
(6,  1, 16, 5, 6),
(7,  1, 9,  5, 7),
(8,  1, 10, 5, 8),
(9,  1, 21, 15, 9),
(10, 1, 22, 15, 10),
(11, 1, 20, 5, 11),
(12, 1, 13, 5, 12),

-- 试卷2：地层破裂压力 (questions 5,12,17,23 + 安全题)
(13, 2, 5,  10, 1),
(14, 2, 12, 10, 2),
(15, 2, 17, 10, 3),
(16, 2, 9,  10, 4),
(17, 2, 10, 10, 5),
(18, 2, 13, 10, 6),
(19, 2, 23, 20, 7),
(20, 2, 20, 10, 8),

-- 试卷3：岩石物性 (questions 6,7,14,18,24)
(21, 3, 6,  10, 1),
(22, 3, 7,  10, 2),
(23, 3, 14, 15, 3),
(24, 3, 18, 10, 4),
(25, 3, 10, 10, 5),
(26, 3, 20, 10, 6),
(27, 3, 24, 20, 7),
(28, 3, 13, 15, 8),

-- 试卷4：采油工程 (questions 8,15,19,25)
(29, 4, 8,  10, 1),
(30, 4, 15, 15, 2),
(31, 4, 19, 10, 3),
(32, 4, 10, 10, 4),
(33, 4, 20, 10, 5),
(34, 4, 13, 15, 6),
(35, 4, 25, 20, 7),

-- 试卷5：安全通识 (questions 9,10,13,20,25)
(36, 5, 9,  15, 1),
(37, 5, 10, 15, 2),
(38, 5, 13, 20, 3),
(39, 5, 20, 20, 4),
(40, 5, 25, 30, 5);

-- ==================== 12. 考试记录 ====================
INSERT IGNORE INTO t_exam_record (id, student_id, paper_id, experiment_id, total_score, objective_score, subjective_score, status, passed, start_time, submit_time) VALUES
(1, (SELECT id FROM t_user WHERE username='student01'), 1, 1, 85, 45, 40, 'GRADED', 1, '2026-07-02 09:00:00', '2026-07-02 09:25:00'),
(2, (SELECT id FROM t_user WHERE username='student02'), 1, 1, 72, 40, 32, 'GRADED', 1, '2026-07-02 09:30:00', '2026-07-02 09:55:00'),
(3, (SELECT id FROM t_user WHERE username='student03'), 1, 1, 55, 30, 25, 'GRADED', 0, '2026-07-02 14:00:00', '2026-07-02 14:28:00'),
(4, (SELECT id FROM t_user WHERE username='student01'), 5, NULL, 92, 65, 27, 'GRADED', 1, '2026-07-01 10:00:00', '2026-07-01 10:16:00'),
(5, (SELECT id FROM t_user WHERE username='student02'), 5, NULL, 78, 55, 23, 'GRADED', 0, '2026-07-01 10:30:00', '2026-07-01 10:48:00');

-- ==================== 13. 考试作答 ====================
INSERT IGNORE INTO t_exam_answer (id, record_id, question_id, student_answer, is_correct, score) VALUES
-- 记录1：student01 试卷1（客观题答案）
(1,  1, 1,  'B', 1, 5),
(2,  1, 2,  'B', 1, 5),
(3,  1, 3,  'B', 1, 5),
(4,  1, 4,  'A', 1, 5),
(5,  1, 11, '["A","B","C","D","E"]', 1, 5),
(6,  1, 16, '错误', 1, 5),
(7,  1, 9,  'D', 1, 5),
(8,  1, 10, 'B', 1, 5),
(9,  1, 20, '错误', 1, 5),
(10, 1, 13, '["A","C","E"]', 1, 5),
-- 记录2：student02 试卷1
(11, 2, 1,  'B', 1, 5),
(12, 2, 2,  'B', 1, 5),
(13, 2, 3,  'C', 0, 0),
(14, 2, 4,  'A', 1, 5),
(15, 2, 11, '["A","B","C","D"]', 0, 0),
(16, 2, 16, '正确', 0, 0),
(17, 2, 9,  'C', 0, 0),
(18, 2, 10, 'B', 1, 5),
(19, 2, 20, '错误', 1, 5),
(20, 2, 13, '["A","C","E"]', 1, 5),
-- 记录3：student03 试卷1
(21, 3, 1,  'B', 1, 5),
(22, 3, 2,  'C', 0, 0),
(23, 3, 3,  'C', 0, 0),
(24, 3, 4,  'B', 0, 0),
(25, 3, 11, '["A","B","C"]', 0, 0),
(26, 3, 16, '正确', 0, 0),
(27, 3, 9,  'B', 0, 0),
(28, 3, 10, 'B', 1, 5),
(29, 3, 20, '错误', 1, 5),
(30, 3, 13, '["A","C"]', 0, 0),
-- 记录4：student01 试卷5
(31, 4, 9,  'D', 1, 15),
(32, 4, 10, 'B', 1, 15),
(33, 4, 13, '["A","C","E"]', 1, 20),
(34, 4, 20, '错误', 1, 20),
-- 记录5：student02 试卷5
(35, 5, 9,  'C', 0, 0),
(36, 5, 10, 'B', 1, 15),
(37, 5, 13, '["A","C"]', 0, 0),
(38, 5, 20, '错误', 1, 20);

-- ==================== 14. 预约 ====================
INSERT IGNORE INTO t_reservation (id, student_id, time_slot_id, lab_id, experiment_id, purpose, status, teacher_id, review_comment, review_time) VALUES
(1, (SELECT id FROM t_user WHERE username='student01'),
   (SELECT id FROM t_lab_time_slot WHERE lab_id=1 AND `date`='2026-07-10' AND start_time='08:00:00'),
   1, 1, '完成钻井液密度与流变性测定实验，巩固课堂所学理论知识。',
   'APPROVED', (SELECT id FROM t_user WHERE username='teacher01'),
   '预约通过，请准时到达实验室并穿戴好PPE。', '2026-07-05 10:00:00'),
(2, (SELECT id FROM t_user WHERE username='student02'),
   (SELECT id FROM t_lab_time_slot WHERE lab_id=1 AND `date`='2026-07-10' AND start_time='08:00:00'),
   1, 1, '进行钻井液性能测试实验操作练习。',
   'APPROVED', (SELECT id FROM t_user WHERE username='teacher01'),
   '同意，请注意实验安全。', '2026-07-05 10:30:00'),
(3, (SELECT id FROM t_user WHERE username='student03'),
   (SELECT id FROM t_lab_time_slot WHERE lab_id=1 AND `date`='2026-07-10' AND start_time='10:00:00'),
   1, 1, '补充完成上次未完成的实验步骤。',
   'PENDING', NULL, NULL, NULL),
(4, (SELECT id FROM t_user WHERE username='student01'),
   (SELECT id FROM t_lab_time_slot WHERE lab_id=1 AND `date`='2026-07-11' AND start_time='14:00:00'),
   1, 2, '进行地层破裂压力试验学习。',
   'PENDING', NULL, NULL, NULL),
(5, (SELECT id FROM t_user WHERE username='student03'),
   (SELECT id FROM t_lab_time_slot WHERE lab_id=2 AND `date`='2026-07-10' AND start_time='08:00:00'),
   2, 5, '学习抽油机示功图测试方法。',
   'REJECTED', (SELECT id FROM t_user WHERE username='teacher02'),
   '该时段设备正在维护，请选择其他时间。', '2026-07-05 09:00:00');

-- ==================== 15. 实验报告 ====================
INSERT IGNORE INTO t_report (id, student_id, experiment_id, title, content, status, submit_time, latest_submit_time, is_deleted) VALUES
(1, (SELECT id FROM t_user WHERE username='student01'), 1,
   '钻井液密度与流变性测定实验报告',
   '一、实验目的\n掌握钻井液密度计和旋转粘度计的使用方法，测定钻井液的密度和流变参数。\n\n二、实验原理\n1. 密度测定基于杠杆平衡原理\n2. 流变性测定基于旋转粘度计在不同剪切速率下的读数计算\n\n三、实验数据\n密度：1.25g/cm³（3次平均）\nΦ600读数：48\nΦ300读数：32\n\n四、计算结果\nAV = 24 mPa·s\nPV = 16 mPa·s\nYP = 16 Pa\n\n五、结论\n钻井液样品各项指标符合工程要求，流变性能良好。',
   'GRADED', '2026-07-03 15:00:00', '2026-07-03 15:00:00', 0),
(2, (SELECT id FROM t_user WHERE username='student02'), 1,
   '钻井液密度与流变性测定实验报告',
   '本实验完成了钻井液密度和流变参数的测定。密度测定值为1.23g/cm³。粘度测量中Φ600为45，Φ300为30。计算得AV=22.5 mPa·s，PV=15 mPa·s，YP=15 Pa。实验过程顺利，数据可靠。',
   'GRADED', '2026-07-03 16:30:00', '2026-07-03 16:30:00', 0),
(3, (SELECT id FROM t_user WHERE username='student01'), 3,
   '岩石孔隙度与渗透率测定实验报告',
   '本实验使用氦气孔隙度仪和气体渗透率仪对砂岩岩心样品进行了物性测定。孔隙度为18.5%，渗透率为125mD。实验结果与文献值吻合较好。',
   'SUBMITTED', '2026-07-04 10:00:00', '2026-07-04 10:00:00', 0),
(4, (SELECT id FROM t_user WHERE username='student02'), 3,
   '岩石孔隙度与渗透率测定实验报告',
   '完成了岩心物性参数测定。氦气孔隙度为20.1%，克氏校正后渗透率为98mD。实验过程中注意了氦气安全和设备操作规范。',
   'DRAFT', NULL, NULL, 0);

-- ==================== 16. 报告评分 ====================
INSERT IGNORE INTO t_report_score (id, report_id, teacher_id, score, comment, is_latest, grade_time) VALUES
(1, 1, (SELECT id FROM t_user WHERE username='teacher01'), 88,
   '报告结构完整，数据记录规范，计算过程正确。建议在结论部分增加对实验误差来源的分析。', 1, '2026-07-04 09:00:00'),
(2, 2, (SELECT id FROM t_user WHERE username='teacher01'), 75,
   '数据基本正确，但报告过于简略，缺少对实验原理的阐述和数据的误差分析。希望下次能更详细地撰写。', 1, '2026-07-04 10:00:00');

-- ==================== 17. 学习记录 ====================
INSERT IGNORE INTO t_learning_record (id, student_id, resource_id, experiment_id, progress, duration_seconds, finish_flag, first_time, last_time) VALUES
(1, (SELECT id FROM t_user WHERE username='student01'), 1, 1, 100.00, 1800, 1, '2026-07-02 08:00:00', '2026-07-02 08:30:00'),
(2, (SELECT id FROM t_user WHERE username='student01'), 2, 1, 100.00, 1200, 1, '2026-07-02 08:30:00', '2026-07-02 08:50:00'),
(3, (SELECT id FROM t_user WHERE username='student02'), 1, 1, 100.00, 2400, 1, '2026-07-02 09:00:00', '2026-07-02 09:40:00'),
(4, (SELECT id FROM t_user WHERE username='student02'), 2, 1, 75.00,  900, 0, '2026-07-02 09:45:00', '2026-07-02 10:00:00'),
(5, (SELECT id FROM t_user WHERE username='student01'), 6, 3, 100.00, 1500, 1, '2026-07-03 09:00:00', '2026-07-03 09:25:00'),
(6, (SELECT id FROM t_user WHERE username='student01'), 7, 3, 60.00,  600, 0, '2026-07-03 09:30:00', '2026-07-03 09:40:00'),
(7, (SELECT id FROM t_user WHERE username='student03'), 1, 1, 100.00, 3000, 1, '2026-07-02 13:00:00', '2026-07-02 13:50:00'),
(8, (SELECT id FROM t_user WHERE username='student03'), 9, 5, 80.00,  1800, 0, '2026-07-04 14:00:00', '2026-07-04 14:30:00');

-- ==================== 18. AI推荐记录 ====================
INSERT IGNORE INTO t_recommend_record (id, student_id, experiment_id, resource_id, total_score, score_breakdown, reason, clicked) VALUES
(1, (SELECT id FROM t_user WHERE username='student01'), 1, 3, 85.50,
    '{"knowledge_match":90,"learning_progress":80,"difficulty_match":85,"time_available":87}',
    '根据你在钻井液实验中的学习进度和知识掌握情况，推荐参考API RP 13B-1标准文档以深入理解测试标准。', 1),
(2, (SELECT id FROM t_user WHERE username='student02'), 1, 3, 72.30,
    '{"knowledge_match":75,"learning_progress":65,"difficulty_match":80,"time_available":70}',
    '你尚未完成粘度测定视频的学习，建议先学习操作视频再参考标准文档。', 0),
(3, (SELECT id FROM t_user WHERE username='student03'), 5, 10, 78.00,
    '{"knowledge_match":80,"learning_progress":70,"difficulty_match":85,"time_available":78}',
    '你在抽油机实验中表现良好，推荐学习示功图诊断方法详解以提升故障诊断能力。', 1),
(4, (SELECT id FROM t_user WHERE username='student01'), 3, 7, 90.20,
    '{"knowledge_match":92,"learning_progress":88,"difficulty_match":90,"time_available":91}',
    '你已经完成了基础实验指导书的学习，推荐阅读仪器操作指南以更熟练地使用实验设备。', 0);

-- ==================== 19. AI聊天记录 ====================
INSERT IGNORE INTO t_ai_chat_record (id, user_id, scene, question, answer, tool_name, experiment_id, manual_revision) VALUES
(1, (SELECT id FROM t_user WHERE username='student01'), 'experiment_help',
    '钻井液实验中，旋转粘度计的Φ600读数一直在波动，这是什么原因？应该怎么处理？',
    '读数波动可能有以下几个原因：\n1. 钻井液样品不均匀，存在气泡或沉淀——建议充分搅拌样品后重新测量\n2. 转子未完全浸入到刻度线——重新调整转子深度\n3. 粘度计未调水平——检查水平泡是否居中\n4. 电源电压不稳定——检查电源连接\n\n建议先排除以上因素，如果问题仍然存在请联系实验指导教师。',
    NULL, 1, NULL),
(2, (SELECT id FROM t_user WHERE username='student01'), 'safety_consult',
    'H₂S中毒的急救措施有哪些？',
    'H₂S中毒急救措施：\n1. 立即将中毒者转移到空气新鲜处，保持呼吸道通畅\n2. 给予吸氧，有条件使用高压氧治疗\n3. 对呼吸困难或停止者进行人工呼吸（注意：施救者必须佩戴呼吸防护设备，避免二次中毒）\n4. 眼睛接触时立即用大量清水冲洗至少15分钟\n5. 立即拨打120急救电话\n6. 保留现场信息供医疗人员参考\n\n预防胜于急救，进入含H₂S区域前务必佩戴检测仪和呼吸器。',
    NULL, NULL, NULL),
(3, (SELECT id FROM t_user WHERE username='student02'), 'experiment_help',
    '岩石孔隙度测定的结果比文献值偏低很多，可能的原因有哪些？',
    '孔隙度偏低可能原因：\n1. 岩心干燥不充分，残留水分占据部分孔隙\n2. 气路存在微漏，导致压力读数偏高\n3. 岩心样品含有粘土矿物，可能发生水化膨胀\n4. 氦气未充分扩散到微小孔隙中\n\n建议：\n- 延长干燥时间，确保恒重\n- 进行气路检漏\n- 多次重复测量取平均值\n- 与同一批次的同学对比数据',
    NULL, 3, NULL),
(4, (SELECT id FROM t_user WHERE username='student03'), 'report_writing',
    '实验报告的"误差分析"部分应该怎么写？',
    '实验报告误差分析应包括：\n1. 系统误差：仪器校准偏差、测量方法固有局限性\n2. 随机误差：读数波动、环境温度变化\n3. 人为误差：操作熟练度、读数习惯\n4. 对每种误差来源给出改进建议\n\n示例：\n"本次实验的密度测量相对误差为1.2%，主要来源于密度计游码读数的人为判断偏差和样品温度未严格控制在25±0.5℃范围内。建议后续实验增加测量次数并使用恒温水浴控制温度。"',
    NULL, NULL, NULL),
(5, (SELECT id FROM t_user WHERE username='student01'), 'exam_prep',
    '明天要考钻井液实验安全准入，重点复习哪些内容？',
    '建议重点复习以下内容：\n1. 钻井液的基本功能和性能参数（密度、粘度、滤失量等）\n2. 密度计和旋转粘度计的操作步骤\n3. 表观粘度、塑性粘度和动切力的计算公式\n4. 实验安全操作规范（PPE穿戴、应急处理）\n5. H₂S防护知识\n\n考试包含单选、多选、判断和简答题型。简答题重点准备"钻井液主要功能"和"流变参数含义"这两道题。',
    NULL, 1, NULL);

-- ==================== 20. 更多实验室时间段 ====================
INSERT IGNORE INTO t_lab_time_slot (id, lab_id, experiment_id, `date`, start_time, end_time, capacity, status, create_by) VALUES
(5,  1, 1, '2026-07-12', '08:00', '10:00', 20, 'AVAILABLE', (SELECT id FROM t_user WHERE username='teacher01')),
(6,  1, 1, '2026-07-12', '10:00', '12:00', 20, 'AVAILABLE', (SELECT id FROM t_user WHERE username='teacher01')),
(7,  1, 2, '2026-07-13', '14:00', '16:00', 12, 'AVAILABLE', (SELECT id FROM t_user WHERE username='teacher01')),
(8,  1, 2, '2026-07-14', '08:00', '10:00', 12, 'AVAILABLE', (SELECT id FROM t_user WHERE username='teacher01')),
(9,  2, 3, '2026-07-12', '08:00', '10:00', 15, 'AVAILABLE', (SELECT id FROM t_user WHERE username='teacher01')),
(10, 2, 4, '2026-07-13', '10:00', '12:00', 10, 'AVAILABLE', (SELECT id FROM t_user WHERE username='teacher01')),
(11, 2, 5, '2026-07-14', '14:00', '16:00', 20, 'AVAILABLE', (SELECT id FROM t_user WHERE username='teacher02')),
(12, 2, 5, '2026-07-15', '08:00', '10:00', 20, 'AVAILABLE', (SELECT id FROM t_user WHERE username='teacher02'));

-- ============================================
-- 完成
-- ============================================
