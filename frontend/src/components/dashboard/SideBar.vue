<template>
  <div class="sidebar-container">
    <el-aside :width="isCollapse ? '64px' : '220px'" class="dashboard-sidebar">
      <el-menu 
        :default-active="activeMenu" 
        class="sidebar-menu" 
        :collapse="isCollapse"
        :collapse-transition="false"
        background-color="transparent"
        text-color="#e2e8f0"
        active-text-color="#ffffff"
        router
      >
        <el-menu-item index="/dashboard">
          <el-icon><HomeFilled /></el-icon>
          <template #title>首页</template>
        </el-menu-item>
        <el-menu-item index="/device-list">
          <el-icon><Monitor /></el-icon>
          <template #title>设备信息</template>
        </el-menu-item>
        <el-menu-item index="/device-map">
          <el-icon><Location /></el-icon>
          <template #title>设备分布</template>
        </el-menu-item>
        <el-menu-item index="/worker-list">
          <el-icon><User /></el-icon>
          <template #title>维修人员</template>
        </el-menu-item>
        <el-menu-item index="/maintenance-records">
          <el-icon><Document /></el-icon>
          <template #title>维修记录</template>
        </el-menu-item>
      </el-menu>
      
      <div class="collapse-button" @click="toggleCollapse">
        <div class="collapse-button-inner">
          <el-icon v-if="!isCollapse"><ArrowLeft /></el-icon>
          <el-icon v-else><ArrowRight /></el-icon>
        </div>
      </div>
    </el-aside>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import {
  Monitor, HomeFilled, User, Document, ArrowLeft, ArrowRight, Location
} from '@element-plus/icons-vue';

const router = useRouter();
const route = useRoute();
const isCollapse = ref(false);

const activeMenu = computed(() => {
  return route.path;
});

const toggleCollapse = () => {
  isCollapse.value = !isCollapse.value;
};
</script>

<style scoped>
:root {
  --sidebar-bg: #1e293b;
  --sidebar-text: #e2e8f0;
}

.sidebar-container {
  position: relative;
  height: 100%;
}

.dashboard-sidebar {
  background: linear-gradient(180deg, #2d3748 0%, #1a202c 100%);
  height: calc(100vh - 64px);
  overflow-y: auto;
  box-shadow: 2px 0 12px rgba(0, 0, 0, 0.1);
  transition: width 0.3s ease;
  border-right: 1px solid rgba(255, 255, 255, 0.05);
  display: flex;
  flex-direction: column;
  padding-top: 20px;
}

.sidebar-menu {
  border-right: none;
  flex: 1;
}

.sidebar-menu :deep(.el-menu-item) {
  height: 50px;
  line-height: 50px;
  margin: 8px 0;
  border-radius: 0 24px 24px 0;
  margin-right: 12px;
  color: #e2e8f0;
  font-weight: 500;
}

.sidebar-menu :deep(.el-menu-item.is-active) {
  background: rgba(49, 130, 206, 0.2);
  font-weight: 600;
  color: white;
}

.sidebar-menu :deep(.el-menu-item:hover) {
  background: rgba(255, 255, 255, 0.05);
  color: #63b3ed;
}

.collapse-button {
  position: relative;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  margin: 16px auto;
  transition: all 0.3s;
  width: 70%;
}

.collapse-button-inner {
  background: rgba(45, 55, 72, 0.7);
  border-radius: 20px;
  padding: 8px 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #e2e8f0;
  box-shadow: 0 2px 5px rgba(0, 0, 0, 0.2);
  transition: all 0.3s ease;
  border: 1px solid rgba(255, 255, 255, 0.05);
}

.collapse-button-inner:hover {
  background: rgba(49, 130, 206, 0.2);
  color: #ffffff;
  transform: translateY(-2px);
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.3);
}

/* 滚动条美化 */
.dashboard-sidebar::-webkit-scrollbar {
  width: 4px;
}

.dashboard-sidebar::-webkit-scrollbar-track {
  background: transparent;
}

.dashboard-sidebar::-webkit-scrollbar-thumb {
  background-color: rgba(255, 255, 255, 0.1);
  border-radius: 2px;
}

.dashboard-sidebar::-webkit-scrollbar-thumb:hover {
  background-color: rgba(255, 255, 255, 0.2);
}
</style> 