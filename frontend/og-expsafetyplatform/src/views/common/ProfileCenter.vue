<template>
  <div class="profile-page">
    <section class="profile-head">
      <el-avatar :size="72" :src="profileForm.avatarUrl" icon="UserFilled" />
      <div>
        <h1>{{ profileForm.realName || authStore.userInfo?.username }}</h1>
        <p>{{ roleLabel }} · {{ authStore.userInfo?.username }}</p>
      </div>
    </section>

    <section class="profile-grid">
      <el-card shadow="never" class="panel">
        <template #header>
          <div class="panel-header">
            <span>个人资料</span>
            <el-tag type="info">账号与角色由管理员维护</el-tag>
          </div>
        </template>
        <el-form ref="profileRef" :model="profileForm" :rules="profileRules" label-width="96px" v-loading="loading">
          <el-form-item label="账号">
            <el-input :model-value="authStore.userInfo?.username" disabled />
          </el-form-item>
          <el-form-item label="角色">
            <el-input :model-value="roleLabel" disabled />
          </el-form-item>
          <el-form-item label="姓名" prop="realName">
            <el-input v-model="profileForm.realName" maxlength="50" show-word-limit />
          </el-form-item>
          <el-form-item label="联系方式" prop="phone">
            <el-input v-model="profileForm.phone" maxlength="30" show-word-limit />
          </el-form-item>
          <el-form-item label="邮箱" prop="email">
            <el-input v-model="profileForm.email" maxlength="100" />
          </el-form-item>
          <el-form-item label="专业" prop="major">
            <el-input v-model="profileForm.major" maxlength="100" show-word-limit />
          </el-form-item>
          <el-form-item label="班级" prop="className">
            <el-input v-model="profileForm.className" maxlength="100" show-word-limit />
          </el-form-item>
          <el-form-item label="头像地址" prop="avatarUrl">
            <el-input v-model="profileForm.avatarUrl" maxlength="500" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="savingProfile" @click="saveProfile">保存资料</el-button>
          </el-form-item>
        </el-form>
      </el-card>

      <el-card shadow="never" class="panel">
        <template #header>
          <div class="panel-header">
            <span>密码修改</span>
            <el-tag type="warning">提交后需重新登录</el-tag>
          </div>
        </template>
        <el-form ref="passwordRef" :model="passwordForm" :rules="passwordRules" label-width="96px">
          <el-form-item label="旧密码" prop="oldPassword">
            <el-input v-model="passwordForm.oldPassword" type="password" show-password />
          </el-form-item>
          <el-form-item label="新密码" prop="newPassword">
            <el-input v-model="passwordForm.newPassword" type="password" show-password />
          </el-form-item>
          <el-form-item label="确认密码" prop="confirmPassword">
            <el-input v-model="passwordForm.confirmPassword" type="password" show-password />
          </el-form-item>
          <el-form-item>
            <el-button type="danger" :loading="savingPassword" @click="savePassword">修改密码</el-button>
          </el-form-item>
        </el-form>
      </el-card>

      <el-card v-if="authStore.role !== 'admin'" ref="certificationPanel" shadow="never" class="panel">
        <template #header>
          <div class="panel-header">
            <span>教师认证</span>
            <el-tag :type="certificationTag.type">{{ certificationTag.label }}</el-tag>
          </div>
        </template>
        <el-alert
          v-if="teacherCertification?.reviewComment"
          :title="teacherCertification.reviewComment"
          :type="teacherCertification.status === 'REJECTED' ? 'warning' : 'info'"
          show-icon
          :closable="false"
          class="cert-alert"
        />
        <el-form label-width="96px">
          <el-form-item label="所属学校">
            <el-input v-model="certificationForm.school" maxlength="100" placeholder="请输入所属学校" />
          </el-form-item>
          <el-form-item label="工号">
            <el-input v-model="certificationForm.employeeNo" maxlength="50" placeholder="请输入教师工号" />
          </el-form-item>
          <el-form-item label="教育邮箱">
            <el-input v-model="certificationForm.educationEmail" maxlength="120" placeholder="请输入教育邮箱" />
          </el-form-item>
          <el-form-item>
            <el-button
              type="primary"
              :loading="savingCertification"
              :disabled="teacherCertification?.status === 'PENDING' || teacherCertification?.status === 'APPROVED'"
              @click="saveCertification"
            >
              提交认证
            </el-button>
          </el-form-item>
        </el-form>
      </el-card>
    </section>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useAuthStore } from '@/stores/authStore'
import { ROLE_LABELS } from '@/utils/constant'
import { changePassword } from '@/api/auth'
import { getMyProfile, updateMyProfile } from '@/api/user'
import { applyTeacherCertification, getMyTeacherCertification } from '@/api/teacherCertification'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()
const loading = ref(false)
const savingProfile = ref(false)
const savingPassword = ref(false)
const savingCertification = ref(false)
const profileRef = ref(null)
const passwordRef = ref(null)
const certificationPanel = ref(null)
const teacherCertification = ref(null)

const roleLabel = computed(() => ROLE_LABELS[authStore.role] || '普通用户')

const profileForm = reactive({
  realName: '',
  phone: '',
  email: '',
  major: '',
  className: '',
  avatarUrl: '',
})

const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: '',
})

const certificationForm = reactive({
  school: '',
  employeeNo: '',
  educationEmail: '',
})

const certificationTag = computed(() => {
  const status = teacherCertification.value?.status
  if (status === 'APPROVED' || authStore.userInfo?.teacherCertified) return { label: '已认证', type: 'success' }
  if (status === 'PENDING') return { label: '审核中', type: 'warning' }
  if (status === 'REJECTED') return { label: '已驳回', type: 'danger' }
  return { label: '未认证', type: 'info' }
})

const profileRules = {
  realName: [{ max: 50, message: '姓名长度不能超过50个字符', trigger: 'blur' }],
  phone: [{ max: 30, message: '联系方式长度不能超过30个字符', trigger: 'blur' }],
  email: [{ type: 'email', message: '邮箱格式不正确', trigger: 'blur' }],
}

const passwordRules = {
  oldPassword: [{ required: true, message: '请输入旧密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 64, message: '新密码长度应为6到64个字符', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    {
      validator: (_rule, value, callback) => {
        if (value !== passwordForm.newPassword) callback(new Error('两次输入的新密码不一致'))
        else callback()
      },
      trigger: 'blur',
    },
  ],
}

onMounted(async () => {
  await loadProfile()
  scrollToCertificationPanel()
})

watch(() => route.query.panel, () => {
  scrollToCertificationPanel()
})

async function loadProfile() {
  loading.value = true
  try {
    const data = await getMyProfile()
    Object.assign(profileForm, {
      realName: data?.realName || '',
      phone: data?.phone || '',
      email: data?.email || '',
      major: data?.major || '',
      className: data?.className || '',
      avatarUrl: data?.avatarUrl || '',
    })
    authStore.setUserInfo(data)
    await loadTeacherCertification()
  } finally {
    loading.value = false
  }
}

async function loadTeacherCertification() {
  if (authStore.role === 'admin') return
  const data = await getMyTeacherCertification().catch(() => null)
  teacherCertification.value = data
  Object.assign(certificationForm, {
    school: data?.school || '',
    employeeNo: data?.employeeNo || '',
    educationEmail: data?.educationEmail || profileForm.email || '',
  })
}

async function scrollToCertificationPanel() {
  if (route.query.panel !== 'teacher-certification' || authStore.role === 'admin') {
    return
  }
  await nextTick()
  certificationPanel.value?.$el?.scrollIntoView?.({ behavior: 'smooth', block: 'start' })
}

async function saveProfile() {
  const valid = await profileRef.value.validate().catch(() => false)
  if (!valid) return
  savingProfile.value = true
  try {
    const data = await updateMyProfile(profileForm)
    authStore.setUserInfo(data)
    ElMessage.success('个人资料已保存')
  } finally {
    savingProfile.value = false
  }
}

async function savePassword() {
  const valid = await passwordRef.value.validate().catch(() => false)
  if (!valid) return
  await ElMessageBox.confirm('修改密码后当前登录会失效，需要重新登录。确认继续吗？', '修改密码', { type: 'warning' })
  savingPassword.value = true
  try {
    await changePassword({
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword,
    })
    ElMessage.success('密码已修改，请重新登录')
    authStore.logout()
    router.push('/login')
  } finally {
    savingPassword.value = false
  }
}

async function saveCertification() {
  if (!certificationForm.school.trim() || !certificationForm.employeeNo.trim() || !certificationForm.educationEmail.trim()) {
    ElMessage.warning('请填写学校、工号和教育邮箱')
    return
  }
  savingCertification.value = true
  try {
    await applyTeacherCertification({
      school: certificationForm.school.trim(),
      employeeNo: certificationForm.employeeNo.trim(),
      educationEmail: certificationForm.educationEmail.trim(),
    })
    ElMessage.success('教师认证申请已提交')
    await loadTeacherCertification()
  } finally {
    savingCertification.value = false
  }
}
</script>

<style scoped>
.profile-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.profile-head {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
}
.profile-head h1 {
  font-size: 22px;
  color: #1f2937;
}
.profile-head p {
  margin-top: 6px;
  color: #6b7280;
}
.profile-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.4fr) minmax(320px, 0.8fr);
  gap: 16px;
}
.panel {
  border-radius: 8px;
}
.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.cert-alert {
  margin-bottom: 12px;
}
@media (max-width: 960px) {
  .profile-grid {
    grid-template-columns: 1fr;
  }
}
</style>
