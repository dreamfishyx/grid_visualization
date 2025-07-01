<template>
  <div class="login-container">
    <div class="login-panel" :class="{ 'flip': isSignUpMode }">
      <!-- 登录面板 -->
      <div class="panel-side login-side">
        <div class="panel-content">
          <h1>登录</h1>
          <el-form ref="loginFormRef" :model="loginForm" :rules="loginRules" @submit.prevent="handleLogin" status-icon>
            <el-form-item prop="userName">
              <el-input v-model="loginForm.userName" placeholder="用户名" :prefix-icon="User" />
            </el-form-item>
            <el-form-item prop="password">
              <el-input v-model="loginForm.password" type="password" placeholder="密码" :prefix-icon="Lock"
                @keyup.enter="handleLogin" show-password />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" native-type="submit" class="auth-button" :loading="loginLoading" @click="handleLogin">
                登录
              </el-button>
            </el-form-item>
          </el-form>
          <div class="form-footer">
            <span>还没有账号？</span>
            <el-button type="primary" link @click="isSignUpMode = true">注册</el-button>
          </div>
        </div>
      </div>

      <!-- 注册面板 -->
      <div class="panel-side register-side">
        <div class="panel-content">
          <h1>注册</h1>
          <el-form ref="registerFormRef" :model="registerForm" :rules="registerRules" @submit.prevent="handleRegister"
            status-icon>
            <el-form-item prop="userName">
              <el-input v-model="registerForm.userName" placeholder="用户名" :prefix-icon="User" />
            </el-form-item>
            <el-form-item prop="email">
              <el-input v-model="registerForm.email" placeholder="电子邮箱" :prefix-icon="Message">
                <template #append>
                  <el-button :disabled="isSending || !isEmailValid || countdown > 0" @click="handleSendCode"
                    :loading="isSending" type="primary" class="code-button"
                    :class="{ 'is-disabled': !isEmailValid || countdown > 0 }">
                    {{ codeButtonText }}
                  </el-button>
                </template>
              </el-input>
              <div v-if="registerForm.email && !isEmailValid" class="form-tip">
                <el-icon>
                  <Warning />
                </el-icon>
                <span>邮箱格式不正确，示例：example@domain.com</span>
              </div>
            </el-form-item>
            <el-form-item prop="verifyCode">
              <el-input v-model="registerForm.verifyCode" placeholder="验证码" :prefix-icon="Key" maxlength="6"
                show-word-limit />
              <div class="form-tip" v-if="countdown > 0">
                <span>验证码有效期为60秒，请尽快填写</span>
              </div>
            </el-form-item>
            <el-form-item prop="password">
              <el-input v-model="registerForm.password" type="password" placeholder="密码" :prefix-icon="Lock"
                show-password />
            </el-form-item>
            <el-form-item prop="confirmPassword">
              <el-input v-model="registerForm.confirmPassword" type="password" placeholder="确认密码" :prefix-icon="Lock"
                show-password />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" native-type="submit" class="auth-button" :loading="registerLoading" @click="handleRegister">
                注册
              </el-button>
            </el-form-item>
          </el-form>
          <div class="form-footer">
            <span>已有账号？</span>
            <el-button type="primary" link @click="isSignUpMode = false">登录</el-button>
          </div>
        </div>
      </div>

      <!-- 欢迎面板 -->
      <div class="welcome-panel" :class="{ 'flip': isSignUpMode }">
        <div class="welcome-content">
          <div class="avatar-container">
            <el-icon :size="80" color="#ffffff">
              <Lightning />
            </el-icon>
          </div>
          <h1>WELCOME</h1>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { useAuthStore } from '@/store/useAuthStore';
import { sendVerifyCode } from '@/api/auth';
import { ref, reactive, computed, onBeforeUnmount } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { ElMessage } from 'element-plus';
import { Lightning } from '@element-plus/icons-vue';

const router = useRouter();
const route = useRoute();

// 表单引用
const loginFormRef = ref(null);
const registerFormRef = ref(null);

// 控制显示登录还是注册表单
const isSignUpMode = ref(false);

// 如果URL参数中有mode=register，则切换到注册模式
if (route.query.mode === 'register') {
  isSignUpMode.value = true;
}

// 加载状态
const loginLoading = ref(false);
const registerLoading = ref(false);
const isSending = ref(false);
const countdown = ref(0);
let countdownTimer = null; // 添加计时器引用

// 登录表单数据
const loginForm = reactive({
  userName: '',
  password: ''
});

// 注册表单数据
const registerForm = reactive({
  userName: '',
  email: '',
  verifyCode: '',
  password: '',
  confirmPassword: ''
});

// 邮箱验证
const validateEmail = (email) => {
  if (!email || email.indexOf('@') === -1) return false;

  // 使用通用的邮箱正则表达式
  const regex = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/;
  return regex.test(email);
};

const isEmailValid = computed(() => validateEmail(registerForm.email));


// 验证码按钮文本
const codeButtonText = computed(() => {
  if (countdown.value > 0) {
    return `${countdown.value}秒`;
  }
  return '获取验证码';
});

// 自定义验证规则
const validateConfirmPassword = (rule, value, callback) => {
  if (value === '') {
    callback(new Error('请再次输入密码'));
  } else if (value !== registerForm.password) {
    callback(new Error('两次输入密码不一致'));
  } else {
    callback();
  }
};

const validateUsername = (rule, value, callback) => {
  if (value === '') {
    callback(new Error('请输入用户名'));
  } else if (value.length < 3) {
    callback(new Error('用户名长度至少为3个字符'));
  } else if (value.length > 20) {
    callback(new Error('用户名长度不能超过20个字符'));
  } else {
    callback();
  }
};

const validatePassword = (rule, value, callback) => {
  if (value === '') {
    callback(new Error('请输入密码'));
  } else if (value.length < 6) {
    callback(new Error('密码长度至少为6个字符'));
  } else if (value.length > 20) {
    callback(new Error('密码长度不能超过20个字符'));
  } else {
    callback();
  }
};

const validateEmailInput = (rule, value, callback) => {
  if (value === '') {
    callback(new Error('请输入邮箱'));
  } else if (!validateEmail(value)) {
    callback(new Error('邮箱格式不正确'));
  } else {
    callback();
  }
};

const validateVerifyCode = (rule, value, callback) => {
  if (value === '') {
    callback(new Error('请输入验证码'));
  } else if (!/^\d{6}$/.test(value)) {
    callback(new Error('验证码为6位数字'));
  } else {
    callback();
  }
};

// 登录验证规则
const loginRules = {
  userName: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { validator: validateUsername, trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { validator: validatePassword, trigger: 'blur' }
  ]
};

// 注册验证规则
const registerRules = {
  userName: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { validator: validateUsername, trigger: 'blur' }
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { validator: validateEmailInput, trigger: 'blur' }
  ],
  verifyCode: [
    { required: true, message: '请输入验证码', trigger: 'blur' },
    { validator: validateVerifyCode, trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { validator: validatePassword, trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ]
};

// 登录处理
const handleLogin = async () => {
  try {
    const valid = await loginFormRef.value.validate();
    if (!valid) return;

    loginLoading.value = true;
    const userStore = useAuthStore();

    // 调用登录接口
    await userStore.userLogin({
      userName: loginForm.userName,
      password: loginForm.password
    });

    // 获取用户信息
    // await userStore.userInfo();
    ElMessage({
      message: '登录成功',
      type: 'success',
      duration: 2000
    })
    const redirect = '/dashboard';
    router.push(redirect);
  } finally {
    loginLoading.value = false;
  }
};

// 发送验证码
const handleSendCode = async () => {
  if (!isEmailValid.value) {
    ElMessage({
      message: '请输入有效的邮箱地址',
      type: 'warning',
      duration: 3000
    })
    return;
  }
  try {
    isSending.value = true;
    await sendVerifyCode(registerForm.email);
    ElMessage({
      message: '验证码已发送，请查收邮箱',
      type: 'success',
      duration: 2000
    })

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

  } finally {
    isSending.value = false;
  }
};

// 注册处理 
const handleRegister = async () => {
  try {
    const valid = await registerFormRef.value.validate();
    if (!valid) return;

    registerLoading.value = true;
    const userStore = useAuthStore();

    // 调用注册接口
    await userStore.userRegister({
      userName: registerForm.userName,
      email: registerForm.email,
      verifyCode: registerForm.verifyCode,
      password: registerForm.password
    });

    ElMessage({
      message: '注册成功，请登录',
      type: 'success',
      duration: 2000
    })
    isSignUpMode.value = false; // 切换到登录页
    registerFormRef.value.resetFields(); // 清空表单
  } finally {
    registerLoading.value = false;
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
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
  background: linear-gradient(125deg, #bae6fd, #7dd3fc, #38bdf8);
  position: relative;
  overflow: hidden;
}

.login-container:before {
  content: "";
  position: absolute;
  width: 200%;
  height: 200%;
  top: -50%;
  left: -50%;
  z-index: 0;
  background: url("data:image/svg+xml,%3Csvg width='100' height='100' viewBox='0 0 100 100' xmlns='http://www.w3.org/2000/svg'%3E%3Cpath d='M11 18c3.866 0 7-3.134 7-7s-3.134-7-7-7-7 3.134-7 7 3.134 7 7 7zm48 25c3.866 0 7-3.134 7-7s-3.134-7-7-7-7 3.134-7 7 3.134 7 7 7zm-43-7c1.657 0 3-1.343 3-3s-1.343-3-3-3-3 1.343-3 3 1.343 3 3 3zm63 31c1.657 0 3-1.343 3-3s-1.343-3-3-3-3 1.343-3 3 1.343 3 3 3zM34 90c1.657 0 3-1.343 3-3s-1.343-3-3-3-3 1.343-3 3 1.343 3 3 3zm56-76c1.657 0 3-1.343 3-3s-1.343-3-3-3-3 1.343-3 3 1.343 3 3 3zM12 86c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm28-65c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm23-11c2.76 0 5-2.24 5-5s-2.24-5-5-5-5 2.24-5 5 2.24 5 5 5zm-6 60c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm29 22c2.76 0 5-2.24 5-5s-2.24-5-5-5-5 2.24-5 5 2.24 5 5 5zM32 63c2.76 0 5-2.24 5-5s-2.24-5-5-5-5 2.24-5 5 2.24 5 5 5zm57-13c2.76 0 5-2.24 5-5s-2.24-5-5-5-5 2.24-5 5 2.24 5 5 5zm-9-21c1.105 0 2-.895 2-2s-.895-2-2-2-2 .895-2 2 .895 2 2 2zM60 91c1.105 0 2-.895 2-2s-.895-2-2-2-2 .895-2 2 .895 2 2 2zM35 41c1.105 0 2-.895 2-2s-.895-2-2-2-2 .895-2 2 .895 2 2 2zM12 60c1.105 0 2-.895 2-2s-.895-2-2-2-2 .895-2 2 .895 2 2 2z' fill='%23ffffff' fill-opacity='0.05' fill-rule='evenodd'/%3E%3C/svg%3E"),
    linear-gradient(125deg, #bae6fd, #7dd3fc, #38bdf8);
  animation: gradientAnimation 20s linear infinite;
}

/* 添加浮动粒子效果 */
.login-container:after {
  content: "";
  position: absolute;
  width: 100%;
  height: 100%;
  top: 0;
  left: 0;
  background: url("data:image/svg+xml,%3Csvg width='80' height='80' viewBox='0 0 80 80' xmlns='http://www.w3.org/2000/svg'%3E%3Ccircle cx='10' cy='10' r='5' fill='%23ffffff' fill-opacity='0.1'/%3E%3Ccircle cx='40' cy='20' r='3' fill='%23ffffff' fill-opacity='0.1'/%3E%3Ccircle cx='70' cy='50' r='7' fill='%23ffffff' fill-opacity='0.07'/%3E%3Ccircle cx='30' cy='60' r='4' fill='%23ffffff' fill-opacity='0.1'/%3E%3C/svg%3E");
  animation: floatingParticles 30s linear infinite;
  pointer-events: none;
  z-index: 0;
}

@keyframes floatingParticles {
  0% {
    transform: translateY(0) translateX(0);
  }

  25% {
    transform: translateY(-5px) translateX(5px);
  }

  50% {
    transform: translateY(-10px) translateX(0);
  }

  75% {
    transform: translateY(-5px) translateX(-5px);
  }

  100% {
    transform: translateY(0) translateX(0);
  }
}

@keyframes gradientAnimation {
  0% {
    transform: rotate(0deg);
  }

  100% {
    transform: rotate(360deg);
  }
}

.login-panel {
  display: flex;
  position: relative;
  width: 850px;
  height: 500px;
  border-radius: 24px;
  overflow: hidden;
  box-shadow: 0 25px 50px rgba(0, 0, 0, 0.25),
    0 10px 20px rgba(0, 0, 0, 0.2),
    0 0 0 1px rgba(255, 255, 255, 0.1) inset;
  transition: all 0.6s cubic-bezier(0.22, 1, 0.36, 1);
  background-color: rgba(255, 255, 255, 0.9);
  z-index: 1;
  backdrop-filter: blur(15px);
  transform: perspective(1200px) rotateX(0deg);
  transform-style: preserve-3d;
}

.login-panel:hover {
  box-shadow: 0 30px 60px rgba(0, 0, 0, 0.3),
    0 15px 30px rgba(0, 0, 0, 0.2),
    0 0 0 1px rgba(255, 255, 255, 0.15) inset;
  transform: perspective(1200px) rotateX(2deg) translateY(-5px);
}

.panel-side {
  position: absolute;
  top: 0;
  width: 50%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.6s cubic-bezier(0.22, 1, 0.36, 1);
  background-color: white;
  box-shadow: 0 0 30px rgba(0, 0, 0, 0.05);
}

.login-side {
  left: 0;
  z-index: 2;
}

.register-side {
  left: 100%;
  opacity: 0;
  z-index: 0;
  transform: translateX(0);
}

.welcome-panel {
  position: absolute;
  left: 50%;
  width: 50%;
  height: 100%;
  background: linear-gradient(135deg, #7dd3fc, #38bdf8, #0ea5e9);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  z-index: 3;
  transition: all 0.6s cubic-bezier(0.22, 1, 0.36, 1);
  box-shadow: 0 0 40px rgba(0, 0, 0, 0.1);
}

.welcome-panel:before {
  content: "";
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: url("data:image/svg+xml,%3Csvg width='100' height='100' viewBox='0 0 100 100' xmlns='http://www.w3.org/2000/svg'%3E%3Cpath d='M11 18c3.866 0 7-3.134 7-7s-3.134-7-7-7-7 3.134-7 7 3.134 7 7 7zm48 25c3.866 0 7-3.134 7-7s-3.134-7-7-7-7 3.134-7 7 3.134 7 7 7zm-43-7c1.657 0 3-1.343 3-3s-1.343-3-3-3-3 1.343-3 3 1.343 3 3 3zm63 31c1.657 0 3-1.343 3-3s-1.343-3-3-3-3 1.343-3 3 1.343 3 3 3z' fill='%23ffffff' fill-opacity='0.1' fill-rule='evenodd'/%3E%3C/svg%3E");
  opacity: 0.7;
}

.welcome-panel.flip {
  transform: translateX(-100%);
  z-index: 0;
}

.panel-content {
  width: 100%;
  max-width: 320px;
  padding: 30px;
}

.welcome-content {
  text-align: center;
  padding: 30px;
}

h1 {
  text-align: center;
  margin-bottom: 30px;
  color: #333;
  font-weight: 600;
}

.welcome-content h1 {
  color: #fff;
  font-size: 42px;
  letter-spacing: 6px;
  margin-bottom: 0;
  margin-top: 0;
  text-shadow: 0 8px 32px #2193b0, 0 2px 0 #fff8;
  font-family: 'ZCOOL KuaiLe', 'Comic Sans MS', cursive, sans-serif;
  font-weight: bold;
  text-transform: uppercase;
  transition: color 0.3s, text-shadow 0.3s;
  filter: brightness(1.15) drop-shadow(0 2px 8px #38bdf8);
}

.welcome-content h1:hover {
  color: #ffe066;
  text-shadow: 0 12px 36px #2193b0, 0 4px 0 #fff8;
}

.welcome-content p {
  color: white;
  margin-top: 20px;
  font-size: 18px;
}

.avatar-container {
  width: 90px;
  height: 90px;
  margin: 0 auto 40px auto;
  display: flex;
  align-items: center;
  justify-content: center;
  background: none;
  border: none;
  border-radius: 0;
  box-shadow: none;
  animation: none;
}

.avatar-container .el-icon {
  font-size: 80px !important;
  color: #fff;
  filter: drop-shadow(0 4px 16px #2193b0) drop-shadow(0 1px 0 #fff8);
  transition: transform 0.3s cubic-bezier(.22,1,.36,1);
}
.avatar-container .el-icon:hover {
  transform: scale(1.08) rotate(-8deg);
  color: #ffe066;
  filter: drop-shadow(0 8px 24px #2193b0) drop-shadow(0 2px 0 #fff8);
}

.form-group {
  margin-bottom: 20px;
}

.auth-button {
  width: 100%;
  border-radius: 20px;
  padding: 12px 0;
  font-size: 14px;
  margin-top: 10px;
  transition: all 0.3s ease;
  position: relative;
  overflow: hidden;
  z-index: 1;
  background: linear-gradient(135deg, #38bdf8, #0ea5e9) !important;
  border: none !important;
}

.auth-button:before {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, rgba(255, 255, 255, 0.05), rgba(255, 255, 255, 0.1), rgba(255, 255, 255, 0.05));
  z-index: -1;
  transition: all 0.4s ease;
}

.auth-button:hover:before {
  left: 100%;
}

.form-footer {
  text-align: center;
  margin-top: 20px;
  color: #666;
}

.form-footer .el-button {
  padding: 0 5px;
  color: #0ea5e9;
  font-weight: 500;
}

.code-button {
  width: 90px;
  padding: 8px 0;
  font-size: 12px;
}

/* 表单提示样式 */
.form-tip {
  font-size: 12px;
  color: #e6a23c;
  line-height: 1.4;
  margin-top: 4px;
  display: flex;
  align-items: center;
}

.form-tip .el-icon {
  margin-right: 4px;
  flex-shrink: 0;
}

.is-disabled {
  cursor: not-allowed !important;
  opacity: 0.7;
}

/* 翻转状态 */
.login-panel.flip .login-side {
  opacity: 0;
  z-index: 0;
  transform: translateX(-100%);
}

.login-panel.flip .register-side {
  opacity: 1;
  z-index: 1;
  transform: translateX(-100%);
  left: 100%;
}

/* 响应式设计 */
@media (max-width: 900px) {
  .login-panel {
    width: 90%;
    max-width: 450px;
    flex-direction: column;
    height: auto;
  }

  .panel-side,
  .welcome-panel {
    position: relative;
    width: 100%;
    left: 0;
    right: 0;
  }

  .welcome-panel {
    min-height: 250px;
    order: -1;
  }

  .login-side,
  .register-side {
    min-height: 500px;
  }

  .login-panel.flip .welcome-panel {
    transform: translateY(-100%);
  }

  .register-side {
    transform: translateY(100%);
  }

  .login-panel.flip .register-side {
    transform: translateY(0);
  }
}
</style>