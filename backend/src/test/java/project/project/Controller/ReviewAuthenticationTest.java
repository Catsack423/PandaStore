package project.project.Controller;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.json.JsonMapper;
import project.project.Entity.review.Review;
import project.project.Entity.user.Customer;
import project.project.Entity.user.UserRole;
import project.project.Filter.JwtAuthenticationFilter;
import project.project.Repository.CustomerRepository;
import project.project.Repository.UserRepository;
import project.project.Security.AuthenticatedUser;
import project.project.Security.CurrentUser;
import project.project.Security.SessionAuthenticator;
import project.project.Service.api.ReviewService;

class ReviewAuthenticationTest {
    private final ReviewService reviews = mock(ReviewService.class);
    private final SessionAuthenticator authenticator = mock(SessionAuthenticator.class);
    private final CustomerRepository customers = mock(CustomerRepository.class);
    private final CurrentUser currentUser = new CurrentUser(mock(UserRepository.class), customers);
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        ReviewController controller = new ReviewController(reviews, currentUser);
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(authenticator, new JsonMapper());
        mvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .addFilters(filter).build();

        when(authenticator.authenticate("valid.token.value"))
                .thenReturn(Optional.of(new AuthenticatedUser(10, UserRole.CUSTOMER)));
        Customer customer = mock(Customer.class);
        when(customer.getCustomerId()).thenReturn(20L);
        when(customers.findByUser_UserId(10L)).thenReturn(Optional.of(customer));
    }

    @Test
    void createAndUpdateUseCustomerFromTokenEvenIfAnotherIdIsSent() throws Exception {
        Review review = new Review();
        review.setRating(5);
        when(reviews.createReview(20L, 30L, 5, "good")).thenReturn(review);
        when(reviews.updateReview(20L, 40L, 5, "good")).thenReturn(review);

        mvc.perform(post("/api/reviews").header("Authorization", "Bearer valid.token.value")
                .param("customerId", "999")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"orderItemId\":30,\"rating\":5,\"comment\":\"good\"}"))
                .andExpect(status().isCreated());
        mvc.perform(put("/api/reviews/40").header("Authorization", "Bearer valid.token.value")
                .param("customerId", "999")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"rating\":5,\"comment\":\"good\"}"))
                .andExpect(status().isOk());

        verify(reviews).createReview(20L, 30L, 5, "good");
        verify(reviews).updateReview(20L, 40L, 5, "good");
    }

    @Test
    void eligibilityUsesCustomerFromToken() throws Exception {
        when(reviews.isEligibleToReview(20L, 30L)).thenReturn(true);

        mvc.perform(get("/api/reviews/check-eligibility")
                .header("Authorization", "Bearer valid.token.value")
                .param("customerId", "999").param("orderItemId", "30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(true));

        verify(reviews).isEligibleToReview(20L, 30L);
    }

    @Test
    void reviewActionsRequireTokenAndCustomerRole() throws Exception {
        mvc.perform(post("/api/reviews").contentType(MediaType.APPLICATION_JSON)
                .content("{\"orderItemId\":30,\"rating\":5}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(put("/api/reviews/40").contentType(MediaType.APPLICATION_JSON)
                .content("{\"rating\":5}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/reviews/check-eligibility").param("orderItemId", "30"))
                .andExpect(status().isUnauthorized());

        when(authenticator.authenticate("seller.token.value"))
                .thenReturn(Optional.of(new AuthenticatedUser(11, UserRole.SELLER)));
        mvc.perform(get("/api/reviews/check-eligibility")
                .header("Authorization", "Bearer seller.token.value")
                .param("orderItemId", "30"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(reviews);
    }
}
