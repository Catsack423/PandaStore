package project.project.Service.implement;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import project.project.DTO.address.AddressResponse;
import project.project.DTO.address.CreateAddressRequest;
import project.project.DTO.cart.CartDtos;
import project.project.DTO.order.OrderGroupResponse;
import project.project.DTO.shipping.ShippingFeeResponse;
import project.project.Entity.order.PaymentMethod;
import project.project.Repository.CartRepository;
import project.project.Repository.OrderGroupRepository;
import project.project.Service.api.AddressService;
import project.project.Service.api.CartService;
import project.project.Service.api.OrderOrchestrationService;
import project.project.Service.api.ShippingService;
import project.project.Service.strategy.shipping.ShippingFeeStrategyFactory;

import java.math.BigDecimal;
import java.util.*;

@Service
@Transactional(readOnly = true)
public class CustomerCheckoutService {
    private final CartRepository cartRepository;
    private final OrderGroupRepository orderGroups;
    private final CartService carts;
    private final AddressService addresses;
    private final ShippingService shipping;
    private final ShippingFeeStrategyFactory methods;
    private final OrderOrchestrationService orders;

    public CustomerCheckoutService(CartRepository cartRepository, OrderGroupRepository orderGroups, CartService carts,
            AddressService addresses, ShippingService shipping,
            ShippingFeeStrategyFactory methods, OrderOrchestrationService orders) {
        this.cartRepository = cartRepository;
        this.orderGroups = orderGroups;
        this.carts = carts;
        this.addresses = addresses;
        this.shipping = shipping;
        this.methods = methods;
        this.orders = orders;
    }

    public record Item(Long productId, String productName, Long sellerId, String shopName,
            BigDecimal unitPrice, Integer quantity, String imageUrl) {}
    public record Context(Long customerId, List<Item> items, List<AddressResponse> addresses,
            List<String> shippingMethods, List<PaymentMethod> paymentMethods) {}
    public record SyncCart(@NotNull List<CartDtos.@Valid @NotNull AddItem> items) {}
    public record Quote(@NotNull @Positive Long sellerId, @NotBlank String shippingMethod,
            @NotNull @Positive Long addressId) {}
    public record PlaceOrder(@NotNull @Positive Long shippingAddressId,
            @NotEmpty Map<@NotNull @Positive Long, @NotBlank String> sellerShippingMethods,
            @NotNull PaymentMethod paymentMethod) {}

    public Context context(Long customerId) {
        var items = cartRepository.findByCustomer_CustomerId(customerId)
                .map(cart -> cart.getItems().stream().map(item -> {
                    var product = item.getProduct();
                    return new Item(product.getProductId(), product.getName(),
                            product.getSeller().getSellerId(), product.getSeller().getShopName(),
                            product.getPrice(), item.getQuantity(), product.getImages().isEmpty()
                                    ? null : product.getImages().getFirst().getImageUrl());
                }).toList()).orElse(List.of());
        return new Context(customerId, items,
                addresses.getAllAddressesByCustomerId(customerId).stream()
                        .map(AddressResponse::fromEntity).toList(),
                methods.getAvailableMethods(), List.of(PaymentMethod.values()));
    }

    /** The visible cart is the complete checkout selection. Quantities are absolute, so retries do not add twice. */
    @Transactional
    public Context synchronizeCart(Long customerId, SyncCart request) {
        var desired = new LinkedHashMap<Long, Integer>();
        for (var item : request.items()) {
            if (desired.putIfAbsent(item.productId(), item.quantity()) != null) {
                throw new IllegalArgumentException("Duplicate product in checkout cart");
            }
        }
        var cart = carts.createCart(customerId);
        for (var item : List.copyOf(cart.getItems())) {
            if (!desired.containsKey(item.getProduct().getProductId())) {
                carts.removeItemFromCart(customerId, item.getCartItemId());
            }
        }
        for (var entry : desired.entrySet()) {
            var existing = cart.getItems().stream()
                    .filter(i -> i.getProduct().getProductId().equals(entry.getKey())).findFirst();
            if (existing.isPresent()) {
                carts.updateItemQuantity(customerId, existing.get().getCartItemId(), entry.getValue());
            } else {
                carts.addItemToCart(customerId, entry.getKey(), entry.getValue());
            }
        }
        return context(customerId);
    }

    public ShippingFeeResponse quote(Long customerId, Quote request) {
        requireAddress(customerId, request.addressId());
        if (context(customerId).items().stream().noneMatch(i -> i.sellerId().equals(request.sellerId()))) {
            throw new IllegalArgumentException("Shop is not in your cart");
        }
        String method = methods.requireSupportedMethod(request.shippingMethod());
        return new ShippingFeeResponse(request.sellerId(), method,
                shipping.calculateShippingFee(request.sellerId(), method, request.addressId()));
    }

    @Transactional
    public AddressResponse addAddress(Long customerId, CreateAddressRequest request) {
        // Identity comes from the session, never from a browser-provided customer ID.
        var ownedRequest = new CreateAddressRequest(customerId, request.receiverName(), request.phoneNumber(),
                request.addressLine(), request.district(), request.province(), request.postalCode(), request.isDefault());
        return AddressResponse.fromEntity(addresses.addAddressToCustomerByCustomerId(customerId, ownedRequest));
    }

    @Transactional
    public AddressResponse updateAddress(Long customerId, Long addressId, CreateAddressRequest request) {
        var ownedRequest = new CreateAddressRequest(customerId, request.receiverName(), request.phoneNumber(),
                request.addressLine(), request.district(), request.province(), request.postalCode(), request.isDefault());
        return AddressResponse.fromEntity(addresses.updateAddress(customerId, addressId, ownedRequest));
    }

    @Transactional
    public OrderGroupResponse placeOrder(Long customerId, PlaceOrder request) {
        requireAddress(customerId, request.shippingAddressId());
        var cart = carts.createCart(customerId);
        Set<Long> sellers = new HashSet<>();
        cart.getItems().forEach(i -> sellers.add(i.getProduct().getSeller().getSellerId()));
        if (sellers.isEmpty()) throw new IllegalArgumentException("Your cart is empty");
        if (!sellers.equals(request.sellerShippingMethods().keySet())) {
            throw new IllegalArgumentException("Choose a shipping method for every shop in your cart");
        }
        Map<Long, String> selectedMethods = new HashMap<>();
        request.sellerShippingMethods().forEach((seller, method) ->
                selectedMethods.put(seller, methods.requireSupportedMethod(method)));
        // Checkout always includes every cart item, with stock validation by the cart service.
        for (var item : List.copyOf(cart.getItems())) {
            carts.updateItemSelection(customerId, item.getCartItemId(), true);
        }
        return OrderGroupResponse.fromEntity(orders.createOrderGroupFromCart(customerId,
                request.shippingAddressId(), selectedMethods, request.paymentMethod()));
    }

    public OrderGroupResponse order(Long customerId, Long orderGroupId) {
        var group = orders.getOrderGroupDetails(orderGroupId);
        if (!group.getCustomer().getCustomerId().equals(customerId)) {
            throw new jakarta.persistence.EntityNotFoundException("Order not found");
        }
        return OrderGroupResponse.fromEntity(group);
    }

    public List<OrderGroupResponse> orderHistory(Long customerId) {
        return orderGroups.findByCustomer_CustomerId(customerId).stream()
                .sorted(Comparator.comparing(project.project.Entity.order.OrderGroup::getCreatedAt).reversed())
                .map(OrderGroupResponse::fromEntity).toList();
    }

    private void requireAddress(Long customerId, Long addressId) {
        if (addresses.getAllAddressesByCustomerId(customerId).stream()
                .noneMatch(a -> a.getAddressId().equals(addressId))) {
            throw new IllegalArgumentException("Shipping address does not belong to customer");
        }
    }
}
