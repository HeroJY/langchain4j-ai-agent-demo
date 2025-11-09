package com.example.langchain4jdeepseek.service;

import org.springframework.stereotype.Component;
import org.springframework.core.io.ClassPathResource;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.time.LocalDateTime;
import java.io.IOException;

/**
 * 系统提示词动态管理器
 * 支持根据不同场景动态调整系统提示词
 */
@Component
public class SystemPromptManager {
    
    // 存储不同场景的系统提示词模板
    private final Map<String, String> promptTemplates = new HashMap<>();
    
    // 存储动态变量
    private final Map<String, String> dynamicVariables = new HashMap<>();
    
    public SystemPromptManager() {
        // 初始化默认的系统提示词模板
        initializeDefaultTemplates();
    }
    
    /**
     * 初始化默认的系统提示词模板
     */
    private void initializeDefaultTemplates() {
        // 首先加载默认场景，确保至少有一个可用的提示词
        loadPromptFromFile("default");
        
        // 从文件加载其他场景的系统提示词模板
        loadPromptFromFile("translator");
        loadPromptFromFile("code_reviewer");
        loadPromptFromFile("technical_writer");
        loadPromptFromFile("customer_support");
    }
    
    /**
     * 从文件加载指定场景的提示词
     * @param scenario 场景标识
     */
    private void loadPromptFromFile(String scenario) {
        try {
            ClassPathResource resource = new ClassPathResource("system-prompts/" + scenario + ".prompt");
            if (resource.exists()) {
                // 使用getInputStream()而不是getFile()，这样可以在JAR包中正常工作
                String content = new String(resource.getInputStream().readAllBytes()).trim();
                promptTemplates.put(scenario, content);
            } else {
                // 文件不存在时使用默认提示词
                if ("default".equals(scenario)) {
                    // 如果是默认文件不存在，使用硬编码的默认提示词
                    promptTemplates.put(scenario, "You are a helpful AI assistant. Please provide accurate and helpful responses.");
                } else {
                    System.err.println("Prompt file not found for scenario: " + scenario + ", using default prompt");
                    // 对于其他场景，使用默认提示词
                    promptTemplates.put(scenario, promptTemplates.getOrDefault("default", "You are a helpful AI assistant."));
                }
            }
        } catch (IOException e) {
            // 如果文件读取失败，使用默认提示词
            System.err.println("Failed to read prompt file for scenario: " + scenario + ", using default prompt. Error: " + e.getMessage());
            if ("default".equals(scenario)) {
                // 如果是默认文件读取失败，使用硬编码的默认提示词
                promptTemplates.put(scenario, "You are a helpful AI assistant. Please provide accurate and helpful responses.");
            } else {
                // 对于其他场景，使用默认提示词
                promptTemplates.put(scenario, promptTemplates.getOrDefault("default", "You are a helpful AI assistant."));
            }
        }
    }
    
    /**
     * 获取指定场景的系统提示词
     * @param scenario 场景标识
     * @return 系统提示词
     */
    public String getSystemPrompt(String scenario) {
        String template = promptTemplates.getOrDefault(scenario, promptTemplates.get("default"));
        return replaceDynamicVariables(template);
    }
    
    /**
     * 获取默认系统提示词
     * @return 默认系统提示词
     */
    public String getDefaultSystemPrompt() {
        return getSystemPrompt("default");
    }
    
    /**
     * 添加或更新系统提示词模板
     * @param scenario 场景标识
     * @param template 提示词模板
     */
    public void addPromptTemplate(String scenario, String template) {
        promptTemplates.put(scenario, template);
    }
    
    /**
     * 移除系统提示词模板
     * @param scenario 场景标识
     */
    public void removePromptTemplate(String scenario) {
        promptTemplates.remove(scenario);
    }
    
    /**
     * 获取所有可用的场景
     * @return 场景列表
     */
    public List<String> getAvailableScenarios() {
        return List.copyOf(promptTemplates.keySet());
    }
    
    /**
     * 设置动态变量
     * @param key 变量名
     * @param value 变量值
     */
    public void setDynamicVariable(String key, String value) {
        dynamicVariables.put(key, value);
    }
    
    /**
     * 移除动态变量
     * @param key 变量名
     */
    public void removeDynamicVariable(String key) {
        dynamicVariables.remove(key);
    }
    
    /**
     * 批量设置动态变量
     * @param variables 变量映射
     */
    public void setDynamicVariables(Map<String, String> variables) {
        dynamicVariables.putAll(variables);
    }
    
    /**
     * 替换模板中的动态变量
     * @param template 模板字符串
     * @return 替换后的字符串
     */
    private String replaceDynamicVariables(String template) {
        String result = template;
        
        // 添加时间相关变量
        LocalDateTime now = LocalDateTime.now();
        result = result.replace("${current_date}", now.toLocalDate().toString());
        result = result.replace("${current_time}", now.toLocalTime().toString());
        result = result.replace("${current_datetime}", now.toString());
        
        // 替换自定义变量
        for (Map.Entry<String, String> entry : dynamicVariables.entrySet()) {
            result = result.replace("${" + entry.getKey() + "}", entry.getValue());
        }
        
        return result;
    }
    
    /**
     * 构建上下文感知的系统提示词
     * @param baseScenario 基础场景
     * @param context 上下文信息
     * @return 完整的系统提示词
     */
    public String buildContextualPrompt(String baseScenario, Map<String, Object> context) {
        StringBuilder promptBuilder = new StringBuilder(getSystemPrompt(baseScenario));
        
        if (context != null && !context.isEmpty()) {
            promptBuilder.append("\n\nContext Information:\n");
            for (Map.Entry<String, Object> entry : context.entrySet()) {
                promptBuilder.append(entry.getKey())
                            .append(": ")
                            .append(entry.getValue())
                            .append("\n");
            }
        }
        
        return promptBuilder.toString();
    }
}