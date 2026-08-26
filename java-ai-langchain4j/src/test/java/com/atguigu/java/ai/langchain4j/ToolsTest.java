package com.atguigu.java.ai.langchain4j;

import com.atguigu.java.ai.langchain4j.assistant.SeparateChatAssistant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

// 功能完善：工具演示需要真实模型参与，改为显式集成测试以保证默认构建可重复。
@EnabledIfEnvironmentVariable(named = "XIAOZHI_RUN_INTEGRATION_TESTS", matches = "(?i)true")
@SpringBootTest
public class ToolsTest {
    
    @Autowired
    private SeparateChatAssistant separateChatAssistant;
    
    @Test
    public void testCalculatorTools() {
    	String answer = separateChatAssistant.chat(1, "1+2等于几，475695037565的平方根是多少？");
    	//答案：3，689706.4865
    	System.out.println(answer);
    }
}
