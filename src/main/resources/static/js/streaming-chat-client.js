// 流式聊天客户端示例
class StreamingChatClient {
    constructor(baseUrl = '') {
        this.baseUrl = baseUrl;
        this.eventSource = null;
        this.currentSessionId = null;
    }
    
    // 发送消息并接收流式响应
    async sendMessage(message, options = {}) {
        const { scenario = '', onToken = () => {}, onComplete = () => {}, onError = () => {} } = options;
        
        try {
            // 准备请求数据
            const requestData = { message };
            if (scenario) {
                requestData.scenario = scenario;
            }
            
            // 使用fetch API处理流式响应
            const response = await fetch(`${this.baseUrl}/api/chat/stream`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Accept': 'text/event-stream',
                    'Cache-Control': 'no-cache'
                },
                body: JSON.stringify(requestData)
            });
            
            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }
            
            const reader = response.body.getReader();
            const decoder = new TextDecoder();
            let buffer = '';
            
            // 处理流式响应
            while (true) {
                const { done, value } = await reader.read();
                
                if (done) {
                    onComplete();
                    break;
                }
                
                // 解码数据并处理
                buffer += decoder.decode(value, { stream: true });
                const lines = buffer.split('\n');
                buffer = lines.pop(); // 保留不完整的行
                
                for (const line of lines) {
                    if (line.trim() === '') continue;
                    
                    // 处理SSE格式数据
                    if (line.startsWith('data: ')) {
                        const data = line.substring(6);
                        
                        try {
                            // 尝试解析JSON数据
                            const eventData = JSON.parse(data);
                            
                            if (eventData.type === 'token') {
                                onToken(eventData.content);
                            } else if (eventData.type === 'session-id') {
                                this.currentSessionId = eventData.content;
                            } else if (eventData.type === 'complete') {
                                onComplete();
                            } else if (eventData.type === 'error') {
                                onError(eventData.content);
                            }
                        } catch (e) {
                            // 如果不是JSON格式，直接作为token处理
                            onToken(data);
                        }
                    }
                }
            }
        } catch (error) {
            onError(error.message);
        }
    }
    
    // 获取会话内容
    async getSessionContent(sessionId) {
        try {
            const response = await fetch(`${this.baseUrl}/api/chat/stream/${sessionId}`);
            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }
            return await response.json();
        } catch (error) {
            console.error('获取会话内容失败:', error);
            throw error;
        }
    }
    
    // 获取可用场景
    async getScenarios() {
        try {
            const response = await fetch(`${this.baseUrl}/api/chat/scenarios`);
            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }
            return await response.json();
        } catch (error) {
            console.error('获取场景失败:', error);
            throw error;
        }
    }
    
    // 关闭连接
    close() {
        if (this.eventSource) {
            this.eventSource.close();
            this.eventSource = null;
        }
    }
}

// 使用示例
const chatClient = new StreamingChatClient();

// 示例1: 基本用法
async function basicExample() {
    console.log('开始基本示例...');
    
    let fullResponse = '';
    
    await chatClient.sendMessage('你好，请介绍一下你自己', {
        onToken: (token) => {
            // 处理每个token
            fullResponse += token;
            console.log('收到token:', token);
        },
        onComplete: () => {
            console.log('完整响应:', fullResponse);
        },
        onError: (error) => {
            console.error('错误:', error);
        }
    });
}

// 示例2: 带场景的用法
async function scenarioExample() {
    console.log('开始场景示例...');
    
    let fullResponse = '';
    
    // 获取可用场景
    const scenarios = await chatClient.getScenarios();
    console.log('可用场景:', scenarios);
    
    // 使用第一个场景
    if (scenarios.length > 0) {
        const scenario = scenarios[0];
        console.log(`使用场景: ${scenario}`);
        
        await chatClient.sendMessage('请帮我完成一个任务', {
            scenario: scenario,
            onToken: (token) => {
                fullResponse += token;
                console.log('收到token:', token);
            },
            onComplete: () => {
                console.log('完整响应:', fullResponse);
                
                // 获取会话内容
                if (chatClient.currentSessionId) {
                    chatClient.getSessionContent(chatClient.currentSessionId)
                        .then(content => {
                            console.log('会话内容:', content);
                        })
                        .catch(error => {
                            console.error('获取会话内容失败:', error);
                        });
                }
            },
            onError: (error) => {
                console.error('错误:', error);
            }
        });
    }
}

// 示例3: 集成到HTML页面
function integrateWithHtml() {
    // 获取DOM元素
    const messageInput = document.getElementById('messageInput');
    const sendButton = document.getElementById('sendButton');
    const messagesContainer = document.getElementById('messages');
    
    if (!messageInput || !sendButton || !messagesContainer) {
        console.error('缺少必要的DOM元素');
        return;
    }
    
    // 添加消息到聊天界面
    function addMessage(text, sender) {
        const messageElement = document.createElement('div');
        messageElement.className = `message ${sender}-message`;
        messageElement.textContent = text;
        
        messagesContainer.appendChild(messageElement);
        messagesContainer.scrollTop = messagesContainer.scrollHeight;
        
        return messageElement;
    }
    
    // 发送消息
    async function sendMessage() {
        const message = messageInput.value.trim();
        if (!message) return;
        
        // 清空输入框并禁用发送按钮
        messageInput.value = '';
        sendButton.disabled = true;
        
        // 添加用户消息
        addMessage(message, 'user');
        
        // 创建AI消息元素
        let aiMessageElement = addMessage('', 'ai');
        let fullResponse = '';
        
        try {
            await chatClient.sendMessage(message, {
                onToken: (token) => {
                    // 处理英文单词间的空格问题
                    let tokenToAdd = token;
                    
                    // 如果当前响应不为空且当前token不以空格开头，且当前响应最后一个字符不是空格
                    // 并且当前token是英文单词（包含字母），则在前面添加空格
                    if (fullResponse.length > 0 && 
                        !tokenToAdd.startsWith(' ') && 
                        !fullResponse.endsWith(' ') &&
                        !fullResponse.endsWith('\n') &&
                        /^[a-zA-Z]/.test(tokenToAdd)) {
                        
                        // 检查当前响应最后一个字符是否是标点符号，如果是则添加空格
                        const lastChar = fullResponse.slice(-1);
                        if (/[a-zA-Z0-9,.!?;:]/.test(lastChar)) {
                            tokenToAdd = ' ' + tokenToAdd;
                        }
                    }
                    
                    fullResponse += tokenToAdd;
                    aiMessageElement.textContent = fullResponse;
                },
                onComplete: () => {
                    sendButton.disabled = false;
                },
                onError: (error) => {
                    aiMessageElement.textContent += `\n\n错误: ${error}`;
                    sendButton.disabled = false;
                }
            });
        } catch (error) {
            aiMessageElement.textContent = `错误: ${error.message}`;
            sendButton.disabled = false;
        }
    }
    
    // 添加事件监听器
    sendButton.addEventListener('click', sendMessage);
    messageInput.addEventListener('keypress', (e) => {
        if (e.key === 'Enter') {
            sendMessage();
        }
    });
}

// 导出客户端类
if (typeof module !== 'undefined' && module.exports) {
    module.exports = StreamingChatClient;
}