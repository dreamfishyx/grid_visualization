#!/bin/bash

# Description: 快速搭建本项目的本地开发运行环境,需要说明的是:ip自动获取可能不准确,建议手动输入
# Usage: ./script.sh [-start | -stop | -clear | -help]
# Author: fish
# Date: 2025.4.5

# 定义路径变量
ROOT_PATH="/home/fish/grid"
MYSQL_CONFIG_PATH="$ROOT_PATH/mysql/conf"
MYSQL_CONFIG_FILE="my.cnf"
REDIS_CONFIG_PATH="$ROOT_PATH/redis/conf"
REDIS_CONFIG_FILE="redis.conf"
MYSQL_HOST="localhost"
MYSQL_INIT_FILE="init.sql"
DOCKER_COMPOSE_FILE="compose.yaml"
MYSQL_PASSWORD_DEFAULT="abc123456"

# 获取系统ip
get_ip() {
   local ip
  # 先尝试从用户输入中获取IP地址
  read -r -p "Enter the system IP address (default:get from network interface): " ip
  # 如果用户输入了IP地址,则验证格式
  if [[ -n "$ip" && ! "$ip" =~ ^[0-9]+\.[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
    echo "error: Invalid IP address format."
    exit 1
  fi
  # 如果用户没有输入IP地址,则尝试从网络接口中获取
  if [[ -z "$ip" ]]; then
    interface=$(ip route | grep default | awk '{print $5}' | head -n1)
    ip=$(ip a show dev "$interface" | grep 'inet ' | awk '{print $2}' | cut -d'/' -f1) ||
        {
            echo "error: Unable to get the system IP address."
            exit 1
        }
  fi
  echo "Successfully obtained the system IP address: ${ip},which will be exported as an environment variable."
  # 设置环境变量
  export host_ip="$ip"
  echo "host_ip=$host_ip"
}

# 获取一些必要的输入(如密码等),用于初始化数据库
get_input(){
  read -r -s -p "Enter MySQL root password (which you set in $DOCKER_COMPOSE_FILE.default: configured in script): " MYSQL_PASSWORD
  echo
  read -r -p "Enter MySQL user name (default: root): " MYSQL_USER
  read -r -p "Enter MySQL container name (default: mysql): " MYSQL_CONTAINER_NAME
  read -r -p "Enter Kafka container name (default: kafka): " KAFKA_CONTAINER_NAME
  MYSQL_USER=${MYSQL_USER:-root}
  MYSQL_CONTAINER_NAME=${MYSQL_CONTAINER_NAME:-mysql}
  MYSQL_PASSWORD="${MYSQL_PASSWORD:-$MYSQL_PASSWORD_DEFAULT}"
  KAFKA_CONTAINER_NAME=${KAFKA_CONTAINER_NAME:-kafka}
}

# 获取脚本所在目录
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# 检查 docker 是否安装
check_docker() {
    if ! command -v docker &> /dev/null; then
        echo "docker could not be found. Please install it first."
        exit 1
    fi
}

# 检查 docker compose 是否安装
check_compose() {
    if ! command -v docker compose &> /dev/null; then
        echo "docker compose could not be found. Please install it first."
        exit 1
    fi
}

# 开启服务
start_services(){
  # 验证必要文件存在
      for file in "$MYSQL_CONFIG_FILE" "$REDIS_CONFIG_FILE" "$MYSQL_INIT_FILE" "$DOCKER_COMPOSE_FILE"; do
          if [ ! -f "$SCRIPT_DIR/$file" ]; then
              echo "error : $file not found in $SCRIPT_DIR"
              exit 1
          fi
      done

      # 创建配置目录
      mkdir -p "$MYSQL_CONFIG_PATH" "$REDIS_CONFIG_PATH"

      # 复制配置文件
      cp -f "$SCRIPT_DIR/$MYSQL_CONFIG_FILE" "$MYSQL_CONFIG_PATH/" || {
          echo "error: copy $MYSQL_CONFIG_FILE failed"
          exit 1
      }
      cp -f "$SCRIPT_DIR/$REDIS_CONFIG_FILE" "$REDIS_CONFIG_PATH/" || {
          echo "error: copy $REDIS_CONFIG_FILE failed"
          exit 1
      }
      # 启动容器
      docker compose -f "$SCRIPT_DIR/$DOCKER_COMPOSE_FILE" up -d || {
          echo "error: docker compose up failed"
          exit 1
      }

      # 等待 kafka 服务就绪
      local max_retry=30
      local retry_count=0
      echo "Waiting for Kafka to start..."
      until docker exec "$KAFKA_CONTAINER_NAME" /opt/kafka/bin/kafka-broker-api-versions.sh \
          --bootstrap-server "${host_ip}":9092 >/dev/null 2>&1; do
          if [ $retry_count -ge $max_retry ]; then
              echo "[ERROR] Kafka failed to start after $max_retry attempts."
              docker logs "$KAFKA_CONTAINER_NAME"
              exit 1
          fi
          echo "Retrying Kafka connection (attempt $((retry_count + 1))..."
          sleep 10
          ((retry_count++))
      done

      echo "Kafka container is ready.Just wait a few seconds to ensure Kafka service is fully up..."
      sleep 5

      # 创建 __consumer_offsets 主题(不知道为啥不自动创建,挺坑的,导致后续消费者无法消费消息)
      echo "Creating __consumer_offsets topic..."
      docker exec -it "$KAFKA_CONTAINER_NAME" /opt/kafka/bin/kafka-topics.sh \
        --create \
        --bootstrap-server "${host_ip}":9092 \
        --topic __consumer_offsets \
        --partitions 50 \
        --replication-factor 1 \
        --config cleanup.policy=compact

      # 等待 mysql 服务就绪
      local max_retry=30
      local retry_count=0
      echo "Waiting for mysql containers to start ..."
      until docker exec "$MYSQL_CONTAINER_NAME" mysqladmin ping --silent; do
          if [ $retry_count -ge $max_retry ]; then
              echo "error: MySQL container failed to start after $max_retry attempts."
              echo "Please run 'docker logs $MYSQL_CONTAINER_NAME' for more details."
              exit 1
          fi
          echo "Retrying MySQL connection (attempt $((retry_count + 1))..."
          sleep 5
          ((retry_count++))
      done

      # 稍等片刻,确保 MySQL 服务完全启动
      echo "MySQL container started successfully.Just waiting for a few seconds to ensure MySQL service is fully up..."
      sleep 5

      # 等待 MySQL 服务启动
      while ! docker exec "$MYSQL_CONTAINER_NAME" mysqladmin ping -h "$MYSQL_HOST" --silent; do
          sleep 1
      done
      echo "Getting ready to execute database initialization script..."

      # 运行数据库初始化脚本
      docker exec -i "$MYSQL_CONTAINER_NAME" mysql -h "$MYSQL_HOST" -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" < "$SCRIPT_DIR/$MYSQL_INIT_FILE" || {
          echo "error: Failed to execute MySQL initialization script."
          exit 1
      }
      echo "MySQL initialization script executed successfully."

      # 通过 docker ps 查看容器状态
      echo -e "Docker containers are running:\n"
      docker ps --format "table {{.ID}}\t{{.Image}}\t{{.Status}}\t{{.Names}}"
      echo -e "\nTo stop the containers, run: $0 -stop"
}

# 检查 docker 和 docker compose 是否安装
check_docker
check_compose
# 根据传入参数执行脚本
case "$1" in
    "")
        # 获取系统ip并设置环境变量
        get_ip
        # 启动服务
        docker compose -f "$SCRIPT_DIR/$DOCKER_COMPOSE_FILE" up -d
    ;;
    -start | -t)
        # 获取系统ip并设置环境变量
        get_ip
        # 启动服务
        docker compose -f "$SCRIPT_DIR/$DOCKER_COMPOSE_FILE" up -d
    ;;
    -init | -i)
        # 获取系统ip并设置环境变量
        get_ip
        # 获取必要的输入
        get_input
        # 启动服务
        start_services
        ;;
    -stop | -p)
        # 停止并删除容器
        echo "Stopping and removing containers..."
        docker compose -f "$SCRIPT_DIR/$DOCKER_COMPOSE_FILE" down -v || {
            echo "error: docker compose down failed"
            exit 1
        }
        echo "Containers have been stopped and removed."
        ;;
    -clear | -c)
        read -r -p " Are you sure you want to delete all data and stop the containers? (y/n): " confirm
                if [[ $confirm == [yY] ]]; then
                    docker compose -f "$SCRIPT_DIR/$DOCKER_COMPOSE_FILE" down -v
                    sudo rm -rf "$ROOT_PATH"
                    echo "All data has been cleaned up, and containers have been stopped and removed."
                else
                    echo "Operation cancelled. No data has been deleted and containers have not been stopped."
                fi
        ;;
    -help | -h)
        # 显示帮助信息
        echo "This script is used to quickly set up a local development environment.To use it, please ensure that docker and docker compose are installed."
        echo "[Usage]: $0 [-init | -start(default) | -stop | -clear | -help]"
        echo "  -init,-i: Initialize the services"
        echo "  -start,-t: Start the containers(services)"
        echo "  -stop,-p: Stop the services"
        echo "  -clear,-c: Stop the services and remove all data"
        echo "  -help,-h: Show this help message"
        ;;
    *)
        # 无效参数处理
        echo "error : Invalid argument. Please use -help for usage information."
        exit 1
esac