<template>
  <div class="classroom-page">
    <aside class="classroom-sidebar">
      <div class="classroom-cover" :style="{ backgroundImage: `url(${courseCover})` }">
        <span>课堂门户</span>
      </div>
      <h2>{{ courseTitle }}</h2>
      <div class="sidebar-progress">
        <strong>{{ completion.done }}/{{ completion.total }}</strong>
        <span>已完成任务点</span>
      </div>
      <button
        v-for="item in menuItems"
        :key="item.name"
        :class="{ active: activeModule === item.name }"
        type="button"
        @click="switchModule(item.name)"
      >
        <el-icon><component :is="item.icon" /></el-icon>
        <span>{{ item.label }}</span>
      </button>
    </aside>

    <main class="classroom-content">
      <section class="module-head">
        <div>
          <el-button text :icon="Back" @click="router.push('/classrooms')">返回课堂</el-button>
          <h1>{{ activeMenu.label }}</h1>
          <p>{{ activeMenu.desc }}</p>
        </div>
        <el-button :icon="Refresh" @click="reloadModule">刷新</el-button>
      </section>

      <section v-if="activeModule === 'ai'" class="module-panel ai-panel">
        <div class="panel-title">
          <h2>AI 辅助学习</h2>
          <span>默认结合当前课堂、实验章节和油气工程实验安全规范回答。</span>
        </div>
        <el-select v-model="selectedExperimentId" placeholder="选择实验上下文" clearable>
          <el-option v-for="item in experiments" :key="item.id" :label="item.expName" :value="item.id" />
        </el-select>
        <el-input
          v-model="aiQuestion"
          type="textarea"
          :rows="5"
          placeholder="例如：钻井液密度测定实验为什么要先校准仪器？"
        />
        <div class="actions">
          <el-button type="primary" :icon="MagicStick" :loading="aiLoading" @click="askClassroomAi">提问</el-button>
        </div>
        <div v-if="aiAnswer" class="answer-box">{{ aiAnswer }}</div>
      </section>

      <section v-else-if="activeModule === 'tasks'" class="module-panel">
        <div class="panel-title">
          <h2>待完成任务</h2>
          <span>按实验章节聚合资源预习、安全考核、报告和预约任务。</span>
        </div>
        <el-empty v-if="pendingTasks.length === 0" description="暂无待完成任务" />
        <div v-else class="task-list">
          <article v-for="item in pendingTasks" :key="item.key" class="task-card">
            <div>
              <h3>{{ item.title }}</h3>
              <p>{{ item.experimentName }} · {{ item.typeLabel }}</p>
              <small>{{ item.reason }}</small>
            </div>
            <el-tag :type="item.required ? 'warning' : 'info'">{{ item.required ? '必做' : '选学' }}</el-tag>
            <el-button type="primary" @click="openTask(item)">{{ item.actionLabel }}</el-button>
          </article>
        </div>
      </section>

      <section v-else-if="activeModule === 'chapters'" class="module-panel chapters-panel">
        <div class="chapter-toolbar">
          <el-input v-model="chapterKeyword" clearable :prefix-icon="Search" placeholder="搜索实验章节" />
        </div>
        <p class="catalog-label">实验章节目录</p>
        <el-empty v-if="filteredExperiments.length === 0" description="暂无实验章节" />
        <div v-else class="chapter-list">
          <section v-for="(experiment, index) in filteredExperiments" :key="experiment.id" class="chapter-group">
            <button type="button" class="chapter-group-head" @click="toggleChapter(experiment.id)">
              <span class="chapter-no">{{ index + 1 }}</span>
              <strong>{{ experiment.expName }}</strong>
              <el-icon><ArrowUp v-if="expandedChapters.includes(experiment.id)" /><ArrowDown v-else /></el-icon>
            </button>
            <div v-show="expandedChapters.includes(experiment.id)" class="chapter-nodes">
              <button
                v-for="node in chapterNodes(experiment)"
                :key="node.key"
                type="button"
                @click="openChapter(experiment.id)"
              >
                <span class="node-state" :class="{ done: node.done }">
                  <el-icon><Check /></el-icon>
                </span>
                <span>{{ node.title }}</span>
              </button>
            </div>
          </section>
        </div>
      </section>

      <section v-else-if="activeModule === 'discussion'" class="module-panel discussion-panel">
        <div class="panel-title">
          <h2>课堂讨论与教师答疑</h2>
          <el-button type="primary" :icon="Plus" @click="openDiscussionCreate">发布问题</el-button>
        </div>
        <div class="discussion-grid">
          <div class="topic-list" v-loading="discussionLoading">
            <el-empty v-if="!discussionLoading && discussions.length === 0" description="本课堂暂无讨论" />
            <article
              v-for="topic in discussions"
              :key="topic.id"
              :class="{ active: topic.id === currentDiscussion?.id }"
              @click="selectDiscussion(topic.id)"
            >
              <h3>{{ topic.title }}</h3>
              <p>{{ topic.content }}</p>
              <span>{{ topic.userName || '用户' }} · {{ topic.replyCount || 0 }} 回复 · {{ discussionStatus(topic.status) }}</span>
            </article>
          </div>
          <div class="topic-detail">
            <template v-if="currentDiscussion">
              <h2>{{ currentDiscussion.title }}</h2>
              <p class="topic-content">{{ currentDiscussion.content }}</p>
              <div class="reply-list">
                <article v-for="reply in currentDiscussion.replies || []" :key="reply.id" :class="{ teacher: reply.isTeacherReply }">
                  <strong>{{ reply.userName }}</strong>
                  <el-tag v-if="reply.isTeacherReply" size="small" type="success">教师答疑</el-tag>
                  <p>{{ reply.content }}</p>
                </article>
              </div>
              <el-input v-model="replyContent" type="textarea" :rows="4" placeholder="回复本课堂问题" />
              <div class="actions">
                <el-button type="primary" :loading="discussionSaving" @click="submitReply">回复</el-button>
              </div>
            </template>
            <el-empty v-else description="请选择一个课堂讨论" />
          </div>
        </div>
      </section>

      <section v-else-if="activeModule === 'report'" class="module-panel">
        <div class="panel-title">
          <h2>报告提交</h2>
          <span>支持正文提交，也可以填写报告文件链接。</span>
        </div>
        <el-select v-model="selectedExperimentId" placeholder="选择实验" @change="handleReportExperimentChange">
          <el-option v-for="item in experiments" :key="item.id" :label="item.expName" :value="item.id" />
        </el-select>
        <div class="report-guide">
          <p><b>实验名称：</b>{{ activeExperimentDetail?.experiment?.expName || '请选择实验' }}</p>
          <p><b>报告要求：</b>{{ reportRequirementText }}</p>
          <p v-if="activeExperimentDetail?.experiment?.reportTemplateUrl">
            <b>报告模板：</b><a :href="activeExperimentDetail.experiment.reportTemplateUrl" target="_blank" rel="noreferrer">查看/下载模板</a>
          </p>
          <p><b>评分标准：</b>{{ activeExperimentDetail?.experiment?.gradingCriteria || '教师暂未配置评分标准。' }}</p>
          <p><b>教师反馈：</b>{{ currentReport?.feedback || currentReport?.reviewComment || '暂无反馈。' }}</p>
        </div>
        <el-form :model="reportForm" label-width="88px">
          <el-form-item label="报告标题"><el-input v-model="reportForm.title" /></el-form-item>
          <el-form-item label="文件链接"><el-input v-model="reportForm.fileUrl" placeholder="可选，填写报告文件URL" /></el-form-item>
          <el-form-item label="报告正文">
            <el-input v-model="reportForm.content" type="textarea" :rows="10" placeholder="填写实验过程、数据记录、结果分析和安全反思" />
          </el-form-item>
        </el-form>
        <div class="actions">
          <el-button :loading="reportSaving" @click="saveReportDraft">保存草稿</el-button>
          <el-button type="primary" :loading="reportSubmitting" @click="submitCurrentReport">提交报告</el-button>
        </div>
      </section>

      <section v-else-if="activeModule === 'exam'" class="module-panel">
        <el-tabs v-model="examTab" @tab-change="loadExamTab">
          <el-tab-pane label="可参加考核" name="available">
            <div class="toolbar">
              <el-input :model-value="courseId" disabled placeholder="当前课堂ID" />
              <el-button type="primary" :icon="Search" @click="loadAvailableExams">查询</el-button>
            </div>
            <el-table v-loading="examLoading" :data="availableExams" stripe>
              <el-table-column prop="title" label="试卷名称" min-width="180" />
              <el-table-column prop="description" label="说明" min-width="220" />
              <el-table-column prop="totalScore" label="总分" width="90" />
              <el-table-column prop="passScore" label="及格分" width="90" />
              <el-table-column prop="duration" label="时长(分钟)" width="110" />
              <el-table-column prop="remainingAttempts" label="剩余次数" width="100" />
              <el-table-column label="操作" width="130" fixed="right">
                <template #default="{ row }">
                  <el-button type="primary" :icon="VideoPlay" @click="startPaper(row)">开始考核</el-button>
                </template>
              </el-table-column>
            </el-table>
          </el-tab-pane>
          <el-tab-pane label="考核记录" name="records">
            <el-table v-loading="examLoading" :data="examRecords" stripe>
              <el-table-column prop="paperId" label="试卷ID" width="90" />
              <el-table-column prop="totalScore" label="得分" width="90" />
              <el-table-column label="状态" width="120">
                <template #default="{ row }"><el-tag :type="examStatus(row.status).type">{{ examStatus(row.status).label }}</el-tag></template>
              </el-table-column>
              <el-table-column label="是否通过" width="100">
                <template #default="{ row }"><el-tag :type="row.passed ? 'success' : 'danger'">{{ row.passed ? '通过' : '未通过' }}</el-tag></template>
              </el-table-column>
              <el-table-column prop="submitTime" label="提交时间" min-width="160" />
            </el-table>
          </el-tab-pane>
          <el-tab-pane label="错题解析" name="wrong">
            <el-table v-loading="examLoading" :data="wrongQuestions" stripe>
              <el-table-column label="题目" min-width="220">
                <template #default="{ row }">{{ row.question?.content || '-' }}</template>
              </el-table-column>
              <el-table-column prop="wrongAnswer" label="我的答案" min-width="120" />
              <el-table-column prop="correctAnswer" label="正确答案" min-width="120" />
              <el-table-column label="解析" min-width="220">
                <template #default="{ row }">{{ row.analysis || row.question?.analysis || '暂无解析' }}</template>
              </el-table-column>
            </el-table>
          </el-tab-pane>
        </el-tabs>
      </section>

      <section v-else-if="activeModule === 'resources'" class="module-panel">
        <div class="panel-title">
          <h2>课堂资料</h2>
          <span>仅显示本课堂资料；公共资源请到“资源学习”。</span>
        </div>
        <el-alert v-if="canManageCourse" class="inline-alert" type="info" :closable="false" title="你是本课堂管理者，可上传课堂资料。" />
        <div v-if="canManageCourse" class="resource-create">
          <el-select v-model="resourceForm.experimentId" placeholder="绑定实验">
            <el-option v-for="item in experiments" :key="item.id" :label="item.expName" :value="item.id" />
          </el-select>
          <el-input v-model="resourceForm.title" placeholder="资料标题" />
          <el-select v-model="resourceForm.resourceType" placeholder="类型">
            <el-option label="文档" value="DOCUMENT" />
            <el-option label="视频" value="VIDEO" />
            <el-option label="图片" value="IMAGE" />
            <el-option label="外链" value="LINK" />
          </el-select>
          <el-input v-model="resourceForm.url" placeholder="外链地址，可选" />
          <el-upload :auto-upload="false" :limit="1" :on-change="onResourceFile" :on-remove="clearResourceFile">
            <el-button>选择文件</el-button>
          </el-upload>
          <el-button type="primary" :loading="resourceSaving" @click="createClassroomResource">上传课堂资料</el-button>
        </div>
        <el-empty v-if="resources.length === 0" description="本课堂暂无资料" />
        <div v-else class="resource-list">
          <article v-for="item in resources" :key="item.id" class="resource-card">
            <div>
              <h3>{{ item.title }}</h3>
              <p>{{ item.description || item.knowledgePoint || '课堂学习资料' }}</p>
              <span>{{ item.resourceType || 'RESOURCE' }} · {{ item.requiredFlag ? '必学' : '拓展' }}</span>
            </div>
            <el-button type="primary" :icon="View" @click="previewResource(item.id)">查看</el-button>
          </article>
        </div>
      </section>

      <section v-else-if="activeModule === 'reservation'" class="module-panel">
        <div class="panel-title">
          <h2>实验预约</h2>
          <span>先选择实验，通过安全知识考核后才能查看可预约时间段。</span>
        </div>
        <div class="reservation-grid">
          <div class="experiment-list">
            <button
              v-for="item in reservableExperiments"
              :key="item.id"
              :class="{ active: Number(item.id) === Number(reservationExperimentId) }"
              type="button"
              @click="chooseReservationExperiment(item)"
            >
              <strong>{{ item.expName }}</strong>
              <span>{{ item.durationMinutes || 0 }} 分钟 · {{ item.reservationEnabled ? '可预约' : '未启用' }}</span>
            </button>
          </div>
          <div class="slot-list">
            <div class="toolbar">
              <el-date-picker v-model="reservationDate" value-format="YYYY-MM-DD" placeholder="预约日期" @change="loadSlots" />
              <el-button :icon="Search" @click="loadSlots">查询时段</el-button>
            </div>
            <el-alert
              v-if="reservationAdmission && !reservationAdmission.qualified"
              type="warning"
              :closable="false"
              title="请完成相关安全知识考核"
            />
            <el-empty v-if="!reservationAdmission?.qualified" description="请选择已完成安全考核的实验" />
            <el-table v-else v-loading="slotLoading" :data="availableSlots" stripe>
              <el-table-column label="日期" min-width="110">
                <template #default="{ row }">{{ slotOf(row).date || '-' }}</template>
              </el-table-column>
              <el-table-column label="时段" min-width="140">
                <template #default="{ row }">{{ slotOf(row).startTime || '-' }} - {{ slotOf(row).endTime || '-' }}</template>
              </el-table-column>
              <el-table-column label="余量" width="90">
                <template #default="{ row }">{{ row.remaining ?? '-' }}</template>
              </el-table-column>
              <el-table-column label="操作" width="110">
                <template #default="{ row }">
                  <el-button type="primary" @click="reserveSlot(row)">预约</el-button>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </div>
      </section>

      <section v-else class="module-panel">
        <div class="panel-title">
          <h2>学习记录</h2>
          <span>汇总资源进度、任务完成、考核、报告和预约状态。</span>
        </div>
        <div class="record-summary">
          <div><strong>{{ completion.percent }}%</strong><span>任务完成率</span></div>
          <div><strong>{{ records.length }}</strong><span>学习记录</span></div>
          <div><strong>{{ examRecords.length }}</strong><span>考核记录</span></div>
          <div><strong>{{ reports.length }}</strong><span>报告记录</span></div>
        </div>
        <div class="record-timeline">
          <article v-for="experiment in experiments" :key="experiment.id">
            <div>
              <h3>{{ experiment.expName }}</h3>
              <p>{{ experiment.description || '实验章节学习记录' }}</p>
            </div>
            <el-progress :percentage="experimentProgress(experiment)" />
          </article>
        </div>
      </section>
    </main>

    <ResourceViewer v-model="viewerVisible" :resource-id="activeResourceId" @completed="reloadModule" />

    <el-dialog v-model="discussionCreateVisible" title="发布课堂问题" width="620px">
      <el-form label-width="72px">
        <el-form-item label="实验">
          <el-select v-model="discussionForm.experimentId" clearable placeholder="可选，关联实验章节">
            <el-option v-for="item in experiments" :key="item.id" :label="item.expName" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="标题"><el-input v-model="discussionForm.title" /></el-form-item>
        <el-form-item label="内容"><el-input v-model="discussionForm.content" type="textarea" :rows="5" /></el-form-item>
        <el-form-item label="匿名"><el-switch v-model="discussionForm.isAnonymous" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="discussionCreateVisible = false">取消</el-button>
        <el-button type="primary" :loading="discussionSaving" @click="submitDiscussion">发布</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="examVisible" :title="examSession.paperTitle || '安全知识考核'" width="860px" top="4vh">
      <div class="exam-meta">
        <el-tag type="primary">剩余 {{ countdownText }}</el-tag>
        <el-tag v-if="lastSaveTime">已保存 {{ lastSaveTime }}</el-tag>
      </div>
      <div class="question-list">
        <article v-for="(question, index) in examSession.questions" :key="question.id" class="question-box">
          <div class="question-title">
            <strong>{{ index + 1 }}. {{ question.content }}</strong>
            <el-tag>{{ questionTypeLabel(question.type) }} / {{ question.score || 0 }} 分</el-tag>
          </div>
          <el-radio-group v-if="['SINGLE', 'JUDGE'].includes(question.type)" v-model="answers[question.id]">
            <el-radio v-for="option in normalizedOptions(question)" :key="option.value" :label="option.value">{{ option.label }}</el-radio>
          </el-radio-group>
          <el-checkbox-group v-else-if="question.type === 'MULTIPLE'" v-model="answers[question.id]">
            <el-checkbox v-for="option in normalizedOptions(question)" :key="option.value" :label="option.value">{{ option.label }}</el-checkbox>
          </el-checkbox-group>
          <el-input v-else v-model="answers[question.id]" type="textarea" :rows="3" />
        </article>
      </div>
      <template #footer>
        <el-button @click="saveCurrentAnswers">保存</el-button>
        <el-button @click="examVisible = false">稍后提交</el-button>
        <el-button type="primary" :loading="examSubmitting" @click="submitCurrentExam">提交试卷</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  ArrowDown,
  ArrowUp,
  Back,
  Calendar,
  ChatLineRound,
  Check,
  Collection,
  DataBoard,
  Document,
  EditPen,
  Folder,
  MagicStick,
  Plus,
  Reading,
  Refresh,
  Search,
  View,
  VideoPlay,
} from '@element-plus/icons-vue'
import ResourceViewer from '@/components/learning/ResourceViewer.vue'
import { useAuthStore } from '@/stores/authStore'
import { askAi } from '@/api/ai'
import { getCourseDetail } from '@/api/course'
import { getExperimentDetail } from '@/api/experiment'
import { createDiscussion, getDiscussionDetail, getDiscussions, replyDiscussion } from '@/api/discussion'
import { getAvailableExams, getExamRecords, getWrongQuestions, saveExamAnswers, startExam, submitExam } from '@/api/exam'
import { getLearningPath } from '@/api/learningTask'
import { getMyLearningRecords } from '@/api/learningRecord'
import { createReport, getMyReports, submitReport, updateReport } from '@/api/report'
import { createReservation, getAvailableSlots, getMyReservations } from '@/api/reservation'
import { createResource, getResources, uploadResource } from '@/api/resource'
import { getAdmissionStatus } from '@/api/exam'
import procedureSafety from '@/assets/amazing/procedure-safety.png'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const courseId = computed(() => Number(route.params.courseId))
const activeModule = ref(String(route.query.module || 'chapters'))
const courseDetail = ref(null)
const experimentDetails = reactive({})
const taskPaths = reactive({})
const reports = ref([])
const records = ref([])
const reservations = ref([])
const resources = ref([])
const selectedExperimentId = ref(null)
const activeResourceId = ref(null)
const viewerVisible = ref(false)
const chapterKeyword = ref('')
const expandedChapters = ref([])
const aiQuestion = ref('')
const aiAnswer = ref('')
const aiLoading = ref(false)
const discussionLoading = ref(false)
const discussionSaving = ref(false)
const discussions = ref([])
const currentDiscussion = ref(null)
const replyContent = ref('')
const discussionCreateVisible = ref(false)
const discussionForm = reactive({ experimentId: null, title: '', content: '', isAnonymous: false })
const reportSaving = ref(false)
const reportSubmitting = ref(false)
const reportForm = reactive({ id: null, title: '', content: '', fileUrl: '', status: '' })
const examTab = ref('available')
const examLoading = ref(false)
const availableExams = ref([])
const examRecords = ref([])
const wrongQuestions = ref([])
const examVisible = ref(false)
const examSubmitting = ref(false)
const examSession = reactive({ recordId: null, paperId: null, paperTitle: '', questions: [], endTime: '' })
const answers = reactive({})
const remainingSeconds = ref(0)
const lastSaveTime = ref('')
let countdownTimer = null
const resourceSaving = ref(false)
const resourceFile = ref(null)
const resourceForm = reactive({ experimentId: null, title: '', resourceType: 'DOCUMENT', url: '' })
const reservationExperimentId = ref(null)
const reservationAdmission = ref(null)
const reservationDate = ref('')
const availableSlots = ref([])
const slotLoading = ref(false)

const menuItems = [
  { name: 'ai', label: 'AI助教', desc: '面向当前课堂的实验原理、风险和报告要求解释。', icon: MagicStick },
  { name: 'tasks', label: '任务', desc: '查看待完成任务并进入对应学习环节。', icon: DataBoard },
  { name: 'chapters', label: '章节', desc: '只展示本课堂实验章节目录。', icon: Reading },
  { name: 'discussion', label: '讨论', desc: '本课堂内的问题讨论和教师答疑。', icon: ChatLineRound },
  { name: 'report', label: '报告提交', desc: '提交实验报告正文或文件链接。', icon: Document },
  { name: 'exam', label: '安全知识考核', desc: '参加本课堂安全准入考核并查看错题解析。', icon: EditPen },
  { name: 'resources', label: '资料', desc: '本课堂学习资料，区别于公共资源学习区。', icon: Folder },
  { name: 'reservation', label: '实验预约', desc: '通过安全考核后预约实验时段。', icon: Calendar },
  { name: 'records', label: '学习记录', desc: '可视化查看学习进度和任务完成情况。', icon: Collection },
]

const activeMenu = computed(() => menuItems.find((item) => item.name === activeModule.value) || menuItems[2])
const experiments = computed(() => courseDetail.value?.experiments || [])
const courseTitle = computed(() => courseDetail.value?.course?.courseName || '课堂学习')
const courseCover = computed(() => courseDetail.value?.course?.coverUrl || procedureSafety)
const canManageCourse = computed(() => authStore.hasPermission('course:update') || authStore.hasPermission('resource:update'))
const activeExperimentDetail = computed(() => experimentDetails[selectedExperimentId.value] || null)
const currentReport = computed(() => reports.value.find((item) => Number(item.experimentId) === Number(selectedExperimentId.value)))
const reportRequirementText = computed(() => {
  const experiment = activeExperimentDetail.value?.experiment
  return experiment?.dataRecordRequirement
    || experiment?.reportRequirement
    || experiment?.description
    || '按教师要求提交实验过程、数据记录、结果分析和安全反思。'
})
const reservableExperiments = computed(() => experiments.value.filter((item) => item.status === 1 || item.reservationEnabled))
const filteredExperiments = computed(() => {
  const keyword = chapterKeyword.value.trim().toLowerCase()
  if (!keyword) return experiments.value
  return experiments.value.filter((item) => [item.expName, item.expCode, item.description].filter(Boolean).join(' ').toLowerCase().includes(keyword))
})
const completion = computed(() => {
  let total = 0
  let done = 0
  experiments.value.forEach((experiment) => {
    const path = taskPaths[experiment.id]
    const tasks = path?.tasks || []
    if (tasks.length) {
      total += tasks.length
      done += tasks.filter((item) => item.completed || item.state === 'COMPLETED').length
    } else {
      total += 1
      if (Number(experiment.learningProgress || 0) >= 100) done += 1
    }
  })
  return { total, done, percent: total ? Math.round((done / total) * 100) : 0 }
})
const pendingTasks = computed(() => {
  const result = []
  experiments.value.forEach((experiment) => {
    const path = taskPaths[experiment.id]
    ;(path?.tasks || []).forEach((item) => {
      if (item.completed || item.state === 'COMPLETED') return
      result.push({
        key: `${experiment.id}-${item.task.id}`,
        experimentId: experiment.id,
        experimentName: experiment.expName,
        title: item.task.taskName,
        type: item.task.taskType,
        typeLabel: taskTypeLabel(item.task.taskType),
        targetModule: taskTargetModule(item.task),
        actionLabel: taskActionLabel(item.task),
        required: item.task.requiredFlag,
        reason: item.lockedReason || item.task.description || '按课堂要求完成该任务。',
      })
    })
  })
  return result
})
const countdownText = computed(() => {
  const min = Math.floor(remainingSeconds.value / 60)
  const sec = remainingSeconds.value % 60
  return `${String(min).padStart(2, '0')}:${String(sec).padStart(2, '0')}`
})

watch(() => route.query.module, (value) => {
  if (value && value !== activeModule.value) activeModule.value = String(value)
})

watch(activeModule, () => reloadModule())
watch(selectedExperimentId, async (value) => {
  if (value && !experimentDetails[value]) {
    experimentDetails[value] = await getExperimentDetail(value)
  }
})

onMounted(loadClassroom)
onUnmounted(stopExamTimer)

async function loadClassroom() {
  courseDetail.value = await getCourseDetail(courseId.value)
  if (experiments.value.length) {
    selectedExperimentId.value = selectedExperimentId.value || experiments.value[0].id
    reservationExperimentId.value = reservationExperimentId.value || experiments.value[0].id
    expandedChapters.value = experiments.value.map((item) => item.id)
  }
  await Promise.allSettled([
    loadExperimentDetails(),
    loadTaskPaths(),
    loadReports(),
    loadRecords(),
    loadResources(),
    loadDiscussions(),
    loadExamTab(),
    loadReservations(),
  ])
}

async function loadExperimentDetails() {
  await Promise.all(experiments.value.map(async (experiment) => {
    if (!experimentDetails[experiment.id]) {
      experimentDetails[experiment.id] = await getExperimentDetail(experiment.id).catch(() => null)
    }
  }))
}

async function ensureExperimentDetail(experimentId) {
  if (experimentId && !experimentDetails[experimentId]) {
    experimentDetails[experimentId] = await getExperimentDetail(experimentId).catch(() => null)
  }
}

async function loadTaskPaths() {
  await Promise.all(experiments.value.map(async (experiment) => {
    taskPaths[experiment.id] = await getLearningPath(experiment.id).catch(() => ({ tasks: [] }))
  }))
}

async function loadReports() {
  const result = await getMyReports({ pageNum: 1, pageSize: 100 }, { silent: true }).catch(() => ({ records: [] }))
  reports.value = result?.records || []
  loadReportForm()
}

async function loadRecords() {
  const result = await getMyLearningRecords({ silent: true }).catch(() => [])
  records.value = Array.isArray(result) ? result : (result?.records || [])
}

async function loadResources() {
  const result = await getResources({ pageNum: 1, pageSize: 100, courseId: courseId.value, status: 1 }, { silent: true }).catch(() => ({ records: [] }))
  resources.value = result?.records || []
}

async function loadReservations() {
  const result = await getMyReservations({ pageNum: 1, pageSize: 100 }, { silent: true }).catch(() => ({ records: [] }))
  reservations.value = result?.records || []
}

async function reloadModule() {
  if (activeModule.value === 'discussion') await loadDiscussions()
  if (activeModule.value === 'exam') await loadExamTab()
  if (activeModule.value === 'resources') await loadResources()
  if (activeModule.value === 'records') await Promise.allSettled([loadRecords(), loadTaskPaths(), loadReports(), loadReservations(), loadExamRecords()])
  if (activeModule.value === 'reservation') await loadReservations()
  if (activeModule.value === 'report') await loadReports()
}

function switchModule(name) {
  router.replace({ path: route.path, query: { ...route.query, module: name } })
  activeModule.value = name
}

function toggleChapter(id) {
  expandedChapters.value = expandedChapters.value.includes(id)
    ? expandedChapters.value.filter((item) => item !== id)
    : [...expandedChapters.value, id]
}

function chapterNodes(experiment) {
  const detail = experimentDetails[experiment.id]
  const steps = detail?.steps || []
  if (!steps.length) {
    return [{ key: `exp-${experiment.id}`, title: '暂无实验步骤，点击进入章节详情', done: experimentProgress(experiment) >= 100 }]
  }
  return steps
    .slice()
    .sort((a, b) => Number(a.stepNo || 0) - Number(b.stepNo || 0))
    .map((step) => ({
      key: `step-${step.id || step.stepNo}`,
      title: `${step.stepNo || ''} ${step.title || '实验步骤'}`.trim(),
      done: experimentProgress(experiment) >= 100,
    }))
}

async function openChapter(experimentId) {
  await router.push(`/classrooms/${courseId.value}/chapters/${experimentId}`)
}

async function openTask(item) {
  selectedExperimentId.value = item.experimentId
  await ensureExperimentDetail(item.experimentId)
  if (item.targetModule === 'report') {
    loadReportForm()
    switchModule('report')
    return
  }
  if (item.targetModule === 'exam') {
    switchModule('exam')
    return
  }
  if (item.targetModule === 'reservation') {
    reservationExperimentId.value = item.experimentId
    switchModule('reservation')
    return
  }
  if (item.targetModule === 'chapter') {
    await openChapter(item.experimentId)
    return
  }
  switchModule(item.targetModule || 'chapters')
}

async function askClassroomAi() {
  if (!aiQuestion.value.trim()) {
    ElMessage.warning('请输入问题')
    return
  }
  aiLoading.value = true
  try {
    const result = await askAi({
      scene: 'SAFETY_QA',
      courseId: courseId.value,
      experimentId: selectedExperimentId.value || undefined,
      question: aiQuestion.value.trim(),
    })
    aiAnswer.value = result?.answer || result?.content || String(result || '')
  } finally {
    aiLoading.value = false
  }
}

async function loadDiscussions() {
  discussionLoading.value = true
  try {
    const result = await getDiscussions({ pageNum: 1, pageSize: 50, courseId: courseId.value })
    discussions.value = result?.records || []
    if (discussions.value.length && !currentDiscussion.value) await selectDiscussion(discussions.value[0].id)
    if (!discussions.value.length) currentDiscussion.value = null
  } finally {
    discussionLoading.value = false
  }
}

async function selectDiscussion(id) {
  currentDiscussion.value = await getDiscussionDetail(id)
  replyContent.value = ''
}

function openDiscussionCreate() {
  Object.assign(discussionForm, { experimentId: selectedExperimentId.value, title: '', content: '', isAnonymous: false })
  discussionCreateVisible.value = true
}

async function submitDiscussion() {
  if (!discussionForm.title.trim() || !discussionForm.content.trim()) {
    ElMessage.warning('请填写标题和内容')
    return
  }
  discussionSaving.value = true
  try {
    const id = await createDiscussion({
      courseId: courseId.value,
      experimentId: discussionForm.experimentId || undefined,
      title: discussionForm.title.trim(),
      content: discussionForm.content.trim(),
      isAnonymous: discussionForm.isAnonymous ? 1 : 0,
    })
    discussionCreateVisible.value = false
    await loadDiscussions()
    await selectDiscussion(id?.id || id)
  } finally {
    discussionSaving.value = false
  }
}

async function submitReply() {
  if (!replyContent.value.trim()) {
    ElMessage.warning('请输入回复内容')
    return
  }
  discussionSaving.value = true
  try {
    await replyDiscussion(currentDiscussion.value.id, { content: replyContent.value.trim() })
    await selectDiscussion(currentDiscussion.value.id)
    await loadDiscussions()
  } finally {
    discussionSaving.value = false
  }
}

function loadReportForm() {
  const report = currentReport.value
  Object.assign(reportForm, {
    id: report?.id || null,
    title: report?.title || `${activeExperimentDetail.value?.experiment?.expName || '实验'}报告`,
    content: report?.content || '',
    fileUrl: report?.fileUrl || '',
    status: report?.status || '',
  })
}

async function handleReportExperimentChange() {
  await ensureExperimentDetail(selectedExperimentId.value)
  loadReportForm()
}

async function saveReportDraft() {
  if (!selectedExperimentId.value) {
    ElMessage.warning('请先选择实验')
    return null
  }
  if (!reportForm.title.trim()) {
    ElMessage.warning('请填写报告标题')
    return null
  }
  reportSaving.value = true
  try {
    const payload = {
      experimentId: selectedExperimentId.value,
      title: reportForm.title.trim(),
      content: reportForm.content.trim(),
      fileUrl: reportForm.fileUrl || undefined,
    }
    let id = reportForm.id
    if (id) await updateReport(id, payload)
    else {
      const result = await createReport(payload)
      id = result?.id
      reportForm.id = id
    }
    ElMessage.success('报告草稿已保存')
    await loadReports()
    return id
  } finally {
    reportSaving.value = false
  }
}

async function submitCurrentReport() {
  if (!reportForm.content.trim()) {
    ElMessage.warning('请填写报告正文')
    return
  }
  reportSubmitting.value = true
  try {
    const id = await saveReportDraft()
    if (!id) return
    await submitReport(id)
    ElMessage.success('报告已提交')
    await loadReports()
  } finally {
    reportSubmitting.value = false
  }
}

async function loadExamTab() {
  if (examTab.value === 'available') await loadAvailableExams()
  if (examTab.value === 'records') await loadExamRecords()
  if (examTab.value === 'wrong') await loadWrongQuestions()
}

async function loadAvailableExams() {
  examLoading.value = true
  try {
    const result = await getAvailableExams({ pageNum: 1, pageSize: 100, courseId: courseId.value })
    availableExams.value = result?.records || []
  } finally {
    examLoading.value = false
  }
}

async function loadExamRecords() {
  examLoading.value = true
  try {
    const result = await getExamRecords({ pageNum: 1, pageSize: 100 })
    examRecords.value = (result?.records || []).filter((item) => {
      const paper = availableExams.value.find((exam) => exam.id === item.paperId)
      return !paper || Number(paper.courseId || courseId.value) === Number(courseId.value)
    })
  } finally {
    examLoading.value = false
  }
}

async function loadWrongQuestions() {
  examLoading.value = true
  try {
    const result = await getWrongQuestions({ pageNum: 1, pageSize: 100, courseId: courseId.value })
    wrongQuestions.value = result?.records || []
  } finally {
    examLoading.value = false
  }
}

async function startPaper(paper) {
  const result = await startExam(paper.id)
  if (!result) return
  Object.assign(examSession, {
    recordId: result.recordId,
    paperId: result.paperId || paper.id,
    paperTitle: result.paperTitle || paper.title,
    questions: result.questions || [],
    endTime: result.endTime,
  })
  Object.keys(answers).forEach((key) => delete answers[key])
  examSession.questions.forEach((question) => {
    const saved = result?.answers?.[question.id] ?? result?.answers?.[String(question.id)]
    answers[question.id] = question.type === 'MULTIPLE' ? String(saved || '').split(',').filter(Boolean) : (saved || '')
  })
  startExamTimer(result.endTime)
  examVisible.value = true
}

async function saveCurrentAnswers() {
  if (!examSession.recordId) return
  await saveExamAnswers(examSession.recordId, { answers: answerPayload() })
  lastSaveTime.value = new Date().toLocaleTimeString()
  ElMessage.success('答案已保存')
}

async function submitCurrentExam() {
  await ElMessageBox.confirm('确认提交安全知识考核吗？', '提交确认', { type: 'warning' })
  examSubmitting.value = true
  try {
    const result = await submitExam(examSession.recordId, { answers: answerPayload() })
    stopExamTimer()
    examVisible.value = false
    ElMessageBox.alert(`得分：${result?.totalScore || 0}，结果：${result?.passed ? '通过' : '未通过'}`, '提交成功', { type: result?.passed ? 'success' : 'warning' })
    await Promise.allSettled([loadAvailableExams(), loadExamRecords(), loadTaskPaths()])
  } finally {
    examSubmitting.value = false
  }
}

function answerPayload() {
  return examSession.questions.map((question) => ({
    questionId: question.id,
    answer: Array.isArray(answers[question.id]) ? answers[question.id].join(',') : answers[question.id],
  }))
}

function startExamTimer(endTime) {
  stopExamTimer()
  const end = new Date(endTime).getTime()
  const tick = () => {
    remainingSeconds.value = Math.max(0, Math.floor((end - Date.now()) / 1000))
  }
  tick()
  countdownTimer = window.setInterval(tick, 1000)
}

function stopExamTimer() {
  if (countdownTimer) window.clearInterval(countdownTimer)
  countdownTimer = null
}

function onResourceFile(file) {
  resourceFile.value = file.raw
}

function clearResourceFile() {
  resourceFile.value = null
}

async function createClassroomResource() {
  if (!resourceForm.experimentId || !resourceForm.title.trim()) {
    ElMessage.warning('请选择实验并填写资料标题')
    return
  }
  resourceSaving.value = true
  try {
    let fileMeta = {}
    if (resourceFile.value) fileMeta = await uploadResource(resourceFile.value)
    await createResource({
      experimentId: resourceForm.experimentId,
      title: resourceForm.title.trim(),
      resourceType: resourceForm.resourceType,
      url: resourceForm.url || undefined,
      filePath: fileMeta.filePath,
      originalFilename: fileMeta.originalFilename,
      contentType: fileMeta.contentType,
      fileSize: fileMeta.fileSize,
      openScope: 'COURSE',
      status: 1,
    })
    ElMessage.success('课堂资料已上传')
    Object.assign(resourceForm, { experimentId: selectedExperimentId.value, title: '', resourceType: 'DOCUMENT', url: '' })
    resourceFile.value = null
    await loadResources()
  } finally {
    resourceSaving.value = false
  }
}

async function chooseReservationExperiment(experiment) {
  reservationExperimentId.value = experiment.id
  reservationAdmission.value = await getAdmissionStatus(experiment.id).catch(() => null)
  availableSlots.value = []
  if (!reservationAdmission.value?.qualified) {
    ElMessage.warning('请完成相关安全知识考核')
    return
  }
  await loadSlots()
}

async function loadSlots() {
  if (!reservationExperimentId.value || !reservationAdmission.value?.qualified) return
  slotLoading.value = true
  try {
    const result = await getAvailableSlots({
      pageNum: 1,
      pageSize: 100,
      experimentId: reservationExperimentId.value,
      date: reservationDate.value || undefined,
    })
    availableSlots.value = result?.records || []
  } finally {
    slotLoading.value = false
  }
}

async function reserveSlot(row) {
  const slot = slotOf(row)
  await createReservation({
    timeSlotId: slot.id,
    labId: slot.labId,
    experimentId: reservationExperimentId.value,
    purpose: `${courseTitle.value} - ${experiments.value.find((item) => item.id === reservationExperimentId.value)?.expName || '实验预约'}`,
  })
  ElMessage.success('预约已提交')
  await Promise.allSettled([loadSlots(), loadReservations()])
}

function previewResource(id) {
  activeResourceId.value = id
  viewerVisible.value = true
}

function experimentProgress(experiment) {
  return Math.round(Number(experiment.learningProgress || taskPaths[experiment.id]?.progress || 0))
}

function resourceProgress(id) {
  return Math.round(Number(records.value.find((item) => Number(item.resourceId) === Number(id))?.progress || 0))
}

function slotOf(row) {
  return row?.timeSlot || row || {}
}

function discussionStatus(status) {
  return { OPEN: '开放', RESOLVED: '已解决', CLOSED: '已关闭' }[status] || status || '开放'
}

function taskTypeLabel(type) {
  return {
    READ_RESOURCE: '阅读资源',
    WATCH_VIDEO: '观看视频',
    SAFETY_KNOWLEDGE: '安全知识',
    PRACTICE: '练习',
    EXAM: '安全考核',
    REPORT: '报告提交',
    SUBMIT_REPORT: '报告提交',
    CHECKLIST: '准备清单',
  }[type] || type || '任务'
}

function taskTargetModule(task) {
  const type = String(task?.taskType || '').toUpperCase()
  const name = String(task?.taskName || '')
  if (['REPORT', 'SUBMIT_REPORT'].includes(type) || name.includes('报告')) return 'report'
  if (['EXAM', 'PRACTICE', 'SAFETY_KNOWLEDGE'].includes(type) || name.includes('考核') || name.includes('考试')) return 'exam'
  if (name.includes('预约')) return 'reservation'
  if (['READ_RESOURCE', 'WATCH_VIDEO'].includes(type)) return 'chapter'
  return 'chapters'
}

function taskActionLabel(task) {
  return {
    report: '提交报告',
    exam: '参加考核',
    reservation: '实验预约',
    chapter: '进入学习',
    chapters: '查看章节',
  }[taskTargetModule(task)] || '进入'
}

function examStatus(status) {
  return {
    IN_PROGRESS: { label: '进行中', type: 'warning' },
    PENDING_REVIEW: { label: '待批改', type: 'primary' },
    GRADED: { label: '已评分', type: 'success' },
    EXPIRED: { label: '已超时', type: 'danger' },
    SUBMITTED: { label: '已提交', type: 'primary' },
    REVIEWED: { label: '已复核', type: 'success' },
  }[status] || { label: status || '未知', type: 'info' }
}

function normalizedOptions(question) {
  if (question.type === 'JUDGE') return [{ value: 'TRUE', label: '正确' }, { value: 'FALSE', label: '错误' }]
  try {
    const parsed = typeof question.options === 'string' ? JSON.parse(question.options || '[]') : question.options
    if (Array.isArray(parsed)) return parsed.map((item, index) => optionFromValue(item, index))
    return Object.entries(parsed || {}).map(([value, label]) => ({ value, label: `${value}. ${label}` }))
  } catch {
    return String(question.options || '').split(/\n|;/).filter(Boolean).map((item, index) => optionFromValue(item, index))
  }
}

function optionFromValue(item, index) {
  if (typeof item === 'object') return { value: item.value || item.key || String.fromCharCode(65 + index), label: item.label || item.text || item.content }
  const value = String(item).match(/^([A-Z])[\.\、:：]/)?.[1] || String.fromCharCode(65 + index)
  return { value, label: String(item) }
}

function questionTypeLabel(type) {
  return { SINGLE: '单选题', MULTIPLE: '多选题', JUDGE: '判断题', SHORT_ANSWER: '简答题' }[type] || type || '题目'
}
</script>

<style scoped>
.classroom-page { display: grid; grid-template-columns: 224px minmax(0, 1fr); gap: 18px; max-width: 1480px; height: calc(100vh - 112px); min-height: 640px; margin: 0 auto; overflow: hidden; }
.classroom-sidebar { height: 100%; overflow-y: auto; display: flex; flex-direction: column; gap: 10px; background: #fff; border: 1px solid #e7ebf0; border-radius: 8px; padding: 14px; }
.classroom-cover { height: 96px; border-radius: 8px; background-size: cover; background-position: center; display: flex; align-items: flex-end; padding: 10px; color: #fff; }
.classroom-cover span { padding: 4px 8px; border-radius: 6px; background: rgba(20, 38, 62, 0.72); font-size: 12px; }
.classroom-sidebar h2 { color: #13233a; font-size: 17px; line-height: 1.35; text-align: center; }
.sidebar-progress { display: grid; gap: 2px; background: #f8fafc; border: 1px solid #edf1f5; border-radius: 8px; padding: 10px; }
.sidebar-progress strong { color: #13233a; font-size: 20px; }
.sidebar-progress span { color: #667085; font-size: 12px; }
.classroom-sidebar button { display: flex; align-items: center; gap: 10px; border: 0; border-radius: 8px; background: transparent; color: #5d6978; padding: 11px 12px; cursor: pointer; font-size: 14px; text-align: left; }
.classroom-sidebar button:hover, .classroom-sidebar button.active { color: #1f6feb; background: #eef6ff; }
.classroom-content { min-width: 0; height: 100%; overflow-y: auto; padding-right: 4px; }
.module-head { display: flex; justify-content: space-between; align-items: flex-end; gap: 16px; background: #fff; border: 1px solid #e7ebf0; border-radius: 8px; padding: 14px; margin-bottom: 14px; }
.module-head h1 { color: #13233a; font-size: 24px; line-height: 1.25; margin: 4px 0; }
.module-head p, .panel-title span, .task-card p, .resource-card p, .record-timeline p { color: #667085; line-height: 1.6; }
.module-panel { background: #fff; border: 1px solid #e7ebf0; border-radius: 8px; padding: 16px; min-height: 600px; }
.panel-title { display: flex; justify-content: space-between; align-items: center; gap: 12px; margin-bottom: 14px; }
.panel-title h2 { color: #13233a; font-size: 19px; }
.ai-panel { display: grid; gap: 12px; align-content: start; }
.answer-box, .report-guide, .inline-alert { margin-top: 12px; }
.answer-box { white-space: pre-wrap; color: #344054; background: #f8fafc; border: 1px solid #edf1f5; border-radius: 8px; padding: 12px; line-height: 1.7; }
.actions { display: flex; justify-content: flex-end; gap: 8px; margin-top: 12px; }
.task-list, .resource-list, .record-timeline { display: grid; gap: 12px; }
.task-card, .resource-card, .record-timeline article { display: grid; grid-template-columns: minmax(0, 1fr) auto auto; gap: 12px; align-items: center; border: 1px solid #edf1f5; border-radius: 8px; padding: 12px; }
.task-card h3, .resource-card h3, .record-timeline h3 { color: #13233a; font-size: 16px; margin-bottom: 4px; }
.task-card small { color: #98a2b3; }
.chapter-toolbar { display: flex; justify-content: flex-end; align-items: center; gap: 16px; padding: 0 0 16px; border-bottom: 1px solid #edf1f5; }
.chapter-toolbar .el-input { width: 300px; }
.catalog-label { color: #667085; font-weight: 700; margin: 22px 0 14px; }
.chapter-list { display: grid; gap: 12px; }
.chapter-group { border: 1px solid #edf1f5; border-radius: 8px; overflow: hidden; background: #fff; }
.chapter-group-head { width: 100%; display: grid; grid-template-columns: 42px minmax(0, 1fr) auto; align-items: center; gap: 12px; border: 0; background: #f5f8fb; padding: 16px; cursor: pointer; text-align: left; }
.chapter-no { width: 28px; height: 28px; border-radius: 50%; background: #d7dde5; color: #fff; display: inline-flex; justify-content: center; align-items: center; font-weight: 700; }
.chapter-group-head strong { color: #13233a; font-size: 17px; }
.chapter-nodes { display: grid; padding: 8px 16px 16px 58px; }
.chapter-nodes button { display: flex; align-items: center; gap: 14px; border: 0; border-radius: 6px; background: #fff; padding: 12px 8px; color: #13233a; cursor: pointer; font-size: 15px; text-align: left; }
.chapter-nodes button:hover { background: #eef6ff; color: #1f6feb; }
.node-state { width: 24px; height: 24px; border-radius: 50%; background: #cfd8e3; color: #fff; display: inline-flex; align-items: center; justify-content: center; flex: 0 0 auto; }
.node-state.done { background: #4cc96f; }
.discussion-grid { display: grid; grid-template-columns: 380px minmax(0, 1fr); gap: 14px; }
.topic-list, .topic-detail { border: 1px solid #edf1f5; border-radius: 8px; padding: 12px; min-height: 460px; }
.topic-list { display: grid; align-content: start; gap: 10px; }
.topic-list article { border: 1px solid #edf1f5; border-radius: 8px; padding: 12px; cursor: pointer; }
.topic-list article.active { border-color: #409eff; background: #eef6ff; }
.topic-list h3, .topic-detail h2 { color: #13233a; font-size: 16px; margin-bottom: 6px; }
.topic-list p, .topic-content, .reply-list p { color: #344054; line-height: 1.7; white-space: pre-wrap; }
.topic-list span { color: #98a2b3; font-size: 12px; }
.topic-content { background: #f8fafc; border-radius: 8px; padding: 12px; margin-bottom: 12px; }
.reply-list { display: grid; gap: 10px; margin-bottom: 12px; }
.reply-list article { border: 1px solid #edf1f5; border-radius: 8px; padding: 10px; }
.reply-list article.teacher { border-color: #bfe8c7; background: #f0f9f2; }
.report-guide { display: grid; gap: 6px; background: #f8fafc; border: 1px solid #edf1f5; border-radius: 8px; padding: 12px; margin-bottom: 14px; }
.toolbar, .resource-create { display: flex; flex-wrap: wrap; gap: 10px; align-items: center; margin-bottom: 14px; }
.toolbar .el-input, .resource-create .el-input, .resource-create .el-select { max-width: 220px; }
.reservation-grid { display: grid; grid-template-columns: 320px minmax(0, 1fr); gap: 14px; }
.experiment-list { display: grid; gap: 10px; align-content: start; }
.experiment-list button { text-align: left; border: 1px solid #edf1f5; border-radius: 8px; background: #fff; padding: 12px; cursor: pointer; }
.experiment-list button.active { border-color: #409eff; background: #eef6ff; }
.experiment-list strong { display: block; color: #13233a; margin-bottom: 4px; }
.experiment-list span { color: #667085; font-size: 12px; }
.slot-list { min-width: 0; }
.record-summary { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 12px; margin-bottom: 14px; }
.record-summary div { background: #f8fafc; border: 1px solid #edf1f5; border-radius: 8px; padding: 12px; }
.record-summary strong { display: block; color: #13233a; font-size: 24px; }
.record-summary span { color: #667085; font-size: 12px; }
.exam-meta { display: flex; gap: 10px; margin-bottom: 12px; }
.question-list { max-height: 58vh; overflow: auto; }
.question-box { border: 1px solid #edf1f5; border-radius: 8px; padding: 12px; margin-bottom: 10px; }
.question-title { display: flex; justify-content: space-between; gap: 12px; margin-bottom: 10px; color: #13233a; }
@media (max-width: 1100px) {
  .classroom-page { height: auto; min-height: 0; overflow: visible; }
  .classroom-page, .discussion-grid, .reservation-grid { grid-template-columns: 1fr; }
  .classroom-sidebar { height: auto; overflow: visible; display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); }
  .classroom-content { height: auto; overflow: visible; padding-right: 0; }
  .classroom-cover, .classroom-sidebar h2, .sidebar-progress { grid-column: 1 / -1; }
}
@media (max-width: 760px) {
  .module-head, .panel-title, .chapter-toolbar { align-items: stretch; flex-direction: column; }
  .chapter-toolbar .el-input, .toolbar .el-input, .resource-create .el-input, .resource-create .el-select { width: 100%; max-width: none; }
  .task-card, .resource-card, .record-timeline article, .record-summary { grid-template-columns: 1fr; }
  .classroom-sidebar { grid-template-columns: 1fr; }
}
</style>
