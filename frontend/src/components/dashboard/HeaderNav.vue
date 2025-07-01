<template>
  <div>
    <el-header class="dashboard-header">
      <div class="logo">
        <router-link to="/" class="logo-link">
          <el-icon class="logo-icon"><Lightning /></el-icon>
          <span>智能接地线在线监测</span>
        </router-link>
      </div>
      
      <div class="header-actions">
        <!-- 通知中心组件 -->
        <notification-center 
          :notifications="notifications"
          @show-notification="handleShowNotification"
          @mark-all-read="handleMarkAllAsRead"
        />
        
        <!-- 用户信息组件 -->
        <user-profile 
          :user-name="userName"
          @logout="handleLogout"
        />
      </div>
    </el-header>
  </div>
</template>

<script setup>
import { Lightning } from '@element-plus/icons-vue';
import NotificationCenter from '@/components/dashboard/NotificationCenter.vue';
import UserProfile from '@/components/dashboard/UserProfile.vue';

const props = defineProps({
  userName: {
    type: String,
    default: '管理员'
  },
  notifications: {
    type: Array,
    default: () => []
  }
});

const emit = defineEmits(['show-notification', 'mark-all-read', 'logout']);

// 处理显示通知详情
const handleShowNotification = (notification) => {
  emit('show-notification', notification);
};

// 标记所有通知为已读
const handleMarkAllAsRead = () => {
  emit('mark-all-read');
};

// 退出登录
const handleLogout = () => {
  emit('logout');
};
</script>

<style scoped>
/* 顶部导航栏样式 */
.dashboard-header {
  background-color: #ffffff;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  height: 64px;
  z-index: 10;
  position: relative;
}

.logo {
  display: flex;
  align-items: center;
  transition: all 0.3s ease;
}

.logo:hover {
  transform: scale(1.03);
}

.logo-link {
  display: flex;
  align-items: center;
  text-decoration: none;
}

.logo-icon {
  margin-right: 12px;
  font-size: 28px;
  color: #4183e0;
  filter: drop-shadow(0 2px 5px rgba(65, 131, 224, 0.4));
}

.logo span {
  font-size: 20px;
  font-weight: 600;
  background: linear-gradient(45deg, #3461b3, #4183e0, #58a6ff);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
  letter-spacing: 1px;
  text-shadow: 0 2px 10px rgba(65, 131, 224, 0.1);
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 20px;
}
</style> 