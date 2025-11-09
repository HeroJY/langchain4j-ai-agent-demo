#!/bin/bash

# 本地启动脚本 - 使用application-local.properties配置文件
# 方法1: 使用Spring Profile (推荐)
# 方法2: 直接指定配置文件位置

# 检查参数
if [ "$1" = "--profile" ] || [ -z "$1" ]; then
    echo "使用Spring Profile方式启动..."
    # 设置Spring配置文件为local
    export SPRING_PROFILES_ACTIVE=local
    # 启动Spring Boot应用
    mvn spring-boot:run
elif [ "$1" = "--config" ]; then
    echo "使用直接指定配置文件方式启动..."
    # 直接指定配置文件路径启动Spring Boot应用
    mvn spring-boot:run -Dspring-boot.run.arguments="--spring.config.location=classpath:/application.properties,classpath:/application-local.properties"
else
    echo "使用方法:"
    echo "  $0              # 使用Spring Profile方式启动 (默认)"
    echo "  $0 --profile    # 使用Spring Profile方式启动"
    echo "  $0 --config     # 直接指定配置文件方式启动"
    exit 1
fi