package com.atguigu.java.ai.langchain4j;

import com.atguigu.java.ai.langchain4j.assistant.Assistant;
import dev.langchain4j.community.model.dashscope.QwenChatModel;
import dev.langchain4j.community.model.dashscope.WanxImageModel;
import dev.langchain4j.data.image.Image;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.service.AiServices;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

// 功能完善：该测试会调用真实模型和图片接口，必须由联调开关显式授权。
@EnabledIfEnvironmentVariable(named = "XIAOZHI_RUN_INTEGRATION_TESTS", matches = "(?i)true")
@SpringBootTest
public class LLMTest {
    
    /**
     * gpt-4o-mini语言模型接入测试
     */
    @Test
    public void testGPTDemo() {
        //初始化模型
        OpenAiChatModel model = OpenAiChatModel.builder()
                //LangChain4j提供的代理服务器，该代理服务器会将演示密钥替换成真实密钥， 再将请求转发给OpenAI API
                .baseUrl("http://langchain4j.dev/demo/openai/v1")
                .apiKey("demo") //设置模型apiKey
                .modelName("gpt-4o-mini") //设置模型名称
                .build();
        
        //向模型提问
        String answer = model.chat("who is 赵仪？");
        //输出结果
        System.out.println(answer);
    }
    
    
    /**
     * 整合SpringBoot
     */
    @Autowired
    private OpenAiChatModel chatModel;
    
    @Test
    public void testSpringBoot() {
        //向模型提问
        //String answer = openAiChatModel.chat("你好，乡宁一中的赵仪是谁？");
        String answer = chatModel.chat("帮我生成一张小树的图片");
        //输出结果
        System.out.println(answer);
    }
    
    @Value("${langchain4j.open-ai.chat-model.api-key}")
    private String apiKey;
    
    @Test
    public void testWanxImageModel() {
        WanxImageModel wanxImageModel = WanxImageModel.builder()
                .modelName("wanx2.1-t2i-plus")
                .apiKey(apiKey)
                .build();
        Response<Image> response = wanxImageModel.generate("奇幻森林精灵：在一片弥漫着轻柔薄雾的\n" +
                "                古老森林深处，阳光透过茂密枝叶洒下金色光斑。一位身材娇小、长着透明薄翼的精灵少女站在一朵硕大的蘑菇上。她\n" +
                "                有着海藻般的绿色长发，发间点缀着蓝色的小花，皮肤泛着珍珠般的微光。身上穿着由翠绿树叶和白色藤蔓编织而成的\n" +
                "                连衣裙，手中捧着一颗散发着柔和光芒的水晶球，周围环绕着五彩斑斓的蝴蝶，脚下是铺满苔藓的地面，蘑菇和蕨类植\n" +
                "                物丛生，营造出神秘而梦幻的氛围。");
        System.out.println(response.content().url());
    }
    
    
    @Autowired
    private OllamaChatModel ollamaChatModel;
    
    @Test
    public void testOllama() {
        String answer = ollamaChatModel.chat("你好，乡宁一中的赵仪是谁？");
        System.out.println(answer);
    }
    
    
    
    
}
