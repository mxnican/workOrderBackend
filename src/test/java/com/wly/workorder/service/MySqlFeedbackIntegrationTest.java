package com.wly.workorder.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.wly.workorder.auth.AuthContext;
import com.wly.workorder.auth.AuthRole;
import com.wly.workorder.auth.AuthSession;
import com.wly.workorder.model.TicketModels.CreateFeedbackRequest;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest(properties = {
  "spring.datasource.url=jdbc:mysql://127.0.0.1:3306/work_order?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai",
  "spring.datasource.username=root",
  "spring.datasource.password=123456",
  "spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver",
  "spring.sql.init.mode=always"
})
class MySqlFeedbackIntegrationTest {
  @Autowired
  private TicketService ticketService;

  @Autowired
  private JdbcTemplate jdbcTemplate;

  @Test
  void create_feedback_works_on_mysql() {
    AuthContext.set(new AuthSession("mysql-token", "user", "Tester", "", AuthRole.USER));
    String title = "mysql feedback " + System.currentTimeMillis();
    try {
      assertDoesNotThrow(() -> ticketService.createFeedback(
        new CreateFeedbackRequest(title, "mysql description", "Tester", List.of(), List.of())
      ));
    } finally {
      AuthContext.clear();
    }

    Integer count = jdbcTemplate.queryForObject(
      "select count(*) from wo_feedback where title = ?",
      Integer.class,
      title
    );

    assertEquals(1, count);
  }
}
