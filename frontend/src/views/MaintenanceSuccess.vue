<template>
  <div class="success-page">
    <div class="success-container">
      <el-result
        icon="success"
        title="维修记录提交成功"
        sub-title="您的维修记录已成功提交，感谢您的配合！"
      >
        <template #extra>
          <el-button type="primary" @click="goToRecords">查看维修记录</el-button>
          <el-button @click="goBack">返回首页</el-button>
        </template>
      </el-result>
    </div>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router';

const router = useRouter();

const goToRecords = () => {
  router.push('/maintenance-records');
};

const goBack = () => {
  router.push('/');
};
</script>

<style scoped>
.success-page {
  min-height: 100vh;
  background: linear-gradient(135deg, #f0f9ff 0%, #e6f7ff 100%);
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 20px;
}

.success-container {
  background: white;
  padding: 40px;
  border-radius: 16px;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.08);
  max-width: 600px;
  width: 100%;
}
</style> 