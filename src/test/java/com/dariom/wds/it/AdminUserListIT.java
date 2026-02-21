package com.dariom.wds.it;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AdminUserListIT {

  @Resource
  private MockMvc mockMvc;

  @Resource
  private IntegrationTestHelper itHelper;

  @Test
  void getAllUsers_noAuth_returnsUnauthorized() throws Exception {
    // Act / Assert
    mockMvc.perform(get("/admin/users"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void getAllUsers_userRole_returnsForbidden() throws Exception {
    // Arrange
    var userBearer = itHelper.userBearer();

    // Act / Assert
    mockMvc.perform(get("/admin/users")
            .header("Authorization", userBearer))
        .andExpect(status().isForbidden());
  }

  @Test
  void getAllUsers_adminRole_returnsPagedUsers() throws Exception {
    // Arrange
    var adminBearer = itHelper.adminBearer();
    itHelper.createUser("00000000-0000-0000-0000-000000000001", "alice@test.com", "Alice Smith");
    itHelper.createUser("00000000-0000-0000-0000-000000000002", "bob@test.com", "Bob Jones");

    // Act
    var result = mockMvc.perform(get("/admin/users")
        .header("Authorization", adminBearer));

    // Assert
    result
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()").value(2))
        .andExpect(jsonPath("$.content[0].id").isString())
        .andExpect(jsonPath("$.content[0].fullName").isString())
        .andExpect(jsonPath("$.content[0].displayName").isString())
        .andExpect(jsonPath("$.content[0].createdOn").isString())
        .andExpect(jsonPath("$.page.size").value(50))
        .andExpect(jsonPath("$.page.totalElements").value(2))
        .andExpect(jsonPath("$.page.number").value(0));
  }

  @Test
  void getAllUsers_customPagination_respectsParams() throws Exception {
    // Arrange
    var adminBearer = itHelper.adminBearer();
    itHelper.createUser("00000000-0000-0000-0000-000000000001", "alice@test.com", "Alice Smith");
    itHelper.createUser("00000000-0000-0000-0000-000000000002", "bob@test.com", "Bob Jones");
    itHelper.createUser("00000000-0000-0000-0000-000000000003", "carol@test.com", "Carol White");

    // Act
    var result = mockMvc.perform(get("/admin/users")
        .param("page", "0")
        .param("size", "2")
        .header("Authorization", adminBearer));

    // Assert
    result
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()").value(2))
        .andExpect(jsonPath("$.page.size").value(2))
        .andExpect(jsonPath("$.page.totalElements").value(3))
        .andExpect(jsonPath("$.page.totalPages").value(2));
  }

  @Test
  void getAllUsers_withFilters_returnsMatchingUsers() throws Exception {
    // Arrange
    var adminBearer = itHelper.adminBearer();
    itHelper.createUser("00000000-0000-0000-0000-000000000001", "alice@test.com", "Alice Smith");
    itHelper.createUser("00000000-0000-0000-0000-000000000002", "bob@acme.com", "Bob Jones");
    itHelper.createUser("00000000-0000-0000-0000-000000000003", "anna@test.com", "Anna Miles");

    // Act
    var result = mockMvc.perform(get("/admin/users")
        .param("fullName", "an")
        .param("email", "test.com")
        .header("Authorization", adminBearer));

    // Assert
    result
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()").value(1))
        .andExpect(jsonPath("$.content[0].fullName").value("Anna Miles"))
        .andExpect(jsonPath("$.content[0].email").value("anna@test.com"))
        .andExpect(jsonPath("$.page.totalElements").value(1));
  }
}
