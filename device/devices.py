import struct
import random
import threading
import time
import logging
from cmd import Cmd
from typing import Dict
import requests
import csv
import string
from prettytable import PrettyTable

logging.basicConfig(
    filename='iot_system.log',
    level=logging.INFO,
    format='%(asctime)s | %(levelname)-8s | %(message)s',
    datefmt='%Y-%m-%d %H:%M:%S'
)

class IoTDevice:
    """物联网设备类"""
    def __init__(self, device_id: int, lon: float, lat: float, interval: int = 30, initial_resistance: float = 0.0):
        self.device_id = device_id
        self.lon = lon
        self.lat = lat
        self.interval = interval
        self.active = False
        self.registered = False
        self._thread: threading.Thread = None
        self.api_key = self._generate_api_key()
        self.server_url = ""
        self.initial_resistance = initial_resistance

    def _generate_api_key(self) -> bytes:
        chars = string.ascii_letters + string.digits
        return bytes(''.join(random.choices(chars, k=16)), 'ascii')

    def _pack_register(self) -> tuple:
        """注册数据包格式（有符号整型修正）
        格式: >i16sii (4+16+4+4=28字节)
        """
        lon_scaled = int(self.lon * 1e6)
        lat_scaled = int(self.lat * 1e6)
        data_bytes = struct.pack(
            '>i16sii',  # i:4B, 16s:16B, i:4B, i:4B
            self.device_id,
            self.api_key,
            lon_scaled,
            lat_scaled
        )
        readable_data = {
            "device_id": self.device_id,
            "api_key": self.api_key.decode(),
            "lon": f"{self.lon:.6f}",
            "lat": f"{self.lat:.6f}"
        }
        return data_bytes, readable_data

    def _pack_sensor_data(self) -> tuple:
        """传感器数据包格式
        格式: >i16sH (4+16+2=22字节)
        """
        resistance = self.initial_resistance + random.uniform(-5, 5)
        resistance = max(0.0, min(655.35, resistance))
        data_bytes = struct.pack(
            '>i16sH',  # i:4B, 16s:16B, H:2B
            self.device_id,
            self.api_key,
            int(resistance * 100)
        )
        readable_data = {
            "device_id": self.device_id,
            "api_key": self.api_key.decode(),
            "resistance": f"{resistance:.2f}Ω"
        }
        return data_bytes, readable_data

    def _pack_maintenance(self) -> tuple:  
        """维护报告包格式
        格式: >i16s (4+16=20字节)
        """
        data_bytes = struct.pack(
            '>i16s',  # i:4B, 16s:16B
            self.device_id,
            self.api_key
        )
        readable_data = {
            "device_id": self.device_id,
            "api_key": self.api_key.decode()
        }
        return data_bytes, readable_data

    def _log_operation(self, operation: str, data: dict):
        log_msg = f"[Device {self.device_id}] {operation} - " + \
                  " | ".join(f"{k}:{v}" for k, v in data.items())
        logging.info(log_msg)

    def register(self, server_url: str) -> bool:
        try:
            self.server_url = server_url
            data_bytes, readable = self._pack_register()
            response = requests.post(
                f"{server_url}/register",
                data=data_bytes,
                headers={'Content-Type': 'application/octet-stream'},
                timeout=5
            )
            self.registered = response.status_code == 200
            self._log_operation("REGISTER", readable)
            return self.registered
        except Exception as e:
            logging.error(f"注册失败: {str(e)}")
            return False

    def send_sensor_data(self):
        if not self.registered:
            return False
        data_bytes, readable = self._pack_sensor_data()
        try:
            response = requests.post(
                f"{self.server_url}/send",
                data=data_bytes,
                headers={'Content-Type': 'application/octet-stream'},
                timeout=5
            )
            self._log_operation("SENSOR_DATA", readable)
            return response.ok
        except Exception as e:
            logging.error(f"传感器数据发送失败: {str(e)}")
            return False

    def send_maintenance_report(self) -> bool:  
        data_bytes, readable = self._pack_maintenance()
        try:
            response = requests.post(
                f"{self.server_url}/maintenance",
                data=data_bytes,
                headers={'Content-Type': 'application/octet-stream'},
                timeout=5
            )
            self._log_operation("MAINTENANCE", readable)
            return response.ok
        except Exception as e:
            logging.error(f"维护报告发送失败: {str(e)}")
            return False

    def _sensor_loop(self):
        while self.active:
            try:
                self.send_sensor_data()
                time.sleep(self.interval)
            except Exception as e:
                logging.error(f"传感器循环错误: {str(e)}")
                time.sleep(5)

    def start(self):
        if not self.active:
            self.active = True
            self._thread = threading.Thread(
                target=self._sensor_loop,
                daemon=True
            )
            self._thread.start()
            logging.info(f"设备 {self.device_id} 已启动")

    def stop(self):
        self.active = False
        logging.info(f"设备 {self.device_id} 已停止")

class DeviceManager(Cmd):
    prompt = 'IoT Manager> '
    intro = '物联网设备管理系统（输入 help 查看命令）'
    
    def __init__(self, server_url: str, csv_path: str):
        super().__init__()
        self.server_url = server_url
        self.devices: Dict[int, IoTDevice] = {}
        self._load_devices(csv_path)

    def _load_devices(self, csv_path: str):
        try:
            with open(csv_path, newline='') as f:
                reader = csv.DictReader(f)
                for row in reader:
                    device_id = int(row['device_id'])
                    self.devices[device_id] = IoTDevice(
                        device_id=device_id,
                        lon=float(row['longitude']),
                        lat=float(row['latitude']),
                        interval=int(row.get('interval', 30)),
                        initial_resistance=float(row['initial_resistance'])
                    )
            logging.info(f"从 {csv_path} 加载了 {len(self.devices)} 台设备")
        except Exception as e:
            logging.error(f"CSV加载失败: {str(e)}")
            raise

    def do_list(self, arg):
        """列出所有设备"""
        table = PrettyTable()
        table.field_names = ["ID", "状态", "已注册", "间隔", "经度", "纬度", "初始电阻", "API Key"]
        table.align = "l"
        for device in self.devices.values():
            status = "运行中" if device.active else "已停止"
            table.add_row([
                device.device_id,
                status,
                "是" if device.registered else "否",
                f"{device.interval}s",
                f"{device.lon:.6f}",
                f"{device.lat:.6f}",
                f"{device.initial_resistance:.2f}Ω",
                device.api_key.decode()
            ])
        print(table)

    def do_register(self, arg):
        """注册设备: register [设备ID/all]"""
        if arg.lower() == 'all':
            for device in self.devices.values():
                if device.register(self.server_url):
                    print(f"设备 {device.device_id} 注册成功")
                else:
                    print(f"设备 {device.device_id} 注册失败")
        else:
            try:
                device_id = int(arg)
                device = self.devices[device_id]
                if device.register(self.server_url):
                    print(f"设备 {device_id} 注册成功")
                else:
                    print(f"设备 {device_id} 注册失败")
            except (KeyError, ValueError):
                print("无效设备ID")

    def do_start(self, arg):
        """启动设备: start [设备ID/all]"""
        if arg.lower() == 'all':
            for device in self.devices.values():
                if device.registered:
                    device.start()
                else:
                    print(f"设备 {device.device_id} 未注册，无法启动")
            print("已启动所有已注册设备")
        else:
            try:
                device_id = int(arg)
                device = self.devices[device_id]
                if device.registered:
                    device.start()
                    print(f"设备 {device_id} 已启动")
                else:
                    print(f"设备 {device_id} 未注册，请先注册")
            except (KeyError, ValueError):
                print("无效设备ID")

    def do_stop(self, arg):
        """停止设备: stop [设备ID/all]"""
        if arg.lower() == 'all':
            for device in self.devices.values():
                if device.active:
                    device.stop()
                    print(f"设备 {device.device_id} 已停止")
                else:
                    print(f"设备 {device.device_id} 未在运行")
        else:
            try:
                device_id = int(arg)
                device = self.devices[device_id]
                if device.active:
                    device.stop()
                    print(f"设备 {device_id} 已停止")
                else:
                    print(f"设备 {device_id} 未在运行")
            except (KeyError, ValueError):
                print("无效设备ID")

    def do_maintenance(self, arg):
        """发送维护验证: maintenance [设备ID]"""
        try:
            device_id = int(arg)
            device = self.devices[device_id]
            if device.send_maintenance_report():
                print("维护验证请求已发送")
            else:
                print("发送失败，请查看日志")
        except (ValueError, KeyError):
            print("参数错误，格式：maintenance [设备ID]")

    def do_exit(self, arg):
        """退出系统"""
        for device in self.devices.values():
            device.stop()
        print("系统已安全关闭")
        return True

if __name__ == '__main__':
    SERVER_URL = "http://127.0.0.1:8080/data/device"
    CSV_PATH = "devices.csv"
    try:
        DeviceManager(SERVER_URL, CSV_PATH).cmdloop()
    except Exception as e:
        print(f"系统启动失败: {str(e)}")