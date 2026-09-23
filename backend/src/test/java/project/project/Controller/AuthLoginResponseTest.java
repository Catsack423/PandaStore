package project.project.Controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import project.project.DTO.auth.LoginResponse;
import project.project.Entity.user.User;
import project.project.Entity.user.UserRole;
import project.project.Entity.user.UserStatus;
import project.project.Security.CurrentUser;
import project.project.Service.api.AuthService;
import project.project.Service.implement.PasswordService;

class AuthLoginResponseTest {
    @Test
    void loginReturnsTokenRoleAndSafeUserDetails() throws Exception {
        AuthService auth = mock(AuthService.class);
        User user = new User("seller", "seller@example.com", "secret-hash",
                UserRole.SELLER, UserStatus.ACTIVE);
        user.setUserId(42L);
        when(auth.loginWithUser("seller", "password123"))
                .thenReturn(LoginResponse.from("signed.jwt.token", user));

        AuthController controller = new AuthController(auth, mock(CurrentUser.class), mock(PasswordService.class));
        var mvc = MockMvcBuilders.standaloneSetup(controller).build();

        String body = mvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"usernameOrEmail\":\"seller\",\"password\":\"password123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token").value("signed.jwt.token"))
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.role").value("SELLER"))
                .andExpect(jsonPath("$.data.user.userId").value(42))
                .andExpect(jsonPath("$.data.user.email").value("seller@example.com"))
                .andReturn().getResponse().getContentAsString();

        assertFalse(body.contains("secret-hash"));
        assertFalse(body.contains("passwordHash"));
        verify(auth).loginWithUser("seller", "password123");
        verify(auth, never()).login(any(), any());
    }
}
