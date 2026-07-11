<template>
  <div class="cover-uploader">
    <img v-if="preview" :src="preview" alt="课程封面预览" />
    <el-upload :auto-upload="false" :show-file-list="false" accept=".jpg,.jpeg,.png,.webp" :on-change="pick">
      <el-button>选择本地封面</el-button>
    </el-upload>
    <el-button v-if="preview" text type="danger" @click="remove">删除封面</el-button>
    <small>支持 JPEG、PNG、WebP，最大 5MB</small>
  </div>
</template>

<script setup>
import { onBeforeUnmount, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'

const props = defineProps({ modelValue: { type: String, default: '' } })
const emit = defineEmits(['file-change', 'remove'])
const preview = ref(props.modelValue)
let objectUrl = ''
watch(() => props.modelValue, (value) => { if (!objectUrl) preview.value = value })

function pick(uploadFile) {
  const file = uploadFile.raw
  if (!file) return
  if (!['image/jpeg', 'image/png', 'image/webp'].includes(file.type) || file.size > 5 * 1024 * 1024) {
    ElMessage.warning('课程封面只支持 JPEG/PNG/WebP，且不能超过 5MB')
    return
  }
  release()
  objectUrl = URL.createObjectURL(file)
  preview.value = objectUrl
  emit('file-change', file)
}
function remove() { release(); preview.value = ''; emit('remove') }
function release() { if (objectUrl) URL.revokeObjectURL(objectUrl); objectUrl = '' }
onBeforeUnmount(release)
</script>

<style scoped>
.cover-uploader { display:flex; flex-wrap:wrap; align-items:center; gap:10px; }
.cover-uploader img { width:160px; height:90px; object-fit:cover; border-radius:8px; }
.cover-uploader small { width:100%; color:#98a2b3; }
</style>
