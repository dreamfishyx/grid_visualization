<template>
  <el-card 
    class="status-card" 
    :class="cardClass" 
    shadow="hover" 
    @click="handleClick(cardData.type)"
  >
    <div class="card-content">
      <div>
        <div class="status-value">{{ cardData.value }}</div>
        <div class="status-title">{{ cardData.title }}</div>
      </div>
      <div class="status-chart">
        <el-progress 
          type="circle" 
          :percentage="cardData.percentage" 
          :width="70" 
          :stroke-width="10"
          :color="cardData.color"
        ></el-progress>
      </div>
    </div>
  </el-card>
</template>

<script setup>
import { computed } from 'vue';

const props = defineProps({
  cardData: {
    type: Object,
    required: true
  }
});

const emit = defineEmits(['card-click']);

const cardClass = computed(() => {
  return `${props.cardData.type}-card`;
});

const handleClick = (type) => {
  emit('card-click', type);
};
</script>

<style scoped>
.status-card {
  margin-bottom: 28px;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.3s ease;
  overflow: hidden;
  border: none;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
}

.status-card:hover {
  transform: translateY(-6px);
  box-shadow: 0 12px 24px rgba(0, 0, 0, 0.1);
}

.card-content {
  padding: 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  position: relative;
  background-color: white;
}

.status-value {
  font-size: 38px;
  font-weight: 700;
  line-height: 1;
  color: #1e293b;
  margin-bottom: 8px;
  background: linear-gradient(90deg, #1e293b, #334155);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
}

.status-title {
  font-size: 14px;
  color: #64748b;
  font-weight: 500;
}

.status-chart {
  display: flex;
  justify-content: center;
  align-items: center;
}

/* 设置卡片左边框颜色 */
.online-card {
  border-left: none;
  position: relative;
}

.online-card::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  height: 100%;
  width: 4px;
  background-color: #10b981;
  border-radius: 0 2px 2px 0;
}

.offline-card {
  border-left: none;
  position: relative;
}

.offline-card::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  height: 100%;
  width: 4px;
  background-color: #f43f5e;
  border-radius: 0 2px 2px 0;
}

.anomaly-card {
  border-left: none;
  position: relative;
}

.anomaly-card::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  height: 100%;
  width: 4px;
  background-color: #f59e0b;
  border-radius: 0 2px 2px 0;
}

/* 卡片渐变背景 */
.online-card .card-content::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(135deg, rgba(16, 185, 129, 0.08) 0%, rgba(16, 185, 129, 0) 100%);
  z-index: 0;
}

.offline-card .card-content::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(135deg, rgba(244, 63, 94, 0.08) 0%, rgba(244, 63, 94, 0) 100%);
  z-index: 0;
}

.anomaly-card .card-content::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(135deg, rgba(245, 158, 11, 0.08) 0%, rgba(245, 158, 11, 0) 100%);
  z-index: 0;
}
</style> 