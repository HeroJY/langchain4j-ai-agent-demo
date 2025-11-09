#!/bin/bash

# 综合测试脚本 - 测试所有功能

echo "=== LangChain4J + DeepSeek MVP 综合测试 ==="
echo ""

# 检查服务器是否运行
echo "1. 检查服务器状态..."
curl -s http://localhost:8080/api/chat -H "Content-Type: application/json" -d '{"message": "ping"}' | grep -q "response"
if [ $? -eq 0 ]; then
    echo "✓ 服务器运行正常"
else
    echo "✗ 服务器未运行，请先启动服务器"
    exit 1
fi

echo ""
echo "2. 测试基本对话功能..."

# 测试基本对话
echo "问: 你好，DeepSeek！"
RESPONSE=$(curl -s -X POST http://localhost:8080/api/chat -H "Content-Type: application/json" -d '{"message": "你好，DeepSeek！"}')
echo "答: $(echo $RESPONSE | jq -r '.response')"
echo ""

# 测试技术问题
echo "问: 什么是人工智能？"
RESPONSE=$(curl -s -X POST http://localhost:8080/api/chat -H "Content-Type: application/json" -d '{"message": "什么是人工智能？"}')
echo "答: $(echo $RESPONSE | jq -r '.response')"
echo ""

echo "3. 测试工具调用功能..."

# 测试命令执行工具 - 列出文件
echo "问: 请列出当前目录的文件"
RESPONSE=$(curl -s -X POST http://localhost:8080/api/chat -H "Content-Type: application/json" -d '{"message": "请列出当前目录的文件"}')
echo "答: $(echo $RESPONSE | jq -r '.response')"
echo ""

# 测试命令执行工具 - 系统信息
echo "问: 请告诉我当前的操作系统信息"
RESPONSE=$(curl -s -X POST http://localhost:8080/api/chat -H "Content-Type: application/json" -d '{"message": "请告诉我当前的操作系统信息"}')
echo "答: $(echo $RESPONSE | jq -r '.response')"
echo ""

# 测试命令执行工具 - 数学计算
echo "问: 请计算2的10次方是多少"
RESPONSE=$(curl -s -X POST http://localhost:8080/api/chat -H "Content-Type: application/json" -d '{"message": "请计算2的10次方是多少"}')
echo "答: $(echo $RESPONSE | jq -r '.response')"
echo ""

# 测试Tavily搜索工具
echo "问: 请搜索人工智能的最新发展"
RESPONSE=$(curl -s -X POST http://localhost:8080/api/chat -H "Content-Type: application/json" -d '{"message": "请搜索人工智能的最新发展"}')
echo "答: $(echo $RESPONSE | jq -r '.response')"
echo ""

echo "4. 测试上下文管理功能..."

# 测试上下文管理
echo "问: 我的名字是张三"
RESPONSE=$(curl -s -X POST http://localhost:8080/api/chat -H "Content-Type: application/json" -d '{"message": "我的名字是张三"}')
echo "答: $(echo $RESPONSE | jq -r '.response')"
echo ""

echo "问: 我刚才告诉你我的名字是什么？"
RESPONSE=$(curl -s -X POST http://localhost:8080/api/chat -H "Content-Type: application/json" -d '{"message": "我刚才告诉你我的名字是什么？"}')
echo "答: $(echo $RESPONSE | jq -r '.response')"
echo ""

echo "=== 所有测试完成 ==="