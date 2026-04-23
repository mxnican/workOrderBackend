package com.wly.workorder.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@SpringBootTest(properties = {
  "spring.datasource.url=jdbc:h2:mem:workorder_auth;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false",
  "spring.datasource.driver-class-name=org.h2.Driver",
  "spring.datasource.username=sa",
  "spring.datasource.password=",
  "spring.sql.init.mode=always"
})
@AutoConfigureMockMvc
class AuthFlowTest {
  @Autowired
  private MockMvc mockMvc;
  @Autowired
  private ObjectMapper objectMapper;
  @Autowired
  private JdbcTemplate jdbcTemplate;

  @Test
  void user_can_login_and_access_feedback_but_not_admin_work_order() throws Exception {
    String token = login("user", "user123");

    mockMvc.perform(get("/api/feedback/page")
        .header("Authorization", token)
        .param("pageNum", "1")
        .param("pageSize", "10"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.code").value(200));

    mockMvc.perform(get("/api/work-order/page")
        .header("Authorization", token)
        .param("pageNum", "1")
        .param("pageSize", "10"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.code").value(403));
  }

  @Test
  void admin_can_login_and_access_work_order() throws Exception {
    String token = login("admin", "admin123");

    mockMvc.perform(get("/api/work-order/page")
        .header("Authorization", token)
        .param("pageNum", "1")
        .param("pageSize", "10"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.code").value(200));
  }

  @Test
  void admin_can_access_shared_feedback_routes() throws Exception {
    String token = login("admin", "admin123");

    mockMvc.perform(get("/api/feedback/page")
        .header("Authorization", token)
        .param("pageNum", "1")
        .param("pageSize", "10"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.code").value(200));
  }

  @Test
  void admin_can_see_chinese_seeded_work_orders() throws Exception {
    String token = login("admin", "admin123");

    MvcResult result = mockMvc.perform(get("/api/work-order/page")
        .header("Authorization", token)
        .param("pageNum", "1")
        .param("pageSize", "20"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.code").value(200))
      .andReturn();

    Assertions.assertTrue(
      jdbcTemplate.queryForObject(
        "select count(*) from wo_feedback where title = ?",
        Integer.class,
        "导出任务在大数据量下失败"
      ) > 0,
      "seeded work orders should use Chinese titles"
    );
  }

  @Test
  void admin_can_read_work_order_detail_without_feedback_route() throws Exception {
    String token = login("admin", "admin123");

    mockMvc.perform(get("/api/work-order/fb-1")
        .header("Authorization", token))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.code").value(200))
      .andExpect(jsonPath("$.data.id").value("fb-1"));
  }

  @Test
  void invalid_login_should_return_401_payload_not_500() throws Exception {
    mockMvc.perform(post("/api/auth/login")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"username\":\"user\",\"password\":\"wrong\"}"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.code").value(401))
      .andExpect(jsonPath("$.msg").value("invalid username or password"));
  }

  @Test
  void user_can_update_profile_and_password() throws Exception {
    String token = login("user", "user123");

    try {
      mockMvc.perform(put("/api/auth/profile")
          .header("Authorization", token)
          .contentType(MediaType.APPLICATION_JSON)
          .content("{\"displayName\":\"前端用户\",\"avatarUrl\":\"https://cdn.example.com/avatar.png\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(200))
        .andExpect(jsonPath("$.data.displayName").value("前端用户"))
        .andExpect(jsonPath("$.data.avatarUrl").value("https://cdn.example.com/avatar.png"));

      mockMvc.perform(get("/api/auth/me")
          .header("Authorization", token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(200))
        .andExpect(jsonPath("$.data.displayName").value("前端用户"))
        .andExpect(jsonPath("$.data.avatarUrl").value("https://cdn.example.com/avatar.png"));

      mockMvc.perform(put("/api/auth/password")
          .header("Authorization", token)
          .contentType(MediaType.APPLICATION_JSON)
          .content("{\"oldPassword\":\"user123\",\"newPassword\":\"user456\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(200));

      mockMvc.perform(post("/api/auth/login")
          .contentType(MediaType.APPLICATION_JSON)
          .content("{\"username\":\"user\",\"password\":\"user456\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(200))
        .andExpect(jsonPath("$.data.user.displayName").value("前端用户"))
        .andExpect(jsonPath("$.data.user.avatarUrl").value("https://cdn.example.com/avatar.png"));

      mockMvc.perform(get("/api/auth/demo-account/user"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(200))
        .andExpect(jsonPath("$.data.username").value("user"))
        .andExpect(jsonPath("$.data.password").value("user456"));
    } finally {
      mockMvc.perform(put("/api/auth/password")
          .header("Authorization", token)
          .contentType(MediaType.APPLICATION_JSON)
          .content("{\"oldPassword\":\"user456\",\"newPassword\":\"user123\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(200));

      mockMvc.perform(put("/api/auth/profile")
          .header("Authorization", token)
          .contentType(MediaType.APPLICATION_JSON)
          .content("{\"displayName\":\"普通用户\",\"avatarUrl\":\"\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(200));
    }
  }

  @Test
  void user_created_feedback_should_appear_in_admin_work_order_queue() throws Exception {
    String userToken = login("user", "user123");
    String adminToken = login("admin", "admin123");

    MvcResult created = mockMvc.perform(post("/api/feedback")
        .header("Authorization", userToken)
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"title\":\"Shared ticket\",\"description\":\"Same row for both roles\",\"accountName\":\"Tester\"}"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.code").value(200))
      .andReturn();

    JsonNode createdRoot = objectMapper.readTree(created.getResponse().getContentAsString());
    String feedbackId = createdRoot.path("data").path("id").asText();
    String feedbackCode = createdRoot.path("data").path("code").asText();

    MvcResult workOrderPage = mockMvc.perform(get("/api/work-order/page")
        .header("Authorization", adminToken)
        .param("pageNum", "1")
        .param("pageSize", "20"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.code").value(200))
      .andReturn();

    JsonNode workOrderRoot = objectMapper.readTree(workOrderPage.getResponse().getContentAsString());
    Assertions.assertTrue(
      containsCode(workOrderRoot.path("data").path("records"), feedbackCode),
      "admin work-order queue should include the user-created feedback row"
    );

    mockMvc.perform(post("/api/work-order/" + feedbackId + "/status")
        .header("Authorization", adminToken)
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"status\":\"PROCESSING\",\"remark\":\"Assigned by admin\"}"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.code").value(200));

    mockMvc.perform(get("/api/feedback/" + feedbackId)
        .header("Authorization", userToken))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.code").value(200))
      .andExpect(jsonPath("$.data.status").value("PROCESSING"));
  }

  @Test
  void admin_can_reply_through_work_order_route_and_user_can_see_it() throws Exception {
    String userToken = login("user", "user123");
    String adminToken = login("admin", "admin123");

    MvcResult created = mockMvc.perform(post("/api/feedback")
        .header("Authorization", userToken)
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"title\":\"Reply route\",\"description\":\"Shared thread reply\",\"accountName\":\"Tester\"}"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.code").value(200))
      .andReturn();

    JsonNode createdRoot = objectMapper.readTree(created.getResponse().getContentAsString());
    String feedbackId = createdRoot.path("data").path("id").asText();

    mockMvc.perform(post("/api/work-order/" + feedbackId + "/reply")
        .header("Authorization", adminToken)
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"content\":\"Admin processing note\",\"author\":\"Support\"}"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.code").value(200));

    MvcResult detail = mockMvc.perform(get("/api/feedback/" + feedbackId)
        .header("Authorization", userToken))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.code").value(200))
      .andReturn();

    JsonNode detailRoot = objectMapper.readTree(detail.getResponse().getContentAsString());
    Assertions.assertTrue(
      containsReplyWithRole(detailRoot.path("data").path("replies"), "Admin processing note", "service"),
      "user should see the admin reply in the shared feedback thread"
    );
  }

  @Test
  void user_reply_through_feedback_route_should_store_user_role() throws Exception {
    String userToken = login("user", "user123");

    MvcResult created = mockMvc.perform(post("/api/feedback")
        .header("Authorization", userToken)
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"title\":\"User reply role\",\"description\":\"Role check\",\"accountName\":\"Tester\"}"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.code").value(200))
      .andReturn();

    JsonNode createdRoot = objectMapper.readTree(created.getResponse().getContentAsString());
    String feedbackId = createdRoot.path("data").path("id").asText();

    mockMvc.perform(post("/api/feedback/" + feedbackId + "/reply")
        .header("Authorization", userToken)
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"content\":\"User follow-up note\",\"author\":\"普通用户\"}"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.code").value(200));

    MvcResult detail = mockMvc.perform(get("/api/feedback/" + feedbackId)
        .header("Authorization", userToken))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.code").value(200))
      .andReturn();

    JsonNode detailRoot = objectMapper.readTree(detail.getResponse().getContentAsString());
    Assertions.assertTrue(
      containsReplyWithRole(detailRoot.path("data").path("replies"), "User follow-up note", "user"),
      "user replies should be stored with the user role"
    );
  }

  @Test
  void user_can_create_feedback() throws Exception {
    String token = login("user", "user123");

    MvcResult result = mockMvc.perform(post("/api/feedback")
        .header("Authorization", token)
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"title\":\"Test title\",\"description\":\"Test description\",\"accountName\":\"Tester\"}"))
      .andExpect(status().isOk())
      .andReturn();

    JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
    Assertions.assertEquals(200, root.path("code").asInt());
    Assertions.assertEquals("Test title", root.path("data").path("title").asText());
  }

  private String login(String username, String password) throws Exception {
    MockHttpServletRequestBuilder request = post("/api/auth/login")
      .contentType(MediaType.APPLICATION_JSON)
      .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}");

    MvcResult result = mockMvc.perform(request)
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.code").value(200))
      .andReturn();

    JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
    return root.path("data").path("token").asText();
  }

  private boolean containsCode(JsonNode records, String code) {
    for (JsonNode record : records) {
      if (code.equals(record.path("code").asText())) {
        return true;
      }
    }
    return false;
  }

  private boolean containsTitle(JsonNode records, String title) {
    for (JsonNode record : records) {
      if (title.equals(record.path("title").asText())) {
        return true;
      }
    }
    return false;
  }

  private boolean containsReplyWithRole(JsonNode replies, String content, String role) {
    for (JsonNode reply : replies) {
      if (content.equals(reply.path("content").asText()) && role.equals(reply.path("role").asText())) {
        return true;
      }
    }
    return false;
  }
}
