<template>
  <dashboard-layout>
    <!-- 搜索和筛选区域 -->
    <div class="filter-container">
      <el-input v-model="state.searchQuery" placeholder="搜索人员姓名" class="search-input" clearable prefix-icon="Search" />
      <el-select v-model="state.genderFilter" placeholder="性别" class="filter-select">
        <el-option label="全部" value="" />
        <el-option label="男" :value="0" />
        <el-option label="女" :value="1" />
      </el-select>
      <el-button type="primary" @click="handleSearch" class="action-btn search-btn">
        <el-icon>
          <Search />
        </el-icon>
        搜索
      </el-button>
      <el-button type="success" @click="showAddWorkerDialog" class="action-btn add-btn">
        <el-icon>
          <Plus />
        </el-icon>
        添加人员
      </el-button>
    </div>
    
    <!-- 维修人员表格 -->
    <el-card class="worker-table-card">
      <div class="table-header">
        <h3 class="table-title">维修人员</h3>
      </div>
      <el-table :data="state.workers" style="width: 100%" border stripe v-loading="state.loading" :cell-style="{ fontSize: '14px' }"
        :header-cell-style="{ backgroundColor: '#f1f5f9', color: '#334155', fontWeight: 600 }">
        <el-table-column prop="workerId" label="编号" min-width="120" />
        <el-table-column prop="name" label="姓名" min-width="120" />
        <el-table-column prop="gender" label="性别" width="80">
          <template #default="scope">
            {{ dataParser.getGenderText(scope.row.gender) }}
          </template>
        </el-table-column>
        <el-table-column prop="email" label="邮箱" min-width="160" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="scope">
            {{ dataParser.getStatusText(scope.row.status) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="scope">
            <div class="action-buttons">
              <el-button @click="editWorker(scope.row)" class="action-btn edit-btn" size="small">
                <el-icon>
                  <Edit />
                </el-icon>
                修改
              </el-button>
              <el-button @click="deleteWorker(scope.row.workerId)" class="action-btn delete-btn" size="small">
                <el-icon>
                  <Delete />
                </el-icon>
                删除
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
      
      <!-- 分页 -->
      <div class="pagination-container">
        <el-pagination :current-page="state.currentPage" :page-size="state.pageSize" :page-sizes="[3, 5, 10, 15]"
          layout="total, sizes, prev, pager, next, jumper" :total="state.totalWorkers"
          @update:current-page="handleCurrentChange" @update:page-size="handleSizeChange" background />
      </div>
    </el-card>

    <!-- 新增/编辑维修人员对话框 -->
    <el-dialog v-model="dialogState.visible" :title="dialogState.title" width="550px" :append-to-body="true" :lock-scroll="true"
      :center="true" destroy-on-close class="worker-dialog">
      <el-form ref="workerFormRef" :model="dialogState.form" :rules="workerRules" label-width="100px" label-position="left">
        <el-form-item label="姓名" prop="name">
          <el-input v-model="dialogState.form.name" placeholder="请输入姓名" />
        </el-form-item>
        <el-form-item label="性别" prop="gender" v-if="!dialogState.isEditing">
          <el-radio-group v-model="dialogState.form.gender">
            <el-radio :label="0">男</el-radio>
            <el-radio :label="1">女</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="dialogState.form.email" placeholder="请输入邮箱" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="dialogState.visible = false">取消</el-button>
          <el-button type="primary" @click="submitWorkerForm" :loading="state.loading">确认</el-button>
        </div>
      </template>
    </el-dialog>
  </dashboard-layout>
</template>

<script setup>
import { ref, computed, onMounted, reactive, onBeforeUnmount } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { Search, Delete, Edit, Refresh, Plus } from '@element-plus/icons-vue';
import { getWorkerList, getWorkerInfo, deleteWorker as apiDeleteWorker, addWorker as apiAddWorker, updateWorker as apiUpdateWorker } from '@/api/worker';
import { EventSourcePolyfill } from 'event-source-polyfill';
import { useAuthStore } from '@/store/useAuthStore';

// 认证状态
const authStore = useAuthStore();

// SSE连接
let eventSource = null;

// 枚举常量
const WorkerStatus = {
  FREE: 0,  // 空闲
  BUSY: 1   // 忙碌
};

const Gender = {
  MALE: 0,  // 男
  FEMALE: 1 // 女
};

// 导入共享布局组件
import DashboardLayout from '@/components/layout/DashboardLayout.vue';

// ===== 状态管理 =====
// 列表数据和分页状态
const state = reactive({
  workers: [],
  searchQuery: '',
  genderFilter: '',
  currentPage: 1,
  pageSize: 5,
  totalWorkers: 0,
  loading: false
});

// 对话框状态
const dialogState = reactive({
  visible: false,
  title: '添加维修人员',
  isEditing: false,
  form: {
  workerId: '',
  name: '',
    gender: Gender.MALE,
  email: '',
    status: WorkerStatus.FREE
  }
});

// 表单引用和验证规则
const workerFormRef = ref(null);
const workerRules = {
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  email: [
    { required: true, message: '请输入邮箱地址', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' }
  ],
  gender: [{ required: true, message: '请选择性别', trigger: 'change' }]
};

// ===== 工具函数 =====
// 数据解析工具
const dataParser = {
  // 解析性别数据
  parseGender(gender) {
    if (gender === null || gender === undefined) return Gender.MALE;
    
    if (typeof gender === 'number') return gender;
    if (typeof gender === 'object' && gender !== null && 'code' in gender) return gender.code;
    if (typeof gender === 'string') return gender === 'MALE' ? Gender.MALE : Gender.FEMALE;
    
    return Gender.MALE;
  },
  
  // 解析状态数据
  parseStatus(status) {
    if (status === null || status === undefined) return WorkerStatus.FREE;
    
    if (typeof status === 'number') return status;
    if (typeof status === 'object' && status !== null && 'code' in status) return status.code;
    if (typeof status === 'string') return status === 'FREE' ? WorkerStatus.FREE : WorkerStatus.BUSY;
    
    return WorkerStatus.FREE;
  },
  
  // 获取性别显示文本
  getGenderText(gender) {
    return dataParser.parseGender(gender) === Gender.MALE ? '男' : '女';
  },
  
  // 获取状态显示文本
  getStatusText(status) {
    return dataParser.parseStatus(status) === WorkerStatus.FREE ? '空闲' : '忙碌';
  }
};

// ===== API交互函数 =====
// 获取维修人员列表
const fetchWorkers = async () => {
  state.loading = true;
  try {
    const response = await getWorkerList(
      state.currentPage, 
      state.pageSize, 
      state.searchQuery, 
      state.genderFilter === '' ? null : state.genderFilter
    );
    
    if (response.status === 200) {
      const pageInfo = response.data;
      if (pageInfo && pageInfo.list) {
        // 调试信息
        if (process.env.NODE_ENV === 'development' && pageInfo.list.length > 0) {
          console.log('维修人员数据示例:', JSON.stringify(pageInfo.list[0], null, 2));
        }
        
        state.workers = pageInfo.list;
        state.totalWorkers = pageInfo.total;
      } else if (pageInfo) {
        // 尝试处理不同的数据结构
        if (Array.isArray(pageInfo)) {
          state.workers = pageInfo;
          state.totalWorkers = pageInfo.length;
        } else {
          console.error('返回的分页数据格式不正确:', pageInfo);
          ElMessage.error('获取维修人员列表数据格式不正确');
          state.workers = [];
          state.totalWorkers = 0;
        }
      } else {
        ElMessage.error('未接收到有效数据');
        state.workers = [];
        state.totalWorkers = 0;
      }
    } else {
      ElMessage.error(`获取维修人员列表失败：${response.message || '未知错误'}`);
      state.workers = [];
      state.totalWorkers = 0;
    }
  } catch (error) {
    handleApiError(error, '获取维修人员列表失败');
    state.workers = [];
    state.totalWorkers = 0;
  } finally {
    state.loading = false;
  }
};

// 通用错误处理函数
const handleApiError = (error, prefix = '操作失败') => {
  console.error(`${prefix}:`, error);
  // 避免重复显示频率限制错误
  if (error.message !== 'REQUEST_TOO_FREQUENT') {
    ElMessage.error(`${prefix}: ${error.message || '请稍后再试'}`);
  }
};

// ===== 事件处理函数 =====
// 处理搜索
const handleSearch = () => {
  if (process.env.NODE_ENV === 'development') {
    console.log('执行搜索，搜索条件:', {
      关键词: state.searchQuery,
      性别过滤: state.genderFilter,
      页码: state.currentPage,
      页大小: state.pageSize
    });
  }
  // 搜索时需要重置到第一页
  state.currentPage = 1;
  fetchWorkers();
};

// 处理页面大小变化
const handleSizeChange = (val) => {
  state.pageSize = val;
  fetchWorkers();
};

// 处理页码变化
const handleCurrentChange = (val) => {
  state.currentPage = val;
  fetchWorkers();
};

// 显示添加维修人员对话框
const showAddWorkerDialog = () => {
  resetForm();
  dialogState.title = '添加维修人员';
  dialogState.visible = true;
  dialogState.isEditing = false;
};

// 编辑维修人员
const editWorker = (worker) => {
  if (process.env.NODE_ENV === 'development') {
    console.log('编辑维修人员原始数据:', JSON.stringify(worker, null, 2));
  }
  
  resetForm();
  
  // 复制并处理数据
  const processedWorker = {
    workerId: worker.workerId,
    name: worker.name,
    // 性别在编辑模式下不可修改，但仍需设置
    gender: dataParser.parseGender(worker.gender),
    email: worker.email,
    status: dataParser.parseStatus(worker.status)
  };
  
  // 将处理后的数据赋值给表单
  Object.assign(dialogState.form, processedWorker);
  
  if (process.env.NODE_ENV === 'development') {
    console.log('处理后的表单数据:', JSON.stringify(dialogState.form, null, 2));
  }
  
  dialogState.title = '编辑维修人员';
  dialogState.visible = true;
  dialogState.isEditing = true;
};

// 删除维修人员
const deleteWorker = (workerId) => {
  ElMessageBox.confirm(
    '确定要删除该维修人员吗？此操作不可撤销。',
    '删除确认',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    }
  )
    .then(async () => {
      state.loading = true;
      try {
        const response = await apiDeleteWorker(workerId);
        
        if (response.status === 200) {
          ElMessage.success('维修人员已成功删除');
          
          // 如果当前页只有一条数据，且不是第一页，则删除后返回上一页
          if (state.workers.length === 1 && state.currentPage > 1) {
            state.currentPage--;
          }
          fetchWorkers(); // 刷新当前页或上一页数据
        } else {
          ElMessage.error(`删除维修人员失败：${response.message || '未知错误'}`);
        }
      } catch (error) {
        handleApiError(error, '删除维修人员失败');
      } finally {
        state.loading = false;
      }
    })
    .catch(() => {
      ElMessage.info('已取消删除');
    });
};

// 提交维修人员表单
const submitWorkerForm = () => {
  if (!workerFormRef.value) return;
  
  workerFormRef.value.validate(async (valid) => {
    if (!valid) return false;
    
    state.loading = true;
      try {
      // 创建提交数据的副本
      const formData = { ...dialogState.form };
      
      if (process.env.NODE_ENV === 'development') {
        console.log('提交的表单数据:', JSON.stringify(formData, null, 2));
      }
      
      if (dialogState.isEditing) {
        // 使用update API更新维修人员
        try {
          // 确保包含workerId参数
          if (!formData.workerId) {
            ElMessage.error('缺少维修人员ID，无法更新');
            return;
          }
          
          if (process.env.NODE_ENV === 'development') {
            console.log('更新维修人员，ID:', formData.workerId);
          }
          
          const updateResponse = await apiUpdateWorker(formData);
          
          if (updateResponse.status === 200) {
            ElMessage.success('维修人员更新成功');
            fetchWorkers(); // 刷新列表，保持在当前页
            dialogState.visible = false;
          } else {
            ElMessage.error(`更新维修人员失败：${updateResponse.message || '未知错误'}`);
          }
        } catch (error) {
          handleApiError(error, '更新维修人员失败');
          }
        } else {
          // 添加新维修人员
        try {
          const response = await apiAddWorker(formData);
          
          if (response.status === 200) {
          ElMessage.success('维修人员添加成功');
            // 如果当前不是第一页且该页已满，添加后应该显示最后一页
            const lastPage = Math.ceil((state.totalWorkers + 1) / state.pageSize);
            if (lastPage > state.currentPage) {
              state.currentPage = lastPage;
            }
            fetchWorkers(); // 刷新当前页数据
            dialogState.visible = false;
          } else {
            ElMessage.error(`添加维修人员失败：${response.message || '未知错误'}`);
          }
        } catch (error) {
          handleApiError(error, '添加维修人员失败');
        }
      }
      } finally {
      state.loading = false;
    }
  });
};

// 重置表单
const resetForm = () => {
  if (workerFormRef.value) {
    workerFormRef.value.resetFields();
  }
  
  // 重置表单为默认值
  Object.assign(dialogState.form, {
    workerId: '',
    name: '',
    gender: Gender.MALE,
    email: '',
    status: WorkerStatus.FREE
  });
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
    console.log('正在建立维修人员列表SSE连接...');
    
    // 使用EventSourcePolyfill，支持添加自定义头部
    eventSource = new EventSourcePolyfill('http://localhost:8080/sse/subscribe', {
      headers: {
        'Authorization': `Bearer ${token}`
      },
      heartbeatTimeout: 60000, // 增加心跳超时时间到60秒
      connectionTimeout: 30000, // 连接超时30秒
      reconnectInterval: 5000 // 重连间隔5秒
    });
    
    // 连接建立时的处理
    eventSource.onopen = () => {
      console.log('维修人员列表SSE连接已建立');
    };
    
    // 接收到消息时的处理
    eventSource.addEventListener(userId.toString(), (event) => {
      // 当消息为维修人员更新时，重新获取数据
      if (event.data === 'worker-update') {
        console.log('收到维修人员更新消息，正在重新获取维修人员信息...');
        fetchWorkers(); // 使用当前的搜索条件和分页重新获取数据
      }
    });
    
    // 接收心跳消息
    eventSource.addEventListener('heartbeat', (event) => {
      console.log('维修人员列表收到心跳消息:', event.data);
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

// 页面加载时获取维修人员列表
onMounted(() => {
  fetchWorkers();
  setupSSEConnection(); // 建立SSE连接
});

// 组件卸载前的清理工作
onBeforeUnmount(() => {
  closeSSEConnection(); // 关闭SSE连接
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
  width: 320px;
}

.filter-select {
  width: 150px;
}

.worker-table-card {
  border-radius: 14px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.07);
  margin-bottom: 30px;
  border: none;
  overflow: hidden;
  transition: all 0.3s ease;
}

.worker-table-card:hover {
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

.edit-btn {
  background: linear-gradient(145deg, #3b82f6, #60a5fa) !important;
  color: white !important;
}

.delete-btn {
  background: linear-gradient(145deg, #ef4444, #f87171) !important;
  color: white !important;
}

.search-btn {
  background: linear-gradient(145deg, #0ea5e9, #38bdf8) !important;
  color: white !important;
}

.add-btn {
  background: linear-gradient(145deg, #10b981, #34d399) !important;
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

/* 搜索区域和按钮美化 */
.filter-container .el-button {
  transition: all 0.3s ease;
  border-radius: 6px;
  font-weight: 500;
  letter-spacing: 0.5px;
  height: 40px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.08);
}

.filter-container .el-button:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.15);
}

.filter-container .el-button:active {
  transform: translateY(0);
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.1);
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
:deep(.worker-dialog) {
  display: flex;
  align-items: center;
  justify-content: center;
}

:deep(.worker-dialog .el-dialog) {
  margin: 0 auto !important;
  position: relative;
  border-radius: 12px;
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.1), 0 2px 6px rgba(0, 0, 0, 0.08);
  overflow: hidden;
}

:deep(.worker-dialog .el-dialog__header) {
  padding: 20px 24px;
  margin: 0;
  border-bottom: 1px solid #f1f5f9;
  background: #f8fafc;
}

:deep(.worker-dialog .el-dialog__title) {
  font-weight: 600;
  font-size: 18px;
  color: #1e293b;
}

:deep(.worker-dialog .el-dialog__body) {
  padding: 24px;
}

:deep(.worker-dialog .el-dialog__footer) {
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
</style> 