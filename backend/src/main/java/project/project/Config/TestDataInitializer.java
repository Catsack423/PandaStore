package project.project.Config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import project.project.Entity.order.*;
import project.project.Entity.product.Category;
import project.project.Entity.product.Product;
import project.project.Entity.product.ProductStatus;
import project.project.Entity.review.Review;
import project.project.Entity.seller.Seller;
import project.project.Entity.seller.SellerStatus;
import project.project.Entity.user.*;
import project.project.Repository.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Component
public class TestDataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final SellerRepository sellerRepository;
    private final AddressRepository addressRepository;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final OrderGroupRepository orderGroupRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ShipmentRepository shipmentRepository;
    private final ReviewRepository reviewRepository;
    private final PaymentRepository paymentRepository;

    public TestDataInitializer(
            UserRepository userRepository,
            CustomerRepository customerRepository,
            SellerRepository sellerRepository,
            AddressRepository addressRepository,
            CategoryRepository categoryRepository,
            ProductRepository productRepository,
            CartRepository cartRepository,
            CartItemRepository cartItemRepository,
            OrderGroupRepository orderGroupRepository,
            OrderRepository orderRepository,
            OrderItemRepository orderItemRepository,
            ShipmentRepository shipmentRepository,
            ReviewRepository reviewRepository,
            PaymentRepository paymentRepository) {
        this.userRepository = userRepository;
        this.customerRepository = customerRepository;
        this.sellerRepository = sellerRepository;
        this.addressRepository = addressRepository;
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.orderGroupRepository = orderGroupRepository;
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.shipmentRepository = shipmentRepository;
        this.reviewRepository = reviewRepository;
        this.paymentRepository = paymentRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (sellerRepository.count() > 0) {
            return;
        }
        seedData();
    }

    @jakarta.persistence.PersistenceContext
    private jakarta.persistence.EntityManager entityManager;

    @Transactional
    public synchronized void resetTestData() {
        entityManager.createNativeQuery("SET REFERENTIAL_INTEGRITY FALSE").executeUpdate();
        String[] tables = {
            "review_replies", "reviews", "shipments", "order_items", "orders",
            "payments", "order_groups", "cart_items", "carts", "product_categories",
            "product_images", "products", "categories", "addresses", "customers",
            "seller_bank_accounts", "seller_applications", "sellers", "notifications",
            "auth_sessions", "users"
        };
        for (String table : tables) {
            try {
                entityManager.createNativeQuery("TRUNCATE TABLE " + table + " RESTART IDENTITY").executeUpdate();
            } catch (Exception ignored) {}
        }
        entityManager.createNativeQuery("SET REFERENTIAL_INTEGRITY TRUE").executeUpdate();
        entityManager.clear();
        seedData();
    }

    @Transactional
    public synchronized void seedData() {
        User sellerUser = new User();
        sellerUser.setUsername("seller1");
        sellerUser.setEmail("seller@pandastore.com");
        sellerUser.setPasswordHash("$2a$10$abcdefghijklmnopqrstuv");
        sellerUser.setRole(UserRole.SELLER);
        sellerUser.setStatus(UserStatus.ACTIVE);
        sellerUser = userRepository.save(sellerUser);

        // 2. Create Seller (ID=1)
        Seller seller = new Seller();
        seller.setUser(sellerUser);
        seller.setShopName("Panda Official Shop");
        seller.setShopDescription("ร้านค้าอย่างเป็นทางการของ PandaStore จำหน่ายสินค้าคุณภาพเยี่ยม");
        seller.setShopPhone("0812345678");
        seller.setShopEmail("seller@pandastore.com");
        seller.setShopAddress("99/9 ถ.มิตรภาพ ต.ในเมือง อ.เมือง จ.ขอนแก่น 40000");
        seller.setStatus(SellerStatus.ACTIVE);
        seller.setRating(new BigDecimal("5.00"));
        seller = sellerRepository.save(seller);

        // 3. Create Customer User (ID=2)
        User customerUser = new User();
        customerUser.setUsername("customer1");
        customerUser.setEmail("customer@pandastore.com");
        customerUser.setPasswordHash("$2a$10$abcdefghijklmnopqrstuv");
        customerUser.setRole(UserRole.CUSTOMER);
        customerUser.setStatus(UserStatus.ACTIVE);
        customerUser = userRepository.save(customerUser);

        // 4. Create Customer (ID=1)
        Customer customer = new Customer();
        customer.setUser(customerUser);
        customer.setFullName("สมชาย รักดี");
        customer.setPhoneNumber("0899998888");
        customer = customerRepository.save(customer);

        // 5. Create Address (ID=1)
        Address address = new Address();
        address.setCustomer(customer);
        address.setReceiverName("สมชาย รักดี");
        address.setPhoneNumber("0899998888");
        address.setAddressLine("123 หมู่ 4 ต.ในเมือง");
        address.setDistrict("เมืองขอนแก่น");
        address.setProvince("ขอนแก่น");
        address.setPostalCode("40000");
        address.setIsDefault(true);
        address = addressRepository.save(address);

        // 6. Create Category (ID=1)
        Category category = new Category();
        category.setCategoryName("เสื้อผ้าแฟชั่น (Fashion & Apparel)");
        category.setDescription("เสื้อยืด ฮู้ด กางเกง หมวก Panda Limited Edition");
        category = categoryRepository.save(category);

        // 7. Create Products (ID=1, ID=2)
        Product product1 = new Product();
        product1.setSeller(seller);
        product1.setName("Panda Limited Edition T-Shirt #101");
        product1.setDescription("เสื้อยืดสกรีนลายแพนด้าแท้ 100% สวมใส่สบาย ระบายอากาศดีเยี่ยม");
        product1.setPrice(new BigDecimal("350.00"));
        product1.setStock(200);
        product1.setStatus(ProductStatus.ACTIVE);
        product1.setAverageRating(new BigDecimal("5.00"));
        product1.setReviewCount(1);
        product1.setShippingInfo("Standard Delivery จัดส่งภายใน 1-3 วันทำการ");
        product1.getCategories().add(category);
        product1 = productRepository.save(product1);

        Product product2 = new Product();
        product2.setSeller(seller);
        product2.setName("Panda Hoodie Pro Edition #202");
        product2.setDescription("เสื้อฮู้ดลายแพนด้า ผ้าหนานุ่ม อบอุ่น มีกระเป๋าหน้า");
        product2.setPrice(new BigDecimal("650.00"));
        product2.setStock(100);
        product2.setStatus(ProductStatus.ACTIVE);
        product2.setAverageRating(BigDecimal.ZERO);
        product2.setReviewCount(0);
        product2.setShippingInfo("Kerry Express จัดส่งด่วน");
        product2.getCategories().add(category);
        product2 = productRepository.save(product2);

        // 8. Create Cart with multiple selectable items for checkout
        Cart cart = new Cart();
        cart.setCustomer(customer);
        cart = cartRepository.save(cart);

        for (int i = 0; i < 20; i++) {
            CartItem cartItem = new CartItem();
            cartItem.setCart(cart);
            cartItem.setProduct(product1);
            cartItem.setQuantity(1);
            cartItem.setIsSelected(true);
            cartItemRepository.save(cartItem);
        }

        // 9. Create OrderGroup (ID=1)
        OrderGroup orderGroup = new OrderGroup();
        orderGroup.setGroupNumber("GRP-2026-0001");
        orderGroup.setCustomer(customer);
        orderGroup.setShippingAddress(address);
        orderGroup.setTotalProductsAmount(new BigDecimal("3500.00"));
        orderGroup.setTotalShippingFee(new BigDecimal("45.00"));
        orderGroup.setTotalDiscount(BigDecimal.ZERO);
        orderGroup.setGrandTotal(new BigDecimal("3545.00"));
        orderGroup.setPaymentStatus(OrderGroupPaymentStatus.PAID);
        orderGroup = orderGroupRepository.save(orderGroup);

        // 10. Create Payment for OrderGroup (Required for Refund / Reject / Cancel)
        Payment payment = new Payment();
        payment.setOrderGroup(orderGroup);
        payment.setAmount(new BigDecimal("5000.00"));
        payment.setPaymentMethod(PaymentMethod.PROMPTPAY);
        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setRefundedAmount(BigDecimal.ZERO);
        payment.setPaidAt(LocalDateTime.now().minusDays(3));
        paymentRepository.save(payment);

        // 11. Create Orders 1 through 7 FIRST so that order_id maps exactly 1..7:
        // Order 1 (ID=1): WAITING_SELLER_CONFIRM (For 3.1 Get Sub-Order & 3.4 Seller Accept)
        Order order1 = new Order();
        order1.setSubOrderNumber("ORD-2026-0001");
        order1.setOrderGroup(orderGroup);
        order1.setSeller(seller);
        order1.setSubtotal(new BigDecimal("350.00"));
        order1.setShippingFee(new BigDecimal("45.00"));
        order1.setTotalAmount(new BigDecimal("395.00"));
        order1.setOrderStatus(OrderStatus.WAITING_SELLER_CONFIRM);
        order1 = orderRepository.save(order1);

        // Order 2 (ID=2): WAITING_SELLER_CONFIRM (For 3.5 Seller Reject)
        Order order2 = new Order();
        order2.setSubOrderNumber("ORD-2026-0002");
        order2.setOrderGroup(orderGroup);
        order2.setSeller(seller);
        order2.setSubtotal(new BigDecimal("350.00"));
        order2.setShippingFee(new BigDecimal("45.00"));
        order2.setTotalAmount(new BigDecimal("395.00"));
        order2.setOrderStatus(OrderStatus.WAITING_SELLER_CONFIRM);
        order2 = orderRepository.save(order2);

        // Order 3 (ID=3): PREPARING (For 4.2 Assign Tracking Number)
        Order order3 = new Order();
        order3.setSubOrderNumber("ORD-2026-0003");
        order3.setOrderGroup(orderGroup);
        order3.setSeller(seller);
        order3.setSubtotal(new BigDecimal("350.00"));
        order3.setShippingFee(new BigDecimal("45.00"));
        order3.setTotalAmount(new BigDecimal("395.00"));
        order3.setOrderStatus(OrderStatus.PREPARING);
        order3 = orderRepository.save(order3);

        // Order 4 (ID=4): SHIPPED (For 3.6 Confirm Delivered, 4.3 Get Shipment, 4.4 Update Status)
        Order order4 = new Order();
        order4.setSubOrderNumber("ORD-2026-0004");
        order4.setOrderGroup(orderGroup);
        order4.setSeller(seller);
        order4.setSubtotal(new BigDecimal("350.00"));
        order4.setShippingFee(new BigDecimal("45.00"));
        order4.setTotalAmount(new BigDecimal("395.00"));
        order4.setOrderStatus(OrderStatus.SHIPPED);
        order4.setShippedAt(LocalDateTime.now().minusDays(1));
        order4 = orderRepository.save(order4);

        // Order 5 (ID=5): WAITING_SELLER_CONFIRM (For 3.7 Customer Cancel Sub-Order)
        Order order5 = new Order();
        order5.setSubOrderNumber("ORD-2026-0005");
        order5.setOrderGroup(orderGroup);
        order5.setSeller(seller);
        order5.setSubtotal(new BigDecimal("350.00"));
        order5.setShippingFee(new BigDecimal("45.00"));
        order5.setTotalAmount(new BigDecimal("395.00"));
        order5.setOrderStatus(OrderStatus.WAITING_SELLER_CONFIRM);
        order5 = orderRepository.save(order5);

        // Order 6 (ID=6): WAITING_SELLER_CONFIRM (For 3.8 TC14 Cancel with Whitespace)
        Order order6 = new Order();
        order6.setSubOrderNumber("ORD-2026-0006");
        order6.setOrderGroup(orderGroup);
        order6.setSeller(seller);
        order6.setSubtotal(new BigDecimal("350.00"));
        order6.setShippingFee(new BigDecimal("45.00"));
        order6.setTotalAmount(new BigDecimal("395.00"));
        order6.setOrderStatus(OrderStatus.WAITING_SELLER_CONFIRM);
        order6 = orderRepository.save(order6);

        // Order 7 (ID=7): COMPLETED (For Module 5 Reviews)
        Order order7 = new Order();
        order7.setSubOrderNumber("ORD-2026-0007");
        order7.setOrderGroup(orderGroup);
        order7.setSeller(seller);
        order7.setSubtotal(new BigDecimal("1350.00"));
        order7.setShippingFee(new BigDecimal("45.00"));
        order7.setTotalAmount(new BigDecimal("1395.00"));
        order7.setOrderStatus(OrderStatus.COMPLETED);
        order7.setShippedAt(LocalDateTime.now().minusDays(3));
        order7.setCompletedAt(LocalDateTime.now().minusDays(1));
        order7 = orderRepository.save(order7);

        // 12. Save OrderItems for Order 7 FIRST so that order_item_id maps exactly 1, 2, 3:
        // OrderItem 1 (ID=1): product1, isReviewed = false (For 5.1 Eligibility check & 5.2 Create Standard Review)
        OrderItem item1 = saveOrderItem(order7, product1, 1, new BigDecimal("350.00"), false);

        // OrderItem 2 (ID=2): product2, isReviewed = false (For 5.3 Create Review { review: อื่นๆ })
        OrderItem item2 = saveOrderItem(order7, product2, 1, new BigDecimal("650.00"), false);

        // OrderItem 3 (ID=3): product1, isReviewed = true (For 5.4 Update Review & 5.5 Seller Reply)
        OrderItem item3 = saveOrderItem(order7, product1, 1, new BigDecimal("350.00"), true);

        // 13. Save OrderItems for Orders 1..6 (IDs 4..9) so stock restore functions have items to restore
        saveOrderItem(order1, product1, 1, new BigDecimal("350.00"), false);
        saveOrderItem(order2, product1, 1, new BigDecimal("350.00"), false);
        saveOrderItem(order3, product1, 1, new BigDecimal("350.00"), false);
        saveOrderItem(order4, product1, 1, new BigDecimal("350.00"), false);
        saveOrderItem(order5, product1, 1, new BigDecimal("350.00"), false);
        saveOrderItem(order6, product1, 1, new BigDecimal("350.00"), false);

        // 14. Save Shipment 1 (ID=1) for Order 4
        Shipment shipment1 = new Shipment();
        shipment1.setOrder(order4);
        shipment1.setCourierName("Flash Express");
        shipment1.setTrackingNumber("TH0192839182");
        shipment1.setShippingStatus(ShippingStatus.SHIPPED);
        shipment1.setShippedAt(LocalDateTime.now().minusDays(1));
        shipmentRepository.save(shipment1);

        // 15. Save Review 1 (ID=1) for OrderItem 3
        Review review1 = new Review();
        review1.setOrderItem(item3);
        review1.setProduct(product1);
        review1.setCustomer(customer);
        review1.setRating(5);
        review1.setComment("สินค้าคุณภาพยอดเยี่ยมมาก ลายสกรีนแพนด้าสวยงาม ผ้านุ่มใส่สบายมากครับ");
        review1.setIsAutoReview(false);
        reviewRepository.save(review1);

        System.out.println("✅ [TestDataInitializer] Complete ordered test data seeded successfully!");
        System.out.println("   - Seller ID=1, Customer ID=1, Address ID=1, Category ID=1");
        System.out.println("   - Product ID=1, ID=2");
        System.out.println("   - Cart with 20 items for Customer ID=1");
        System.out.println("   - OrderGroup ID=1 with Payment (5000 THB, SUCCESS)");
        System.out.println("   - Orders: Order 1 (WAITING), Order 2 (WAITING), Order 3 (PREPARING), Order 4 (SHIPPED + Shipment 1), Order 5 (WAITING), Order 6 (WAITING), Order 7 (COMPLETED)");
        System.out.println("   - OrderItems: OrderItem ID=1 (unreviewed, COMPLETED), ID=2 (unreviewed, COMPLETED), ID=3 (reviewed with Review ID=1)");
    }

    private OrderItem saveOrderItem(Order order, Product product, int quantity, BigDecimal unitPrice, boolean isReviewed) {
        OrderItem item = new OrderItem();
        item.setOrder(order);
        item.setProduct(product);
        item.setProductName(product.getName());
        item.setQuantity(quantity);
        item.setUnitPrice(unitPrice);
        item.setTotalPrice(unitPrice.multiply(BigDecimal.valueOf(quantity)));
        item.setIsReviewed(isReviewed);
        return orderItemRepository.save(item);
    }
}
