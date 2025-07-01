<template>
  <div class="notification-center">
    <el-badge :value="unreadCount" class="notification-icon" :hidden="unreadCount === 0">
      <el-popover :visible="popoverVisible" @update:visible="popoverVisible = $event" placement="bottom-end"
        :width="380" trigger="click">
        <template #reference>
          <el-button class="icon-button" link @click="fetchNotifications">
            <el-icon>
              <Bell />
            </el-icon>
          </el-button>
        </template>

        <div class="notification-popover">
          <div class="notification-header">
            <h3>通知中心</h3>
            <el-button v-if="notifications.length > 0" type="primary" link size="small" @click="markAllAsRead">
              全部标为已读
            </el-button>
          </div>

          <el-skeleton v-if="loading" :rows="3" animated />

          <div v-else-if="notifications.length === 0" class="empty-state">
            <div class="empty-icon">
              <el-icon>
                <Bell />
              </el-icon>
            </div>
            <p>暂无未读通知</p>
          </div>

          <div v-else class="notification-list">
            <div v-for="notification in notifications" :key="notification.messageId" class="notification-item"
              @click="viewNotificationDetail(notification)">
              <div class="notification-content">
                <div :class="`notification-tag ${getTypeClass(notification.type)}`">
                  {{ typeMap[getTypeClass(notification.type)] }}
                </div>
                <h4>{{ notification.title }}</h4>
                <p>{{ notification.content }}</p>
                <div class="notification-time">{{ formatTime(notification.createTime) }}</div>
              </div>
            </div>
          </div>
        </div>
      </el-popover>
    </el-badge>

    <notification-detail v-model="detailVisible" :notification="selectedNotification" :typeMap="typeMap"
      @mark-as-read="markAsRead" />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue';
import { ElMessage } from 'element-plus';
import { Bell } from '@element-plus/icons-vue';
import NotificationDetail from './NotificationDetail.vue';
import { getUnreadMessages, markMessageAsRead } from '@/api/message';
import dayjs from 'dayjs';
import { useAuthStore } from '@/store/useAuthStore';
import { EventSourcePolyfill } from 'event-source-polyfill';

const authStore = useAuthStore();

const notifications = ref([]);
const popoverVisible = ref(false);
const detailVisible = ref(false);
const selectedNotification = ref(null);
const loading = ref(false);
let eventSource = null;

// 消息类型映射
const typeMap = {
  'info': '通知',
  'warning': '警告',
  'error': '错误'
};

// 计算未读消息数量
const unreadCount = computed(() => notifications.value.length);

// 获取通知类型对应的CSS类
function getTypeClass(type) {
  if (!type) {
    console.warn('通知类型为空');
    return 'info';
  }

  // 处理对象类型
  if (typeof type === 'object') {
    if ('code' in type) {
      const code = Number(type.code);
      return codeToTypeMap[code] || 'info';
    }
    console.warn('通知类型对象缺少code属性:', type);
    return 'info';
  }

  // 处理数字类型
  if (typeof type === 'number') {
    return codeToTypeMap[type] || 'info';
  }

  // 处理字符串类型
  if (typeof type === 'string') {
    // 字符串类型处理：先转为小写
    const lowerType = type.toLowerCase();

    // 如果是小写类型，直接返回
    if (['info', 'warning', 'error'].includes(lowerType)) {
      return lowerType;
    }

    // 处理大写枚举名称
    if (['INFO', 'WARNING', 'ERROR'].includes(type)) {
      return type.toLowerCase();
    }

    // 尝试解析字符串为数字
    const typeNum = parseInt(type);
    if (!isNaN(typeNum) && typeNum >= 0 && typeNum <= 2) {
      return codeToTypeMap[typeNum];
    }
  }

  console.warn('无法识别的通知类型:', type);
  return 'info'; // 默认返回info类型
}

// 代码到类型的映射表
const codeToTypeMap = {
  0: 'info',
  1: 'warning',
  2: 'error'
};

// 格式化时间
function formatTime(timestamp) {
  if (!timestamp) return '';
  try {
    return dayjs(timestamp).format('YYYY-MM-DD HH:mm:ss');
  } catch (error) {
    console.error('时间格式化错误:', error);
    return String(timestamp);
  }
}

// 获取未读通知列表
async function fetchNotifications() {
  if (loading.value) return;

  loading.value = true;
  try {
    const res = await getUnreadMessages();
    if (res.data) {
      notifications.value = res.data;
    }
  } catch (error) {
    console.error('获取未读通知失败:', error);
    ElMessage.error('获取未读通知失败');
  } finally {
    loading.value = false;
  }
}

// 查看通知详情
function viewNotificationDetail(notification) {
  if (!notification) {
    console.warn('尝试查看空通知');
    return;
  }

  try {
    const typeString = getTypeClass(notification.type);
    selectedNotification.value = {
      id: notification.messageId,
      title: notification.title || '未知通知',
      type: typeString,
      time: formatTime(notification.createTime),
      description: notification.content || '',
      read: notification.hasRead?.code === 1
    };
    detailVisible.value = true;
    popoverVisible.value = false;
  } catch (error) {
    console.error('处理通知详情时出错:', error);
    ElMessage.error('查看通知详情失败');
  }
}

// 标记通知为已读
async function markAsRead(messageId) {
  if (!messageId) {
    console.warn('消息ID为空，无法标记为已读');
    return;
  }

  try {
    await markMessageAsRead(messageId);
    ElMessage.success('已标记为已读');
    detailVisible.value = false;

    // 从当前列表中移除已读消息
    const index = notifications.value.findIndex(msg => msg.messageId === messageId);
    if (index !== -1) {
      notifications.value.splice(index, 1);
    }
  } catch (error) {
    console.error('标记已读失败:', error);
    ElMessage.error('标记已读失败');
  }
}

// 标记所有通知为已读
async function markAllAsRead() {
  try {
    await Promise.all(notifications.value.map(notification => markAsRead(notification.messageId)));
    ElMessage.success('所有通知已标记为已读');
  } catch (error) {
    console.error('标记所有通知为已读失败:', error);
    ElMessage.error('标记所有通知为已读失败');
  }
}

// 初始化SSE连接
function setupSSE() {
  // // 检查是否已经存在有效连接，避免重复创建
  // if (eventSource && eventSource.readyState !== 2) { // readyState !== CLOSED
  //   console.log('SSE连接已存在，复用现有连接');
  //   return; // 直接使用现有连接
  // }

  // 清理旧连接
  if (eventSource) {
    eventSource.close();
    eventSource = null;
  }

  const userId = authStore.userId;
  const token = authStore.accessToken;

  if (!userId) {
    console.warn('未找到用户ID，无法建立SSE连接');
    return;
  }

  if (!token) {
    console.warn('未找到认证令牌，无法建立SSE连接');
    return;
  }

  try {
    console.log('通知中心正在建立SSE连接...');

    // 使用EventSourcePolyfill，支持添加自定义头部
    eventSource = new EventSourcePolyfill('http://localhost:8080/sse/subscribe', {
      headers: {
        'Authorization': `Bearer ${token}`
      },
      heartbeatTimeout: 60000, // 心跳超时时间60秒
      connectionTimeout: 30000, // 连接超时30秒
      reconnectInterval: 5000  // 重连间隔5秒
    });

    // 连接建立时的处理
    eventSource.onopen = () => {
      console.log('通知中心SSE连接已建立');
    };

    // 接收到消息时的处理
    eventSource.addEventListener(userId.toString(), (event) => {
      console.log('通知中心收到SSE消息:', event.data);

      // 当消息为消息更新时，重新获取未读消息
      if (event.data === 'message-update') {
        console.log('检测到消息更新，重新获取未读消息');
        fetchNotifications();
      }
    });

    // 接收心跳消息
    eventSource.addEventListener('heartbeat', (event) => {
      // 只在开发环境输出心跳日志
      if (process.env.NODE_ENV === 'development') {
        console.log('通知中心收到心跳消息:', event.data);
      }
    });

    // 错误处理
    eventSource.onerror = (error) => {
      console.error('SSE连接错误:', error);

      if (eventSource) {
        eventSource.close();
        eventSource = null;
      }

      // 尝试重新连接
      setTimeout(() => {
        console.log('正在尝试重新连接SSE...');
        setupSSE();
      }, 5000);
    };
  } catch (error) {
    console.error('建立SSE连接失败:', error);
  }
}

onMounted(async () => {
  await fetchNotifications();
  setupSSE();
});

onUnmounted(() => {
  // 关闭SSE连接
  if (eventSource) {
    eventSource.close();
    eventSource = null;
  }
});
</script>

<style scoped>
.notification-center {
  position: relative;
}

.icon-button {
  font-size: 20px;
  color: #64748b;
}

.notification-popover {
  max-height: 500px;
  display: flex;
  flex-direction: column;
}

.notification-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid #e2e8f0;
}

.notification-header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  color: #94a3b8;
}

.empty-icon {
  font-size: 36px;
  margin-bottom: 16px;
  color: #cbd5e1;
}

.notification-list {
  overflow-y: auto;
  max-height: 400px;
}

.notification-item {
  padding: 16px;
  border-bottom: 1px solid #e2e8f0;
  cursor: pointer;
  transition: background-color 0.2s;
}

.notification-item:hover {
  background-color: #f8fafc;
}

.notification-content h4 {
  margin: 8px 0;
  font-size: 14px;
  color: #1e293b;
}

.notification-content p {
  margin: 0 0 10px;
  font-size: 13px;
  color: #64748b;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  -webkit-box-orient: vertical;
  box-orient: vertical;
  overflow: hidden;
}

.notification-time {
  font-size: 12px;
  color: #94a3b8;
}

.notification-tag {
  display: inline-block;
  padding: 3px 8px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;
  margin-bottom: 8px;
}

.notification-tag.info {
  background-color: rgba(59, 130, 246, 0.1);
  color: #3b82f6;
}

.notification-tag.warning {
  background-color: rgba(245, 158, 11, 0.1);
  color: #f59e0b;
}

.notification-tag.error {
  background-color: rgba(239, 68, 68, 0.1);
  color: #ef4444;
}
</style>