 <template>
   <div class="login-page" :style="{ backgroundImage: `linear-gradient(90deg, rgba(19, 35, 58, 0.76), rgba(19, 35, 58, 0.18)), url(${labHero})` }">
     <div class="login-card">
       <div class="login-header">
         <div class="platform-logo">
           <el-icon :size="40" color="#409eff"><Platform /></el-icon>
         </div>
         <h2 class="platform-title">AmazingTeaching</h2>
         <p class="platform-subtitle">油气工程实验可视化学习平台</p>
       </div>
 
       <el-form
         ref="formRef"
         :model="form"
         :rules="rules"
         class="login-form"
         @keyup.enter="handleLogin"
       >
         <el-form-item prop="username">
           <el-input
             v-model="form.username"
             placeholder="学号 / 工号 / 管理员账号"
             :prefix-icon="User"
             size="large"
           />
         </el-form-item>
         <el-form-item prop="password">
           <el-input
             v-model="form.password"
             type="password"
             placeholder="密码"
             :prefix-icon="Lock"
             size="large"
             show-password
           />
         </el-form-item>
         <el-form-item prop="captcha">
           <div class="captcha-row">
             <el-input
               v-model="form.captcha"
               placeholder="验证码"
               size="large"
               maxlength="4"
             />
             <div class="captcha-box" @click="refreshCaptcha">
               <span class="captcha-text">{{ captchaText }}</span>
             </div>
           </div>
         </el-form-item>
         <el-form-item>
           <el-button
             type="primary"
             size="large"
             class="login-btn"
             :loading="loading"
             @click="handleLogin"
           >
             {{ loading ? '登录中...' : '登 录' }}
           </el-button>
         </el-form-item>
       </el-form>
 
       <div class="login-footer">
         <span class="footer-link">忘记密码？</span>
         <span class="footer-link" @click="registerVisible = true">注册账号</span>
       </div>
     </div>

     <el-dialog v-model="registerVisible" title="学生账号注册" width="520px">
       <el-form ref="registerRef" :model="registerForm" :rules="registerRules" label-width="96px">
         <el-form-item label="学号" prop="username">
           <el-input v-model="registerForm.username" maxlength="50" />
         </el-form-item>
         <el-form-item label="姓名" prop="realName">
           <el-input v-model="registerForm.realName" maxlength="50" />
         </el-form-item>
         <el-form-item label="密码" prop="password">
           <el-input v-model="registerForm.password" type="password" show-password />
         </el-form-item>
         <el-form-item label="联系方式" prop="phone">
           <el-input v-model="registerForm.phone" maxlength="30" />
         </el-form-item>
         <el-form-item label="专业" prop="major">
           <el-input v-model="registerForm.major" maxlength="100" />
         </el-form-item>
         <el-form-item label="班级" prop="className">
           <el-input v-model="registerForm.className" maxlength="100" />
         </el-form-item>
         <el-form-item label="邮箱" prop="email">
           <el-input v-model="registerForm.email" maxlength="100" />
         </el-form-item>
       </el-form>
       <template #footer>
         <el-button @click="registerVisible = false">取消</el-button>
         <el-button type="primary" :loading="registering" @click="handleRegister">注册</el-button>
       </template>
     </el-dialog>
   </div>
 </template>
 
 <script setup>
 import { ref, reactive } from 'vue'
 import { useRouter, useRoute } from 'vue-router'
 import { ElMessage } from 'element-plus'
 import { User, Lock, Platform } from '@element-plus/icons-vue'
 import { useAuthStore } from '@/stores/authStore'
 import { getRoleHomePath } from '@/utils/role'
 import { register } from '@/api/auth'
 import labHero from '@/assets/amazing/lab-hero.png'
 
 const router = useRouter()
 const route = useRoute()
 const authStore = useAuthStore()
 
 const formRef = ref(null)
 const registerRef = ref(null)
 const loading = ref(false)
 const registering = ref(false)
 const registerVisible = ref(false)
 
 const form = reactive({
   username: '',
   password: '',
   captcha: '',
 })

 const registerForm = reactive({
   username: '',
   realName: '',
   password: '',
   phone: '',
   major: '',
   className: '',
   email: '',
 })
 
 const rules = {
   username: [
     { required: true, message: '请输入用户名', trigger: 'blur' },
     { min: 2, max: 50, message: '长度在 2 到 50 个字符', trigger: 'blur' },
   ],
   password: [
     { required: true, message: '请输入密码', trigger: 'blur' },
     { min: 6, max: 20, message: '长度在 6 到 20 个字符', trigger: 'blur' },
   ],
   captcha: [
     { required: true, message: '请输入验证码', trigger: 'blur' },
     { len: 4, message: '验证码为4位', trigger: 'blur' },
   ],
 }

 const registerRules = {
   username: [
     { required: true, message: '请输入学号', trigger: 'blur' },
     { min: 2, max: 50, message: '长度在 2 到 50 个字符', trigger: 'blur' },
   ],
   realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
   password: [
     { required: true, message: '请输入密码', trigger: 'blur' },
     { min: 6, max: 64, message: '长度在 6 到 64 个字符', trigger: 'blur' },
   ],
   email: [{ type: 'email', message: '邮箱格式不正确', trigger: 'blur' }],
 }
 
 // Mock captcha
 const captchaText = ref('')
 function refreshCaptcha() {
   captchaText.value = Math.random().toString(36).substring(2, 6).toUpperCase()
 }
 refreshCaptcha()
 
 async function handleLogin() {
   const valid = await formRef.value.validate().catch(() => false)
   if (!valid) return
   if (form.captcha.toUpperCase() !== captchaText.value) {
     ElMessage.error('验证码不正确')
     refreshCaptcha()
     return
   }
   loading.value = true
   try {
     const loginResult = await authStore.login({
       username: form.username,
       password: form.password,
     })
     ElMessage.success('登录成功')
     const roleHomePath = getRoleHomePath(loginResult.role)
     const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : roleHomePath
     const targetPath = redirect === '/' || redirect === '/home' ? roleHomePath : redirect
     router.push(targetPath)
   } catch (e) {
     ElMessage.error(e.message || '登录失败，请检查账号和密码')
     refreshCaptcha()
   } finally {
     loading.value = false
   }
 }

 async function handleRegister() {
   const valid = await registerRef.value.validate().catch(() => false)
   if (!valid) return
   registering.value = true
   try {
     await register(registerForm)
     ElMessage.success('注册成功，请登录')
     form.username = registerForm.username
     registerVisible.value = false
     Object.assign(registerForm, {
       username: '',
       realName: '',
       password: '',
       phone: '',
       major: '',
       className: '',
       email: '',
     })
   } finally {
     registering.value = false
   }
 }
 </script>
 
 <style scoped>
 .login-page {
   position: relative;
   z-index: 1;
   width: 100%;
   display: flex;
   align-items: center;
   justify-content: center;
   min-height: 100vh;
   padding: 20px;
   background-size: cover;
   background-position: center;
 }
 .login-card {
   width: 420px;
   max-width: 100%;
   background: rgba(255, 255, 255, 0.95);
   backdrop-filter: blur(20px);
   border-radius: 16px;
   padding: 40px 36px 32px;
   box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
 }
 .login-header {
   text-align: center;
   margin-bottom: 28px;
 }
 .platform-logo {
   margin-bottom: 12px;
 }
 .platform-title {
   font-size: 20px;
   font-weight: 700;
   color: #1a3a5c;
   margin-bottom: 6px;
 }
 .platform-subtitle {
   font-size: 13px;
   color: #999;
 }
 .login-form {
   margin-bottom: 16px;
 }
 .captcha-row {
   display: flex;
   gap: 12px;
   width: 100%;
 }
 .captcha-box {
   width: 110px;
   height: 40px;
   background: linear-gradient(135deg, #1a3a5c, #2d5f8a);
   border-radius: 8px;
   display: flex;
   align-items: center;
   justify-content: center;
   cursor: pointer;
   flex-shrink: 0;
   user-select: none;
 }
 .captcha-text {
   font-family: 'Courier New', monospace;
   font-size: 18px;
   font-weight: bold;
   color: #fff;
   letter-spacing: 4px;
   font-style: italic;
   text-decoration: line-through;
   opacity: 0.8;
 }
 .login-btn {
   width: 100%;
   height: 44px;
   font-size: 16px;
   border-radius: 10px;
 }
 .login-footer {
   display: flex;
   justify-content: space-between;
   font-size: 13px;
 }
 .footer-link {
   color: #409eff;
   cursor: pointer;
 }
 .footer-link:hover {
   color: #66b1ff;
 }
 </style>
