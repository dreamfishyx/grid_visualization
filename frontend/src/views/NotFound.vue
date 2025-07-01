<template>
  <div class="not-found">
    <h1>404</h1>
    <h2>页面不存在</h2>
    <el-button type="primary" @click="goHome">返回首页</el-button>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'

const router = useRouter()

const goHome = () => {
  router.push('/')
}
</script>

<style scoped>
.not-found {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100vh;
  text-align: center;
}

h1 {
  font-size: 6rem;
  margin: 0;
  color: #409eff;
}

h2 {
  margin-top: 0;
  margin-bottom: 2rem;
  color: #606266;
}
</style> 