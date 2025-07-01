<template>
  <div>
    <!-- 用户信息下拉菜单 -->
    <el-dropdown trigger="click" @command="handleCommand">
      <div class="user-info">
        <div class="avatar-container">
          <img :src="authStore.avatar" alt="用户头像" class="custom-avatar" />
        </div>
        <span class="admin-name">{{ authStore.userName }}</span>
        <el-icon>
          <ArrowDown />
        </el-icon>
      </div>
      <template #dropdown>
        <el-dropdown-menu>
          <el-dropdown-item command="profile">个人信息</el-dropdown-item>
          <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
        </el-dropdown-menu>
      </template>
    </el-dropdown>

    <!-- 用户信息对话框 -->
    <el-dialog v-model="userProfileVisible" title="个人信息" width="500px" :append-to-body="true" :lock-scroll="true" center
      destroy-on-close class="user-profile-dialog">
      <div class="user-profile-content">
        <div class="user-info-header">
          <div class="user-avatar-large">
            <img :src="authStore.avatar" alt="用户头像" />
          </div>
          <div class="user-base-info">
            <h3>{{ authStore.userName }}</h3>
          </div>
        </div>

        <el-tabs class="centered-tabs">
          <el-tab-pane label="账号信息">
            <div class="user-info-list">
              <div class="info-item">
                <span class="info-label">用户ID:</span>
                <span class="info-value">{{ authStore.userId }}</span>
              </div>
              <div class="info-item">
                <span class="info-label">用户名:</span>
                <span class="info-value">{{ authStore.userName }}</span>
              </div>
              <div class="info-item">
                <span class="info-label">邮箱:</span>
                <span class="info-value">{{ authStore.email }}</span>
              </div>
            </div>
          </el-tab-pane>

          <el-tab-pane label="修改密码">
            <el-form ref="passwordFormRef" :model="passwordForm" :rules="passwordRules" label-width="100px">
              <el-form-item label="新密码" prop="newPassword">
                <el-input v-model="passwordForm.newPassword" type="password" placeholder="请输入新密码" show-password />
              </el-form-item>

              <el-form-item label="确认密码" prop="confirmPassword">
                <el-input v-model="passwordForm.confirmPassword" type="password" placeholder="请再次输入新密码" show-password />
              </el-form-item>

              <el-form-item label="验证码" prop="verifyCode">
                <div class="verify-code-container">
                  <el-input v-model="passwordForm.verifyCode" placeholder="请输入验证码" maxlength="6" show-word-limit />
                  <el-button type="primary" :disabled="codeBtnDisabled" :loading="isSending" @click="sendVerifyCode">
                    {{ codeButtonText }}
                  </el-button>
                </div>
                <div class="form-tip" v-if="countdown > 0">
                  <span>验证码已发送至 {{ authStore.email }}, 有效期为60秒</span>
                </div>
              </el-form-item>

              <el-form-item>
                <el-button type="primary" @click="submitPasswordChange">确认修改</el-button>
                <el-button @click="resetPasswordForm">重置</el-button>
              </el-form-item>
            </el-form>
          </el-tab-pane>
        </el-tabs>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onBeforeUnmount } from 'vue';
import { useRouter } from 'vue-router';
import { ArrowDown } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import { useAuthStore } from '@/store/useAuthStore';
import { sendVerifyCodeDuringResetPassword, resetPassword } from '@/api/auth';

const authStore = useAuthStore();
const router = useRouter();

// 用户信息对话框
const userProfileVisible = ref(false);
const passwordForm = ref({
  newPassword: '',
  confirmPassword: '',
  verifyCode: ''
});
const passwordFormRef = ref(null);
const passwordRules = {
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于6个字符', trigger: 'blur' },
    { max: 20, message: '密码长度不能超过20个字符', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== passwordForm.value.newPassword) {
          callback(new Error('两次输入的密码不一致'));
        } else {
          callback();
        }
      },
      trigger: 'blur'
    }
  ],
  verifyCode: [
    { required: true, message: '请输入验证码', trigger: 'blur' },
    { pattern: /^\d{6}$/, message: '验证码为6位数字', trigger: 'blur' }
  ]
};

// 发送验证码相关
const isSending = ref(false);
const countdown = ref(0);
const codeBtnDisabled = computed(() => isSending.value || countdown.value > 0);
const codeButtonText = computed(() => {
  if (countdown.value > 0) {
    return `${countdown.value}秒后重新获取`;
  }
  return '获取验证码';
});
let countdownTimer = null;

// 发送验证码
const sendVerifyCode = async () => {
  if (codeBtnDisabled.value) return;
  try {
    isSending.value = true;
    // 调用API发送验证码到用户邮箱
    await sendVerifyCodeDuringResetPassword();
    ElMessage.success('验证码已发送，请查收邮箱');

    // 清除已有定时器
    if (countdownTimer) {
      clearInterval(countdownTimer);
    }

    // 启动倒计时
    countdown.value = 60;
    countdownTimer = setInterval(() => {
      countdown.value--;
      if (countdown.value <= 0) {
        clearInterval(countdownTimer);
        countdownTimer = null;
      }
    }, 1000);
  } catch (error) {
    console.error('发送验证码失败:', error);
    ElMessage.error('发送验证码失败，请稍后重试');
  } finally {
    isSending.value = false;
  }
};

// 提交密码修改
const submitPasswordChange = async () => {
  if (!passwordFormRef.value) return;

  await passwordFormRef.value.validate(async (valid) => {
    if (valid) {
      try {
        // 调用API修改密码
        await resetPassword({
          verifyCode: passwordForm.value.verifyCode,
          password: passwordForm.value.newPassword
        });

        ElMessage.success('密码修改成功，请重新登录');
        userProfileVisible.value = false;
        resetPasswordForm();

        await new Promise(resolve => setTimeout(resolve, 1000));

        // 退出登录
        authStore.logout();
        router.push('/auth');
      } catch (error) {
        console.error('修改密码失败:', error);
        ElMessage.error('修改密码失败: ' + (error.message || '请稍后重试'));
      }
    }
  });
};

// 重置密码表单
const resetPasswordForm = () => {
  if (passwordFormRef.value) {
    passwordFormRef.value.resetFields();
  }
  passwordForm.value = {
    newPassword: '',
    confirmPassword: '',
    verifyCode: ''
  };
};

// 处理命令
const handleCommand = (command) => {
  if (command === 'logout') {
    authStore.logout();
    ElMessage({
      message: '退出成功',
      type: 'success',
      duration: 2000
    })
    router.push('/auth');

  } else if (command === 'profile') {
    userProfileVisible.value = true;
  }
};

// 组件卸载时清理定时器
onBeforeUnmount(() => {
  if (countdownTimer) {
    clearInterval(countdownTimer);
    countdownTimer = null;
  }
});
</script>

<style scoped>
.user-info {
  display: flex;
  align-items: center;
  cursor: pointer;
  padding: 6px 12px;
  border-radius: 20px;
  background-color: #f8fafc;
  transition: all 0.3s ease;
}

.user-info:hover {
  background-color: #e0f2fe;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.08);
}

.avatar-container {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
}

.custom-avatar {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.admin-name {
  font-size: 14px;
  color: #1e293b;
  margin: 0 6px 0 8px;
  font-weight: 500;
}

/* 用户信息对话框样式 */
:deep(.user-profile-dialog) {
  display: flex;
  align-items: center;
  justify-content: center;
}

:deep(.user-profile-dialog .el-dialog) {
  margin: 0 auto !important;
  position: relative;
  border-radius: 12px;
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.1), 0 2px 6px rgba(0, 0, 0, 0.08);
  overflow: hidden;
}

:deep(.user-profile-dialog .el-dialog__header) {
  padding: 20px 24px;
  margin: 0;
  border-bottom: 1px solid #f1f5f9;
  background: #f8fafc;
}

:deep(.user-profile-dialog .el-dialog__title) {
  font-weight: 600;
  font-size: 18px;
  color: #1e293b;
}

:deep(.user-profile-dialog .el-dialog__body) {
  padding: 0;
}

.user-profile-content {
  padding: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  width: 100%;
}

.user-info-header {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 30px 0;
  background: #f8fafc;
  color: #1e293b;
  text-align: center;
  border-bottom: 1px solid #e2e8f0;
  width: 100%;
  margin-bottom: 20px;
  padding-bottom: 15px;
}

.user-avatar-large {
  width: 100px;
  height: 100px;
  border-radius: 50%;
  overflow: hidden;
  border: 4px solid white;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  margin-bottom: 16px;
}

.user-avatar-large img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.user-base-info {
  margin: 0;
  text-align: center;
}

.user-base-info h3 {
  margin: 0 0 6px 0;
  font-size: 22px;
  font-weight: 600;
  color: #1e293b;
}

.user-base-info p {
  margin: 0;
  font-size: 14px;
  color: #64748b;
}

.centered-tabs {
  display: flex;
  flex-direction: column;
  align-items: center;
  width: 100%;
}

.centered-tabs :deep(.el-tabs__header) {
  margin-bottom: 20px;
  width: 100%;
  order: -1;
}

.centered-tabs :deep(.el-tabs__content) {
  width: 100%;
  order: 2;
}

.centered-tabs :deep(.el-tabs__nav) {
  display: flex;
  justify-content: center;
  width: 100%;
}

.user-info-list {
  padding: 24px 10px;
  max-width: 300px;
  margin: 0 auto;
  text-align: left;
  width: 80%;
}

.info-item {
  padding: 16px 0;
  margin-bottom: 10px;
  display: flex;
  font-size: 15px;
  border-bottom: 1px solid #f0f0f0;
}

.info-item:last-child {
  margin-bottom: 0;
  border-bottom: none;
}

.info-label {
  color: #3b82f6;
  font-weight: 600;
  font-size: 15px;
  width: 100px;
}

.info-value {
  flex: 1;
  color: #1e293b;
  font-weight: 500;
  margin-left: 20px;
  font-size: 15px;
}

.user-profile-dialog :deep(.el-dialog__body) {
  padding: 20px;
}

.user-profile-dialog :deep(.el-dialog__header) {
  padding: 15px 20px;
  margin-right: 0;
  text-align: center;
  border-bottom: 1px solid #eee;
}

.user-profile-dialog :deep(.el-dialog__headerbtn) {
  top: 15px;
}

.user-profile-dialog :deep(.el-dialog__title) {
  font-size: 18px;
  font-weight: 600;
}

.form-tip {
  font-size: 12px;
  color: #909399;
  line-height: 1.4;
  margin-top: 6px;
  display: flex;
  align-items: center;
}

.verify-code-container {
  display: flex;
}

.verify-code-container .el-input {
  flex: 1;
  margin-right: 10px;
}

.verify-code-container .el-button {
  min-width: 120px;
}

:deep(.el-form .el-form-item) {
  margin-bottom: 20px;
}

:deep(.el-input__wrapper) {
  box-shadow: 0 0 0 1px #e2e8f0 inset;
  border-radius: 8px;
  transition: all 0.3s ease;
}

:deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px #cbd5e1 inset;
}

:deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px #3b82f6 inset !important;
}

:deep(.el-form-item__label) {
  color: #64748b;
  font-weight: 500;
}

:deep(.el-button) {
  border-radius: 8px;
  font-weight: 500;
  transition: all 0.3s ease;
  padding: 10px 20px;
}

:deep(.el-button--primary) {
  background: #3b82f6;
  border-color: #3b82f6;
}

:deep(.el-button--primary:hover) {
  background: #2563eb;
  border-color: #2563eb;
  transform: translateY(-2px);
  box-shadow: 0 4px 6px rgba(37, 99, 235, 0.2);
}

:deep(.el-button--default:hover) {
  transform: translateY(-2px);
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
}

/* 修改密码卡片样式 */
.centered-tabs .el-tab-pane {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 320px;
  background: #f9fafb;
  border-radius: 16px;
  box-shadow: 0 4px 24px rgba(60, 100, 180, 0.08);
  padding: 32px 24px;
  margin: 0 auto;
  max-width: 400px;
}

/* 表单项间距 */
:deep(.el-form .el-form-item) {
  margin-bottom: 28px;
}

/* 输入框美化 */
:deep(.el-input__wrapper) {
  border-radius: 10px;
  box-shadow: 0 0 0 1.5px #dbeafe inset;
  background: #fff;
}

/* 标签美化 */
:deep(.el-form-item__label) {
  color: #3b82f6;
  font-weight: 600;
  font-size: 15px;
}

/* 验证码区域美化 */
.verify-code-container {
  display: flex;
  align-items: center;
  gap: 12px;
}

.verify-code-container .el-input {
  flex: 1;
}

.verify-code-container .el-button {
  min-width: 110px;
  border-radius: 10px;
  font-weight: 600;
  background: linear-gradient(90deg, #3b82f6 0%, #60a5fa 100%);
  border: none;
  color: #fff;
  box-shadow: 0 2px 8px rgba(59, 130, 246, 0.12);
}

.verify-code-container .el-button:disabled {
  background: #cbd5e1;
  color: #fff;
}

/* 验证码提示美化 */
.form-tip {
  background: #f1f5f9;
  color: #2563eb;
  border-radius: 6px;
  padding: 6px 12px;
  margin-top: 8px;
  font-size: 13px;
}

/* 按钮美化 */
:deep(.el-button--primary) {
  border-radius: 10px;
  font-weight: 600;
  background: linear-gradient(90deg, #3b82f6 0%, #60a5fa 100%);
  border: none;
  box-shadow: 0 2px 8px rgba(59, 130, 246, 0.12);
}

:deep(.el-button--default) {
  border-radius: 10px;
  font-weight: 600;
  background: #f3f4f6;
  color: #3b82f6;
  border: none;
}

:deep(.el-button--primary:hover) {
  background: linear-gradient(90deg, #2563eb 0%, #3b82f6 100%);
}
</style>