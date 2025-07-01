<template>
  <div class="maintenance-page">
    <div class="maintenance-upload-container">
      <el-card class="upload-card">
        <template #header>
          <div class="card-header">
            <div class="header-left">
              <h2 class="upload-title">
                <el-icon class="logo-icon" :size="24">
                  <Lightning />
                </el-icon>
                维修记录上传
              </h2>
              <p class="upload-subtitle">请填写设备的维修详情</p>
            </div>
            <el-tag v-if="recordId" type="primary" size="large" class="record-id-tag">记录ID: {{ recordId }}</el-tag>
          </div>
        </template>

        <el-form ref="uploadFormRef" :model="formData" :rules="rules" label-width="100px" label-position="left"
          class="upload-form" v-loading="loading">
          <!-- 设备信息 -->
          <div class="form-section">
            <el-divider content-position="left">
              <div class="divider-content">
                <el-icon>
                  <Monitor />
                </el-icon>
                <span>设备信息</span>
              </div>
            </el-divider>
            <el-row :gutter="20">
              <el-col :span="12">
                <el-form-item label="设备ID" prop="deviceId">
                  <el-input v-model="formData.deviceId" disabled placeholder="设备ID会自动获取" prefix-icon="Setting" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="维修人员" prop="workerName">
                  <el-input v-model="formData.workerName" disabled placeholder="维修人员信息会自动获取" prefix-icon="User" />
                </el-form-item>
              </el-col>
            </el-row>
          </div>

          <!-- 故障信息 -->
          <div class="form-section">
            <el-divider content-position="left">
              <div class="divider-content">
                <el-icon>
                  <Warning />
                </el-icon>
                <span>故障信息</span>
              </div>
            </el-divider>
            <el-form-item label="故障描述" prop="faultDescription">
              <el-input v-model="formData.faultDescription" type="textarea" :rows="4" placeholder="请详细描述设备故障情况..."
                class="custom-textarea" />
            </el-form-item>
          </div>

          <!-- 维修信息 -->
          <div class="form-section">
            <el-divider content-position="left">
              <div class="divider-content">
                <el-icon>
                  <Tools />
                </el-icon>
                <span>维修信息</span>
              </div>
            </el-divider>
            <el-form-item label="维修过程" prop="maintenanceProcess">
              <el-input v-model="formData.maintenanceProcess" type="textarea" :rows="6" placeholder="请详细描述维修步骤和过程..."
                class="custom-textarea" />
            </el-form-item>
          </div>

          <!-- 提交按钮 -->
          <div class="form-actions">
            <el-button type="primary" @click="submitForm" class="submit-button">
              <el-icon>
                <Check />
              </el-icon>
              提交维修记录
            </el-button>
          </div>
        </el-form>
      </el-card>

      <div class="page-footer">
        <p>© 2025 智能接地线在线监测 - 维修记录管理</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, reactive } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import {
  Check,
  Warning,
  Tools,
  Monitor,
  User,
  Setting,
  Lightning
} from '@element-plus/icons-vue';
import { getMaintenanceRecordById, uploadMaintenanceRecord } from '@/api/maintenanceRecords';

const route = useRoute();
const router = useRouter();
const recordId = ref('');
const loading = ref(false);
const uploadFormRef = ref(null);

// 表单数据
const formData = reactive({
  deviceId: '',
  workerName: '',
  faultDescription: '',
  maintenanceProcess: ''
});

// 表单验证规则
const rules = {
  faultDescription: [
    { required: true, message: '请输入故障描述', trigger: 'blur' },
    { min: 1, max: 255, message: '故障描述长度在1到255个字符之间', trigger: 'blur' }
  ],
  maintenanceProcess: [
    { required: true, message: '请输入维修过程', trigger: 'blur' },
    { min: 1, max: 255, message: '维修过程长度在1到255个字符之间', trigger: 'blur' }
  ]
};

// 从路由参数获取记录ID并加载数据
onMounted(async () => {
  // 获取路由参数中的记录ID
  if (route.params.id) {
    recordId.value = route.params.id;
    console.log('recordId', recordId.value)
    await fetchMaintenanceRecord(recordId.value);
  } else {
    ElMessage({
      message: '维修记录ID不存在，请返回重试',
      type: 'error',
      duration: 2000
    })
    setTimeout(() => {
      window.close(); // 关闭当前窗口
    }, 2000);
  }
});

// 获取维修记录详情
const fetchMaintenanceRecord = async (id) => {
  if (!id) {
    ElMessage.error('维修记录ID无效');
    router.push('/404');
    return;
  }

  loading.value = true;
  try {
    // 调用API获取维修记录详情
    const res = await getMaintenanceRecordById(id);

    // 检查响应状态
    if (res.status !== 200) {
      throw new Error(res.message || '获取维修记录详情失败');
    }

    // 处理获取到的数据
    if (res.data) {
      const record = res.data;
      // 设置设备信息
      formData.deviceId = record.deviceId;

      // 处理维修人员信息
      if (record.worker && record.worker.name) {
        formData.workerName = record.worker.name;
      } else if (record.workerName) {
        formData.workerName = record.workerName;
      } else {
        formData.workerName = '未分配';
      }

      // 如果记录中有故障描述，在表单中预填
      if (record.description) {
        formData.faultDescription = record.description;
      }

      // 如果记录中有维修过程，在表单中预填
      if (record.process) {
        formData.maintenanceProcess = record.process;
      }

      console.log('成功获取维修记录:', record);
    } else {
      // 未找到数据，跳转到404页面
      ElMessage({
        message: '未找到维修记录',
        type: 'error',
        duration: 2000
      });
      router.push('/404');
    }
  } catch (error) {
    console.error('获取维修记录详情失败:', error);
    ElMessage({
      message: `获取维修记录详情失败: ${error.message || '请稍后重试'}`,
      type: 'error',
      duration: 3000
    });
    // 出错时也跳转到404页面
    setTimeout(() => {
      router.push('/404');
    }, 1500);
  } finally {
    loading.value = false;
  }
};

// 提交表单
const submitForm = () => {
  uploadFormRef.value.validate(async (valid) => {
    if (!valid) {
      ElMessage({
        message: '表单填写不完整，请检查',
        type: 'warning',
        duration: 2000
      });
      return;
    }

    // 确认提交
    ElMessageBox.confirm(
      '确定要提交维修记录吗？提交后无法修改',
      '提交确认',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    ).then(async () => {
      loading.value = true;

      try {
        // 验证ID
        const id = parseInt(recordId.value);
        if (isNaN(id) || id <= 0) {
          throw new Error('无效的维修记录ID');
        }

        // 准备上传数据
        const recordData = {
          id: id,
          description: formData.faultDescription,
          process: formData.maintenanceProcess
        };

        // 调用API上传维修记录
        const res = await uploadMaintenanceRecord(recordData);

        // 检查响应状态
        if (res.status !== 200) {
          throw new Error(res.message || '上传失败');
        }

        // 显示成功消息
        ElMessage({
          message: '维修记录上传成功',
          type: 'success',
          duration: 2000
        });

        // 延迟后跳转上传成功页面
        setTimeout(() => {
          router.push('/maintenance-success');
        }, 1500);

      } catch (error) {
        console.error('维修记录上传失败:', error);
        ElMessage({
          message: `维修记录上传失败: ${error.message || '请稍后重试'}`,
          type: 'error',
          duration: 3000
        });
      } finally {
        loading.value = false;
      }
    }).catch(() => {
      // 用户取消了提交，不做任何处理
    });
  });
};

// 返回上一页
const goBack = () => {
  window.close(); // 关闭当前窗口
};
</script>

<style scoped>
.maintenance-page {
  min-height: 100vh;
  background: linear-gradient(135deg, #f0f9ff 0%, #e6f7ff 100%);
  display: flex;
  flex-direction: column;
  align-items: center;
  padding-bottom: 30px;
}

.logo-icon {
  color: #1e40af;
  margin-right: 8px;
  filter: drop-shadow(0 2px 3px rgba(0, 0, 0, 0.1));
  vertical-align: middle;
}

.maintenance-upload-container {
  width: 100%;
  max-width: 900px;
  margin: 0 auto;
  padding: 0 20px;
}

.upload-card {
  border-radius: 16px;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.08), 0 6px 12px rgba(0, 0, 0, 0.05);
  overflow: hidden;
  border: none;
  background: #ffffff;
  margin-bottom: 30px;
  transition: all 0.3s ease;
}

.upload-card:hover {
  box-shadow: 0 14px 40px rgba(0, 0, 0, 0.12), 0 10px 20px rgba(0, 0, 0, 0.08);
  transform: translateY(-5px);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 25px;
  border-bottom: 1px solid #edf2f7;
  background: #f8fafc;
  position: relative;
}

.header-left {
  display: flex;
  flex-direction: column;
}

.upload-title {
  margin: 0;
  font-size: 2rem;
  color: #1e293b;
  font-weight: 600;
  background: linear-gradient(90deg, #1e40af, #3b82f6);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
  letter-spacing: 0.5px;
}

.upload-subtitle {
  margin: 6px 0 0 0;
  font-size: 1rem;
  color: #64748b;
}

.record-id-tag {
  font-size: 1rem !important;
  padding: 10px 16px !important;
  height: auto !important;
  font-weight: 600 !important;
  border: none !important;
  background: linear-gradient(45deg, #1e40af, #3b82f6) !important;
  box-shadow: 0 4px 10px rgba(59, 130, 246, 0.3) !important;
  letter-spacing: 0.5px;
}

.upload-form {
  padding: 32px 25px;
}

.form-section {
  margin-bottom: 35px;
  background-color: #fafafa;
  border-radius: 12px;
  padding: 25px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  transition: all 0.3s ease;
  border: 1px solid rgba(226, 232, 240, 0.6);
}

.form-section:hover {
  background-color: #f8fafc;
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.06);
  transform: translateY(-3px);
}

.divider-content {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 1.25rem;
  font-weight: 600;
  color: #1e40af;
  padding: 0 10px;
}

.divider-content .el-icon {
  font-size: 1.6rem;
  color: #3b82f6;
  background-color: transparent;
  padding: 8px;
  border-radius: 10px;
  box-shadow: none;
  display: flex;
  align-items: center;
  justify-content: center;
  min-width: 38px;
  min-height: 38px;
}

.custom-textarea {
  font-family: 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', sans-serif;
  border-radius: 10px;
  margin-top: 5px;
}

.custom-textarea :deep(.el-textarea__inner) {
  padding: 15px;
  line-height: 1.6;
  resize: none;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05) inset;
  transition: all 0.3s ease;
  border: 1px solid #e2e8f0;
  font-size: 15px;
}

.custom-textarea :deep(.el-textarea__inner:focus) {
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.25);
  border-color: #60a5fa;
}

.form-actions {
  display: flex;
  justify-content: center;
  margin-top: 50px;
}

.back-button,
.submit-button {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 14px 28px;
  font-size: 16px;
  font-weight: 500;
  border-radius: 10px;
  transition: all 0.3s ease;
  letter-spacing: 0.5px;
}

.back-button {
  border: 1px solid #e2e8f0;
  color: #64748b;
}

.back-button:hover {
  background-color: #f8fafc;
  color: #334155;
  border-color: #cbd5e1;
  transform: translateY(-2px);
}

.submit-button {
  background: linear-gradient(45deg, #1e40af, #3b82f6);
  border: none;
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.25);
}

.submit-button:hover {
  transform: translateY(-3px);
  box-shadow: 0 6px 16px rgba(59, 130, 246, 0.4);
  background: linear-gradient(45deg, #1e3a8a, #3b82f6);
}

.page-footer {
  width: 100%;
  text-align: center;
  color: #64748b;
  padding: 20px 0;
  font-size: 0.9rem;
  margin-top: auto;
  border-top: 1px solid rgba(226, 232, 240, 0.5);
  background-color: rgba(255, 255, 255, 0.5);
}

/* 响应式调整 */
@media (max-width: 768px) {
  .maintenance-upload-container {
    padding: 0 15px 30px;
  }

  .el-form-item {
    margin-bottom: 18px;
  }

  .form-section {
    padding: 15px;
    margin-bottom: 20px;
  }

  .upload-title {
    font-size: 1.5rem;
  }

  .back-button,
  .submit-button {
    padding: 10px 16px;
  }
}
</style>