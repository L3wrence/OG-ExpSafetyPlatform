<template>
  <div class="course-editor-page" v-loading="loading">
    <section class="page-head">
      <div>
        <p class="eyebrow">课堂详细</p>
        <h1>{{ course?.courseName || '课堂详细' }}</h1>
        <p class="page-desc">{{ course?.courseCode || '-' }} · {{ course?.semester || '未设置学期' }}</p>
      </div>
      <div class="head-actions">
        <el-button :icon="Refresh" @click="loadDetail">刷新</el-button>
        <el-button :icon="Back" @click="router.push('/classrooms')">返回我的课堂</el-button>
      </div>
    </section>

    <section class="builder-grid">
      <aside class="step-panel">
        <button v-for="item in steps" :key="item.key" :class="{ active: activeStep === item.key }" type="button" @click="activeStep = item.key">
          <el-icon><component :is="item.icon" /></el-icon>
          <span>{{ item.label }}</span>
        </button>
      </aside>

      <main class="editor-panel">
        <section v-if="activeStep === 'basic'" class="work-section">
          <div class="section-title">
            <h2>课程基本信息</h2>
            <div class="inline-actions">
              <el-button v-if="course?.status !== 1" type="success" :icon="Finished" :disabled="isArchived" @click="publishCurrentCourse">发布</el-button>
              <el-button type="warning" :disabled="isArchived" @click="archiveCurrentCourse">归档</el-button>
            </div>
          </div>
          <el-form ref="courseFormRef" :model="courseForm" :rules="courseRules" label-width="104px">
            <div class="form-grid">
              <el-form-item label="课程名称" prop="courseName"><el-input v-model="courseForm.courseName" :disabled="isArchived" /></el-form-item>
              <el-form-item label="课程编号" prop="courseCode"><el-input v-model="courseForm.courseCode" :disabled="isArchived" /></el-form-item>
              <el-form-item label="课程方向"><el-input v-model="courseForm.direction" :disabled="isArchived" /></el-form-item>
              <el-form-item label="开设学期"><el-input v-model="courseForm.semester" :disabled="isArchived" /></el-form-item>
              <el-form-item label="负责人ID" prop="teacherId"><el-input-number v-model="courseForm.teacherId" :min="1" :disabled="isArchived" /></el-form-item>
              <el-form-item label="状态"><el-tag :type="statusMeta(course?.status).type">{{ statusMeta(course?.status).label }}</el-tag></el-form-item>
              <el-form-item label="学分"><el-input-number v-model="courseForm.credit" :min="0" :max="20" :precision="1" :disabled="isArchived" /></el-form-item>
              <el-form-item label="学时"><el-input-number v-model="courseForm.hours" :min="0" :max="300" :disabled="isArchived" /></el-form-item>
            </div>
            <el-form-item label="考核方式"><el-input v-model="courseForm.assessmentMethod" :disabled="isArchived" /></el-form-item>
            <el-form-item label="封面地址"><el-input v-model="courseForm.coverUrl" :disabled="isArchived" placeholder="可选，留空使用平台默认封面" /></el-form-item>
            <el-form-item label="课程短标语"><el-input v-model="courseForm.tagline" :disabled="isArchived" maxlength="160" /></el-form-item>
            <el-form-item label="亮点标签"><el-input v-model="courseForm.highlightTags" :disabled="isArchived" placeholder="用逗号分隔，如 实验可视化,安全准入" /></el-form-item>
            <el-form-item label="视觉主题"><el-input v-model="courseForm.visualTheme" :disabled="isArchived" placeholder="如 oilfield-lab" /></el-form-item>
            <el-form-item label="课程简介"><el-input v-model="courseForm.description" type="textarea" :rows="3" :disabled="isArchived" /></el-form-item>
            <el-form-item label="学习要求"><el-input v-model="courseForm.learningRequirement" type="textarea" :rows="3" :disabled="isArchived" /></el-form-item>
          </el-form>
          <div class="footer-actions">
            <el-button type="primary" :icon="Check" :loading="savingCourse" :disabled="isArchived" @click="saveCourseInline">保存课程信息</el-button>
          </div>
        </section>

        <section v-if="activeStep === 'classes'" class="work-section">
          <div class="section-title">
            <h2>教学班和学生</h2>
            <el-button type="primary" :icon="Plus" :disabled="isArchived" @click="openClassCreate">新增教学班</el-button>
          </div>
          <el-table :data="classes" stripe empty-text="暂无教学班">
            <el-table-column prop="className" label="教学班" min-width="160" />
            <el-table-column prop="teacherName" label="任课教师" width="120" />
            <el-table-column prop="assistantName" label="助教" width="120" />
            <el-table-column prop="adminClass" label="行政班" min-width="140" />
            <el-table-column prop="studentCount" label="学生数" width="90" />
            <el-table-column label="状态" width="90">
              <template #default="{ row }">
                <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '停用' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="150">
              <template #default="{ row }">
                <el-button text type="primary" :icon="Edit" :disabled="isArchived" @click="openClassEdit(row)">编辑</el-button>
                <el-button text type="danger" :icon="Delete" :disabled="isArchived" @click="removeClass(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
          <section class="student-preview">
            <div class="section-title compact">
              <h3>学生名单预览</h3>
              <el-button text type="primary" @click="router.push('/teacher/courses')">批量名单维护</el-button>
            </div>
            <el-table :data="students.slice(0, 8)" size="small" stripe empty-text="暂无学生">
              <el-table-column prop="username" label="学号" width="130" />
              <el-table-column prop="realName" label="姓名" width="120" />
              <el-table-column prop="teachingClassName" label="教学班" min-width="150" />
              <el-table-column prop="groupName" label="小组" width="110" />
            </el-table>
          </section>
          <section class="invite-panel">
            <div class="section-title compact">
              <h3>课程邀请</h3>
              <el-button type="primary" :icon="Plus" :loading="inviteLoading" :disabled="isArchived" @click="createInviteInline">生成邀请码</el-button>
            </div>
            <el-table :data="invites" size="small" stripe empty-text="暂无邀请码">
              <el-table-column prop="inviteCode" label="邀请码" width="150" />
              <el-table-column prop="className" label="教学班" min-width="150" />
              <el-table-column label="使用情况" width="120">
                <template #default="{ row }">{{ row.usedCount || 0 }}/{{ row.maxUses || '不限' }}</template>
              </el-table-column>
              <el-table-column label="状态" width="90">
                <template #default="{ row }">
                  <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '停用' }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="100">
                <template #default="{ row }">
                  <el-button text type="danger" :disabled="row.status !== 1 || isArchived" @click="disableInviteInline(row)">停用</el-button>
                </template>
              </el-table-column>
            </el-table>
          </section>
        </section>

        <section v-if="activeStep === 'experiments'" class="work-section">
          <div class="section-title">
            <h2>实验章节和规程</h2>
            <el-button type="primary" :icon="Plus" :disabled="isArchived" @click="openExperimentCreate">新增实验章节</el-button>
          </div>
          <div class="experiment-list">
            <article v-for="item in experiments" :key="item.id">
              <div>
                <strong>{{ item.expName }}</strong>
                <span>{{ item.expCode }} · {{ riskLabel(item.riskLevel) }} · {{ item.durationMinutes || 0 }} 分钟</span>
              </div>
              <div class="row-actions">
                <el-tag :type="item.status === 1 ? 'success' : 'info'">{{ item.status === 1 ? '开放' : '未开放' }}</el-tag>
                <el-button text type="primary" :icon="Edit" :disabled="isArchived" @click="openExperimentEdit(item)">编辑</el-button>
                <el-button text type="primary" :icon="Operation" :disabled="isArchived" @click="openStepEditor(item)">步骤/资源/要求</el-button>
                <el-button text :type="item.status === 1 ? 'warning' : 'success'" :disabled="isArchived" @click="toggleExperiment(item)">
                  {{ item.status === 1 ? '停用' : '开放' }}
                </el-button>
              </div>
            </article>
            <el-empty v-if="experiments.length === 0" description="暂无实验章节" />
          </div>
        </section>

        <section v-if="activeStep === 'exam'" class="work-section">
          <div class="section-title">
            <h2>考试管理</h2>
            <div class="section-actions">
              <el-button :icon="Document" @click="router.push(`/teacher/courses/${courseId}/safety-exams`)">题库管理</el-button>
              <el-button type="primary" :icon="Plus" @click="router.push(`/teacher/courses/${courseId}/safety-exams?tab=papers&create=paper`)">创建试卷</el-button>
            </div>
          </div>
          <div class="inline-toolbar">
            <el-input v-model="examFilters.keyword" :prefix-icon="Search" clearable placeholder="搜索试卷标题" @keyup.enter="loadExamPapersInline" />
            <el-select v-model="examFilters.status" clearable placeholder="状态">
              <el-option label="草稿" value="DRAFT" />
              <el-option label="已发布" value="PUBLISHED" />
              <el-option label="已关闭" value="CLOSED" />
            </el-select>
            <el-button type="primary" :icon="Search" @click="loadExamPapersInline">查询</el-button>
          </div>
          <el-table v-loading="examLoading" :data="examPapers" stripe empty-text="暂无试卷">
            <el-table-column prop="title" label="试卷名称" min-width="180" />
            <el-table-column prop="experimentId" label="实验ID" width="90" />
            <el-table-column prop="description" label="说明" min-width="180" show-overflow-tooltip />
            <el-table-column prop="questionCount" label="题数" width="80" />
            <el-table-column prop="totalScore" label="总分" width="90" />
            <el-table-column prop="passScore" label="及格分" width="90" />
            <el-table-column prop="duration" label="时长(分钟)" width="110" />
            <el-table-column label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="paperStatusMeta(row.status).type">{{ paperStatusMeta(row.status).label }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="220" fixed="right">
              <template #default="{ row }">
                <el-button text type="primary" :icon="Edit" @click="openExamEdit(row)">编辑</el-button>
                <el-button text :type="row.status === 'PUBLISHED' ? 'warning' : 'success'" @click="toggleExamStatus(row)">
                  {{ row.status === 'PUBLISHED' ? '关闭' : '发布' }}
                </el-button>
                <el-button text type="danger" :icon="Delete" @click="removeExamPaper(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </section>

        <section v-if="activeStep === 'reports'" class="work-section">
          <div class="section-title">
            <h2>报告管理</h2>
            <el-button :icon="Refresh" @click="loadReportsInline">刷新</el-button>
          </div>
          <div v-for="group in pendingReportGroups" :key="group.experimentId" class="report-group">
            <div class="section-title compact">
              <h3>{{ group.experimentName }}</h3>
              <el-tag type="warning">{{ group.items.length }} 份待批改</el-tag>
            </div>
            <el-table :data="group.items" size="small" stripe>
              <el-table-column prop="studentName" label="学生" width="120" />
              <el-table-column prop="title" label="报告标题" min-width="180" show-overflow-tooltip />
              <el-table-column prop="submitTime" label="提交时间" width="170" />
              <el-table-column label="操作" width="180">
                <template #default="{ row }">
                  <el-button text type="primary" @click="openReportDetail(row)">查看</el-button>
                  <el-button text type="success" @click="openReportGrade(row)">评分</el-button>
                  <el-button text type="danger" @click="openReportReturn(row)">退回</el-button>
                </template>
              </el-table-column>
            </el-table>
          </div>
          <el-empty v-if="pendingReportGroups.length === 0" description="暂无待批改报告" />
        </section>

        <section v-if="activeStep === 'resources'" class="work-section">
          <div class="section-title">
            <h2>资源管理</h2>
            <el-button type="primary" :icon="Plus" @click="openResourceCreate">新增课程资源</el-button>
          </div>
          <div class="inline-toolbar">
            <el-input v-model="resourceFilters.keyword" :prefix-icon="Search" clearable placeholder="关键词、标签、知识点" @keyup.enter="loadResourcesInline" />
            <el-select v-model="resourceFilters.experimentId" clearable placeholder="关联实验">
              <el-option v-for="item in experiments" :key="item.id" :label="item.expName" :value="item.id" />
            </el-select>
            <el-select v-model="resourceFilters.resourceType" clearable placeholder="资源类型">
              <el-option v-for="item in resourceTypes" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
            <el-button type="primary" :icon="Search" @click="loadResourcesInline">查询</el-button>
          </div>
          <el-table v-loading="resourceLoading" :data="courseResources" stripe empty-text="暂无课程资源">
            <el-table-column label="资源名称" min-width="210">
              <template #default="{ row }">
                <div class="title-cell">
                  <strong>{{ row.title }}</strong>
                  <span>{{ row.knowledgePoint || '未设置知识点' }}</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="类型" width="130">
              <template #default="{ row }">{{ resourceTypeLabel(row.resourceType) }}</template>
            </el-table-column>
            <el-table-column prop="knowledgePoint" label="知识点" min-width="140" show-overflow-tooltip />
            <el-table-column label="必学" width="86">
              <template #default="{ row }">
                <el-tag :type="row.requiredFlag ? 'warning' : 'info'">{{ row.requiredFlag ? '必学' : '拓展' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="使用" width="150">
              <template #default="{ row }">
                <span class="metric">看 {{ row.viewCount || 0 }}</span>
                <span class="metric">下 {{ row.downloadCount || 0 }}</span>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="row.invalidFlag ? 'danger' : (row.status === 1 ? 'success' : 'info')">
                  {{ row.invalidFlag ? '失效' : (row.status === 1 ? '开放' : '停用') }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="300" fixed="right">
              <template #default="{ row }">
                <el-button text type="primary" :icon="View" @click="previewResource(row)">预览</el-button>
                <el-button text :icon="DataAnalysis" @click="openResourceStats(row)">统计</el-button>
                <el-button text type="primary" :icon="Edit" @click="openResourceEdit(row)">编辑</el-button>
                <el-button text :type="row.status === 1 ? 'warning' : 'success'" @click="toggleResourceStatus(row)">
                  {{ row.status === 1 ? '停用' : '开放' }}
                </el-button>
                <el-button text :type="row.invalidFlag ? 'success' : 'warning'" @click="toggleResourceInvalid(row)">
                  {{ row.invalidFlag ? '恢复' : '失效' }}
                </el-button>
                <el-button text type="danger" :icon="Delete" @click="removeResourceInline(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </section>

        <section v-if="activeStep === 'reservation'" class="work-section">
          <div class="reservation-block">
            <div class="section-title">
              <h2>预约管理</h2>
              <el-button type="primary" :icon="Plus" @click="openSlotCreateInline">新增预约时段</el-button>
            </div>
            <div class="reservation-config-grid">
              <article v-for="item in experiments" :key="item.id">
                <div>
                  <strong>{{ item.expName }}</strong>
                </div>
                <el-button text type="primary" :icon="Edit" @click="openReservationExperimentEdit(item)">编辑预约要求</el-button>
              </article>
              <el-empty v-if="experiments.length === 0" description="暂无实验章节" />
            </div>
          </div>

          <div class="reservation-block">
            <div class="section-title compact">
              <h3>学生预约处理</h3>
              <div class="inline-toolbar compact-toolbar filter-toolbar">
                <el-date-picker v-model="reservationFilters.date" value-format="YYYY-MM-DD" placeholder="日期" />
                <el-select v-model="reservationFilters.experimentId" clearable filterable placeholder="按实验筛选">
                  <el-option v-for="item in reservationLabOptions" :key="item.id" :label="item.label" :value="item.id" />
                </el-select>
                <el-button type="primary" :icon="Search" @click="loadPendingReservationsInline">查询</el-button>
              </div>
            </div>
            <el-table v-loading="reservationLoading" :data="pendingReservations" stripe empty-text="暂无待处理预约">
              <el-table-column prop="studentName" label="学生" min-width="110" />
              <el-table-column prop="labName" label="实验地点" min-width="130" />
              <el-table-column prop="date" label="日期" width="120" />
              <el-table-column prop="timeRange" label="时段" min-width="140" />
              <el-table-column label="实验" min-width="150">
                <template #default="{ row }">{{ experimentName(row.experimentId) }}</template>
              </el-table-column>
              <el-table-column prop="purpose" label="用途" min-width="180" show-overflow-tooltip />
              <el-table-column label="操作" width="150" fixed="right">
                <template #default="{ row }">
                  <el-button text type="success" @click="openReservationReview(row, 'APPROVED')">通过</el-button>
                  <el-button text type="danger" @click="openReservationReview(row, 'REJECTED')">驳回</el-button>
                </template>
              </el-table-column>
            </el-table>
          </div>

          <div class="reservation-block">
            <div class="section-title compact">
              <h3>预约时间段</h3>
              <div class="inline-toolbar compact-toolbar filter-toolbar">
                <el-date-picker v-model="slotFilters.date" value-format="YYYY-MM-DD" placeholder="日期" />
                <el-select v-model="slotFilters.experimentId" clearable placeholder="实验">
                  <el-option v-for="item in experiments" :key="item.id" :label="item.expName" :value="item.id" />
                </el-select>
                <el-select v-model="slotFilters.status" clearable placeholder="状态">
                  <el-option label="可预约" value="AVAILABLE" />
                  <el-option label="已满" value="FULL" />
                  <el-option label="关闭" value="CLOSED" />
                </el-select>
                <el-button type="primary" :icon="Search" @click="loadTimeSlotsInline">查询</el-button>
              </div>
            </div>
            <el-table v-loading="slotLoading" :data="timeSlots" stripe empty-text="暂无预约时段">
              <el-table-column prop="date" label="日期" width="120" />
              <el-table-column label="时段" min-width="140">
                <template #default="{ row }">{{ row.startTime }} - {{ row.endTime }}</template>
              </el-table-column>
              <el-table-column label="实验" min-width="150">
                <template #default="{ row }">{{ experimentName(row.experimentId) }}</template>
              </el-table-column>
              <el-table-column label="实验室" min-width="160">
                <template #default="{ row }">{{ slotLabName(row) }}</template>
              </el-table-column>
              <el-table-column label="容量" width="130">
                <template #default="{ row }">{{ row.bookedCount || 0 }} / {{ row.capacity || 0 }}</template>
              </el-table-column>
              <el-table-column label="状态" width="100">
                <template #default="{ row }">
                  <el-tag :type="slotStatusMeta(row.status).type">{{ slotStatusMeta(row.status).label }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="150" fixed="right">
                <template #default="{ row }">
                  <el-button text type="primary" @click="openSlotEditInline(row)">编辑</el-button>
                  <el-button text type="danger" @click="removeSlotInline(row)">删除</el-button>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </section>

        <section v-if="activeStep === 'qa'" class="work-section">
          <div class="section-title">
            <h2>答疑</h2>
            <el-button :icon="Refresh" :loading="qaLoading" @click="loadQaTopics">刷新问题</el-button>
          </div>
          <div class="qa-layout">
            <aside class="qa-list" v-loading="qaLoading">
              <article
                v-for="topic in qaTopics"
                :key="topic.id"
                class="qa-topic"
                :class="{ active: qaCurrent?.id === topic.id }"
                @click="selectQaTopic(topic)"
              >
                <div>
                  <strong>{{ topic.title }}</strong>
                  <el-tag size="small" :type="discussionStatusMeta(topic.status).type">{{ discussionStatusMeta(topic.status).label }}</el-tag>
                </div>
                <p>{{ topic.content }}</p>
                <span>{{ topic.userName || '学生' }} · {{ topic.replyCount || 0 }} 回复 · {{ topic.updateTime || topic.createTime || '-' }}</span>
              </article>
              <el-empty v-if="qaTopics.length === 0" description="暂无学生提问" :image-size="80" />
            </aside>
            <section class="qa-detail" v-loading="qaDetailLoading">
              <template v-if="qaCurrent">
                <div class="detail-head">
                  <div>
                    <h2>{{ qaCurrent.title }}</h2>
                    <p>{{ qaCurrent.userName || '学生' }} · {{ qaCurrent.createTime || '-' }}</p>
                  </div>
                  <div class="row-actions">
                    <el-button text type="success" @click="setQaStatus('RESOLVED')">标记已解决</el-button>
                    <el-button text type="warning" @click="setQaStatus('CLOSED')">关闭</el-button>
                  </div>
                </div>
                <p class="qa-content">{{ qaCurrent.content }}</p>
                <div class="qa-replies">
                  <article v-for="reply in qaCurrent.replies || []" :key="reply.id" class="qa-reply" :class="{ teacher: reply.isTeacherReply }">
                    <div>
                      <strong>{{ reply.userName }}</strong>
                      <el-tag v-if="reply.isTeacherReply" size="small" type="success">教师答疑</el-tag>
                      <span>{{ reply.createTime || '-' }}</span>
                    </div>
                    <p>{{ reply.content }}</p>
                  </article>
                  <el-empty v-if="(qaCurrent.replies || []).length === 0" description="暂无回复" :image-size="80" />
                </div>
                <el-input v-model="qaReplyContent" type="textarea" :rows="4" placeholder="输入答疑内容" />
                <div class="reply-actions">
                  <el-button type="primary" :loading="qaSaving" @click="submitQaReply">回复学生</el-button>
                </div>
              </template>
              <el-empty v-else description="请选择一个学生问题" />
            </section>
          </div>
        </section>

        <section v-if="activeStep === 'analytics'" class="work-section">
          <h2>学情预览</h2>
          <section class="summary-band">
            <div><strong>{{ detail?.experimentCount || 0 }}</strong><span>实验章节</span></div>
            <div><strong>{{ detail?.resourceCount || 0 }}</strong><span>学习资源</span></div>
            <div><strong>{{ detail?.studentCount || 0 }}</strong><span>学生</span></div>
            <div><strong>{{ Math.round(Number(detail?.averageProgress || 0)) }}%</strong><span>完成率</span></div>
          </section>
          <div class="analytics-grid">
            <div><strong>{{ students.length }}</strong><span>课堂学生</span></div>
            <div><strong>{{ experiments.length }}</strong><span>实验章节</span></div>
            <div><strong>{{ courseResources.length }}</strong><span>课程资源</span></div>
            <div><strong>{{ Math.round(Number(detail?.averageProgress || 0)) }}%</strong><span>平均完成率</span></div>
          </div>
          <div class="learning-bars">
            <article v-for="item in experiments" :key="item.id">
              <div>
                <strong>{{ item.expName }}</strong>
                <span>{{ experimentReadinessText(item) }}</span>
              </div>
              <el-progress :percentage="experimentProgress(item)" :stroke-width="8" />
            </article>
          </div>
        </section>
      </main>
    </section>

    <el-dialog v-model="classDialogVisible" :title="editingClass ? '编辑教学班' : '新增教学班'" width="560px">
      <el-form ref="classFormRef" :model="classForm" :rules="classRules" label-width="96px">
        <el-form-item label="教学班" prop="className"><el-input v-model="classForm.className" /></el-form-item>
        <el-form-item label="任课教师ID" prop="teacherId"><el-input-number v-model="classForm.teacherId" :min="1" /></el-form-item>
        <el-form-item label="助教ID"><el-input-number v-model="classForm.assistantId" :min="1" /></el-form-item>
        <el-form-item label="行政班"><el-input v-model="classForm.adminClass" /></el-form-item>
        <el-form-item label="学期"><el-input v-model="classForm.semester" /></el-form-item>
        <el-form-item label="状态"><el-switch v-model="classForm.enabled" active-text="启用" inactive-text="停用" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="classDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="savingClass" @click="saveClassInline">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="experimentDialogVisible" :title="editingExperiment ? '编辑实验章节' : '新增实验章节'" width="780px">
      <el-form ref="experimentFormRef" :model="experimentForm" :rules="experimentRules" label-width="110px">
        <div class="form-grid">
          <el-form-item label="实验名称" prop="expName"><el-input v-model="experimentForm.expName" /></el-form-item>
          <el-form-item label="实验编号" prop="expCode"><el-input v-model="experimentForm.expCode" /></el-form-item>
          <el-form-item label="风险等级" prop="riskLevel">
            <el-select v-model="experimentForm.riskLevel">
              <el-option label="低风险" value="LOW" />
              <el-option label="中风险" value="MEDIUM" />
              <el-option label="高风险" value="HIGH" />
            </el-select>
          </el-form-item>
          <el-form-item label="实验时长"><el-input-number v-model="experimentForm.durationMinutes" :min="1" /></el-form-item>
          <el-form-item label="安全考试"><el-switch v-model="experimentForm.examRequired" active-text="需要" inactive-text="不需要" /></el-form-item>
          <el-form-item label="开放预约"><el-switch v-model="experimentForm.reservationEnabled" active-text="开放" inactive-text="关闭" /></el-form-item>
          <el-form-item label="开放状态"><el-switch v-model="experimentForm.enabled" active-text="开放" inactive-text="停用" /></el-form-item>
          <el-form-item label="排序"><el-input-number v-model="experimentForm.sort" :min="0" /></el-form-item>
        </div>
        <el-form-item label="封面地址"><el-input v-model="experimentForm.coverUrl" placeholder="可选，留空使用实验路径默认图" /></el-form-item>
        <el-form-item label="情境导入"><el-input v-model="experimentForm.scenarioIntro" type="textarea" :rows="2" placeholder="用真实工程问题引出本实验" /></el-form-item>
        <el-form-item label="视觉主题"><el-input v-model="experimentForm.visualTheme" placeholder="如 procedure-map" /></el-form-item>
        <el-form-item label="实验目标"><el-input v-model="experimentForm.objective" type="textarea" :rows="2" /></el-form-item>
        <el-form-item label="实验原理"><el-input v-model="experimentForm.principle" type="textarea" :rows="2" /></el-form-item>
        <el-form-item label="仪器材料"><el-input v-model="experimentForm.equipment" type="textarea" :rows="2" /></el-form-item>
        <el-form-item label="实验材料"><el-input v-model="experimentForm.materials" type="textarea" :rows="2" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="experimentDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="savingExperiment" @click="saveExperimentInline">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="examDialogVisible" :title="editingExamPaper ? '编辑试卷' : '新增试卷'" width="660px">
      <el-form :model="examForm" label-width="100px">
        <el-form-item label="试卷标题" required><el-input v-model="examForm.title" /></el-form-item>
        <el-form-item label="关联实验">
          <el-select v-model="examForm.experimentId" clearable placeholder="可选择本课堂实验">
            <el-option v-for="item in experiments" :key="item.id" :label="item.expName" :value="item.id" />
          </el-select>
        </el-form-item>
        <div class="form-grid">
          <el-form-item label="总分"><el-input-number v-model="examForm.totalScore" :min="1" :max="500" /></el-form-item>
          <el-form-item label="及格分"><el-input-number v-model="examForm.passScore" :min="1" :max="500" /></el-form-item>
          <el-form-item label="考试时长"><el-input-number v-model="examForm.duration" :min="1" :max="300" /></el-form-item>
          <el-form-item label="考试次数"><el-input-number v-model="examForm.attemptLimit" :min="1" :max="10" /></el-form-item>
        </div>
        <el-form-item label="答案显示">
          <el-switch v-model="examForm.showAnswerAfterSubmit" :active-value="1" :inactive-value="0" active-text="交卷后显示" />
        </el-form-item>
        <el-form-item label="有效期(天)"><el-input-number v-model="examForm.admissionValidityDays" :min="1" :max="3650" /></el-form-item>
        <el-form-item label="随机抽题">
          <el-switch v-model="examForm.randomEnabled" :active-value="1" :inactive-value="0" />
        </el-form-item>
        <el-form-item v-if="examForm.randomEnabled" label="随机题数">
          <el-input-number v-model="examForm.randomCount" :min="1" :max="200" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="examForm.status">
            <el-option label="草稿" value="DRAFT" />
            <el-option label="已发布" value="PUBLISHED" />
            <el-option label="已关闭" value="CLOSED" />
          </el-select>
        </el-form-item>
        <el-form-item label="说明"><el-input v-model="examForm.description" type="textarea" :rows="4" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="examDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="examSaving" @click="saveExamPaperInline">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="reportDetailVisible" title="报告详情" width="760px">
      <div v-loading="reportDetailLoading" class="detail-body">
        <template v-if="reportDetail">
          <div class="detail-head">
            <h2>{{ reportDetailReport.title }}</h2>
            <el-tag :type="reportStatusMeta(reportDetailReport.status).type">{{ reportStatusMeta(reportDetailReport.status).label }}</el-tag>
          </div>
          <p class="detail-meta">学生：{{ reportDetailReport.studentName || reportDetailReport.studentId }}　实验ID：{{ reportDetailReport.experimentId }}</p>
          <p class="report-content">{{ reportDetailReport.content || '暂无文字内容' }}</p>
          <el-link v-if="reportDetailReport.fileUrl" :href="reportDetailReport.fileUrl" target="_blank" type="primary">查看附件</el-link>
        </template>
      </div>
    </el-dialog>

    <el-dialog v-model="reportGradeVisible" title="报告评分" width="560px">
      <el-form :model="reportGradeForm" label-width="88px">
        <el-form-item label="分数"><el-input-number v-model="reportGradeForm.score" :min="0" :max="100" /></el-form-item>
        <el-form-item label="总评"><el-input v-model="reportGradeForm.comment" type="textarea" :rows="5" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reportGradeVisible = false">取消</el-button>
        <el-button type="primary" :loading="reportSaving" @click="submitReportGrade">保存评分</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="reportReturnVisible" title="退回修改" width="520px">
      <el-input v-model="reportReturnComment" type="textarea" :rows="5" placeholder="填写退回原因和修改建议" />
      <template #footer>
        <el-button @click="reportReturnVisible = false">取消</el-button>
        <el-button type="danger" :loading="reportSaving" @click="submitReportReturn">退回报告</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="resourceDialogVisible" :title="editingResource ? '编辑资源' : '新增资源'" width="780px">
      <el-form ref="resourceFormRef" :model="resourceForm" :rules="resourceRules" label-width="110px">
        <div class="form-grid">
          <el-form-item label="关联实验" prop="experimentId">
            <el-select v-model="resourceForm.experimentId" filterable placeholder="请选择实验">
              <el-option v-for="item in experiments" :key="item.id" :label="item.expName" :value="item.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="标题" prop="title"><el-input v-model="resourceForm.title" /></el-form-item>
          <el-form-item label="资源类型" prop="resourceType">
            <el-select v-model="resourceForm.resourceType" filterable>
              <el-option v-for="item in resourceTypes" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
          <el-form-item label="知识点"><el-input v-model="resourceForm.knowledgePoint" /></el-form-item>
          <el-form-item label="风险类型"><el-input v-model="resourceForm.riskType" /></el-form-item>
          <el-form-item label="标签"><el-input v-model="resourceForm.tags" placeholder="多个标签用逗号分隔" /></el-form-item>
          <el-form-item label="必学资源"><el-switch v-model="resourceForm.required" /></el-form-item>
          <el-form-item label="开放状态"><el-switch v-model="resourceForm.enabled" /></el-form-item>
          <el-form-item label="完成规则">
            <el-select v-model="resourceForm.completionRule">
              <el-option label="确认完成" value="CONFIRM" />
              <el-option label="按进度" value="PROGRESS" />
              <el-option label="按时长" value="TIME" />
              <el-option label="进度+时长" value="PROGRESS_TIME" />
            </el-select>
          </el-form-item>
          <el-form-item label="最低进度"><el-input-number v-model="resourceForm.minProgress" :min="0" :max="100" /></el-form-item>
          <el-form-item label="最短学习秒数"><el-input-number v-model="resourceForm.minStudySeconds" :min="0" /></el-form-item>
          <el-form-item label="排序"><el-input-number v-model="resourceForm.sort" :min="0" /></el-form-item>
        </div>
        <el-form-item label="外部链接"><el-input v-model="resourceForm.url" placeholder="https://..." /></el-form-item>
        <el-form-item label="上传文件">
          <div class="upload-row">
            <el-upload :auto-upload="false" :show-file-list="false" :on-change="handleResourceFilePicked">
              <el-button :icon="Upload">选择文件</el-button>
            </el-upload>
            <span>{{ resourceForm.originalFilename || resourceForm.filePath || '未选择文件' }}</span>
          </div>
        </el-form-item>
        <el-form-item label="资源说明"><el-input v-model="resourceForm.description" type="textarea" :rows="3" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="resourceDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="resourceSaving" @click="saveResourceInline">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="resourceStatsVisible" title="资源使用统计" width="520px">
      <div v-if="resourceStats" class="stats-grid">
        <div><strong>{{ resourceStats.viewCount || 0 }}</strong><span>查看次数</span></div>
        <div><strong>{{ resourceStats.downloadCount || 0 }}</strong><span>下载次数</span></div>
        <div><strong>{{ resourceStats.favoriteCount || 0 }}</strong><span>收藏</span></div>
        <div><strong>{{ resourceStats.completionRate || 0 }}%</strong><span>完成率</span></div>
      </div>
    </el-dialog>

    <el-dialog v-model="reservationExperimentVisible" title="编辑实验预约要求" width="680px">
      <el-form :model="reservationExperimentForm" label-width="110px">
        <el-form-item label="实验名称">
          <el-input v-model="reservationExperimentForm.expName" disabled />
        </el-form-item>
        <el-form-item label="实验地点">
          <el-input v-model="reservationExperimentForm.location" placeholder="如 油气工程实验中心 301" />
        </el-form-item>
        <el-form-item label="适用班级">
          <el-select
            v-model="reservationApplicableClasses"
            multiple
            clearable
            collapse-tags
            collapse-tags-tooltip
            placeholder="请选择该实验适用的教学班"
            :disabled="classOptions.length === 0"
          >
            <el-option
              v-for="item in classOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="准入分数">
          <el-input-number v-model="reservationExperimentForm.safetyPassScore" :min="0" :max="100" />
        </el-form-item>
        <el-form-item label="报告模板">
          <el-input v-model="reservationExperimentForm.reportTemplateUrl" placeholder="报告模板链接，可选" />
        </el-form-item>
        <el-form-item label="开放预约">
          <el-switch v-model="reservationExperimentForm.reservationEnabled" active-text="开放" inactive-text="关闭" />
        </el-form-item>
        <div class="notice-block">
          <h3>实验须知</h3>
          <el-form-item label="危险源"><el-input v-model="reservationExperimentForm.hazardSources" type="textarea" :rows="2" /></el-form-item>
          <el-form-item label="风险类型"><el-input v-model="reservationExperimentForm.riskTypes" type="textarea" :rows="2" /></el-form-item>
          <el-form-item label="PPE要求"><el-input v-model="reservationExperimentForm.ppeRequirements" type="textarea" :rows="2" /></el-form-item>
          <el-form-item label="前置知识"><el-input v-model="reservationExperimentForm.prerequisiteKnowledge" type="textarea" :rows="2" /></el-form-item>
          <el-form-item label="安全要求"><el-input v-model="reservationExperimentForm.safetyRequirement" type="textarea" :rows="3" /></el-form-item>
          <el-form-item label="数据记录"><el-input v-model="reservationExperimentForm.dataRecordRequirement" type="textarea" :rows="2" /></el-form-item>
          <el-form-item label="异常处理"><el-input v-model="reservationExperimentForm.abnormalHandling" type="textarea" :rows="2" /></el-form-item>
          <el-form-item label="应急处置"><el-input v-model="reservationExperimentForm.emergencyProcedure" type="textarea" :rows="2" /></el-form-item>
          <el-form-item label="报告要求"><el-input v-model="reservationExperimentForm.gradingCriteria" type="textarea" :rows="2" /></el-form-item>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="reservationExperimentVisible = false">取消</el-button>
        <el-button type="primary" :loading="reservationSaving" @click="saveReservationExperiment">保存预约要求</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="slotDialogVisible" :title="editingSlot ? '编辑预约时段' : '新增预约时段'" width="620px">
      <el-form :model="slotForm" label-width="92px">
        <el-form-item label="关联实验" required>
          <el-select v-model="slotForm.experimentId" filterable placeholder="请选择实验" @change="fillSlotLab">
            <el-option v-for="item in experiments" :key="item.id" :label="item.expName" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="日期" required><el-date-picker v-model="slotForm.date" value-format="YYYY-MM-DD" /></el-form-item>
        <el-form-item label="开始时间" required><el-time-picker v-model="slotForm.startTime" value-format="HH:mm" format="HH:mm" /></el-form-item>
        <el-form-item label="结束时间" required><el-time-picker v-model="slotForm.endTime" value-format="HH:mm" format="HH:mm" /></el-form-item>
        <el-form-item label="实验室" required>
          <el-select v-model="slotForm.labId" filterable placeholder="请选择实验室">
            <el-option
              v-for="item in experiments"
              :key="`lab-${item.id}`"
              :label="experimentLocation(item.id) || '未设置实验地点'"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="容量"><el-input-number v-model="slotForm.capacity" :min="1" :max="200" /></el-form-item>
        <el-form-item label="状态">
          <el-select v-model="slotForm.status">
            <el-option label="可预约" value="AVAILABLE" />
            <el-option label="已满" value="FULL" />
            <el-option label="关闭" value="CLOSED" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="slotDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="slotSaving" @click="saveSlotInline">保存时段</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="reservationReviewVisible" :title="reservationReviewForm.status === 'APPROVED' ? '通过预约' : '驳回预约'" width="520px">
      <el-input v-model="reservationReviewForm.reviewComment" type="textarea" :rows="5" placeholder="填写审核意见" />
      <template #footer>
        <el-button @click="reservationReviewVisible = false">取消</el-button>
        <el-button type="primary" :loading="reservationSaving" @click="submitReservationReview">提交审核</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="stepDrawerVisible" :title="`${stepEditingExperiment?.expName || '实验'} - 实验步骤`" size="760px">
      <div class="step-editor">
        <div class="section-title compact">
          <h3>步骤内容、视频链接与相关要求</h3>
          <div class="row-actions">
            <el-button :icon="Plus" :disabled="isArchived" @click="addStepRow">新增步骤</el-button>
            <el-button type="primary" :loading="stepSaving" :disabled="isArchived" @click="saveStepRows">保存步骤</el-button>
          </div>
        </div>
        <el-empty v-if="stepRows.length === 0" description="暂无实验步骤" />
        <div v-else class="step-edit-list">
          <article v-for="(step, index) in stepRows" :key="index" class="step-edit-card">
            <div class="step-meta-grid">
              <el-input-number v-model="step.stepNo" :min="1" />
              <el-input v-model="step.title" placeholder="步骤标题" />
              <el-select v-model="step.mediaType" placeholder="展示类型">
                <el-option label="文本" value="TEXT" />
                <el-option label="视频" value="VIDEO" />
                <el-option label="图片" value="IMAGE" />
                <el-option label="流程图" value="FLOWCHART" />
              </el-select>
              <el-input-number v-model="step.estimatedMinutes" :min="0" />
              <el-button text type="danger" :icon="Delete" :disabled="isArchived" @click="stepRows.splice(index, 1)">删除</el-button>
            </div>
            <div class="step-columns">
              <el-input v-model="step.content" type="textarea" :rows="4" placeholder="步骤文字说明，学生章节详情左侧显示该内容" />
              <el-input v-model="step.safetyTip" type="textarea" :rows="4" placeholder="风险提示、操作要求或注意事项" />
            </div>
            <div class="step-media-grid">
              <el-input v-model="step.mediaUrl" placeholder="步骤视频、图片或资料链接" />
              <el-input v-model="step.flowchartData" placeholder="流程图数据或说明" />
            </div>
          </article>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Back,
  Calendar,
  ChatDotRound,
  Check,
  DataAnalysis,
  Delete,
  Document,
  Edit,
  EditPen,
  Finished,
  Folder,
  Operation,
  Plus,
  Reading,
  Refresh,
  Search,
  Upload,
  User,
  View,
} from '@element-plus/icons-vue'
import {
  archiveCourse,
  createCourseInvite,
  createCourseClass,
  deleteCourseClass,
  disableCourseInvite,
  getCourseInvites,
  getCourseDetail,
  publishCourse,
  updateCourse,
  updateCourseClass,
} from '@/api/course'
import { createExperiment, getExperimentDetail, saveExperimentSteps, updateExperiment, updateExperimentStatus } from '@/api/experiment'
import { createExamPaper, deleteExamPaper, getExamPapers, updateExamPaper, updateExamPaperStatus } from '@/api/examPaper'
import { getPendingReports, getReportDetail, gradeReport, returnReport } from '@/api/report'
import {
  createResource,
  deleteResource,
  getResources,
  getResourceStats,
  markResourceInvalid,
  updateResource,
  updateResourceStatus,
  uploadResource,
} from '@/api/resource'
import {
  createTimeSlots,
  deleteTimeSlot,
  getPendingReservations,
  getTimeSlots,
  reviewReservation,
  updateTimeSlot,
} from '@/api/reservation'
import { getDiscussionDetail, getDiscussions, replyDiscussion, updateDiscussionStatus } from '@/api/discussion'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const savingCourse = ref(false)
const savingClass = ref(false)
const savingExperiment = ref(false)
const examLoading = ref(false)
const examSaving = ref(false)
const reportDetailLoading = ref(false)
const reportSaving = ref(false)
const resourceLoading = ref(false)
const resourceSaving = ref(false)
const reservationLoading = ref(false)
const reservationSaving = ref(false)
const slotLoading = ref(false)
const slotSaving = ref(false)
const qaLoading = ref(false)
const qaDetailLoading = ref(false)
const qaSaving = ref(false)
const inviteLoading = ref(false)
const stepSaving = ref(false)
const detail = ref(null)
const activeStep = ref(String(route.query.step || 'basic'))
const classDialogVisible = ref(false)
const experimentDialogVisible = ref(false)
const examDialogVisible = ref(false)
const reportDetailVisible = ref(false)
const reportGradeVisible = ref(false)
const reportReturnVisible = ref(false)
const resourceDialogVisible = ref(false)
const resourceStatsVisible = ref(false)
const reservationExperimentVisible = ref(false)
const slotDialogVisible = ref(false)
const reservationReviewVisible = ref(false)
const stepDrawerVisible = ref(false)
const editingClass = ref(null)
const editingExperiment = ref(null)
const editingExamPaper = ref(null)
const editingResource = ref(null)
const currentReport = ref(null)
const currentReservation = ref(null)
const editingSlot = ref(null)
const reservationEditingExperiment = ref(null)
const stepEditingExperiment = ref(null)
const courseFormRef = ref()
const classFormRef = ref()
const experimentFormRef = ref()
const resourceFormRef = ref()
const invites = ref([])
const examPapers = ref([])
const pendingReports = ref([])
const courseResources = ref([])
const stepRows = ref([])
const reportDetail = ref(null)
const reportReturnComment = ref('')
const resourceStats = ref(null)
const pendingReservations = ref([])
const timeSlots = ref([])
const qaTopics = ref([])
const qaCurrent = ref(null)
const qaReplyContent = ref('')
const examFilters = reactive({ keyword: '', status: '' })
const resourceFilters = reactive({ keyword: '', experimentId: '', resourceType: '' })
const reservationFilters = reactive({ date: '', experimentId: '' })
const slotFilters = reactive({ date: '', experimentId: '', status: '' })

const courseId = computed(() => Number(route.params.courseId))
const course = computed(() => detail.value?.course || null)
const classes = computed(() => detail.value?.teachingClasses || [])
const classOptions = computed(() => classes.value
  .filter((item) => item?.className)
  .map((item) => ({ label: item.className, value: item.className })))
const students = computed(() => detail.value?.students || [])
const experiments = computed(() => detail.value?.experiments || [])
const isArchived = computed(() => course.value?.status === 2)

const courseForm = reactive(defaultCourseForm())
const classForm = reactive(defaultClassForm())
const experimentForm = reactive(defaultExperimentForm())
const examForm = reactive(defaultExamForm())
const reportGradeForm = reactive({ score: 80, comment: '' })
const resourceForm = reactive(defaultResourceForm())
const reservationExperimentForm = reactive(defaultReservationExperimentForm())
const reservationApplicableClasses = ref([])
const slotForm = reactive(defaultSlotForm())
const reservationReviewForm = reactive({ status: 'APPROVED', reviewComment: '' })
const pendingReportGroups = computed(() => {
  const map = new Map()
  pendingReports.value.forEach((item) => {
    const experimentId = item.experimentId || item.expId || 0
    const experimentName = item.experimentName || item.expName || experiments.value.find((exp) => exp.id === experimentId)?.expName || '未关联实验'
    if (!map.has(experimentId)) map.set(experimentId, { experimentId, experimentName, items: [] })
    map.get(experimentId).items.push(item)
  })
  return [...map.values()]
})
const reportDetailReport = computed(() => reportDetail.value?.report || reportDetail.value || {})
const reservationLabOptions = computed(() => experiments.value.map((item) => ({
  id: item.id,
  label: item.location ? `${item.location}（${item.expName}）` : `实验 ${item.id}（${item.expName}）`,
})))

const resourceTypes = [
  { label: '实验指导书', value: 'GUIDE' },
  { label: '课程讲义', value: 'LECTURE' },
  { label: 'PPT课件', value: 'PPT' },
  { label: '教学视频', value: 'TEACHING_VIDEO' },
  { label: '微课', value: 'MICRO_COURSE' },
  { label: '仪器操作视频', value: 'INSTRUMENT_VIDEO' },
  { label: '设备说明书', value: 'DEVICE_MANUAL' },
  { label: '实验案例', value: 'EXPERIMENT_CASE' },
  { label: '事故案例', value: 'ACCIDENT_CASE' },
  { label: '应急处置流程', value: 'EMERGENCY_PROCESS' },
  { label: '参考文献', value: 'REFERENCE' },
  { label: '虚拟仿真实验', value: 'VIRTUAL_SIMULATION' },
  { label: '文档', value: 'DOCUMENT' },
  { label: '图片', value: 'IMAGE' },
  { label: '视频', value: 'VIDEO' },
  { label: '网页链接', value: 'LINK' },
  { label: '文件', value: 'FILE' },
]

const courseRules = {
  courseName: [{ required: true, message: '请输入课程名称', trigger: 'blur' }],
  courseCode: [{ required: true, message: '请输入课程编号', trigger: 'blur' }],
  teacherId: [{ required: true, message: '请输入负责人ID', trigger: 'change' }],
}

const classRules = {
  className: [{ required: true, message: '请输入教学班名称', trigger: 'blur' }],
  teacherId: [{ required: true, message: '请输入任课教师ID', trigger: 'change' }],
}

const experimentRules = {
  expName: [{ required: true, message: '请输入实验名称', trigger: 'blur' }],
  expCode: [{ required: true, message: '请输入实验编号', trigger: 'blur' }],
  riskLevel: [{ required: true, message: '请选择风险等级', trigger: 'change' }],
}
const resourceRules = {
  experimentId: [{ required: true, message: '请选择关联实验', trigger: 'change' }],
  title: [{ required: true, message: '请填写资源标题', trigger: 'blur' }],
  resourceType: [{ required: true, message: '请选择资源类型', trigger: 'change' }],
}

const steps = [
  { key: 'basic', label: '课程基本信息', icon: Reading },
  { key: 'classes', label: '教学班和学生', icon: User },
  { key: 'experiments', label: '实验章节和规程', icon: Operation },
  { key: 'exam', label: '考试管理', icon: EditPen },
  { key: 'reports', label: '报告管理', icon: Document },
  { key: 'resources', label: '资源管理', icon: Folder },
  { key: 'reservation', label: '预约管理', icon: Calendar },
  { key: 'qa', label: '答疑', icon: ChatDotRound },
  { key: 'analytics', label: '学情预览', icon: Finished },
]

onMounted(loadDetail)
watch(activeStep, (step) => {
  if (step === 'qa') loadQaTopics()
})

async function loadDetail() {
  loading.value = true
  try {
    detail.value = await getCourseDetail(courseId.value)
    fillCourseForm()
    await loadSupportingData()
  } finally {
    loading.value = false
  }
}

async function loadSupportingData() {
  const [inviteResult, paperResult, reportResult, resourceResult, reservationResult, slotResult] = await Promise.allSettled([
    getCourseInvites(courseId.value),
    getExamPapers({ courseId: courseId.value, pageNum: 1, pageSize: 100, keyword: examFilters.keyword || undefined, status: examFilters.status || undefined }),
    getPendingReports({ courseId: courseId.value, pageNum: 1, pageSize: 100 }),
    getResources({ courseId: courseId.value, publicFlag: 0, pageNum: 1, pageSize: 100 }),
    getPendingReservations({ pageNum: 1, pageSize: 100 }),
    getTimeSlots({ pageNum: 1, pageSize: 100 }),
  ])
  invites.value = inviteResult.status === 'fulfilled' ? (inviteResult.value || []) : []
  examPapers.value = paperResult.status === 'fulfilled' ? (paperResult.value?.records || paperResult.value || []) : []
  pendingReports.value = reportResult.status === 'fulfilled' ? (reportResult.value?.records || reportResult.value || []) : []
  courseResources.value = resourceResult.status === 'fulfilled' ? (resourceResult.value?.records || resourceResult.value || []) : []
  pendingReservations.value = reservationResult.status === 'fulfilled' ? filterCourseReservations(reservationResult.value?.records || reservationResult.value || []) : []
  timeSlots.value = slotResult.status === 'fulfilled' ? filterCourseSlots(slotResult.value?.records || slotResult.value || []) : []
}

async function loadReportsInline() {
  const result = await getPendingReports({ courseId: courseId.value, pageNum: 1, pageSize: 100 })
  pendingReports.value = result?.records || result || []
}

async function loadResourcesInline() {
  resourceLoading.value = true
  try {
    const result = await getResources({
      courseId: courseId.value,
      publicFlag: 0,
      pageNum: 1,
      pageSize: 100,
      keyword: resourceFilters.keyword || undefined,
      experimentId: resourceFilters.experimentId || undefined,
      resourceType: resourceFilters.resourceType || undefined,
    })
    courseResources.value = result?.records || result || []
  } finally {
    resourceLoading.value = false
  }
}

async function loadExamPapersInline() {
  examLoading.value = true
  try {
    const result = await getExamPapers({
      courseId: courseId.value,
      pageNum: 1,
      pageSize: 100,
      keyword: examFilters.keyword || undefined,
      status: examFilters.status || undefined,
    })
    examPapers.value = result?.records || result || []
  } finally {
    examLoading.value = false
  }
}

async function loadPendingReservationsInline() {
  reservationLoading.value = true
  try {
    const result = await getPendingReservations({ pageNum: 1, pageSize: 100 })
    pendingReservations.value = filterCourseReservations(result?.records || result || [])
  } finally {
    reservationLoading.value = false
  }
}

async function loadTimeSlotsInline() {
  slotLoading.value = true
  try {
    const result = await getTimeSlots({
      pageNum: 1,
      pageSize: 100,
      date: slotFilters.date || undefined,
      status: slotFilters.status || undefined,
    })
    timeSlots.value = filterCourseSlots(result?.records || result || [])
  } finally {
    slotLoading.value = false
  }
}

async function loadQaTopics() {
  qaLoading.value = true
  try {
    const result = await getDiscussions({
      courseId: courseId.value,
      pageNum: 1,
      pageSize: 100,
    })
    qaTopics.value = result?.records || result || []
    if (!qaCurrent.value && qaTopics.value.length) {
      await selectQaTopic(qaTopics.value[0])
    } else if (qaCurrent.value) {
      const stillExists = qaTopics.value.some((item) => Number(item.id) === Number(qaCurrent.value.id))
      if (!stillExists) qaCurrent.value = null
    }
  } finally {
    qaLoading.value = false
  }
}

async function selectQaTopic(topic) {
  if (!topic?.id) return
  qaDetailLoading.value = true
  try {
    qaCurrent.value = await getDiscussionDetail(topic.id)
    qaReplyContent.value = ''
  } finally {
    qaDetailLoading.value = false
  }
}

async function saveCourseInline() {
  await courseFormRef.value?.validate()
  savingCourse.value = true
  try {
    await updateCourse(courseId.value, {
      courseName: courseForm.courseName.trim(),
      courseCode: courseForm.courseCode.trim(),
      direction: courseForm.direction || undefined,
      teacherId: Number(courseForm.teacherId),
      coverUrl: courseForm.coverUrl || undefined,
      tagline: courseForm.tagline || undefined,
      highlightTags: courseForm.highlightTags || undefined,
      visualTheme: courseForm.visualTheme || undefined,
      description: courseForm.description || undefined,
      semester: courseForm.semester || undefined,
      status: course.value?.status === 1 ? 1 : 0,
      sort: Number(courseForm.sort || 0),
      credit: Number(courseForm.credit || 0),
      hours: Number(courseForm.hours || 0),
      assessmentMethod: courseForm.assessmentMethod || undefined,
      learningRequirement: courseForm.learningRequirement || undefined,
      allowEmptyPublish: courseForm.allowEmptyPublish ? 1 : 0,
    })
    ElMessage.success('课程信息已保存')
    await loadDetail()
  } finally {
    savingCourse.value = false
  }
}

async function publishCurrentCourse() {
  await publishCourse(courseId.value, courseForm.allowEmptyPublish)
  ElMessage.success('课程已发布')
  await loadDetail()
}

async function archiveCurrentCourse() {
  await ElMessageBox.confirm('确认归档当前课程吗？归档后课程建设页将以只读方式展示。', '归档课程', { type: 'warning' })
  await archiveCourse(courseId.value)
  ElMessage.success('课程已归档')
  await loadDetail()
}

function openClassCreate() {
  editingClass.value = null
  Object.assign(classForm, defaultClassForm())
  classDialogVisible.value = true
}

function openClassEdit(row) {
  editingClass.value = row
  Object.assign(classForm, {
    className: row.className || '',
    teacherId: row.teacherId || course.value?.teacherId || null,
    assistantId: row.assistantId || null,
    adminClass: row.adminClass || '',
    semester: row.semester || course.value?.semester || '',
    enabled: row.status === 1,
  })
  classDialogVisible.value = true
}

async function saveClassInline() {
  await classFormRef.value?.validate()
  savingClass.value = true
  try {
    const payload = {
      className: classForm.className.trim(),
      teacherId: Number(classForm.teacherId),
      assistantId: classForm.assistantId ? Number(classForm.assistantId) : undefined,
      adminClass: classForm.adminClass || undefined,
      semester: classForm.semester || undefined,
      status: classForm.enabled ? 1 : 0,
    }
    if (editingClass.value) {
      await updateCourseClass(courseId.value, editingClass.value.id, payload)
      ElMessage.success('教学班已更新')
    } else {
      await createCourseClass(courseId.value, payload)
      ElMessage.success('教学班已创建')
    }
    classDialogVisible.value = false
    await loadDetail()
  } finally {
    savingClass.value = false
  }
}

async function removeClass(row) {
  await ElMessageBox.confirm(`确认删除教学班“${row.className}”吗？`, '删除教学班', { type: 'warning' })
  await deleteCourseClass(courseId.value, row.id)
  ElMessage.success('教学班已删除')
  await loadDetail()
}

async function createInviteInline() {
  inviteLoading.value = true
  try {
    await createCourseInvite(courseId.value, { maxUses: 200 })
    ElMessage.success('课堂邀请码已生成')
    invites.value = await getCourseInvites(courseId.value)
  } finally {
    inviteLoading.value = false
  }
}

async function disableInviteInline(row) {
  await ElMessageBox.confirm(`确认停用邀请码“${row.inviteCode}”吗？`, '停用邀请码', { type: 'warning' })
  await disableCourseInvite(row.id)
  ElMessage.success('邀请码已停用')
  invites.value = await getCourseInvites(courseId.value)
}

function openExperimentCreate() {
  editingExperiment.value = null
  Object.assign(experimentForm, defaultExperimentForm())
  experimentDialogVisible.value = true
}

async function openExperimentEdit(row) {
  editingExperiment.value = row
  const detailResult = await getExperimentDetail(row.id)
  fillExperimentForm(detailResult?.experiment || row)
  experimentDialogVisible.value = true
}

async function openStepEditor(row) {
  stepEditingExperiment.value = row
  const detailResult = await getExperimentDetail(row.id)
  stepRows.value = (detailResult?.steps || []).map((item) => ({ mediaType: 'TEXT', requiredFlag: 1, ...item }))
  stepDrawerVisible.value = true
}

function addStepRow() {
  stepRows.value.push({
    stepNo: stepRows.value.length + 1,
    title: '',
    content: '',
    safetyTip: '',
    mediaType: 'TEXT',
    mediaUrl: '',
    flowchartData: '',
    requiredFlag: 1,
    estimatedMinutes: 0,
  })
}

async function saveStepRows() {
  if (!stepEditingExperiment.value?.id) return
  if (stepRows.value.some((item) => !item.stepNo || !item.title?.trim() || !item.content?.trim())) {
    ElMessage.warning('请完善步骤序号、标题和文字说明')
    return
  }
  stepSaving.value = true
  try {
    await saveExperimentSteps(stepEditingExperiment.value.id, stepRows.value.map((item) => ({
      stepNo: Number(item.stepNo),
      title: item.title.trim(),
      content: item.content.trim(),
      safetyTip: item.safetyTip || undefined,
      mediaType: item.mediaType || 'TEXT',
      mediaUrl: item.mediaUrl || undefined,
      flowchartData: item.flowchartData || undefined,
      requiredFlag: item.requiredFlag ?? 1,
      estimatedMinutes: Number(item.estimatedMinutes || 0),
    })))
    ElMessage.success('实验步骤已保存')
    stepDrawerVisible.value = false
    await loadDetail()
  } finally {
    stepSaving.value = false
  }
}

async function saveExperimentInline() {
  await experimentFormRef.value?.validate()
  savingExperiment.value = true
  try {
    const payload = experimentPayload()
    if (editingExperiment.value) {
      await updateExperiment(editingExperiment.value.id, payload)
      ElMessage.success('实验章节已更新')
    } else {
      await createExperiment(payload)
      ElMessage.success('实验章节已创建')
    }
    experimentDialogVisible.value = false
    await loadDetail()
  } finally {
    savingExperiment.value = false
  }
}

async function toggleExperiment(row) {
  await updateExperimentStatus(row.id, row.status === 1 ? 0 : 1)
  ElMessage.success('实验状态已更新')
  await loadDetail()
}

function openExamCreate() {
  editingExamPaper.value = null
  Object.assign(examForm, defaultExamForm())
  examDialogVisible.value = true
}

function openExamEdit(row) {
  editingExamPaper.value = row
  Object.assign(examForm, {
    title: row.title || '',
    description: row.description || '',
    experimentId: row.experimentId || '',
    totalScore: row.totalScore || 100,
    passScore: row.passScore || 60,
    duration: row.duration || 60,
    attemptLimit: row.attemptLimit || 1,
    showAnswerAfterSubmit: row.showAnswerAfterSubmit ?? 1,
    admissionValidityDays: row.admissionValidityDays || 180,
    multipleScorePolicy: row.multipleScorePolicy || 'ALL_OR_NOTHING',
    randomEnabled: row.randomEnabled || 0,
    randomCount: row.randomCount || 0,
    status: row.status || 'DRAFT',
  })
  examDialogVisible.value = true
}

async function saveExamPaperInline() {
  if (!examForm.title.trim()) {
    ElMessage.warning('请填写试卷标题')
    return
  }
  const payload = {
    title: examForm.title.trim(),
    description: examForm.description || undefined,
    courseId: courseId.value,
    experimentId: examForm.experimentId ? Number(examForm.experimentId) : undefined,
    totalScore: Number(examForm.totalScore),
    passScore: Number(examForm.passScore),
    duration: Number(examForm.duration),
    attemptLimit: Number(examForm.attemptLimit),
    showAnswerAfterSubmit: Number(examForm.showAnswerAfterSubmit),
    admissionValidityDays: Number(examForm.admissionValidityDays),
    multipleScorePolicy: examForm.multipleScorePolicy,
    randomEnabled: Number(examForm.randomEnabled),
    randomCount: Number(examForm.randomCount || 0),
    status: examForm.status,
  }
  examSaving.value = true
  try {
    if (editingExamPaper.value) {
      await updateExamPaper(editingExamPaper.value.id, payload)
      ElMessage.success('试卷已更新')
    } else {
      await createExamPaper(payload)
      ElMessage.success('试卷已创建')
    }
    examDialogVisible.value = false
    await loadExamPapersInline()
  } finally {
    examSaving.value = false
  }
}

async function toggleExamStatus(row) {
  const status = row.status === 'PUBLISHED' ? 'CLOSED' : 'PUBLISHED'
  await updateExamPaperStatus(row.id, status)
  ElMessage.success(status === 'PUBLISHED' ? '试卷已发布' : '试卷已关闭')
  await loadExamPapersInline()
}

async function removeExamPaper(row) {
  await ElMessageBox.confirm(`确认删除试卷“${row.title}”吗？`, '删除试卷', { type: 'warning' })
  await deleteExamPaper(row.id)
  ElMessage.success('试卷已删除')
  await loadExamPapersInline()
}

async function openReportDetail(row) {
  reportDetailVisible.value = true
  reportDetailLoading.value = true
  try {
    reportDetail.value = await getReportDetail(row.id)
  } finally {
    reportDetailLoading.value = false
  }
}

function openReportGrade(row) {
  currentReport.value = row
  Object.assign(reportGradeForm, { score: row.latestScore || 80, comment: row.latestComment || '' })
  reportGradeVisible.value = true
}

function openReportReturn(row) {
  currentReport.value = row
  reportReturnComment.value = row.latestComment || ''
  reportReturnVisible.value = true
}

async function submitReportGrade() {
  if (!currentReport.value?.id) return
  reportSaving.value = true
  try {
    await gradeReport(currentReport.value.id, reportGradeForm)
    ElMessage.success('报告已评分')
    reportGradeVisible.value = false
    await loadReportsInline()
  } finally {
    reportSaving.value = false
  }
}

async function submitReportReturn() {
  if (!currentReport.value?.id) return
  if (!reportReturnComment.value.trim()) {
    ElMessage.warning('请填写退回原因')
    return
  }
  reportSaving.value = true
  try {
    await returnReport(currentReport.value.id, { comment: reportReturnComment.value.trim() })
    ElMessage.success('报告已退回')
    reportReturnVisible.value = false
    await loadReportsInline()
  } finally {
    reportSaving.value = false
  }
}

function openResourceCreate() {
  editingResource.value = null
  Object.assign(resourceForm, defaultResourceForm())
  resourceDialogVisible.value = true
}

function openResourceEdit(row) {
  editingResource.value = row
  Object.assign(resourceForm, {
    experimentId: row.experimentId || '',
    title: row.title || '',
    resourceType: row.resourceType || 'GUIDE',
    knowledgePoint: row.knowledgePoint || '',
    riskType: row.riskType || '',
    tags: row.tags || '',
    url: row.url || '',
    filePath: row.filePath || '',
    fileSize: row.fileSize || 0,
    originalFilename: row.originalFilename || '',
    contentType: row.contentType || '',
    required: row.requiredFlag === 1,
    enabled: row.status === 1,
    completionRule: row.completionRule || 'CONFIRM',
    minProgress: row.minProgress ?? 100,
    minStudySeconds: row.minStudySeconds ?? 0,
    sort: row.sort || 0,
    description: row.description || '',
  })
  resourceDialogVisible.value = true
}

async function handleResourceFilePicked(uploadFile) {
  const rawFile = uploadFile?.raw
  if (!rawFile) return
  resourceSaving.value = true
  try {
    const result = await uploadResource(rawFile)
    Object.assign(resourceForm, {
      filePath: result.filePath,
      fileSize: result.fileSize || rawFile.size,
      originalFilename: result.originalFilename || rawFile.name,
      contentType: result.contentType || rawFile.type,
    })
    ElMessage.success('文件已上传')
  } finally {
    resourceSaving.value = false
  }
}

async function saveResourceInline() {
  await resourceFormRef.value?.validate()
  if (!resourceForm.url && !resourceForm.filePath) {
    ElMessage.warning('请上传文件或登记外部链接')
    return
  }
  const payload = {
    courseId: courseId.value,
    experimentId: Number(resourceForm.experimentId),
    title: resourceForm.title.trim(),
    resourceType: resourceForm.resourceType,
    knowledgePoint: resourceForm.knowledgePoint || undefined,
    riskType: resourceForm.riskType || undefined,
    tags: resourceForm.tags || undefined,
    category: resourceForm.required ? 'REQUIRED' : 'EXTENSION',
    description: resourceForm.description || undefined,
    url: resourceForm.url || undefined,
    filePath: resourceForm.filePath || undefined,
    fileSize: Number(resourceForm.fileSize || 0),
    originalFilename: resourceForm.originalFilename || undefined,
    contentType: resourceForm.contentType || undefined,
    requiredFlag: resourceForm.required ? 1 : 0,
    status: resourceForm.enabled ? 1 : 0,
    completionRule: resourceForm.completionRule,
    minProgress: Number(resourceForm.minProgress || 0),
    minStudySeconds: Number(resourceForm.minStudySeconds || 0),
    sort: Number(resourceForm.sort || 0),
  }
  resourceSaving.value = true
  try {
    if (editingResource.value) {
      await updateResource(editingResource.value.id, payload)
      ElMessage.success('资源已更新')
    } else {
      await createResource(payload)
      ElMessage.success('资源已创建')
    }
    resourceDialogVisible.value = false
    await loadResourcesInline()
  } finally {
    resourceSaving.value = false
  }
}

async function toggleResourceStatus(row) {
  await updateResourceStatus(row.id, row.status === 1 ? 0 : 1)
  ElMessage.success('资源状态已更新')
  await loadResourcesInline()
}

async function toggleResourceInvalid(row) {
  await ElMessageBox.confirm(`确认将资源“${row.title}”标记为${row.invalidFlag ? '正常' : '失效'}吗？`, '资源状态', { type: 'warning' })
  await markResourceInvalid(row.id, row.invalidFlag ? 0 : 1)
  ElMessage.success('链接状态已更新')
  await loadResourcesInline()
}

async function removeResourceInline(row) {
  await ElMessageBox.confirm(`确认删除资源“${row.title}”吗？`, '删除资源', { type: 'warning' })
  await deleteResource(row.id)
  ElMessage.success('资源已删除')
  await loadResourcesInline()
}

async function previewResource(row) {
  const target = row.url || row.filePath
  if (!target) {
    ElMessage.warning('资源未配置预览地址')
    return
  }
  window.open(target, '_blank', 'noopener,noreferrer')
}

async function openResourceStats(row) {
  resourceStats.value = await getResourceStats(row.id)
  resourceStatsVisible.value = true
}

async function openReservationExperimentEdit(row) {
  const detailResult = await getExperimentDetail(row.id)
  const source = detailResult?.experiment || row
  reservationEditingExperiment.value = source
  Object.assign(reservationExperimentForm, {
    id: source.id,
    expName: source.expName || '',
    location: source.location || '',
    applicableClasses: source.applicableClasses || '',
    safetyPassScore: source.safetyPassScore || 60,
    reportTemplateUrl: source.reportTemplateUrl || '',
    reservationEnabled: source.reservationEnabled !== 0,
    hazardSources: source.hazardSources || '',
    riskTypes: source.riskTypes || '',
    ppeRequirements: source.ppeRequirements || '',
    prerequisiteKnowledge: source.prerequisiteKnowledge || '',
    safetyRequirement: source.safetyRequirement || '',
    dataRecordRequirement: source.dataRecordRequirement || '',
    abnormalHandling: source.abnormalHandling || '',
    emergencyProcedure: source.emergencyProcedure || '',
    gradingCriteria: source.gradingCriteria || '',
  })
  reservationApplicableClasses.value = parseApplicableClasses(source.applicableClasses)
  reservationExperimentVisible.value = true
}

async function saveReservationExperiment() {
  if (!reservationEditingExperiment.value?.id) return
  const source = reservationEditingExperiment.value
  reservationSaving.value = true
  try {
    const applicableClasses = formatApplicableClasses(reservationApplicableClasses.value)
    await updateExperiment(source.id, {
      courseId: courseId.value,
      expName: source.expName,
      expCode: source.expCode,
      direction: source.direction || course.value?.direction || undefined,
      coverUrl: source.coverUrl || undefined,
      scenarioIntro: source.scenarioIntro || undefined,
      visualTheme: source.visualTheme || undefined,
      description: source.description || undefined,
      objective: source.objective || undefined,
      principle: source.principle || undefined,
      equipment: source.equipment || undefined,
      materials: source.materials || undefined,
      location: reservationExperimentForm.location || undefined,
      applicableClasses: applicableClasses || undefined,
      riskLevel: source.riskLevel || 'MEDIUM',
      hazardSources: reservationExperimentForm.hazardSources || undefined,
      riskTypes: reservationExperimentForm.riskTypes || undefined,
      ppeRequirements: reservationExperimentForm.ppeRequirements || undefined,
      prerequisiteKnowledge: reservationExperimentForm.prerequisiteKnowledge || undefined,
      safetyRequirement: reservationExperimentForm.safetyRequirement || undefined,
      examRequired: source.examRequired ?? 1,
      durationMinutes: Number(source.durationMinutes || 60),
      safetyPassScore: Number(reservationExperimentForm.safetyPassScore || 60),
      dataRecordRequirement: reservationExperimentForm.dataRecordRequirement || undefined,
      abnormalHandling: reservationExperimentForm.abnormalHandling || undefined,
      emergencyProcedure: reservationExperimentForm.emergencyProcedure || undefined,
      reportTemplateUrl: reservationExperimentForm.reportTemplateUrl || undefined,
      gradingCriteria: reservationExperimentForm.gradingCriteria || undefined,
      reservationEnabled: reservationExperimentForm.reservationEnabled ? 1 : 0,
      status: source.status ?? 1,
      sort: Number(source.sort || 0),
    })
    ElMessage.success('预约要求已保存')
    const updated = {
      ...source,
      location: reservationExperimentForm.location || '',
      applicableClasses,
      safetyPassScore: Number(reservationExperimentForm.safetyPassScore || 60),
      reportTemplateUrl: reservationExperimentForm.reportTemplateUrl || '',
      reservationEnabled: reservationExperimentForm.reservationEnabled ? 1 : 0,
      hazardSources: reservationExperimentForm.hazardSources || '',
      riskTypes: reservationExperimentForm.riskTypes || '',
      ppeRequirements: reservationExperimentForm.ppeRequirements || '',
      prerequisiteKnowledge: reservationExperimentForm.prerequisiteKnowledge || '',
      safetyRequirement: reservationExperimentForm.safetyRequirement || '',
      dataRecordRequirement: reservationExperimentForm.dataRecordRequirement || '',
      abnormalHandling: reservationExperimentForm.abnormalHandling || '',
      emergencyProcedure: reservationExperimentForm.emergencyProcedure || '',
      gradingCriteria: reservationExperimentForm.gradingCriteria || '',
    }
    const listItem = detail.value?.experiments?.find((item) => Number(item.id) === Number(source.id))
    if (listItem) Object.assign(listItem, updated)
    reservationExperimentVisible.value = false
    await loadDetail()
  } finally {
    reservationSaving.value = false
  }
}

function openSlotCreateInline() {
  editingSlot.value = null
  Object.assign(slotForm, defaultSlotForm())
  slotDialogVisible.value = true
}

function openSlotEditInline(row) {
  editingSlot.value = row
  Object.assign(slotForm, {
    date: row.date || '',
    startTime: normalizeTime(row.startTime),
    endTime: normalizeTime(row.endTime),
    labId: row.labId || '',
    experimentId: row.experimentId || '',
    capacity: row.capacity || 20,
    status: row.status || 'AVAILABLE',
  })
  slotDialogVisible.value = true
}

function fillSlotLab() {
  slotForm.labId = slotForm.experimentId || ''
}

async function saveSlotInline() {
  if (!slotForm.date || !slotForm.startTime || !slotForm.endTime || !slotForm.labId || !slotForm.experimentId) {
    ElMessage.warning('请填写实验、日期、时间和实验室')
    return
  }
  const payload = {
    date: slotForm.date,
    startTime: slotForm.startTime,
    endTime: slotForm.endTime,
    labId: Number(slotForm.labId),
    experimentId: Number(slotForm.experimentId),
    capacity: Number(slotForm.capacity || 20),
    status: slotForm.status,
  }
  slotSaving.value = true
  try {
    if (editingSlot.value) {
      await updateTimeSlot(editingSlot.value.id, payload)
      ElMessage.success('预约时段已更新')
    } else {
      await createTimeSlots([payload])
      ElMessage.success('预约时段已创建')
    }
    slotDialogVisible.value = false
    await loadTimeSlotsInline()
  } finally {
    slotSaving.value = false
  }
}

async function removeSlotInline(row) {
  await ElMessageBox.confirm('确认删除该预约时段吗？', '删除时段', { type: 'warning' })
  await deleteTimeSlot(row.id)
  ElMessage.success('预约时段已删除')
  await loadTimeSlotsInline()
}

function openReservationReview(row, status) {
  currentReservation.value = row
  Object.assign(reservationReviewForm, {
    status,
    reviewComment: status === 'APPROVED' ? '同意预约' : '',
  })
  reservationReviewVisible.value = true
}

async function submitReservationReview() {
  if (!currentReservation.value?.id) return
  reservationSaving.value = true
  try {
    await reviewReservation(currentReservation.value.id, reservationReviewForm)
    ElMessage.success('预约已处理')
    reservationReviewVisible.value = false
    await Promise.allSettled([loadPendingReservationsInline(), loadTimeSlotsInline()])
  } finally {
    reservationSaving.value = false
  }
}

async function submitQaReply() {
  if (!qaCurrent.value?.id) return
  if (!qaReplyContent.value.trim()) {
    ElMessage.warning('请输入答疑内容')
    return
  }
  qaSaving.value = true
  try {
    await replyDiscussion(qaCurrent.value.id, { content: qaReplyContent.value.trim() })
    await selectQaTopic(qaCurrent.value)
    await loadQaTopics()
    ElMessage.success('已回复学生')
  } finally {
    qaSaving.value = false
  }
}

async function setQaStatus(status) {
  if (!qaCurrent.value?.id) return
  await updateDiscussionStatus(qaCurrent.value.id, status)
  await selectQaTopic(qaCurrent.value)
  await loadQaTopics()
  ElMessage.success('问题状态已更新')
}

function fillCourseForm() {
  const current = course.value
  if (!current) return
  Object.assign(courseForm, {
    courseName: current.courseName || '',
    courseCode: current.courseCode || '',
    direction: current.direction || '',
    teacherId: current.teacherId || null,
    coverUrl: current.coverUrl || '',
    tagline: current.tagline || '',
    highlightTags: current.highlightTags || '',
    visualTheme: current.visualTheme || '',
    description: current.description || '',
    semester: current.semester || '',
    sort: current.sort || 0,
    credit: Number(current.credit || 0),
    hours: Number(current.hours || 0),
    assessmentMethod: current.assessmentMethod || '',
    learningRequirement: current.learningRequirement || detail.value?.learningRequirement || '',
    allowEmptyPublish: current.allowEmptyPublish === 1,
  })
}

function fillExperimentForm(source) {
  Object.assign(experimentForm, {
    expName: source.expName || '',
    expCode: source.expCode || '',
    direction: source.direction || course.value?.direction || '',
    coverUrl: source.coverUrl || '',
    scenarioIntro: source.scenarioIntro || '',
    visualTheme: source.visualTheme || '',
    description: source.description || '',
    objective: source.objective || '',
    principle: source.principle || '',
    equipment: source.equipment || '',
    materials: source.materials || '',
    location: source.location || '',
    applicableClasses: source.applicableClasses || '',
    riskLevel: source.riskLevel || 'MEDIUM',
    hazardSources: source.hazardSources || '',
    riskTypes: source.riskTypes || '',
    ppeRequirements: source.ppeRequirements || '',
    prerequisiteKnowledge: source.prerequisiteKnowledge || '',
    safetyRequirement: source.safetyRequirement || '',
    examRequired: source.examRequired !== 0,
    durationMinutes: source.durationMinutes || 60,
    safetyPassScore: source.safetyPassScore || 60,
    dataRecordRequirement: source.dataRecordRequirement || '',
    abnormalHandling: source.abnormalHandling || '',
    emergencyProcedure: source.emergencyProcedure || '',
    reportTemplateUrl: source.reportTemplateUrl || '',
    gradingCriteria: source.gradingCriteria || '',
    reservationEnabled: source.reservationEnabled !== 0,
    enabled: source.status === 1,
    sort: source.sort || 0,
  })
}

function experimentPayload() {
  return {
    courseId: courseId.value,
    expName: experimentForm.expName.trim(),
    expCode: experimentForm.expCode.trim(),
    direction: experimentForm.direction || undefined,
    coverUrl: experimentForm.coverUrl || undefined,
    scenarioIntro: experimentForm.scenarioIntro || undefined,
    visualTheme: experimentForm.visualTheme || undefined,
    description: experimentForm.description || undefined,
    objective: experimentForm.objective || undefined,
    principle: experimentForm.principle || undefined,
    equipment: experimentForm.equipment || undefined,
    materials: experimentForm.materials || undefined,
    location: experimentForm.location || undefined,
    applicableClasses: experimentForm.applicableClasses || undefined,
    riskLevel: experimentForm.riskLevel,
    hazardSources: experimentForm.hazardSources || undefined,
    riskTypes: experimentForm.riskTypes || undefined,
    ppeRequirements: experimentForm.ppeRequirements || undefined,
    prerequisiteKnowledge: experimentForm.prerequisiteKnowledge || undefined,
    safetyRequirement: experimentForm.safetyRequirement || undefined,
    examRequired: experimentForm.examRequired ? 1 : 0,
    durationMinutes: Number(experimentForm.durationMinutes || 60),
    safetyPassScore: Number(experimentForm.safetyPassScore || 60),
    dataRecordRequirement: experimentForm.dataRecordRequirement || undefined,
    abnormalHandling: experimentForm.abnormalHandling || undefined,
    emergencyProcedure: experimentForm.emergencyProcedure || undefined,
    reportTemplateUrl: experimentForm.reportTemplateUrl || undefined,
    gradingCriteria: experimentForm.gradingCriteria || undefined,
    reservationEnabled: experimentForm.reservationEnabled ? 1 : 0,
    status: experimentForm.enabled ? 1 : 0,
    sort: Number(experimentForm.sort || 0),
  }
}

function defaultCourseForm() {
  return {
    courseName: '',
    courseCode: '',
    direction: '',
    teacherId: null,
    coverUrl: '',
    tagline: '',
    highlightTags: '',
    visualTheme: '',
    description: '',
    semester: '',
    sort: 0,
    credit: 0,
    hours: 0,
    assessmentMethod: '',
    learningRequirement: '',
    allowEmptyPublish: false,
  }
}

function defaultClassForm() {
  return {
    className: '',
    teacherId: course.value?.teacherId || null,
    assistantId: null,
    adminClass: '',
    semester: course.value?.semester || '',
    enabled: true,
  }
}

function defaultExperimentForm() {
  return {
    expName: '',
    expCode: '',
    direction: course.value?.direction || '',
    coverUrl: '',
    scenarioIntro: '',
    visualTheme: '',
    description: '',
    objective: '',
    principle: '',
    equipment: '',
    materials: '',
    location: '',
    applicableClasses: '',
    riskLevel: 'MEDIUM',
    hazardSources: '',
    riskTypes: '',
    ppeRequirements: '',
    prerequisiteKnowledge: '',
    safetyRequirement: '',
    examRequired: true,
    durationMinutes: 60,
    safetyPassScore: 60,
    dataRecordRequirement: '',
    abnormalHandling: '',
    emergencyProcedure: '',
    reportTemplateUrl: '',
    gradingCriteria: '',
    reservationEnabled: true,
    enabled: false,
    sort: experiments.value.length + 1,
  }
}

function defaultExamForm() {
  return {
    title: '',
    description: '',
    experimentId: '',
    totalScore: 100,
    passScore: 60,
    duration: 60,
    attemptLimit: 1,
    showAnswerAfterSubmit: 1,
    admissionValidityDays: 180,
    multipleScorePolicy: 'ALL_OR_NOTHING',
    randomEnabled: 0,
    randomCount: 0,
    status: 'DRAFT',
  }
}

function defaultResourceForm() {
  return {
    experimentId: '',
    title: '',
    resourceType: 'GUIDE',
    knowledgePoint: '',
    riskType: '',
    tags: '',
    url: '',
    filePath: '',
    fileSize: 0,
    originalFilename: '',
    contentType: '',
    required: false,
    enabled: true,
    completionRule: 'CONFIRM',
    minProgress: 100,
    minStudySeconds: 0,
    sort: 0,
    description: '',
  }
}

function defaultReservationExperimentForm() {
  return {
    id: null,
    expName: '',
    location: '',
    applicableClasses: '',
    safetyPassScore: 60,
    reportTemplateUrl: '',
    reservationEnabled: true,
    hazardSources: '',
    riskTypes: '',
    ppeRequirements: '',
    prerequisiteKnowledge: '',
    safetyRequirement: '',
    dataRecordRequirement: '',
    abnormalHandling: '',
    emergencyProcedure: '',
    gradingCriteria: '',
  }
}

function defaultSlotForm() {
  return {
    date: '',
    startTime: '',
    endTime: '',
    labId: '',
    experimentId: '',
    capacity: 20,
    status: 'AVAILABLE',
  }
}

function statusMeta(status) {
  if (status === 1) return { label: '已发布', type: 'success' }
  if (status === 2) return { label: '已归档', type: 'warning' }
  return { label: '草稿', type: 'info' }
}

function riskLabel(risk) {
  return { LOW: '低风险', MEDIUM: '中风险', HIGH: '高风险' }[risk] || risk || '未分级'
}

function experimentReadiness(item) {
  const missing = []
  if (!item.objective && !item.description && !item.scenarioIntro) missing.push('工程情境')
  if (!item.hazardSources && !item.riskTypes && !item.safetyRequirement) missing.push('风险认知')
  if (item.examRequired !== 0 && !item.safetyPassScore) missing.push('准入分数')
  if (item.reservationEnabled !== 1) missing.push('预约')
  if (!item.gradingCriteria && !item.reportTemplateUrl) missing.push('报告标准')
  return { ready: missing.length === 0, missing }
}

function experimentReadinessText(item) {
  const result = experimentReadiness(item)
  if (result.ready) return '已具备预习、风险、准入、预约、报告与反馈路径'
  return `待补齐：${result.missing.join('、')}`
}

function paperStatusMeta(status) {
  return {
    DRAFT: { label: '草稿', type: 'info' },
    PUBLISHED: { label: '已发布', type: 'success' },
    CLOSED: { label: '已关闭', type: 'warning' },
  }[status] || { label: status || '未知', type: 'info' }
}

function reportStatusMeta(status) {
  return {
    DRAFT: { label: '草稿', type: 'info' },
    SUBMITTED: { label: '待批改', type: 'warning' },
    GRADED: { label: '已批改', type: 'success' },
    RETURNED: { label: '退回修改', type: 'danger' },
  }[status] || { label: status || '未知', type: 'info' }
}

function resourceTypeLabel(type) {
  return resourceTypes.find((item) => item.value === type)?.label || type || '-'
}

function experimentName(experimentId) {
  return experiments.value.find((item) => Number(item.id) === Number(experimentId))?.expName || `实验 ${experimentId || '-'}`
}

function experimentLocation(experimentId) {
  const experiment = experiments.value.find((item) => Number(item.id) === Number(experimentId))
  return experiment?.location || ''
}

function slotLabName(row) {
  return experimentLocation(row.experimentId) || row.labName || `实验室 ${row.labId || '-'}`
}

function parseApplicableClasses(value) {
  const options = classOptions.value.map((item) => item.value)
  if (!value) return options
  const values = String(value)
    .split(/[、,，;；]/)
    .map((item) => item.trim())
    .filter(Boolean)
  if (options.length === 0) return values
  return values.filter((item) => options.includes(item))
}

function formatApplicableClasses(values) {
  return (values || [])
    .map((item) => String(item).trim())
    .filter(Boolean)
    .join('、')
}

function filterCourseReservations(records) {
  const experimentIds = new Set(experiments.value.map((item) => Number(item.id)))
  return records.filter((item) => {
    const inCourse = experimentIds.has(Number(item.experimentId))
    const byFilter = !reservationFilters.experimentId || Number(item.experimentId) === Number(reservationFilters.experimentId)
    const byDate = !reservationFilters.date || normalizeDate(item.date) === reservationFilters.date
    return inCourse && byFilter && byDate
  })
}

function filterCourseSlots(records) {
  const experimentIds = new Set(experiments.value.map((item) => Number(item.id)))
  return records.filter((item) => {
    const inCourse = experimentIds.has(Number(item.experimentId))
    const byFilter = !slotFilters.experimentId || Number(item.experimentId) === Number(slotFilters.experimentId)
    return inCourse && byFilter
  })
}

function normalizeTime(value) {
  if (!value) return ''
  const text = String(value)
  const match = text.match(/(\d{2}:\d{2})/)
  return match ? match[1] : text
}

function normalizeDate(value) {
  if (!value) return ''
  return String(value).slice(0, 10)
}

function slotStatusMeta(status) {
  return {
    AVAILABLE: { label: '可预约', type: 'success' },
    FULL: { label: '已满', type: 'warning' },
    CLOSED: { label: '关闭', type: 'info' },
  }[status] || { label: status || '未知', type: 'info' }
}

function discussionStatusMeta(status) {
  return {
    OPEN: { label: '待答疑', type: 'warning' },
    RESOLVED: { label: '已解决', type: 'success' },
    CLOSED: { label: '已关闭', type: 'info' },
  }[status] || { label: status || '未知', type: 'info' }
}

function experimentProgress(item) {
  const result = experimentReadiness(item)
  if (result.ready) return 100
  const total = result.missing.length + 1
  return Math.round((1 / total) * 100)
}
</script>

<style scoped>
.course-editor-page {
  max-width: 1280px;
  height: calc(100vh - 92px);
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.page-head {
  position: sticky;
  top: 0;
  z-index: 6;
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  gap: 16px;
  margin-bottom: 16px;
  padding-bottom: 12px;
  background: #f5f7fa;
}
.head-actions, .inline-actions, .footer-actions, .row-actions { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }
.eyebrow { color: #6b7c8f; font-size: 12px; font-weight: 700; letter-spacing: 0; text-transform: uppercase; margin-bottom: 6px; }
.page-head h1 { color: #13233a; font-size: 26px; line-height: 1.2; margin-bottom: 8px; }
.page-desc { color: #667085; }
.builder-grid {
  display: grid;
  grid-template-columns: 240px minmax(0, 1fr);
  gap: 16px;
  flex: 1 1 auto;
  min-height: 0;
}
.step-panel, .editor-panel { background: #fff; border: 1px solid #e7ebf0; border-radius: 8px; padding: 14px; }
.step-panel {
  display: grid;
  gap: 8px;
  align-content: start;
  overflow-y: auto;
}
.editor-panel {
  min-width: 0;
  overflow-y: auto;
}
.step-panel button { display: flex; align-items: center; gap: 8px; text-align: left; color: #344054; background: #f8fafc; border: 1px solid #edf1f5; border-radius: 8px; padding: 12px; cursor: pointer; }
.step-panel button.active { color: #1f6feb; border-color: #409eff; background: #eef6ff; }
.summary-band { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 10px; margin-bottom: 16px; }
.summary-band div, .requirement-block, .student-preview, .invite-panel, .readiness-band, .report-group { background: #f8fafc; border: 1px solid #edf1f5; border-radius: 8px; padding: 12px; }
.summary-band strong { display: block; color: #13233a; font-size: 24px; }
.summary-band span, .experiment-list span { color: #667085; }
.work-section { display: grid; gap: 14px; }
.section-title { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.section-title.compact { margin-bottom: 10px; }
.inline-toolbar {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
}
.inline-toolbar .el-input,
.inline-toolbar .el-select {
  max-width: 240px;
}
.compact-toolbar {
  margin-bottom: 0;
  justify-content: flex-end;
  flex-wrap: wrap;
}
.filter-toolbar {
  display: flex;
  flex-direction: row;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
  flex-wrap: nowrap;
}
.filter-toolbar .el-date-editor,
.filter-toolbar .el-select {
  width: 180px;
  max-width: 180px;
}
.work-section h2 { color: #13233a; font-size: 18px; }
.student-preview h3 { color: #13233a; font-size: 15px; }
.path-preview { background: #f8fafc; border: 1px solid #edf1f5; border-radius: 8px; padding: 12px; }
.path-preview h3 { color: #13233a; font-size: 15px; }
.readiness-band { display: flex; align-items: center; justify-content: space-between; gap: 16px; border-color: #b7dfc5; background: #f0fbf4; }
.readiness-band strong { display: block; color: #2d6a4f; font-size: 24px; }
.readiness-band span, .readiness-band p { color: #536579; line-height: 1.6; margin: 0; }
.preview-steps { display: grid; grid-template-columns: repeat(6, minmax(0, 1fr)); gap: 8px; }
.preview-steps span { min-height: 58px; display: flex; align-items: center; justify-content: center; text-align: center; color: #177e89; background: #eefafa; border: 1px solid #bfe4e8; border-radius: 8px; font-size: 13px; }
.work-section p { color: #667085; line-height: 1.7; margin: 0; white-space: pre-wrap; }
.form-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); column-gap: 12px; }
.experiment-list { display: grid; gap: 10px; }
.experiment-list article { display: flex; align-items: center; justify-content: space-between; gap: 10px; border: 1px solid #edf1f5; border-radius: 8px; padding: 12px; }
.experiment-list strong { display: block; color: #13233a; margin-bottom: 4px; }
.experiment-list small { display: block; color: #7b8794; line-height: 1.5; margin-top: 4px; }
.action-grid { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 10px; }
.action-grid button { display: flex; align-items: center; gap: 8px; color: #344054; background: #fff; border: 1px solid #e7ebf0; border-radius: 8px; padding: 14px; cursor: pointer; }
.requirement-block b { display: block; color: #13233a; margin-bottom: 6px; }
.analytics-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
}
.analytics-grid div {
  background: #f8fafc;
  border: 1px solid #edf1f5;
  border-radius: 8px;
  padding: 14px;
}
.analytics-grid strong {
  display: block;
  color: #13233a;
  font-size: 24px;
  margin-bottom: 4px;
}
.analytics-grid span {
  color: #667085;
  font-size: 13px;
}
.learning-bars,
.step-edit-list {
  display: grid;
  gap: 12px;
}
.reservation-block {
  border: 1px solid #e7ebf0;
  border-radius: 8px;
  background: #fff;
  padding: 14px;
}
.reservation-block + .reservation-block {
  margin-top: 2px;
}
.learning-bars article,
.step-edit-card,
.reservation-config-grid article {
  border: 1px solid #e7ebf0;
  border-radius: 8px;
  padding: 12px;
  background: #fff;
}
.reservation-config-grid {
  display: grid;
  gap: 10px;
}
.reservation-config-grid article {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 14px;
}
.reservation-config-grid strong {
  display: block;
  color: #13233a;
  margin-bottom: 4px;
}
.notice-block {
  border-top: 1px solid #edf1f5;
  margin-top: 8px;
  padding-top: 12px;
}
.notice-block h3 {
  color: #13233a;
  font-size: 15px;
  margin: 0 0 12px;
}
.learning-bars article > div {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 8px;
}
.learning-bars strong {
  color: #13233a;
}
.learning-bars span {
  color: #667085;
  font-size: 13px;
}
.title-cell strong {
  display: block;
  color: #13233a;
  line-height: 1.4;
}
.title-cell span {
  color: #7b8794;
  font-size: 12px;
}
.metric {
  display: inline-block;
  color: #667085;
  font-size: 12px;
  margin-right: 8px;
}
.upload-row {
  display: flex;
  align-items: center;
  gap: 12px;
  color: #667085;
}
.detail-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: flex-start;
  margin-bottom: 10px;
}
.detail-head h2 {
  color: #13233a;
  font-size: 20px;
}
.detail-meta {
  color: #667085;
  margin-bottom: 14px;
}
.report-content {
  white-space: pre-wrap;
  color: #344054;
  line-height: 1.8;
  background: #f8fafc;
  border: 1px solid #edf1f5;
  border-radius: 8px;
  padding: 14px;
}
.detail-body {
  min-height: 180px;
}
.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
}
.stats-grid div {
  background: #f8fafc;
  border: 1px solid #edf1f5;
  border-radius: 8px;
  padding: 14px;
  text-align: center;
}
.stats-grid strong {
  display: block;
  color: #13233a;
  font-size: 22px;
  margin-bottom: 4px;
}
.stats-grid span {
  color: #667085;
  font-size: 12px;
}
.qa-layout {
  display: grid;
  grid-template-columns: 340px minmax(0, 1fr);
  gap: 14px;
  min-height: 560px;
}
.qa-list,
.qa-detail {
  border: 1px solid #e7ebf0;
  border-radius: 8px;
  background: #fff;
  padding: 12px;
  min-height: 0;
  overflow-y: auto;
}
.qa-list {
  display: grid;
  align-content: start;
  gap: 10px;
}
.qa-topic {
  border: 1px solid #edf1f5;
  border-radius: 8px;
  padding: 12px;
  cursor: pointer;
}
.qa-topic.active {
  border-color: #409eff;
  background: #eef6ff;
}
.qa-topic div,
.qa-reply div {
  display: flex;
  align-items: center;
  gap: 8px;
}
.qa-topic div {
  justify-content: space-between;
}
.qa-topic strong,
.qa-reply strong {
  color: #13233a;
}
.qa-topic p,
.qa-content,
.qa-reply p {
  color: #344054;
  line-height: 1.7;
  white-space: pre-wrap;
}
.qa-topic p {
  margin: 8px 0;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.qa-topic span,
.qa-reply span {
  color: #98a2b3;
  font-size: 12px;
}
.qa-content {
  background: #f8fafc;
  border: 1px solid #edf1f5;
  border-radius: 8px;
  padding: 12px;
  margin-bottom: 12px;
}
.qa-replies {
  display: grid;
  gap: 10px;
  margin-bottom: 12px;
}
.qa-reply {
  border: 1px solid #edf1f5;
  border-radius: 8px;
  padding: 12px;
}
.qa-reply.teacher {
  background: #f0f9f2;
  border-color: #bfe8c7;
}
.reply-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 10px;
}
.step-editor {
  display: grid;
  gap: 14px;
}
.step-meta-grid {
  display: grid;
  grid-template-columns: 110px minmax(180px, 1fr) 120px 110px auto;
  gap: 10px;
  margin-bottom: 10px;
}
.step-columns,
.step-media-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
  margin-top: 10px;
}
@media (max-width: 900px) {
  .course-editor-page { height: auto; overflow: visible; }
  .builder-grid, .summary-band, .form-grid, .action-grid, .preview-steps, .analytics-grid, .step-meta-grid, .step-columns, .step-media-grid, .stats-grid, .qa-layout { grid-template-columns: 1fr; }
  .page-head, .section-title, .inline-toolbar, .experiment-list article, .readiness-band, .reservation-config-grid article { align-items: stretch; flex-direction: column; }
  .inline-toolbar .el-input, .inline-toolbar .el-select { max-width: none; }
  .filter-toolbar { flex-wrap: wrap; justify-content: flex-start; }
  .filter-toolbar .el-date-editor, .filter-toolbar .el-select { width: 100%; max-width: none; }
  .step-panel, .editor-panel { overflow: visible; }
}
</style>
