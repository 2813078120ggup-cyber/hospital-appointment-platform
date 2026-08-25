package com.atguigu.java.ai.langchain4j.assistant;

import dev.langchain4j.service.spring.AiService;
import dev.langchain4j.service.spring.AiServiceWiringMode;
@AiService(wiringMode = AiServiceWiringMode.EXPLICIT, chatModel = "openAiChatModel")
// openAiChatModel由maven提供，properties配置
public interface Assistant {
	//@SystemMessage("你是一名Java面试官")
	String chat(String userMessage);
}