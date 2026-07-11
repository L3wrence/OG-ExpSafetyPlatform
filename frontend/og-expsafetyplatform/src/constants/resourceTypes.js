export const RESOURCE_TYPES = [
  { value: 'VIDEO', label: '视频', accept: '.mp4,.webm', maxMB: 500 },
  { value: 'DOCUMENT', label: '文档', accept: '.pdf,.txt,.md', maxMB: 50 },
  { value: 'IMAGE', label: '图片', accept: '.jpg,.jpeg,.png,.webp,.gif', maxMB: 10 },
  { value: 'AUDIO', label: '音频', accept: '.mp3,.wav,.ogg', maxMB: 100 },
]

export const BUSINESS_CATEGORIES = [
  'GUIDE', 'LECTURE', 'COURSEWARE', 'TEACHING_VIDEO', 'MICRO_COURSE', 'INSTRUMENT_VIDEO',
  'DEVICE_MANUAL', 'EXPERIMENT_CASE', 'ACCIDENT_CASE', 'EMERGENCY_PROCESS', 'REPORT_TEMPLATE', 'REFERENCE', 'OTHER',
].map((value) => ({ value, label: value }))

export function resourceTypeConfig(type) { return RESOURCE_TYPES.find((item) => item.value === type) }
export function resourceTypeLabel(type) { return resourceTypeConfig(type)?.label || type || '-' }
export function validateResourceFile(file, type) {
  const config = resourceTypeConfig(type)
  if (!config) return '请选择资源类型'
  const extension = `.${String(file?.name || '').split('.').pop().toLowerCase()}`
  if (!config.accept.split(',').includes(extension)) return `${config.label}仅支持 ${config.accept}`
  if (file.size > config.maxMB * 1024 * 1024) return `${config.label}不能超过 ${config.maxMB}MB`
  return ''
}
