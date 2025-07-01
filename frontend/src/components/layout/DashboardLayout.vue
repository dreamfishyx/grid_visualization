<template>
  <div class="dashboard-container">
    <!-- 顶部导航栏 -->
    <header-nav :user-name="userStore.userName" :notifications="notifications"
      @show-notification="showNotificationDetail" @mark-all-read="markAllAsRead" @logout="handleLogout" />

    <el-container class="dashboard-main">
      <!-- 侧边栏 -->
      <side-bar />

      <!-- 主内容区域 -->
      <el-main class="dashboard-content">
        <slot></slot>
      </el-main>
    </el-container>

    <!-- 通知详情对话框 -->
    <notification-detail v-if="notificationDetailVisible" v-model="notificationDetailVisible"
      :notification="selectedNotification" :type-map="notificationTypeMap" />
  </div>
</template>

<script setup>
import { ref, provide, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from '@/store/useAuthStore';
import { ElMessage } from 'element-plus';

// 导入组件
import HeaderNav from '@/components/dashboard/HeaderNav.vue';
import SideBar from '@/components/dashboard/SideBar.vue';
import NotificationDetail from '@/components/dashboard/NotificationDetail.vue';

const router = useRouter();
const userStore = useAuthStore();

// 通知类型映射
const notificationTypeMap = {
  'warning': '警告',
  'info': '信息',
  'success': '成功'
};

// 通知详情对话框状态
const notificationDetailVisible = ref(false);
const selectedNotification = ref(null);

// 通知数据
const notifications = ref([
  {
    id: 1,
    type: 'warning',
    title: '变电站A-1号变压器故障',
    description: '温度过高，请立即检查',
    time: '15分钟前',
    read: false,
    extendedInfo: '设备ID: TRANS-A1-2023，位置: 华东区域变电站A，当前温度: 85°C (正常范围: ≤70°C)，故障开始时间: 2023-04-16 14:30:22，已持续: 50分钟，建议操作: 1. 降低负载 2. 检查冷却系统'
  },
  {
    id: 2,
    type: 'info',
    title: '系统升级通知',
    description: '系统将于今晚22:00进行例行升级',
    time: '1小时前',
    read: true,
    extendedInfo: '升级内容: 1. 性能优化 2. 安全补丁 3. 新增数据分析功能，预计维护时间: 2小时，请提前做好相关准备工作'
  }
]);

// 显示通知详情
const showNotificationDetail = (notification) => {
  selectedNotification.value = notification;
  notificationDetailVisible.value = true;
};

// 标记所有通知为已读
const markAllAsRead = () => {
  notifications.value.forEach(notification => {
    notification.read = true;
  });
  ElMessage.success('已将所有通知标记为已读');
};

// 退出登录
const handleLogout = async () => {
  try {
  await userStore.logout();
  ElMessage({
    message: '已退出登录',
    type: 'success',
    duration: 2000
  });
  router.push('/auth');
  } catch (error) {
    ElMessage.error('退出登录失败');
  }
};

// 提供通知数据给所有子组件
provide('notifications', notifications);
</script>

<style scoped>
.dashboard-container {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background-color: var(--background-color);
  color: var(--text-color);
  overflow: hidden;
}

.dashboard-main {
  flex: 1;
  overflow: hidden;
}

.dashboard-content {
  padding: 28px;
  background-color: var(--background-color);
  height: calc(100vh - 64px);
  overflow-y: auto;
  overflow-x: hidden;
  transition: all 0.3s ease;
  will-change: transform, opacity;
}

/* 响应式优化 */
@media (max-width: 1200px) {
  .dashboard-content {
    padding: 20px;
  }
}

@media (max-width: 768px) {
  .dashboard-content {
    padding: 16px;
  }
}

/* 全局动画效果 */
@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }

  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.dashboard-content {
  animation: fadeIn 0.4s ease-out;
}

/* 内容区域滚动条美化 */
.dashboard-content::-webkit-scrollbar {
  width: 6px;
}

.dashboard-content::-webkit-scrollbar-track {
  background: transparent;
}

.dashboard-content::-webkit-scrollbar-thumb {
  background-color: rgba(100, 116, 139, 0.3);
  border-radius: 3px;
}

.dashboard-content::-webkit-scrollbar-thumb:hover {
  background-color: rgba(100, 116, 139, 0.5);
}

.user-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  margin-right: 8px;
  border: 2px solid rgba(59, 130, 246, 0.2);
  box-shadow: 0 3px 6px rgba(0, 0, 0, 0.1);
  transition: all 0.3s ease;
}

.user-avatar:hover {
  transform: scale(1.05);
  border-color: rgba(59, 130, 246, 0.4);
}
</style>