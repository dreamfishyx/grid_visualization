<template>
  <el-dialog 
    :modelValue="modelValue" 
    @update:modelValue="$emit('update:modelValue', $event)"
    :title="notification?.title || '通知详情'" 
    width="500px"
    destroy-on-close
  >
    <div class="notification-detail" v-if="notification">
      <div class="detail-header">
        <div :class="`notification-tag ${notification.type}`">
          {{ getTypeLabel(notification.type) }}
        </div>
        <div class="detail-time">{{ notification.time }}</div>
      </div>
      <div class="detail-content">
        <h3>{{ notification.title }}</h3>
        <p>{{ notification.description }}</p>
        <div class="detail-extended" v-if="notification.extendedInfo">
          <h4>详细信息</h4>
          <p>{{ notification.extendedInfo }}</p>
        </div>
        <div class="detail-actions" v-if="!notification.read">
          <el-button type="primary" @click="markAsRead">标记为已读</el-button>
        </div>
      </div>
    </div>
  </el-dialog>
</template>

<script setup>
import { computed } from 'vue';

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false
  },
  notification: {
    type: Object,
    default: null
  },
  typeMap: {
    type: Object,
    default: () => ({
      'warning': '警告',
      'info': '通知',
      'error': '错误'
    })
  }
});

const emit = defineEmits(['update:modelValue', 'mark-as-read']);

// 获取类型标签（使用计算属性可以缓存结果）
const getTypeLabel = (type) => {
  if (!type || !props.typeMap[type]) {
    console.warn('未知的通知类型:', type);
    return '通知';
  }
  return props.typeMap[type];
};

// 标记消息为已读
const markAsRead = () => {
  if (props.notification && props.notification.id) {
    emit('mark-as-read', props.notification.id);
  }
};
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

.notification-detail {
  padding: 10px;
}

.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.notification-tag {
  padding: 5px 12px;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 600;
}

.notification-tag.warning {
  background-color: rgba(245, 158, 11, 0.1);
  color: #f59e0b;
}

.notification-tag.info {
  background-color: rgba(59, 130, 246, 0.1);
  color: #3b82f6;
}

.notification-tag.error {
  background-color: rgba(239, 68, 68, 0.1);
  color: #ef4444;
}

.detail-time {
  color: #94a3b8;
  font-size: 13px;
}

.detail-content h3 {
  margin-top: 0;
  margin-bottom: 12px;
  color: #1e293b;
  font-size: 18px;
}

.detail-content p {
  color: #64748b;
  line-height: 1.6;
  margin-bottom: 16px;
  font-size: 14px;
}

.detail-extended {
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px solid #e2e8f0;
}

.detail-extended h4 {
  color: #334155;
  margin-top: 0;
  margin-bottom: 12px;
  font-size: 15px;
}

.detail-actions {
  margin-top: 25px;
  display: flex;
  gap: 12px;
}
</style>