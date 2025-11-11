package com.example.langchain4jdeepseek.service;
import dev.langchain4j.model.chat.response.ChatResponse;

/**
 * 自定义流式响应处理器接口
 * @param <T> 响应类型
 */
public interface StreamingResponseHandler<T> {
    /**
     * 处理下一个token
     * @param token 接收到的token
     */
    void onNext(String token);
    
    /**
     * 处理完整响应
     * @param response 完整响应
     */
    void onComplete(ChatResponse response);
    
    /**
     * 处理错误
     * @param error 错误信息
     */
    void onError(Throwable error);
}