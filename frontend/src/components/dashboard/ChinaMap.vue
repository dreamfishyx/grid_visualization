<template>
  <el-row :gutter="20">
    <el-col :span="24">
      <el-card class="map-overview-card" shadow="hover">
        <template #header>
          <div class="card-header">
            <span>设备分布概览</span>
          </div>
        </template>
        <div class="echarts-map-container">
          <div id="china-map" class="china-map-chart"></div>
          <div class="map-loading" v-if="loading">
            <el-icon class="is-loading">
              <Loading />
            </el-icon>
            <span>加载中...</span>
          </div>
        </div>
      </el-card>
    </el-col>
  </el-row>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, watch } from 'vue';
import { ElMessage } from 'element-plus';
import * as echarts from 'echarts';
import { Loading } from '@element-plus/icons-vue';
import chinaMap from '@/assets/china.json';

const props = defineProps({
  loading: {
    type: Boolean,
    default: false
  },
  options: {
    type: Object,
    default: () => ({})
  },
  provinceData: {
    type: Array,
    default: () => []
  }
});

const emit = defineEmits(['province-click']);

// 保存ECharts实例，便于后续清理
let myChart = null;

// 窗口大小变化时重新调整图表
const handleResize = () => {
  if (myChart) {
    myChart.resize();
  }
};

// 初始化中国地图
const initChinaMap = () => {
  const chartDom = document.getElementById('china-map');
  if (!chartDom) return;
  
  // 注册地图数据
  echarts.registerMap('china', chinaMap);
  
  // 获取地图中实际的省份/城市名称
  const mapFeatures = chinaMap.features || [];
  const mapAreaNames = mapFeatures.map(feature => feature.properties.name);
  console.log('地图中的地区名称:', mapAreaNames);
  
  // 初始化ECharts实例
  myChart = echarts.init(chartDom);
  
  // 设置地图配置选项
  const option = {
    // 地图背景颜色
    backgroundColor: '#F8FAFC',
    
    // 标题
    title: {
      text: '全国设备分布',
      left: 'center',
      textStyle: {
        color: '#1e293b',
        fontSize: 16,
        fontWeight: 600
      }
    },
    
    // 提示框
      tooltip: {
        trigger: 'item',
      formatter: (params) => {
        const data = params.data;
        if (!data) return params.name;
        
        return `${params.name}: ${data.value || 0}台设备`;
      }
    },
    
    // 视觉映射组件 - 注意这里使用从props传入的配置
      visualMap: {
      ...props.options.visualMap,
      // 确保显示在左下角
      left: 'left',
      bottom: '10%',
        orient: 'vertical',
      show: true
    },
    
    // 地图系列
      series: [
        {
          name: '设备分布',
          type: 'map',
          map: 'china',
          roam: true,
          zoom: 1.2,
        scaleLimit: {
          min: 1,
          max: 3
        },
        itemStyle: props.options.series.itemStyle,
        emphasis: props.options.series.emphasis,
        select: {
          itemStyle: {
            areaColor: '#60a5fa'
            }
          },
        data: props.provinceData
        }
      ]
    };

  // 应用配置项
    myChart.setOption(option);
    
  // 绑定省份/城市点击事件
  myChart.on('click', (params) => {
    console.log('点击地区:', params.name);
      
    // 查找该地区的完整数据
    const areaData = props.provinceData.find(item => item.name === params.name);
    
    if (areaData) {
      console.log('找到地区数据:', areaData);
      emit('province-click', areaData);
    } else {
      console.log('未找到地区数据，使用默认值');
        emit('province-click', {
          name: params.name,
        value: 0,
        online: 0,
        offline: 0,
        anomaly: 0
        });
      }
    });
    
  // 监听窗口大小变化
  window.addEventListener('resize', handleResize);
};

// 更新地图数据
const updateMapData = () => {
  if (!myChart) return;
  
  console.log('更新地图数据:', props.provinceData);
  
  // 处理数据，确保值有效
  const validData = props.provinceData.map(item => {
    return {
      ...item,
      value: item.value || 0
    };
  });
  
  console.log('处理后的地图数据:', validData);
  
  // 计算数据中的最大值来动态调整visualMap
  const maxValue = Math.max(...validData.map(item => item.value), 10);
  
  // 完整更新配置，包括视觉映射
  myChart.setOption({
    visualMap: {
      ...props.options.visualMap,
      min: 0,
      max: Math.ceil(maxValue * 1.2), // 略大于最大值，留出余量
      left: 'left',
      bottom: '10%',
      orient: 'vertical',
      show: true
    },
    series: [
      {
        data: validData
      }
    ]
  });
};

// 监听provinceData变化
watch(() => props.provinceData, (newVal) => {
  if (newVal && newVal.length > 0) {
    updateMapData();
  }
}, { deep: true });

// 组件卸载前清理资源
onBeforeUnmount(() => {
  if (myChart) {
    window.removeEventListener('resize', handleResize);
    myChart.dispose();
    myChart = null;
  }
});

onMounted(() => {
  try {
    // 延迟初始化，确保DOM已加载完成
    setTimeout(() => {
      initChinaMap();
    }, 500);
  } catch (error) {
    console.error('初始化地图失败:', error);
    ElMessage.error('初始化地图失败，请刷新页面重试');
  }
});
</script>

<style scoped>
.map-overview-card {
  margin-bottom: 28px;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
  border: none;
  transition: all 0.3s ease;
}

.map-overview-card:hover {
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.08);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid #f1f5f9;
}

.card-header span {
  font-weight: 600;
  font-size: 16px;
  color: #1e293b;
}

.echarts-map-container {
  position: relative;
  height: 480px;
  width: 100%;
  overflow: hidden;
  border-radius: 0 0 12px 12px;
  box-shadow: inset 0 0 10px rgba(0, 0, 0, 0.03);
  background-color: #F8FAFC;
}

.china-map-chart {
  width: 100%;
  height: 100%;
}

.map-loading {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  background-color: rgba(255, 255, 255, 0.8);
  z-index: 1;
  backdrop-filter: blur(3px);
}

.map-loading .el-icon {
  font-size: 24px;
  margin-bottom: 12px;
  color: #3b82f6;
}

.map-loading span {
  color: #64748b;
  font-size: 14px;
}

@media (max-width: 768px) {
  .echarts-map-container {
    height: 360px;
  }
}
</style> 