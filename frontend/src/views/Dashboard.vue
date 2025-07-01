<template>
  <dashboard-layout>
    <dashboard-overview />
  </dashboard-layout>
</template>

<script setup>
import DashboardLayout from '@/components/layout/DashboardLayout.vue';
import DashboardOverview from '@/components/dashboard/DashboardOverview.vue';
</script>

<style scoped>
.page-title {
  margin-top: 0;
  margin-bottom: 28px;
  color: #1e293b;
  font-size: 26px;
  font-weight: 700;
  position: relative;
  padding-left: 12px;
}

.page-title::before {
  content: '';
  position: absolute;
  left: 0;
  top: 6px;
  height: 70%;
  width: 4px;
  background: linear-gradient(180deg, #3b82f6 0%, #60a5fa 100%);
  border-radius: 4px;
}
</style>