<template>
  <dashboard-layout>
    <!-- 搜索和筛选区域 -->
    <div class="page-header">
      <div class="filter-container">
        <el-input v-model="searchQuery" placeholder="搜索设备ID" class="search-input" clearable prefix-icon="Search"
          @input="handleSearch" />
        <el-select v-model="statusFilter" placeholder="设备状态" class="filter-select" @change="handleSelect">
          <el-option label="全部" value="" />
          <el-option label="在线" value="online">
            <div class="option-with-indicator">
              <span class="status-dot online"></span>在线
            </div>
          </el-option>
          <el-option label="离线" value="offline">
            <div class="option-with-indicator">
              <span class="status-dot offline"></span>离线
            </div>
          </el-option>
          <el-option label="异常" value="warning">
            <div class="option-with-indicator">
              <span class="status-dot warning"></span>异常
            </div>
          </el-option>
        </el-select>
        <el-button type="primary" @click="searchDevices" class="action-btn search-btn">
          <el-icon>
            <Search />
          </el-icon>
          搜索
        </el-button>
      </div>
    </div>

    <!-- 地图和列表区域 -->
    <el-row :gutter="20" class="map-container">
      <el-col :span="16">
        <el-card class="map-card" shadow="hover">
          <div class="map-header">
            <h3>设备地理分布</h3>
            <div class="device-stats">
              <div class="stat-item">
                <span class="status-dot online"></span>
                <span>在线: {{ getStatusCount('online') }}</span>
              </div>
              <div class="stat-item">
                <span class="status-dot offline"></span>
                <span>离线: {{ getStatusCount('offline') }}</span>
              </div>
              <div class="stat-item">
                <span class="status-dot warning"></span>
                <span>异常: {{ getStatusCount('warning') }}</span>
              </div>
            </div>
          </div>
          <div id="amap-container" class="amap-container"></div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card class="device-list-card" shadow="hover">
          <div class="list-header">
            <h3>设备列表</h3>
            <el-tag effect="dark" type="info">{{ filteredDevices.length }} 个设备</el-tag>
          </div>
          <el-scrollbar height="600px">
            <div v-for="device in filteredDevices" :key="device.deviceId || device.id" class="device-item"
              :class="{ 'selected': selectedDeviceId === (device.deviceId || device.id) }"
              @click="selectDevice(device)">
              <div class="device-info">
                <div class="device-header">
                  <span class="device-id">{{ device.deviceId || device.id }}</span>
                  <div class="device-status" :class="getStatusClass(device.status)">
                    {{ getStatusText(device.status) }}
                  </div>
                </div>
                <div class="device-location">
                  <el-icon>
                    <Location />
                  </el-icon>
                  {{ device.fullAddress || device.city || '未知位置' }}
                </div>
              </div>
            </div>
            <div v-if="filteredDevices.length === 0" class="no-data">
              <el-empty description="没有找到设备" />
            </div>
          </el-scrollbar>
        </el-card>
      </el-col>
    </el-row>

    <!-- 设备详情对话框 -->
    <el-dialog v-model="deviceDetailVisible"
      :title="selectedDevice ? `设备ID: ${selectedDevice.deviceId || selectedDevice.id}` : '设备详情'" width="600px"
      :append-to-body="true" :lock-scroll="true" :center="true" destroy-on-close class="device-dialog">
      <div v-if="selectedDevice" class="device-detail">
        <el-table :data="[selectedDevice]" stripe style="width: 100%" border>
          <el-table-column prop="deviceId" label="设备ID">
            <template #default="scope">
              {{ scope.row.deviceId || scope.row.id }}
            </template>
          </el-table-column>
          <el-table-column label="设备状态">
            <template #default="scope">
              <el-tag :type="getStatusType(scope.row.status)" size="small">
                {{ getStatusText(scope.row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="安装位置">
            <template #default="scope">
              {{ scope.row.fullAddress || scope.row.city || '未知位置' }}
            </template>
          </el-table-column>
          <el-table-column label="安装日期">
            <template #default="scope">
              {{ scope.row.registerTime || '未知日期' }}
            </template>
          </el-table-column>
        </el-table>

        <div v-if="selectedDevice.geoPosition" class="coordinates-info">
          <div class="coordinates-title">经纬度坐标</div>
          <div class="coordinates-value">
            经度: {{ selectedDevice.geoPosition.longitude || '未知' }}, 纬度: {{ selectedDevice.geoPosition.latitude || '未知'
            }}
          </div>
        </div>

        <div class="description-info">
          <div class="description-title">设备描述</div>
          <div class="description-value">{{ selectedDevice.description || '暂无描述' }}</div>
        </div>

        <div class="device-actions">
          <el-button type="success" @click="openDeviceInAmap" :disabled="!selectedDevice.geoPosition">
            <el-icon>
              <Location />
            </el-icon> 在高德地图中查看
          </el-button>
        </div>
      </div>
    </el-dialog>
  </dashboard-layout>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue';
import { ElMessage } from 'element-plus';
import { Search, Location, CircleCheck, CircleClose, Warning, Refresh } from '@element-plus/icons-vue';
import AMapLoader from '@amap/amap-jsapi-loader';
import { EventSourcePolyfill } from 'event-source-polyfill';
import { getAllDevices } from '@/api/device';
import { useAuthStore } from '@/store/useAuthStore';

// 导入共享布局组件
import DashboardLayout from '@/components/layout/DashboardLayout.vue';

// 地图对象
let map = null;
let mapMarkers = [];

// 高德地图 API Key（实际环境中应使用环境变量或配置文件）
const AMAP_KEY = 'e39b2634b7eb9d71f3edd7fd3f40dc0a';

// 认证状态
const authStore = useAuthStore();

// 搜索和筛选
const searchQuery = ref('');
const statusFilter = ref('');
const loading = ref(false);
const deviceDetailVisible = ref(false);
const selectedDevice = ref(null);
const selectedDeviceId = ref(null);

// SSE连接
let eventSource = null;

// 添加变量存储当前选中的标记
let currentHighlightMarker = null;

// 设备数据
const deviceList = ref([]);

// 筛选设备列表
const filteredDevices = computed(() => {
  // 直接使用API返回的设备列表，不在前端再次筛选
  return deviceList.value;
});

// 获取设备列表
const fetchDevices = async () => {
  loading.value = true;

  try {
    // 构建查询参数
    const params = {};

    if (searchQuery.value) {
      params.search = searchQuery.value;
    }

    if (statusFilter.value) {
      const statusCode = getStatusCodeFromFilter(statusFilter.value);
      if (statusCode !== null) {
        params.status = statusCode;
      }
    }

    console.log('发送查询参数:', params);

    const response = await getAllDevices(params);

    if (!response || !response.data) {
      console.error('设备数据返回格式错误:', response);
      ElMessage.error('获取设备数据格式错误');
      return;
    }

    deviceList.value = response.data;

    // 日志输出筛选条件和结果数量
    console.log(`已获取设备数据 - 总数: ${deviceList.value.length}, 筛选条件: ${searchQuery.value || '无'}, 状态: ${statusFilter.value || '全部'}`);

    // 添加设备标记
    if (map) {
      // 确保地图已初始化后添加标记
      addDeviceMarkers();
      ElMessage.success(`已更新 ${deviceList.value.length} 个设备`);
    } else {
      console.warn('地图未初始化，无法添加标记');
    }

  } catch (error) {
    console.error('获取设备列表失败:', error);
    ElMessage.error('获取设备列表失败，请稍后重试');
  } finally {
    loading.value = false;
  }
};

// 将UI筛选值转换为后端状态代码
const getStatusCodeFromFilter = (filter) => {
  console.log('转换UI筛选值到状态代码:', filter);

  switch (filter) {
    case 'online': return 0;  // NORMAL - 在线
    case 'offline': return 1; // OFFLINE - 离线
    case 'warning': return 2; // ABNORMAL - 异常
    default: return null;
  }
};

// 将后端状态代码转换为UI筛选值
const getFilterFromStatusCode = (code) => {
  if (code === null || code === undefined) {
    return '';
  }

  // 处理可能是对象的情况（后端返回的是对象）
  if (typeof code === 'object') {
    if (code.name === 'NORMAL') return 'online';
    if (code.name === 'OFFLINE') return 'offline';
    if (code.name === 'ABNORMAL') return 'warning';

    // 如果有code属性
    if (code.code !== undefined) {
      return getFilterFromStatusCode(code.code);
    }
    return '';
  }

  // 处理可能是字符串的情况
  if (typeof code === 'string') {
    // 枚举名称字符串处理
    if (code === 'NORMAL') return 'online';
    if (code === 'OFFLINE') return 'offline';
    if (code === 'ABNORMAL') return 'warning';

    // 尝试解析数字字符串
    const parsedCode = parseInt(code, 10);
    if (!isNaN(parsedCode)) {
      return getFilterFromStatusCode(parsedCode);
    }
    return '';
  }

  // 处理数字情况
  switch (code) {
    case 0: return 'online';
    case 1: return 'offline';
    case 2: return 'warning';
    default: return '';
  }
};

// 计算各状态设备数量
const getStatusCount = (status) => {
  return filteredDevices.value.filter(device => {
    const deviceStatus = typeof device.status === 'object' ? device.status.code : device.status;
    return getFilterFromStatusCode(deviceStatus) === status;
  }).length;
};

// 初始化高德地图
const initMap = async () => {
  try {
    loading.value = true; // 设置加载状态，防止重复初始化

    window.AMap = await AMapLoader.load({
      key: AMAP_KEY,
      version: '2.0',
      plugins: [
        'AMap.ToolBar',
        'AMap.Scale',
        'AMap.HawkEye',
        'AMap.MapType',
        'AMap.GeolocationControl',
        'AMap.DistrictSearch',
        'AMap.PlaceSearch',
        'AMap.RoadInfoSearch'
      ]
    });

    // 创建地图实例
    map = new window.AMap.Map('amap-container', {
      viewMode: '3D',
      zoom: 11,
      center: [121.5, 31.2], // 上海坐标
      resizeEnable: true,
      mapStyle: 'amap://styles/macaron', // 使用蓝色马卡龙风格，标注更清晰
      features: ['bg', 'road', 'building', 'point'],
      showLabel: true, // 显示地图文字标注
      showIndoorMap: true, // 显示室内地图
      rotateEnable: true, // 允许旋转
      expandZoomRange: true, // 扩展缩放范围
      pitch: 0, // 地图俯仰角度
      labelzIndex: 200, // 标注层级
      wallColor: '#FFFFFF'
    });

    // 道路标记渲染
    const roadNetLayer = new window.AMap.TileLayer();
    roadNetLayer.setMap(map);

    // 强制刷新地图，确保标注加载
    map.on('complete', () => {
      console.log('地图加载完成');
      setTimeout(() => {
        map.plugin(['AMap.RoadInfoSearch'], () => {
          // 刷新道路信息
          map.setZoom(map.getZoom() - 0.1);
          setTimeout(() => {
            map.setZoom(map.getZoom() + 0.1);

            // 地图完全初始化后获取设备数据
            console.log('地图初始化完成，开始获取设备数据');
            fetchDevices().catch(err => {
              console.error('初始数据加载失败:', err);
            }).finally(() => {
              loading.value = false;
            });
          }, 300);
        });
      }, 500);
    });

    // 添加工具条控件
    map.addControl(new window.AMap.ToolBar({
      position: 'RB'
    }));

    // 添加比例尺控件
    map.addControl(new window.AMap.Scale());

    // 添加地图类型切换控件
    map.addControl(new window.AMap.MapType({
      defaultType: 0, // 默认为普通地图
      showRoad: true, // 显示路网
      showTraffic: false, // 默认不显示实时路况
      showSatellite: true, // 显示卫星图层
      position: 'RT'
    }));

    // 尝试加载路况图层，如果不可用则跳过
    let trafficLayer = null;
    try {
      // 先检查Traffic是否可用
      if (window.AMap.Traffic && typeof window.AMap.Traffic === 'function') {
        trafficLayer = new window.AMap.Traffic({
          zIndex: 10,
          autoRefresh: true, // 自动刷新
          interval: 180, // 刷新间隔，默认180s
        });

        // 添加自定义路况切换控件
        const mapControls = document.createElement('div');
        mapControls.className = 'map-controls';

        // 创建路况复选框控件
        const trafficControlWrapper = document.createElement('div');
        trafficControlWrapper.className = 'map-control-item';

        // 创建复选框
        const trafficCheckbox = document.createElement('input');
        trafficCheckbox.type = 'checkbox';
        trafficCheckbox.id = 'traffic-checkbox';
        trafficCheckbox.className = 'control-checkbox';
        trafficCheckbox.checked = false; // 默认不选中

        // 创建标签
        const trafficLabel = document.createElement('label');
        trafficLabel.htmlFor = 'traffic-checkbox';
        trafficLabel.textContent = '路况';
        trafficLabel.className = 'control-label';

        // 添加事件监听
        trafficCheckbox.addEventListener('change', (e) => {
          if (e.target.checked) {
            // 显示路况
            trafficLayer.show();
          } else {
            // 隐藏路况
            trafficLayer.hide();
          }
        });

        // 组装控件
        trafficControlWrapper.appendChild(trafficCheckbox);
        trafficControlWrapper.appendChild(trafficLabel);
        mapControls.appendChild(trafficControlWrapper);

        // 添加到地图
        map.addControl(new window.AMap.Control({
          position: 'RT',
          content: mapControls
        }));

        // 添加路况图层但默认不显示
        map.add(trafficLayer);
        trafficLayer.hide();
      } else {
        console.warn('AMap.Traffic插件不可用，跳过路况图层初始化');
      }
    } catch (trafficError) {
      console.warn('初始化路况图层失败，继续地图初始化:', trafficError);
    }

  } catch (error) {
    console.error('地图加载失败：', error);
    ElMessage.error('地图加载失败，请刷新页面重试');
    loading.value = false;
  }
};

// 添加设备标记点
const addDeviceMarkers = () => {
  try {
    console.log('开始添加设备标记，设备数量:', filteredDevices.value.length);

    // 清除现有标记
    if (mapMarkers.length > 0) {
      console.log('清除现有标记:', mapMarkers.length);
      map.remove(mapMarkers);
      mapMarkers = [];
    }

    // 如果没有设备数据，直接返回
    if (filteredDevices.value.length === 0) {
      console.log('没有设备数据，不添加标记');
      return;
    }

    // 提前定义标记样式
    const createMarkerContent = (status) => {
      const deviceStatus = typeof status === 'object' ? status.code : status;
      const uiStatus = getFilterFromStatusCode(deviceStatus);

      const markerColor = {
        'online': '#10b981',   // 绿色 - 在线设备
        'offline': '#64748b',  // 灰色 - 离线设备
        'warning': '#f59e0b'   // 橙色 - 异常设备
      };

      const color = markerColor[uiStatus] || markerColor.online;
      return `<div style="background-color: ${color}; width: 18px; height: 18px; border: 3px solid white; border-radius: 50%; box-shadow: 0 0 5px rgba(0,0,0,0.5); display: flex; justify-content: center; align-items: center;"></div>`;
    };

    // 批量创建标记提高性能
    const newMarkers = filteredDevices.value.map(device => {
      // 如果设备没有经纬度，跳过
      if (!device.geoPosition || !device.geoPosition.longitude || !device.geoPosition.latitude) {
        console.warn('设备无经纬度信息，跳过标记:', device.id || device.deviceId);
        return null;
      }

      // 经纬度
      const longitude = device.geoPosition.longitude;
      const latitude = device.geoPosition.latitude;

      console.log(`添加设备标记: ID=${device.id || device.deviceId}, 位置=[${longitude}, ${latitude}]`);

      // 创建标记
      const marker = new window.AMap.Marker({
        position: [longitude, latitude],
        title: device.id || device.deviceId,
        content: createMarkerContent(device.status),
        offset: new window.AMap.Pixel(-12, -12),
        // 存储设备信息以便后续使用
        extData: {
          device: device
        },
        zIndex: 100
      });

      // 点击标记事件
      marker.on('click', () => {
        selectDevice(device);
      });

      return marker;
    }).filter(marker => marker !== null); // 过滤掉空标记

    // 统计各状态设备数量
    const statusCount = {
      online: getStatusCount('online'),
      offline: getStatusCount('offline'),
      warning: getStatusCount('warning')
    };

    console.log('设备状态统计:', statusCount);
    console.log('有效标记数量:', newMarkers.length);

    // 添加标记到地图
    if (newMarkers.length > 0) {
      mapMarkers = newMarkers;
      map.add(mapMarkers);

      // 如果是搜索或筛选后的结果，调整地图视图
      if (searchQuery.value || statusFilter.value) {
        console.log('根据筛选结果调整地图视图');
        map.setFitView(mapMarkers);
      }

      // 如果当前有选中的设备，确保高亮显示
      if (selectedDevice.value) {
        highlightDeviceMarker(selectedDevice.value);
      }
    } else {
      console.log('没有有效标记可添加');
    }
  } catch (error) {
    console.error('添加设备标记失败:', error);
    ElMessage.error('加载设备标记失败，请刷新页面重试');
  }
};

// 搜索设备
const handleSearch = () => {
  console.log('触发搜索/筛选:', {
    搜索文本: searchQuery.value,
    状态筛选: statusFilter.value
  });
};

// 根据当前筛选条件搜索设备
const searchDevices = () => {
  console.log('点击搜索按钮，根据当前筛选条件获取设备', {
    搜索文本: searchQuery.value,
    状态筛选: statusFilter.value
  });

  // 根据当前筛选条件获取设备数据
  fetchDevices();
};

// 选择设备
const selectDevice = (device) => {
  selectedDeviceId.value = device.deviceId || device.id;
  selectedDevice.value = device;
  deviceDetailVisible.value = true;

  // 只移除高亮标记
  if (currentHighlightMarker) {
    map.remove(currentHighlightMarker);
    currentHighlightMarker = null;
  }

  // 高亮当前设备
  if (map && device.geoPosition) {
    highlightDeviceMarker(device);
  }
};

// 高亮显示设备标记
const highlightDeviceMarker = (device) => {
  if (currentHighlightMarker) {
    map.remove(currentHighlightMarker);
  }
  if (!device.geoPosition) return;

  const position = [device.geoPosition.longitude, device.geoPosition.latitude];

  // 创建高亮标记
  currentHighlightMarker = new window.AMap.Marker({
    position: position,
    offset: new window.AMap.Pixel(-18, -18),
    content: `<div class="highlight-marker">
      <div class="pulse-circle" style="background-color: ${getStatusColorWithOpacity(device.status, 0.4)}"></div>
      <div class="center-circle" style="background-color: ${getStatusColor(device.status)}"></div>
    </div>`,
    zIndex: 200
  });
  map.add(currentHighlightMarker);

  // 信息窗体
  const infoContent = `<div class="device-info-window">
    <div class="device-id">${device.deviceId || device.id}</div>
    <div class="device-status ${getStatusClass(device.status)}">${getStatusText(device.status)}</div>
    <div class="device-location small">${device.fullAddress || device.city || ''}</div>
  </div>`;
  const infoWindow = new window.AMap.InfoWindow({
    content: infoContent,
    offset: new window.AMap.Pixel(0, -40),
    closeWhenClickMap: false,
    autoMove: true
  });
  infoWindow.open(map, position);

  setTimeout(() => {
    map.setCenter(position);
    setTimeout(() => {
      infoWindow.close();
    }, 6000);
  }, 100);
};

// 获取设备状态对应的颜色（带透明度）
const getStatusColorWithOpacity = (status, opacity = 1) => {
  // 处理空值
  if (status === null || status === undefined) {
    return `rgba(100, 116, 139, ${opacity})`; // 默认灰色
  }

  // 处理可能的对象结构
  let statusCode;
  if (typeof status === 'object') {
    // 可能是 {code: 0} 格式
    if (status.code !== undefined) {
      statusCode = status.code;
    }
    // 可能是枚举对象格式
    else if (status.name !== undefined) {
      if (status.name === 'NORMAL') return `rgba(16, 185, 129, ${opacity})`;
      if (status.name === 'OFFLINE') return `rgba(100, 116, 139, ${opacity})`;
      if (status.name === 'ABNORMAL') return `rgba(245, 158, 11, ${opacity})`;
    } else {
      return `rgba(100, 116, 139, ${opacity})`; // 默认灰色
    }
  } else {
    statusCode = status;
  }

  let uiStatus = getFilterFromStatusCode(statusCode);

  let color;
  switch (uiStatus) {
    case 'online':
      color = `rgba(16, 185, 129, ${opacity})`;
      break;
    case 'offline':
      color = `rgba(100, 116, 139, ${opacity})`;
      break;
    case 'warning':
      color = `rgba(245, 158, 11, ${opacity})`;
      break;
    default:
      color = `rgba(100, 116, 139, ${opacity})`;
  }
  return color;
};

// 获取设备状态对应的颜色
const getStatusColor = (status) => {
  // 处理空值
  if (status === null || status === undefined) {
    return '#64748b'; // 默认灰色
  }

  // 处理可能的对象结构
  let statusCode;
  if (typeof status === 'object') {
    // 可能是 {code: 0} 格式
    if (status.code !== undefined) {
      statusCode = status.code;
    }
    // 可能是枚举对象格式
    else if (status.name !== undefined) {
      if (status.name === 'NORMAL') return '#10b981';
      if (status.name === 'OFFLINE') return '#64748b';
      if (status.name === 'ABNORMAL') return '#f59e0b';
    } else {
      return '#64748b'; // 默认灰色
    }
  } else {
    statusCode = status;
  }

  let uiStatus = getFilterFromStatusCode(statusCode);

  const colorMap = {
    'online': '#10b981',
    'offline': '#64748b',
    'warning': '#f59e0b'
  };
  return colorMap[uiStatus] || '#64748b';
};

// 在高德地图中打开设备位置
const openDeviceInAmap = () => {
  if (!selectedDevice.value || !selectedDevice.value.geoPosition) return;

  const center = `${selectedDevice.value.geoPosition.longitude},${selectedDevice.value.geoPosition.latitude}`;
  const name = `设备ID: ${selectedDevice.value.deviceId || selectedDevice.value.id} - ${selectedDevice.value.fullAddress || selectedDevice.value.city || ''}`;

  // 生成高德地图URL，使用设备ID和位置信息
  const url = `https://uri.amap.com/marker?position=${center}&name=${encodeURIComponent(name)}&src=myapp&coordinate=wgs84&callnative=0`;

  // 在新窗口打开
  window.open(url, '_blank');
};

// 状态相关辅助函数
const getStatusClass = (status) => {
  // 处理空值
  if (status === null || status === undefined) {
    return '';
  }

  // 处理可能的对象结构
  let statusCode;
  if (typeof status === 'object') {
    // 可能是 {code: 0} 格式
    if (status.code !== undefined) {
      statusCode = status.code;
    }
    // 可能是枚举对象格式
    else if (status.name !== undefined) {
      if (status.name === 'NORMAL') return 'status-online';
      if (status.name === 'OFFLINE') return 'status-offline';
      if (status.name === 'ABNORMAL') return 'status-warning';
    }
  } else {
    statusCode = status;
  }

  let uiStatus = getFilterFromStatusCode(statusCode);

  const statusMap = {
    'online': 'status-online',
    'offline': 'status-offline',
    'warning': 'status-warning'
  };
  return statusMap[uiStatus] || '';
};

const getStatusText = (status) => {
  // 处理空值
  if (status === null || status === undefined) {
    return '未知';
  }

  // 处理可能的对象结构
  let statusCode;
  if (typeof status === 'object') {
    // 可能是 {code: 0} 格式
    if (status.code !== undefined) {
      statusCode = status.code;
    }
    // 可能是枚举对象格式
    else if (status.name !== undefined) {
      if (status.name === 'NORMAL') return '在线';
      if (status.name === 'OFFLINE') return '离线';
      if (status.name === 'ABNORMAL') return '异常';
    } else {
      console.log('无法识别的状态对象:', status);
      return '未知';
    }
  } else {
    statusCode = status;
  }

  let uiStatus = getFilterFromStatusCode(statusCode);

  const statusMap = {
    'online': '在线',
    'offline': '离线',
    'warning': '异常'
  };
  return statusMap[uiStatus] || '未知';
};

const getStatusType = (status) => {
  let statusCode = typeof status === 'object' ? status.code : status;
  let uiStatus = getFilterFromStatusCode(statusCode);

  const typeMap = {
    'online': 'success',
    'offline': 'info',
    'warning': 'warning'
  };
  return typeMap[uiStatus] || 'info';
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
    console.log('设备分布正在建立SSE连接...');

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
      console.log('收到设备更新消息:', event.data);

      // 当消息为设备更新时，重新获取设备数据
      if (event.data === 'device-update') {
        console.log('检测到设备更新，使用当前筛选条件重新获取数据');

        // 显示正在更新的提示
        ElMessage({
          message: '检测到设备更新，正在刷新数据...',
          type: 'info',
          duration: 2000
        });

        // 使用当前筛选条件重新获取设备数据并更新UI
        fetchDevices().then(() => {
          console.log('设备数据已更新，列表和地图标记已刷新');
        }).catch(error => {
          console.error('更新设备数据失败:', error);
        });
      }
    });

    // 接收心跳消息
    eventSource.addEventListener('heartbeat', (event) => {
      // 只在开发环境输出心跳日志
      if (process.env.NODE_ENV === 'development') {
        console.log('设备分布收到心跳消息:', event.data);
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

// 组件挂载时初始化地图和SSE连接
onMounted(() => {
  console.log('组件挂载，初始化地图和SSE连接');
  initMap();
  setupSSEConnection();
});

// 组件卸载前销毁地图和关闭SSE连接
onBeforeUnmount(() => {
  closeSSEConnection();
  if (map) {
    map.destroy();
    map = null;
  }
});

// 选择元素时的处理函数
const handleSelect = (option) => {
  console.log('状态选择变更 - 当前值:', statusFilter.value, 'option:', option);

  // 添加调试信息，查看状态筛选转换
  if (statusFilter.value) {
    const statusCode = getStatusCodeFromFilter(statusFilter.value);
    console.log('状态筛选转换:', statusFilter.value, '->', statusCode);
  }
};
</script>

<style scoped>
.page-header {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  margin-bottom: 24px;
  flex-wrap: wrap;
  gap: 16px;
}

.filter-container {
  display: flex;
  gap: 16px;
  align-items: center;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.option-with-indicator {
  display: flex;
  align-items: center;
  gap: 8px;
}

.status-dot {
  display: inline-block;
  width: 10px;
  height: 10px;
  border-radius: 50%;
}

.status-dot.online {
  background-color: #10b981;
}

.status-dot.offline {
  background-color: #64748b;
}

.status-dot.warning {
  background-color: #f59e0b;
}

.search-input {
  width: 300px;
  max-width: 100%;
}

.filter-select {
  width: 150px;
}

.action-btn {
  display: flex;
  align-items: center;
  gap: 6px;
}

.search-btn {
  background-color: var(--primary-color);
  border-color: var(--primary-color);
}

.map-container {
  margin-bottom: 20px;
}

.map-card,
.device-list-card {
  height: 670px;
  overflow: hidden;
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
  transition: all 0.3s ease;
}

.map-card:hover,
.device-list-card:hover {
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.1);
}

.map-header,
.list-header {
  padding: 16px;
  border-bottom: 1px solid var(--border-color);
  display: flex;
  justify-content: space-between;
  align-items: center;
  background-color: #f8fafc;
}

.device-stats {
  display: flex;
  gap: 12px;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  color: var(--text-secondary);
}

.map-header h3,
.list-header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
}

.amap-container {
  width: 100%;
  height: 603px;
}

.device-list-card {
  display: flex;
  flex-direction: column;
}

.device-item {
  padding: 16px;
  border-bottom: 1px solid var(--border-color);
  cursor: pointer;
  transition: all 0.3s ease;
  position: relative;
}

.device-item:hover {
  background-color: rgba(59, 130, 246, 0.05);
  transform: translateY(-2px);
}

.device-item.selected {
  background-color: rgba(59, 130, 246, 0.1);
  border-left: 4px solid var(--primary-color);
}

.device-item.selected::after {
  content: "";
  position: absolute;
  top: 0;
  right: 0;
  width: 4px;
  height: 100%;
  background-color: var(--primary-color);
}

.device-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.device-id {
  font-weight: 600;
  color: var(--text-primary);
  font-size: 15px;
}

.device-status {
  font-size: 12px;
  padding: 2px 10px;
  border-radius: 12px;
  text-align: center;
  font-weight: 500;
  letter-spacing: 0.5px;
}

.status-online {
  background-color: rgba(16, 185, 129, 0.1);
  color: #10b981;
}

.status-offline {
  background-color: rgba(100, 116, 139, 0.1);
  color: #64748b;
}

.status-warning {
  background-color: rgba(245, 158, 11, 0.1);
  color: #f59e0b;
}

.device-type {
  font-size: 12px;
  color: var(--text-secondary);
  margin-bottom: 8px;
}

.device-location {
  font-size: 13px;
  color: var(--text-secondary);
  display: flex;
  align-items: center;
  gap: 4px;
}

.device-metrics-summary {
  margin-top: 8px;
}

.metric-summary {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 4px;
}

.metric-name {
  font-size: 12px;
  color: var(--text-secondary);
}

.metric-value {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
}

.metric-trend-icon {
  margin-left: 4px;
}

.no-data {
  padding: 32px 0;
  display: flex;
  justify-content: center;
}

.device-detail {
  margin-top: 16px;
}

.device-metrics {
  margin-top: 24px;
}

.metrics-title {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 16px;
  color: var(--text-primary);
}

.metrics-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 16px;
}

.metric-card {
  padding: 16px;
  background-color: #f8fafc;
  border-radius: 8px;
  border: 1px solid var(--border-color);
  display: flex;
  flex-direction: column;
  transition: all 0.3s ease;
}

.metric-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 6px 12px rgba(0, 0, 0, 0.08);
}

.metric-icon {
  display: flex;
  justify-content: center;
  align-items: center;
  width: 40px;
  height: 40px;
  border-radius: 50%;
  margin-bottom: 12px;
  background-color: rgba(59, 130, 246, 0.1);
}

.icon-warning {
  background-color: rgba(245, 158, 11, 0.1);
  color: #f59e0b;
}

.icon-up {
  background-color: rgba(239, 68, 68, 0.1);
  color: #ef4444;
}

.icon-down {
  background-color: rgba(16, 185, 129, 0.1);
  color: #10b981;
}

.icon-stable {
  background-color: rgba(100, 116, 139, 0.1);
  color: #64748b;
}

.metric-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.metric-value {
  font-size: 24px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 8px;
}

.metric-name {
  font-size: 14px;
  color: var(--text-secondary);
  margin-bottom: 8px;
}

.metric-trend {
  font-size: 12px;
  display: flex;
  align-items: center;
  gap: 4px;
}

.trend-up {
  color: #ef4444;
}

.trend-down {
  color: #10b981;
}

.trend-stable {
  color: #64748b;
}

.device-actions {
  margin-top: 24px;
  display: flex;
  gap: 12px;
}

/* 地图标记信息窗口样式 */
.map-marker-label {
  padding: 8px 12px;
  background-color: white;
  border-radius: 6px;
  border: 1px solid #eee;
  font-size: 13px;
  color: #333;
  line-height: 1.5;
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1);
  white-space: nowrap;
  min-width: 150px;
}

.map-marker-label .device-id {
  font-weight: 600;
  margin-bottom: 4px;
}

.map-marker-label .device-status {
  font-size: 12px;
  padding: 2px 6px;
  border-radius: 10px;
  display: inline-block;
  margin-bottom: 4px;
}

.map-marker-label .device-location {
  font-size: 12px;
  color: #666;
}

/* 响应式调整 */
@media (max-width: 1200px) {
  .map-container {
    display: flex;
    flex-direction: column;
  }

  .el-col {
    width: 100% !important;
    max-width: 100%;
    flex: 0 0 100%;
  }

  .map-card,
  .device-list-card {
    width: 100%;
    margin-bottom: 20px;
  }

  .amap-container {
    height: 450px;
  }

  .device-list-card {
    height: auto;
    max-height: 600px;
  }

  .list-header+.el-scrollbar {
    height: 450px;
  }

  .search-input {
    width: 100%;
    max-width: 100%;
  }

  .filter-select {
    width: 100%;
    max-width: 100%;
  }

  .filter-container {
    flex-direction: column;
    align-items: stretch;
    width: 100%;
  }

  .page-header {
    justify-content: center;
    width: 100%;
  }

  .action-btn {
    width: 100%;
  }
}

@media (max-width: 768px) {
  .device-dialog .el-dialog {
    width: 95% !important;
    max-width: 95%;
  }

  .metrics-grid {
    grid-template-columns: 1fr;
  }

  .amap-container {
    height: 350px;
  }
}

:deep(.el-table tr:hover) {
  background-color: #f1f5f9 !important;
  transform: translateY(-2px);
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.05);
}

/* 对话框样式 */
:deep(.device-dialog) {
  display: flex;
  align-items: center;
  justify-content: center;
}

:deep(.device-dialog .el-dialog) {
  margin: 0 auto !important;
  position: relative;
  border-radius: 12px;
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.1), 0 2px 6px rgba(0, 0, 0, 0.08);
  overflow: hidden;
}

:deep(.device-dialog .el-dialog__header) {
  padding: 20px 24px;
  margin: 0;
  border-bottom: 1px solid #f1f5f9;
  background: #f8fafc;
}

:deep(.device-dialog .el-dialog__title) {
  font-weight: 600;
  font-size: 18px;
  color: #1e293b;
}

:deep(.device-dialog .el-dialog__body) {
  padding: 24px;
}

:deep(.device-dialog .el-dialog__footer) {
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

.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.status-tag {
  font-size: 14px;
  padding: 6px 12px;
}

.coordinates-info,
.description-info {
  margin-top: 20px;
  padding: 15px;
  background-color: #f8fafc;
  border-radius: 6px;
  border: 1px solid var(--border-color);
}

.coordinates-title,
.description-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 8px;
}

.coordinates-value,
.description-value {
  font-size: 14px;
  color: var(--text-secondary);
  line-height: 1.5;
}

/* 地图类型切换按钮样式 */
.map-type-wrapper {
  display: flex;
  background-color: white;
  padding: 6px;
  border-radius: 4px;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.15);
  margin: 10px;
}

.map-type-btn {
  padding: 6px 12px;
  cursor: pointer;
  border-radius: 4px;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.2s ease;
  user-select: none;
  color: #666;
}

.map-type-btn:hover {
  background-color: #f0f0f0;
}

.map-type-btn.active {
  background-color: #3B82F6;
  color: white;
}

.map-type-btn+.map-type-btn {
  margin-left: 6px;
}

/* 高亮标记样式 */
.highlight-marker {
  position: relative;
  width: 40px;
  height: 40px;
  pointer-events: none;
}

.pulse-circle {
  position: absolute;
  border-radius: 50%;
  height: 100%;
  width: 100%;
  background-color: rgba(59, 130, 246, 0.4);
  opacity: 0;
  animation: pulse 2s infinite;
}

.center-circle {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background-color: currentColor;
  border: 3px solid white;
  box-shadow: 0 0 12px rgba(0, 0, 0, 0.6);
}

@keyframes pulse {
  0% {
    transform: scale(0.5);
    opacity: 1;
  }

  100% {
    transform: scale(1.8);
    opacity: 0;
  }
}

/* 信息窗体样式 */
.device-info-window {
  padding: 12px 16px;
  background-color: white;
  border-radius: 8px;
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.25);
  min-width: 160px;
  text-align: center;
  border: 2px solid #eaeaea;
}

.device-info-window .device-id {
  font-weight: 700;
  color: #1e293b;
  margin-bottom: 8px;
  font-size: 15px;
}

.device-info-window .device-status {
  display: inline-block;
  padding: 3px 10px;
  border-radius: 12px;
  font-size: 12px;
  margin-bottom: 8px;
  font-weight: 500;
}

.device-info-window .device-location.small {
  font-size: 12px;
  color: #64748b;
  word-break: keep-all;
  white-space: nowrap;
}

/* 地图控件样式 */
.map-controls {
  padding: 8px;
  background-color: white;
  border-radius: 4px;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.15);
  margin: 10px;
  font-size: 14px;
}

.map-control-item {
  display: flex;
  align-items: center;
  gap: 5px;
  margin: 5px 0;
  user-select: none;
}

.control-checkbox {
  width: 16px;
  height: 16px;
  cursor: pointer;
}

.control-label {
  cursor: pointer;
  font-size: 14px;
  color: #333;
  font-weight: 500;
}
</style>