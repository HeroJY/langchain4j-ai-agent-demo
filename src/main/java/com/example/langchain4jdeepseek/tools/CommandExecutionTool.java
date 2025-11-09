package com.example.langchain4jdeepseek.tools;

import dev.langchain4j.agent.tool.Tool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.core.io.ClassPathResource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Set;

@Component
public class CommandExecutionTool {
    
    private static final Logger logger = LoggerFactory.getLogger(CommandExecutionTool.class);
    private final Set<String> blacklistedCommands = new HashSet<>();
    
    public CommandExecutionTool() {
        loadBlacklistedCommands();
    }
    
    private void loadBlacklistedCommands() {
        try {
            ClassPathResource resource = new ClassPathResource("command-blacklist.txt");
            InputStream inputStream = resource.getInputStream();
            
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    // 忽略注释行和空行
                    if (!line.trim().isEmpty() && !line.trim().startsWith("#")) {
                        blacklistedCommands.add(line.trim());
                    }
                }
            }
            
            logger.info("Loaded {} blacklisted commands", blacklistedCommands.size());
            
            // 如果黑名单为空，则禁用命令执行功能
            if (blacklistedCommands.isEmpty()) {
                logger.error("Command blacklist is empty or not loaded. Disabling command execution for security reasons.");
                blacklistedCommands.add("*"); // 添加通配符以阻止所有命令
            }
        } catch (IOException e) {
            logger.error("Failed to load command blacklist. Disabling command execution for security reasons.", e);
            // 如果加载失败，禁用命令执行功能
            blacklistedCommands.add("*"); // 添加通配符以阻止所有命令
        }
    }
    
    private boolean isCommandBlacklisted(String command) {
        // 如果黑名单包含通配符，则阻止所有命令
        if (blacklistedCommands.contains("*")) {
            logger.warn("All commands are blocked due to security policy");
            return true;
        }
        
        // 检查命令是否包含黑名单中的任何关键词
        for (String blacklistedCommand : blacklistedCommands) {
            // 跳过空行和注释
            if (blacklistedCommand.isEmpty() || blacklistedCommand.startsWith("#")) {
                continue;
            }
            
            // 检查命令是否包含黑名单关键词
            if (command.contains(blacklistedCommand)) {
                logger.warn("Command '{}' contains blacklisted keyword: '{}'", command, blacklistedCommand);
                return true;
            }
            
            // 检查命令是否以黑名单关键词开头（用于精确匹配命令名）
            String[] commandParts = command.trim().split("\\s+");
            if (commandParts.length > 0 && commandParts[0].equals(blacklistedCommand)) {
                logger.warn("Command '{}' starts with blacklisted command: '{}'", command, blacklistedCommand);
                return true;
            }
        }
        
        return false;
    }

    @Tool("Execute a shell command on the local system and return the output")
    public String executeCommand(String command) {
        logger.info("Attempting to execute command: {}", command);
        
        // 首先检查命令是否在黑名单中
        if (isCommandBlacklisted(command)) {
            String errorMsg = "Error: Command contains blacklisted keywords and cannot be executed for security reasons.";
            logger.error("Blocked execution of blacklisted command: {}", command);
            return errorMsg;
        }
        
        try {
            ProcessBuilder processBuilder = new ProcessBuilder();
            processBuilder.command("bash", "-c", command);
            
            Process process = processBuilder.start();
            
            StringBuilder output = new StringBuilder();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
            
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                logger.error("Command execution failed with exit code: {}", exitCode);
                return "Error: Command execution failed with exit code " + exitCode;
            }
            
            logger.info("Command executed successfully. Output: {}", output.toString());
            return output.toString();
        } catch (IOException | InterruptedException e) {
            logger.error("Error executing command: {}", command, e);
            return "Error executing command: " + e.getMessage();
        }
    }
}