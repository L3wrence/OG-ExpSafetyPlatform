 // Role constants
 export const ROLE_STUDENT = 'student'
 export const ROLE_TEACHER = 'teacher'
 export const ROLE_ADMIN = 'admin'
 
 export const ROLE_LABELS = {
   [ROLE_STUDENT]: '学生',
   [ROLE_TEACHER]: '教师',
   [ROLE_ADMIN]: '管理员',
 }
 
 export const ROLE_COLORS = {
   [ROLE_STUDENT]: '#409eff',
   [ROLE_TEACHER]: '#67c23a',
   [ROLE_ADMIN]: '#e6a23c',
 }
 
 // Status constants
 export const STATUS_MAP = {
   pending: { label: '待审核', type: 'warning' },
   approved: { label: '已通过', type: 'success' },
   rejected: { label: '已驳回', type: 'danger' },
   completed: { label: '已完成', type: 'info' },
   active: { label: '进行中', type: 'primary' },
   closed: { label: '已结课', type: 'info' },
 }
 
 // Course types
 export const COURSE_TYPES = ['必修', '选修', '通识']
 
 // Difficulty levels
 export const DIFFICULTY_MAP = {
   easy: { label: '简单', color: '#67c23a' },
   medium: { label: '中等', color: '#e6a23c' },
   hard: { label: '困难', color: '#f56c6c' },
 }
 
 // Menu icons by module
 export const MODULE_ICONS = {
   home: 'HomeFilled',
   course: 'Reading',
   experiment: 'Monitor',
   exam: 'EditPen',
   knowledge: 'Notebook',
   resource: 'Folder',
   reservation: 'Calendar',
   report: 'Document',
   grade: 'Trophy',
   ai: 'MagicStick',
   dashboard: 'DataBoard',
   user: 'User',
   role: 'Avatar',
   permission: 'Lock',
   notice: 'Bell',
   log: 'List',
   setting: 'Setting',
   profile: 'UserFilled',
 }
