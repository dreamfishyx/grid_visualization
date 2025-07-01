<template>
  <dashboard-layout>
    <!-- 搜索和筛选区域 -->
    <div class="filter-container">
      <el-input
        v-model="searchQuery"
        placeholder="搜索设备ID/维修人员"
        class="search-input"
        clearable
        prefix-icon="Search"
        @keyup.enter="handleSearch"
      />
      <el-select v-model="statusFilter" placeholder="维修状态" class="filter-select">
        <el-option label="全部" :value="null" />
        <el-option label="未完成" :value="0" />
        <el-option label="已完成" :value="1" />
      </el-select>
      <el-date-picker
        v-model="dateRange"
        type="daterange"
        range-separator="至"
        start-placeholder="开始日期"
        end-placeholder="结束日期"
        format="YYYY-MM-DD"
        value-format="YYYY-MM-DD"
        class="date-picker"
      />
      <el-button type="primary" @click="handleSearch" class="action-btn search-btn">
        <el-icon><Search /></el-icon>
        查询
      </el-button>
    </div>
    
    <!-- 维修记录表格 -->
    <el-card class="record-table-card">
      <div class="table-header">
        <h3 class="table-title">维修记录列表</h3>
      </div>
      <el-table
        :data="recordsData"
        style="width: 100%"
        border
        stripe
        v-loading="loading"
        :cell-style="{ fontSize: '14px' }"
        :header-cell-style="{ backgroundColor: '#f1f5f9', color: '#334155', fontWeight: 600 }"
        empty-text="暂无维修记录数据"
      >
        <el-table-column prop="id" label="记录编号" min-width="120" />
        <el-table-column prop="deviceId" label="设备ID" min-width="120" />
        <el-table-column label="维修人员" min-width="100">
          <template #default="scope">
            {{ scope.row.workerName || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="scope">
            <el-tag :type="getStatusType(scope.row.status)">
              {{ getStatusText(scope.row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="报修时间" min-width="170">
          <template #default="scope">
            {{ formatDateTime(scope.row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="完成时间" min-width="170">
          <template #default="scope">
            {{ scope.row.finishedAt ? formatDateTime(scope.row.finishedAt) : '-' }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="scope">
            <div class="action-buttons">
              <el-button 
                @click="viewRecordDetail(scope.row)"
                class="action-btn view-btn"
                size="small"
              >
                <el-icon><View /></el-icon>
                详情
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
      
      <!-- 分页 -->
      <div class="pagination-container">
        <el-pagination
          :current-page="currentPage"
          :page-size="pageSize"
          :page-sizes="[5, 10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          :total="totalRecords"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
          background
        />
      </div>
    </el-card>

    <!-- 记录详情对话框 -->
    <el-dialog
      v-model="recordDetailVisible"
      :title="`维修记录 #${selectedRecord ? selectedRecord.id : ''}`"
      width="700px"
      :append-to-body="true"
      :lock-scroll="true"
      :center="true"
      destroy-on-close
      class="record-dialog"
    >
      <div v-if="selectedRecord" class="record-detail">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="记录编号">{{ selectedRecord.id }}</el-descriptions-item>
          <el-descriptions-item label="设备ID">{{ selectedRecord.deviceId }}</el-descriptions-item>
          <el-descriptions-item label="维修人员">{{ selectedRecord.workerName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="维修状态">
            <el-tag :type="getStatusType(selectedRecord.status)">
              {{ getStatusText(selectedRecord.status) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="报修时间">{{ formatDateTime(selectedRecord.createdAt) }}</el-descriptions-item>
          <el-descriptions-item label="完成时间">{{ selectedRecord.finishedAt ? formatDateTime(selectedRecord.finishedAt) : '未完成' }}</el-descriptions-item>
          <el-descriptions-item label="故障描述" :span="2">{{ selectedRecord.description || '无故障描述' }}</el-descriptions-item>
          <el-descriptions-item label="维修过程" :span="2">{{ selectedRecord.process || '无维修过程记录' }}</el-descriptions-item>
        </el-descriptions>
        
        <div class="record-actions">
          <el-button type="primary" @click="recordDetailVisible = false">关闭</el-button>
        </div>
      </div>
    </el-dialog>
  </dashboard-layout>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue';
import { ElMessage } from 'element-plus';
import { Search, View } from '@element-plus/icons-vue';
import { useRouter } from 'vue-router';
import { EventSourcePolyfill } from 'event-source-polyfill';
import { useAuthStore } from '@/store/useAuthStore';
import { getMaintenanceRecords } from '@/api/maintenanceRecords';
import dayjs from 'dayjs'

// 导入共享布局组件
import DashboardLayout from '@/components/layout/DashboardLayout.vue';

const router = useRouter();
const authStore = useAuthStore();
let eventSource = null;

// 页面状态
const loading = ref(false);
const searchQuery = ref('');
const statusFilter = ref(null);
const dateRange = ref([]);
const currentPage = ref(1);
const pageSize = ref(5);
const totalRecords = ref(0);
const recordsData = ref([]);

// 记录详情对话框
const recordDetailVisible = ref(false);
const selectedRecord = ref(null);

// 格式化日期时间
const formatDateTime = (dateTimeStr) => {
  if (!dateTimeStr) return '-';
  // 统一格式化为 YYYY-MM-DD HH:mm:ss
  return dayjs(dateTimeStr).format('YYYY-MM-DD HH:mm:ss');
};

// 维修状态映射
const getStatusText = (status) => {
  const statusMap = {
    0: '未完成',
    1: '已完成',
    UNFINISHED: '未完成',
    FINISHED: '已完成'
  };
  
  // 检查status是否为字符串格式的数字
  if (typeof status === 'string' && !isNaN(parseInt(status))) {
    return statusMap[parseInt(status)] || '未知';
  }
  
  // 检查是否为对象
  if (typeof status === 'object' && status !== null) {
    if ('code' in status) {
      return statusMap[status.code] || '未知';
    }
  }
  
  // 直接返回映射
  return statusMap[status] || '未知';
};

// 获取维修状态对应的标签类型
const getStatusType = (status) => {
  // 将状态转换为数字代码
  let statusCode = status;
  
  // 检查status是否为字符串格式的数字
  if (typeof status === 'string' && !isNaN(parseInt(status))) {
    statusCode = parseInt(status);
  }
  
  // 检查是否为对象
  if (typeof status === 'object' && status !== null) {
    if ('code' in status) {
      statusCode = status.code;
    }
  }
  
  const typeMap = {
    0: 'warning',
    1: 'success',
    UNFINISHED: 'warning',
    FINISHED: 'success'
  };
  
  return typeMap[statusCode] || 'info';
};

// 获取维修记录列表
const fetchRecords = async () => {
  loading.value = true;
  try {
    // 获取日期范围
    let startTime = null;
    let endTime = null;
    if (dateRange.value && dateRange.value.length === 2) {
      startTime = dateRange.value[0];
      endTime = dateRange.value[1];
    }
    
    // 发送请求
    const res = await getMaintenanceRecords(
      currentPage.value,
      pageSize.value,
      searchQuery.value,
      statusFilter.value,
      startTime,
      endTime
    );
    
    // 处理返回的数据
    if (res.data && res.data.list) {
      // 重新映射字段名
      recordsData.value = res.data.list.map(item => {
        // 检查并处理维修人员名称
        const workerName = item.workerName || (item.worker ? item.worker.name : '-');
        
        return {
          id: item.id,
          deviceId: item.deviceId,
          workerName,
          status: item.status,
          createdAt: item.createdAt,
          finishedAt: item.finishedAt,
          description: item.description,
          process: item.process
        };
      });
      totalRecords.value = res.data.total || 0;
    } else {
      recordsData.value = [];
      totalRecords.value = 0;
      console.warn('获取的维修记录列表为空或格式不正确');
    }
  } catch (error) {
    console.error('获取维修记录列表失败:', error);
    ElMessage.error('获取维修记录列表失败，请稍后重试');
    recordsData.value = [];
    totalRecords.value = 0;
  } finally {
    loading.value = false;
  }
};

// 处理搜索/查询
const handleSearch = () => {
  currentPage.value = 1;
  fetchRecords();
};

// 处理页面大小变化
const handleSizeChange = (val) => {
  pageSize.value = val;
  fetchRecords();
};

// 处理页码变化
const handleCurrentChange = (val) => {
  currentPage.value = val;
  fetchRecords();
};

// 查看维修记录详情
const viewRecordDetail = (record) => {
  selectedRecord.value = record;
  recordDetailVisible.value = true;
};

// 建立SSE连接
const setupSSEConnection = () => {
  // 关闭已有连接
  closeSSEConnection();
  
  // 创建新的SSE连接
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
    console.log('正在建立维修记录SSE连接...');
    
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
      console.log('SSE连接已建立');
    };
    
    // 接收到消息时的处理
    eventSource.addEventListener(userId.toString(), (event) => {
      console.log('收到维修记录更新消息:', event.data);
      
      // 当消息为维修记录更新时，重新获取维修记录数据
      if (event.data === 'maintenance-update') {
        console.log('检测到维修记录更新，使用当前筛选条件重新获取数据');
        
        // 显示正在更新的提示
        ElMessage({
          message: '检测到维修记录更新，正在刷新数据...',
          type: 'info',
          duration: 2000
        });
        
        // 使用当前筛选条件重新获取维修记录数据
        fetchRecords();
      }
    });
    
    // 接收心跳消息
    eventSource.addEventListener('heartbeat', (event) => {
      // 只在开发环境输出心跳日志
      if (process.env.NODE_ENV === 'development') {
        console.log('维修记录收到心跳消息:', event.data);
      }
    });
    
    // 错误处理
    eventSource.onerror = (error) => {
      console.error('SSE连接错误:', error);
      
      // 检查是否为跨域错误
      if (error && error.status === 0) {
        console.error('可能存在跨域问题，请检查CORS配置');
      }
      
      closeSSEConnection();
      
      // 尝试重新连接
      setTimeout(() => {
        console.log('正在尝试重新连接SSE...');
        setupSSEConnection();
      }, 5000);
    };
  } catch (error) {
    console.error('建立SSE连接失败:', error);
  }
};

// 关闭SSE连接
const closeSSEConnection = () => {
  if (eventSource) {
    console.log('正在关闭SSE连接...');
    eventSource.close();
    eventSource = null;
  }
};

// 组件挂载时获取维修记录列表并建立SSE连接
onMounted(() => {
  fetchRecords();
  setupSSEConnection();
});

// 组件卸载前关闭SSE连接
onBeforeUnmount(() => {
  closeSSEConnection();
});
</script>

<style scoped>
.page-title {
  margin-top: 0;
  margin-bottom: 28px;
  color: #1e293b;
  font-size: 28px;
  font-weight: 700;
  position: relative;
  padding-left: 18px;
  background: linear-gradient(90deg, #1e293b, #334155);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
}

.page-title::before {
  content: '';
  position: absolute;
  left: 0;
  top: 6px;
  height: 75%;
  width: 6px;
  background: linear-gradient(180deg, #3b82f6 0%, #60a5fa 100%);
  border-radius: 4px;
}

.filter-container {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 28px;
  flex-wrap: wrap;
  background-color: white;
  padding: 20px;
  border-radius: 14px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.06);
  transition: all 0.3s ease;
  border-top: 4px solid #3b82f6;
}

.filter-container:hover {
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.08);
}

.search-input {
  width: 280px;
}

.filter-select {
  width: 150px;
}

.date-picker {
  width: 320px;
}

.record-table-card {
  border-radius: 14px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.07);
  margin-bottom: 30px;
  border: none;
  overflow: hidden;
  transition: all 0.3s ease;
}

.record-table-card:hover {
  box-shadow: 0 8px 30px rgba(0, 0, 0, 0.1);
}

.table-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  border-bottom: 1px solid #f1f5f9;
  padding-bottom: 20px;
}

.table-title {
  margin: 0;
  font-size: 20px;
  color: #1e293b;
  font-weight: 600;
  position: relative;
  padding-left: 16px;
  background: linear-gradient(90deg, #1e293b, #334155);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
}

.table-title::before {
  content: '';
  position: absolute;
  left: 0;
  top: 4px;
  height: 75%;
  width: 5px;
  background: linear-gradient(180deg, #3b82f6 0%, #60a5fa 100%);
  border-radius: 3px;
}

.pagination-container {
  margin-top: 28px;
  display: flex;
  justify-content: flex-end;
  padding-top: 20px;
  border-top: 1px solid #f1f5f9;
}

.action-buttons {
  display: flex;
  gap: 8px;
  justify-content: center;
}

.action-btn {
  min-width: 72px !important;
  height: 34px !important;
  padding: 0 12px !important;
  font-size: 13px !important;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s ease;
  border: none !important;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.08);
  border-radius: 6px !important;
  gap: 4px;
  font-weight: 500 !important;
  letter-spacing: 0.5px;
}

.view-btn {
  background: linear-gradient(145deg, #3b82f6, #60a5fa) !important;
  color: white !important;
}

.search-btn {
  background: linear-gradient(145deg, #0ea5e9, #38bdf8) !important;
  color: white !important;
}

.action-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.15);
  filter: brightness(105%);
}

.action-btn:active {
  transform: translateY(0);
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.1);
  filter: brightness(95%);
}

/* 记录详情样式 */
.record-detail {
  margin-bottom: 20px;
}

.record-actions {
  margin-top: 24px;
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

/* 表格样式增强 */
:deep(.el-table) {
  border-radius: 10px;
  overflow: hidden;
  border: 1px solid #f1f5f9;
}

:deep(.el-table th) {
  background-color: #f8fafc !important;
  font-weight: 600 !important;
  color: #1e293b !important;
  padding: 16px 0;
  border-bottom: 2px solid #e2e8f0;
}

:deep(.el-table--border .el-table__cell) {
  border-right: 1px solid #e2e8f0;
}

:deep(.el-table tr) {
  transition: all 0.3s ease;
}

:deep(.el-table tr:hover) {
  background-color: #f1f5f9 !important;
  transform: translateY(-2px);
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.05);
}

/* 对话框样式 */
:deep(.record-dialog) {
  display: flex;
  align-items: center;
  justify-content: center;
}

:deep(.record-dialog .el-dialog) {
  margin: 0 auto !important;
  position: relative;
  border-radius: 12px;
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.1), 0 2px 6px rgba(0, 0, 0, 0.08);
  overflow: hidden;
}

:deep(.record-dialog .el-dialog__header) {
  padding: 20px 24px;
  margin: 0;
  border-bottom: 1px solid #f1f5f9;
  background: #f8fafc;
}

:deep(.record-dialog .el-dialog__title) {
  font-weight: 600;
  font-size: 18px;
  color: #1e293b;
}

:deep(.record-dialog .el-dialog__body) {
  padding: 24px;
}

:deep(.record-dialog .el-dialog__footer) {
  padding: 16px 24px;
  border-top: 1px solid #f1f5f9;
  background: #f8fafc;
}

/* 修复遮罩层位置 */
:deep(.el-overlay) {
  position: fixed;
  top: 0;
  right: 0;
  bottom: 0;
  left: 0;
  z-index: 2000;
  height: 100%;
  background-color: rgba(0, 0, 0, 0.5);
  overflow: auto;
}

/* 打印样式优化 */
@media print {
  .dashboard-sidebar,
  .dashboard-header,
  .filter-container,
  .pagination-container,
  .record-actions,
  .el-dialog__header,
  .el-dialog__footer {
    display: none !important;
  }
  
  .record-detail {
    margin: 0;
    padding: 0;
  }
  
  .el-dialog__body {
    padding: 0;
  }
  
  body {
    background-color: white;
  }
}
</style> 