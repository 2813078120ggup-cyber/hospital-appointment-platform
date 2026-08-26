package com.atguigu.java.ai.langchain4j;

import com.atguigu.java.ai.langchain4j.bean.ChatMessages;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

// 功能完善：MongoDB 增删改查演示会改变本地数据，仅在显式集成测试中运行。
@EnabledIfEnvironmentVariable(named = "XIAOZHI_RUN_INTEGRATION_TESTS", matches = "(?i)true")
@SpringBootTest
public class MongoCrudTest {
    
    @Autowired
    private MongoTemplate mongoTemplate;
    
    /**
     * 插入文档
     */
    //@Test
    //public void testInsert() {
    //    mongoTemplate.insert(new ChatMessages(1L, "聊天记录"));
    //
    //}
    
    /**
     * 插入文档
     */
    @Test
    public void testInsert2() {
        ChatMessages chatMessages = new ChatMessages();
        chatMessages.setContent("聊天记录列表");
        mongoTemplate.insert(chatMessages);
    }
    
    /**
     * 根据id查询文档
     */
    @Test
    public void testFindById() {
        ChatMessages chatMessages = mongoTemplate.findById("6801ead733ba9c4a0d9b6c7b",
                ChatMessages.class);
        System.out.println(chatMessages);
    }
    
    /**
     * 修改文档
     * <p>
     * update  user   set content='新的聊天记录列表' where _id='6a8d2a29c4ac8c28e95355c1'
     */
    @Test
    public void testUpdate() {
        Criteria criteria = Criteria.where("_id").is("6a8d2a29c4ac8c28e95355c1");
        Query query = new Query(criteria);
        
        Update update = new Update();
        
        update.set("content", "新的聊天记录列表");
        
        //修改或新增
        mongoTemplate.upsert(query, update, ChatMessages.class);
    }
    
    /**
     * 新增或修改文档
     */
    @Test
    public void testUpdate2() {
        Criteria criteria = Criteria.where("_id").is("100");
        Query query = new Query(criteria);
        Update update = new Update();
        update.set("content", "新的聊天记录列表");
        //修改或新增
        mongoTemplate.upsert(query, update, ChatMessages.class);
    }
    
    /**
     * 删除文档
     * delete from  user where _id='100'
     */
    @Test
    public void testDelete() {
        Criteria criteria = Criteria.where("_id").is("100");
        Query query = new Query(criteria);
        mongoTemplate.remove(query, ChatMessages.class);
    }
}
