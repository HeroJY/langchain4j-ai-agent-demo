package com.example.langchain4jdeepseek.tools;

import com.example.langchain4jdeepseek.config.TavilyConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.P;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

/**
 * Tavily搜索工具类
 * 用于通过Tavily API进行网络搜索，获取最新信息
 */
@Component
public class TavilySearchTool {

    private static final Logger logger = LoggerFactory.getLogger(TavilySearchTool.class);

    private final TavilyConfig tavilyConfig;
    private final ObjectMapper objectMapper;

    @Autowired
    public TavilySearchTool(TavilyConfig tavilyConfig) {
        this.tavilyConfig = tavilyConfig;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 使用Tavily API进行网络搜索，获取最新信息
     * @param query 搜索查询词
     * @return 格式化的搜索结果
     */
    @Tool("当需要查询最新信息时使用，例如查询当前天气、事件、新闻、股票价格等")
    public String searchWeb(@P("The search query") String query) {
        logger.info("正在搜索关键词: {}", query);

        // 检查配置是否正确加载
        if (tavilyConfig.apiUrl == null) {
            logger.error("Tavily API URL 未配置");
            return "搜索出错: Tavily API URL 未配置";
        }
        
        if (tavilyConfig.apiKey == null) {
            logger.error("Tavily API Key 未配置");
            return "搜索出错: Tavily API Key 未配置";
        }

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            // 创建请求载荷
            String requestBody = createRequestBody(query);
            
            // 创建HTTP POST请求
            String fullUrl = tavilyConfig.apiUrl + "/search";
            HttpPost httpPost = new HttpPost(fullUrl);
            httpPost.setEntity(new StringEntity(requestBody, ContentType.APPLICATION_JSON));
            httpPost.setHeader("Authorization", "Bearer " + tavilyConfig.apiKey);
            httpPost.setHeader("Content-Type", "application/json");

            // 执行请求
            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                // 读取响应
                String responseBody = readResponseBody(response);
                
                // 解析并格式化响应
                return parseAndFormatResponse(responseBody);
            }
        } catch (Exception e) {
            logger.error("搜索关键词时发生错误: {}", query, e);
            return "搜索出错: " + e.getMessage();
        }
    }

    /**
     * 创建Tavily API的请求体
     * @param query 搜索查询词
     * @return JSON格式的请求体字符串
     * @throws Exception 创建请求体时可能发生的异常
     */
    private String createRequestBody(String query) throws Exception {
        // 为Tavily API创建JSON请求体
        JsonNode requestBody = objectMapper.createObjectNode()
                .put("query", query)
                .put("include_answer", true)      // 包含答案
                .put("include_images", false)     // 不包含图片
                .put("include_raw_content", false) // 不包含原始内容
                .put("max_results", 5);           // 最大结果数
        
        return objectMapper.writeValueAsString(requestBody);
    }

    /**
     * 读取HTTP响应体
     * @param response HTTP响应对象
     * @return 响应体内容字符串
     * @throws IOException IO异常
     */
    private String readResponseBody(CloseableHttpResponse response) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent()))) {
            return reader.lines().collect(Collectors.joining("\n"));
        }
    }

    /**
     * 解析并格式化API响应
     * @param responseBody 原始响应体字符串
     * @return 格式化的搜索结果
     * @throws Exception 解析过程中可能发生的异常
     */
    private String parseAndFormatResponse(String responseBody) throws Exception {
        JsonNode rootNode = objectMapper.readTree(responseBody);
        
        // 检查响应中是否有错误
        if (rootNode.has("error")) {
            String errorMessage = rootNode.get("error").asText();
            logger.error("Tavily API 错误: {}", errorMessage);
            return "Tavily API 错误: " + errorMessage;
        }
        
        // 提取答案（如果可用）
        StringBuilder result = new StringBuilder();
        if (rootNode.has("answer") && !rootNode.get("answer").isNull()) {
            result.append("答案: ").append(rootNode.get("answer").asText()).append("\n\n");
        }
        
        // 提取结果
        if (rootNode.has("results")) {
            JsonNode resultsNode = rootNode.get("results");
            if (resultsNode.isArray() && resultsNode.size() > 0) {
                result.append("热门结果:\n");
                for (int i = 0; i < Math.min(resultsNode.size(), 3); i++) {
                    JsonNode resultNode = resultsNode.get(i);
                    String title = resultNode.has("title") ? resultNode.get("title").asText() : "无标题";
                    String content = resultNode.has("content") ? resultNode.get("content").asText() : "无内容";
                    String url = resultNode.has("url") ? resultNode.get("url").asText() : "无URL";
                    
                    result.append(String.format("%d. %s\n   %s\n   来源: %s\n\n", i + 1, title, content, url));
                }
            }
        }
        
        return result.length() > 0 ? result.toString().trim() : "未找到结果。";
    }
}