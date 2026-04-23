package com.wly.workorder.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(properties = {
  "spring.datasource.url=jdbc:h2:mem:workorder_upload;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false",
  "spring.datasource.driver-class-name=org.h2.Driver",
  "spring.datasource.username=sa",
  "spring.datasource.password=",
  "spring.sql.init.mode=always",
  "workorder.upload-dir=target/test-uploads"
})
@AutoConfigureMockMvc
class FileUploadControllerTest {
  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @BeforeEach
  void cleanUploadDir() throws IOException {
    Path uploadDir = Path.of("target", "test-uploads");
    if (Files.exists(uploadDir)) {
      try (var paths = Files.walk(uploadDir)) {
        paths.sorted((a, b) -> b.compareTo(a)).forEach(path -> {
          try {
            Files.deleteIfExists(path);
          } catch (IOException ignored) {
          }
        });
      }
    }
    Files.createDirectories(uploadDir);
  }

  @Test
  void upload_image_should_return_real_url_and_be_readable() throws Exception {
    String token = login("user", "user123");
    MockMultipartFile file = new MockMultipartFile(
      "file",
      "示例图片.png",
      "image/png",
      "fake-png-data".getBytes(StandardCharsets.UTF_8)
    );

    MvcResult result = mockMvc.perform(
        multipart("/api/files/upload")
          .file(file)
          .param("kind", "image")
          .header("Authorization", token)
          .contentType(MediaType.MULTIPART_FORM_DATA)
      )
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.code").value(200))
      .andExpect(jsonPath("$.data.url").exists())
      .andExpect(jsonPath("$.data.serverPath").exists())
      .andReturn();

    JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
    String url = root.path("data").path("url").asText();
    String serverPath = root.path("data").path("serverPath").asText();

    Assertions.assertTrue(url.startsWith("/api/files/"), "uploaded image should return a real file url");
    Assertions.assertTrue(Files.exists(Path.of("target", "test-uploads").resolve(serverPath)), "image file should exist on disk");

    mockMvc.perform(get(url).header("Authorization", token))
      .andExpect(status().isOk())
      .andExpect(result1 -> Assertions.assertEquals("fake-png-data", result1.getResponse().getContentAsString()));
  }

  @Test
  void upload_attachment_should_return_real_url_and_be_readable() throws Exception {
    String token = login("user", "user123");
    MockMultipartFile file = new MockMultipartFile(
      "file",
      "上传附件.pdf",
      "application/pdf",
      "%PDF-1.4 fake".getBytes(StandardCharsets.UTF_8)
    );

    MvcResult result = mockMvc.perform(
        multipart("/api/files/upload")
          .file(file)
          .param("kind", "file")
          .header("Authorization", token)
          .contentType(MediaType.MULTIPART_FORM_DATA)
      )
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.code").value(200))
      .andExpect(jsonPath("$.data.url").exists())
      .andReturn();

    JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
    String url = root.path("data").path("url").asText();

    Assertions.assertTrue(url.startsWith("/api/files/"), "uploaded attachment should return a real file url");

    mockMvc.perform(get(url).header("Authorization", token))
      .andExpect(status().isOk())
      .andExpect(result1 -> Assertions.assertTrue(result1.getResponse().getContentAsString().contains("fake")));
  }

  private String login(String username, String password) throws Exception {
    MvcResult result = mockMvc.perform(
        org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/auth/login")
          .contentType(MediaType.APPLICATION_JSON)
          .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}")
      )
      .andExpect(status().isOk())
      .andReturn();

    JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
    return "Bearer " + root.path("data").path("token").asText();
  }
}
