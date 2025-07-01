<template>
  <el-dialog :modelValue="modelValue" @update:modelValue="$emit('update:modelValue', $event)" :title="province?.name + '设备详情'" width="600px">
    <div class="province-detail" v-if="province">
      <el-descriptions border direction="vertical" :column="2">
        <el-descriptions-item label="设备总数">{{ province.value }} 台</el-descriptions-item>
        <el-descriptions-item label="在线设备">{{ province.online }} 台</el-descriptions-item>
        <el-descriptions-item label="故障设备">{{ province.offline }} 台</el-descriptions-item>
        <el-descriptions-item label="异常设备">{{ province.anomaly }} 台</el-descriptions-item>
      </el-descriptions>

      <div class="province-actions">
        <el-button type="primary" @click="$emit('view-detail')">查看详细</el-button>
        <el-button @click="$emit('update:modelValue', false)">关闭</el-button>
      </div>
    </div>
  </el-dialog>
</template>

<script setup>
const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false
  },
  province: {
    type: Object,
    default: null
  }
});

const emit = defineEmits(['update:modelValue', 'view-detail']);
</script>

<style scoped>
:deep(.el-dialog) {
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.2);
}

:deep(.el-dialog__header) {
  background-color: #f8fafc;
  padding: 16px 20px;
  margin: 0;
  border-bottom: 1px solid #e2e8f0;
}

:deep(.el-dialog__title) {
  font-weight: 600;
  color: #1e293b;
  font-size: 16px;
}

:deep(.el-dialog__body) {
  padding: 20px;
}

:deep(.el-dialog__footer) {
  padding: 16px 20px;
  border-top: 1px solid #e2e8f0;
  background-color: #f8fafc;
}

.province-detail {
  padding: 20px 0;
}

.facility-title {
  margin: 24px 0 16px;
  font-size: 16px;
  font-weight: 600;
  color: #1e293b;
}

.province-actions {
  margin-top: 30px;
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

/* 按钮美化 */
:deep(.el-button) {
  border-radius: 8px;
  font-weight: 500;
  transition: all 0.2s ease;
}

:deep(.el-button--primary) {
  background: linear-gradient(90deg, #3b82f6, #60a5fa);
  border: none;
  box-shadow: 0 2px 6px rgba(59, 130, 246, 0.3);
}

:deep(.el-button--primary:hover) {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.4);
  background: linear-gradient(90deg, #2563eb, #3b82f6);
}
</style> 