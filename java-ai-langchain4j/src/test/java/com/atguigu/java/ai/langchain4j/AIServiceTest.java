package com.atguigu.java.ai.langchain4j;

import com.atguigu.java.ai.langchain4j.assistant.Assistant;
import com.atguigu.java.ai.langchain4j.assistant.MemoryChatAssistant;
import com.atguigu.java.ai.langchain4j.assistant.SeparateChatAssistant;
import dev.langchain4j.community.model.dashscope.QwenChatModel;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

// 功能完善：依赖真实模型的演示测试仅在显式联调时启用，默认构建不访问外部服务。
@EnabledIfEnvironmentVariable(named = "XIAOZHI_RUN_INTEGRATION_TESTS", matches = "(?i)true")
@SpringBootTest
public class AIServiceTest {
    
    @Autowired
    private Assistant assistant;
    @Autowired
    private OpenAiChatModel openAiChatModel;
    
    @Test
    public void testChat() {
        //创建AIService
        Assistant assistant = AiServices.create(Assistant.class, openAiChatModel);
        //调用service的接口
        //String answer = assistant.chat("你是谁？");
        //System.out.println(answer);
        String answer1 = assistant.chat("我是环环");
        System.out.println(answer1);
        String answer2 = assistant.chat("我是谁");
        System.out.println(answer2);
    }
    
    
    @Test
    public void testChatMemory3() {
        //创建chatMemory
        MessageWindowChatMemory chatMemory =
                MessageWindowChatMemory.withMaxMessages(10);
        
        //创建AIService
        Assistant assistant = AiServices
                .builder(Assistant.class)
                .chatModel(openAiChatModel)
                .chatMemory(chatMemory)
                .build();
        
        //调用service的接口
        String answer1 = assistant.chat("我是环环");
        System.out.println(answer1);
        String answer2 = assistant.chat("我是谁");
        System.out.println(answer2);
    }
    
    
    @Autowired
    private MemoryChatAssistant memoryChatAssistant;
    
    @Test
    public void testChatMemory4() {
        
        String answer1 = memoryChatAssistant.chat("我是环环");
        System.out.println(answer1);
        
        String answer2 = memoryChatAssistant.chat("我是谁");
        System.out.println(answer2);
        
    }
    
    
    @Autowired
    private SeparateChatAssistant separateChatAssistant;
    
    @Test
    public void testChatMemory5() {
        
        String answer1 = separateChatAssistant.chat(1, "我是张三");
        System.out.println(answer1);
        
        String answer2 = separateChatAssistant.chat(1, "我是谁");
        System.out.println(answer2);
        
        String answer3 = separateChatAssistant.chat(2, "我是谁");
        System.out.println(answer3);
        
    }
    
}
