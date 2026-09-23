package project.project.Service.implement;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import project.project.Entity.notification.NotificationType;
import project.project.Entity.order.OrderItem;
import project.project.Entity.order.OrderStatus;
import project.project.Entity.product.Product;
import project.project.Entity.review.Review;
import project.project.Entity.review.ReviewReply;
import project.project.Entity.seller.Seller;
import project.project.Entity.user.Customer;
import project.project.Exception.InvalidReviewException;
import project.project.Exception.ResourceNotFoundException;
import project.project.Repository.CustomerRepository;
import project.project.Repository.OrderItemRepository;
import project.project.Repository.ProductRepository;
import project.project.Repository.ReviewReplyRepository;
import project.project.Repository.ReviewRepository;
import project.project.Repository.SellerRepository;
import project.project.Service.api.NotificationService;
import project.project.Service.api.ProductService;
import project.project.Service.api.ReviewService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ReviewServiceImp implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final ReviewReplyRepository reviewReplyRepository;
    private final OrderItemRepository orderItemRepository;
    private final CustomerRepository customerRepository;
    private final SellerRepository sellerRepository;
    private final ProductRepository productRepository;
    private final ProductService productService;
    private final NotificationService notificationService;

    public ReviewServiceImp(ReviewRepository reviewRepository,
                            ReviewReplyRepository reviewReplyRepository,
                            OrderItemRepository orderItemRepository,
                            CustomerRepository customerRepository,
                            SellerRepository sellerRepository,
                            ProductRepository productRepository,
                            ProductService productService,
                            @Autowired(required = false) NotificationService notificationService) {
        this.reviewRepository = reviewRepository;
        this.reviewReplyRepository = reviewReplyRepository;
        this.orderItemRepository = orderItemRepository;
        this.customerRepository = customerRepository;
        this.sellerRepository = sellerRepository;
        this.productRepository = productRepository;
        this.productService = productService;
        this.notificationService = notificationService;
    }

    @Override
    public boolean isEligibleToReview(Long customerId, Long orderItemId) {
        if (customerId == null || orderItemId == null) {
            return false;
        }
        OrderItem orderItem = orderItemRepository.findById(orderItemId).orElse(null);
        if (orderItem == null) {
            return false;
        }

        // Must not have been reviewed yet
        if (Boolean.TRUE.equals(orderItem.getIsReviewed()) || reviewRepository.existsByOrderItem_OrderItemId(orderItemId)) {
            return false;
        }

        // Check customer association
        if (orderItem.getOrder() == null ||
                orderItem.getOrder().getOrderGroup() == null ||
                orderItem.getOrder().getOrderGroup().getCustomer() == null) {
            return false;
        }
        if (!orderItem.getOrder().getOrderGroup().getCustomer().getCustomerId().equals(customerId)) {
            return false;
        }

        // Sub-Order must be COMPLETED
        return orderItem.getOrder().getOrderStatus() == OrderStatus.COMPLETED;
    }

    @Override
    @Transactional
    public Review createReview(Long customerId, Long orderItemId, Integer rating, String comment) {
        if (rating == null || rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Rating must be between 1 and 5");
        }
        if (!isEligibleToReview(customerId, orderItemId)) {
            throw new InvalidReviewException("Customer is not eligible to review this order item");
        }

        OrderItem orderItem = orderItemRepository.findById(orderItemId)
                .orElseThrow(() -> new ResourceNotFoundException("OrderItem not found with id: " + orderItemId));
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + customerId));
        Product product = orderItem.getProduct();

        Review review = new Review();
        review.setOrderItem(orderItem);
        review.setProduct(product);
        review.setCustomer(customer);
        review.setRating(rating);
        review.setComment(comment);
        review.setIsAutoReview(false);

        Review savedReview = reviewRepository.save(review);

        orderItem.setIsReviewed(true);
        orderItemRepository.save(orderItem);

        productService.updateAverageRating(product.getProductId(), rating);

        if (notificationService != null && product.getSeller() != null) {
            notificationService.notifySellerNewReview(product.getSeller().getSellerId(), savedReview.getReviewId());
        }

        return savedReview;
    }

    @Override
    @Transactional
    public void generateAutoFiveStarReview(Long orderItemId) {
        if (orderItemId == null) {
            throw new IllegalArgumentException("OrderItem ID cannot be null");
        }
        OrderItem orderItem = orderItemRepository.findById(orderItemId)
                .orElseThrow(() -> new ResourceNotFoundException("OrderItem not found with id: " + orderItemId));

        if (Boolean.TRUE.equals(orderItem.getIsReviewed()) || reviewRepository.existsByOrderItem_OrderItemId(orderItemId)) {
            return;
        }
        if (orderItem.getOrder() == null || orderItem.getOrder().getOrderStatus() != OrderStatus.COMPLETED) {
            return;
        }

        Customer customer = orderItem.getOrder().getOrderGroup() != null ?
                orderItem.getOrder().getOrderGroup().getCustomer() : null;
        Product product = orderItem.getProduct();

        Review review = new Review();
        review.setOrderItem(orderItem);
        review.setProduct(product);
        review.setCustomer(customer);
        review.setRating(5);
        review.setComment("รีวิว 5 ดาวอัตโนมัติจากระบบ");
        review.setIsAutoReview(true);

        Review savedReview = reviewRepository.save(review);

        orderItem.setIsReviewed(true);
        orderItemRepository.save(orderItem);

        productService.updateAverageRating(product.getProductId(), 5);

        if (notificationService != null && product.getSeller() != null) {
            notificationService.notifySellerNewReview(product.getSeller().getSellerId(), savedReview.getReviewId());
        }
    }

    @Override
    @Transactional
    public Review updateReview(Long customerId, Long reviewId, Integer rating, String comment) {
        if (rating == null || rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Rating must be between 1 and 5");
        }
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found with id: " + reviewId));

        if (review.getCustomer() == null || !review.getCustomer().getCustomerId().equals(customerId)) {
            throw new IllegalArgumentException("Customer is not authorized to edit this review");
        }

        review.setRating(rating);
        review.setComment(comment);
        Review updated = reviewRepository.save(review);

        recalculateProductAverageRating(review.getProduct().getProductId());

        return updated;
    }

    @Override
    @Transactional
    public ReviewReply replyReview(Long sellerId, Long reviewId, String replyMessage) {
        if (sellerId == null || reviewId == null) {
            throw new IllegalArgumentException("Seller ID and Review ID cannot be null");
        }
        if (replyMessage == null || replyMessage.trim().isEmpty()) {
            throw new IllegalArgumentException("Reply message cannot be blank");
        }
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found with id: " + reviewId));

        if (review.getProduct() == null || review.getProduct().getSeller() == null ||
                !review.getProduct().getSeller().getSellerId().equals(sellerId)) {
            throw new IllegalArgumentException("Seller does not own the product for this review");
        }

        Seller seller = sellerRepository.findById(sellerId)
                .orElseThrow(() -> new ResourceNotFoundException("Seller not found with id: " + sellerId));

        ReviewReply reply = reviewReplyRepository.findByReview_ReviewId(reviewId)
                .orElse(new ReviewReply());

        reply.setReview(review);
        reply.setSeller(seller);
        reply.setReplyMessage(replyMessage);
        reply.setRepliedAt(LocalDateTime.now());

        ReviewReply savedReply = reviewReplyRepository.save(reply);

        if (notificationService != null && review.getCustomer() != null && review.getCustomer().getUser() != null) {
            notificationService.sendNotification(
                    review.getCustomer().getUser().getUserId(),
                    "ร้านค้าตอบกลับรีวิวของคุณ",
                    "ร้านค้าได้ตอบกลับรีวิวสินค้า: " + review.getProduct().getName(),
                    NotificationType.NEW_REVIEW
            );
        }

        return savedReply;
    }

    @Override
    public List<Review> getReviewsByProduct(Long productId) {
        if (productId == null) {
            throw new IllegalArgumentException("Product ID cannot be null");
        }
        return reviewRepository.findByProduct_ProductIdOrderByCreatedAtDesc(productId);
    }

    @Override
    public Review getReviewByOrderItemId(Long orderItemId) {
        if (orderItemId == null) {
            throw new IllegalArgumentException("OrderItem ID cannot be null");
        }
        return reviewRepository.findByOrderItem_OrderItemId(orderItemId).orElse(null);
    }

    private void recalculateProductAverageRating(Long productId) {
        if (productId == null) return;
        Product product = productRepository.findById(productId).orElse(null);
        if (product == null) return;

        List<Review> reviews = reviewRepository.findByProduct_ProductId(productId);
        if (reviews.isEmpty()) {
            product.setAverageRating(BigDecimal.ZERO);
            product.setReviewCount(0);
        } else {
            double avg = reviews.stream().mapToInt(Review::getRating).average().orElse(0.0);
            product.setReviewCount(reviews.size());
            product.setAverageRating(BigDecimal.valueOf(avg).setScale(2, RoundingMode.HALF_UP));
        }
        productRepository.save(product);
    }
}
