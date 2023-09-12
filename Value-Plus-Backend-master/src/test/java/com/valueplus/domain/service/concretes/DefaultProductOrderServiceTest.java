//package com.valueplus.domain.service.concretes;
//
//import com.valueplus.app.config.audit.AuditEventPublisher;
//import com.valueplus.app.exception.ValuePlusException;
//import com.valueplus.domain.enums.ActionType;
//import com.valueplus.domain.enums.EntityType;
//import com.valueplus.domain.enums.OrderStatus;
//import com.valueplus.domain.mail.EmailService;
//import com.valueplus.domain.model.*;
//import com.valueplus.domain.service.abstracts.WalletService;
//import com.valueplus.domain.util.FunctionUtil;
//import com.valueplus.fixtures.TestFixtures;
//import com.valueplus.persistence.entity.Product;
//import com.valueplus.persistence.entity.ProductOrder;
//import com.valueplus.persistence.entity.ProductOrderTransactions;
//import com.valueplus.persistence.entity.User;
//import com.valueplus.persistence.repository.ProductOrderPaymentRepository;
//import com.valueplus.persistence.repository.ProductOrderRepository;
//import com.valueplus.persistence.repository.ProductRepository;
//import com.valueplus.persistence.specs.ProductOrderSpecification;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.mockito.ArgumentMatchers;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.PageImpl;
//import org.springframework.data.domain.Pageable;
//import org.springframework.http.HttpStatus;
//
//import java.math.BigDecimal;
//import java.time.LocalDate;
//import java.time.LocalDateTime;
//import java.util.HashSet;
//import java.util.List;
//import java.util.Optional;
//import java.util.Set;
//
//import static java.util.Collections.singletonList;
//import static org.assertj.core.api.Assertions.assertThat;
//import static org.assertj.core.api.Assertions.assertThatThrownBy;
//import static org.mockito.ArgumentMatchers.*;
//import static org.mockito.Mockito.*;
//import static org.mockito.MockitoAnnotations.initMocks;
//import static org.springframework.http.HttpStatus.BAD_REQUEST;
//import static org.springframework.http.HttpStatus.NOT_FOUND;
//
//class DefaultProductOrderServiceTest {
//    @Mock
//    private ProductRepository productRepository;
//    @Mock
//    private ProductOrderRepository repository;
//    @Mock
//    private Pageable pageable;
//    @Mock
//    private EmailService emailService;
//    @Mock
//    private WalletService walletService;
//    @Mock
//    private AuditEventPublisher auditEvent;
//    @Mock
//    private ProductOrderPaymentService productOrderPaymentService;
//    @Mock
//    private ProductOrderPaymentRepository productOrderPaymentRepository;
//    @InjectMocks
//    private DefaultProductOrderService orderService;
//    private ProductOrderModel productOrderModel;
//    private ProductOrder entity;
//    private Product product;
//
//    private User agentUser;
//
//    @BeforeEach
//    void setUp() {
//        initMocks(this);
//        User user = TestFixtures.getUser(RoleType.AGENT);
//        agentUser = TestFixtures.mockUser();
//
//        productOrderModel = orderModelFixture();
//        product = Product.builder()
//                .id(1L)
//                .price(BigDecimal.ONE)
//                .build();
//
//        entity = ProductOrder.fromModel(productOrderModel, product, user);
//        entity.setCreatedAt(LocalDateTime.now());
//        entity.setUpdatedAt(LocalDateTime.now());
//
//        doNothing().when(auditEvent)
//                .publish(isA(ProductOrder.class), isA(ProductOrder.class), isA(ActionType.class), isA(EntityType.class));
//    }
//
//    @Test
//    void create_WithBadData() {
//        List<ProductOrderModel> orders = singletonList(productOrderModel);
//
//        assertThatThrownBy(() -> orderService.create(orders, agentUser))
//                .isInstanceOf(ValuePlusException.class)
//                .hasFieldOrPropertyWithValue("httpStatus", BAD_REQUEST)
//                .hasMessage("Order items contains an order with id");
//    }
//
//    @Test
//    void create_WithNonExistingProduct() {
//        when(productRepository.findByIdIn(anyList()))
//                .thenReturn(new HashSet<>());
//
//        productOrderModel.setId(null);
//        List<ProductOrderModel> orders = singletonList(productOrderModel);
//
//        assertThatThrownBy(() -> orderService.create(orders, agentUser))
//                .isInstanceOf(ValuePlusException.class)
//                .hasFieldOrPropertyWithValue("httpStatus", BAD_REQUEST)
//                .hasMessage("Product 1 does not exist");
//
//        verify(productRepository).findByIdIn(anyList());
//    }
//
//    @Test
//    void create_WithInvalidPrice() {
//        when(productRepository.findByIdIn(anyList()))
//                .thenReturn(Set.of(product.setPrice(new BigDecimal(20))));
//        when(repository.saveAll(anyList()))
//                .thenReturn(List.of(entity));
//
//        productOrderModel.setId(null);
//        productOrderModel.setSellingPrice(BigDecimal.TEN);
//        List<ProductOrderModel> orders = singletonList(productOrderModel);
//
//        assertThatThrownBy(() -> orderService.create(orders, agentUser))
//                .isInstanceOf(ValuePlusException.class)
//                .hasFieldOrPropertyWithValue("httpStatus", BAD_REQUEST)
//                .hasMessage("Selling price must not be less than product price");
//
//        verify(productRepository).findByIdIn(anyList());
//    }
//
//    @Test
//    void create_successful() throws ValuePlusException {
//        when(productRepository.findByIdIn(anyList()))
//                .thenReturn(Set.of(product));
//        when(repository.saveAll(anyList()))
//                .thenReturn(List.of(entity));
//
//        when(productOrderPaymentService.productOrdersPayoutService(any()))
//                .thenReturn(new PaymentResponseDTO());
//
//        productOrderModel.setId(null);
//        List<ProductOrderModel> orders = singletonList(productOrderModel);
//
//        List<ProductOrderModel> result = orderService.create(orders, agentUser);
//
//        assertThat(result).isNotNull();
//        assertThat(result.get(0).getCreatedAt()).isNotNull();
//        assertThat(result.get(0).getCustomerName()).isEqualTo("customerName");
//        assertThat(result.get(0).getProductId()).isEqualTo(1L);
//
//        verify(productRepository).findByIdIn(anyList());
//        verify(repository).saveAll(anyList());
//    }
//
//    @Test
//    void getById_successfulAdmin() throws ValuePlusException {
//        User user = TestFixtures.getUser(RoleType.ADMIN);
//
//        when(repository.findById(eq(1L)))
//                .thenReturn(Optional.of(entity));
//
//        ProductOrderModel result = orderService.get(1L, user);
//
//        assertThat(result).isNotNull();
//        assertThat(result.getProductId()).isEqualTo(1L);
//        assertThat(result.getCreatedAt()).isNotNull();
//        assertThat(result.getCustomerName()).isEqualTo("customerName");
//
//        verify(repository).findById(eq(1L));
//    }
//
//    @Test
//    void getById_successfulAgent() throws ValuePlusException {
//        when(repository.findByIdAndUser_id(eq(1L), eq(1L)))
//                .thenReturn(Optional.of(entity));
//
//        ProductOrderModel result = orderService.get(1L, agentUser);
//
//        assertThat(result).isNotNull();
//        assertThat(result.getProductId()).isEqualTo(1L);
//        assertThat(result.getCreatedAt()).isNotNull();
//        assertThat(result.getCustomerName()).isEqualTo("customerName");
//
//        verify(repository).findByIdAndUser_id(eq(1L), eq(1L));
//    }
//
//    @Test
//    void getById_failed() {
//        when(repository.findByIdAndUser_id(eq(1L), eq(1L)))
//                .thenReturn(Optional.empty());
//
//        assertThatThrownBy(() -> orderService.get(1L, agentUser))
//                .isInstanceOf(ValuePlusException.class)
//                .hasFieldOrPropertyWithValue("httpStatus", NOT_FOUND)
//                .hasMessage("Order does not exist");
//
//        verify(repository).findByIdAndUser_id(eq(1L), eq(1L));
//    }
//
//    @Test
//    void getAll_successful_Admin() throws ValuePlusException {
//        User user = TestFixtures.getUser(RoleType.ADMIN);
//
//        when(repository.findAll(eq(pageable)))
//                .thenReturn(new PageImpl<>(singletonList(entity)));
//
//        Page<ProductOrderModel> result = orderService.get(user, pageable);
//
//        assertThat(result).isNotNull();
//        assertThat(result.getContent().get(0).getCreatedAt()).isNotNull();
//        assertThat(result.getContent().get(0).getCustomerName()).isEqualTo("customerName");
//
//        verify(repository).findAll(eq(pageable));
//    }
//
//    @Test
//    void getAll_successful_Agent() throws ValuePlusException {
//        when(repository.findByUser_id(eq(1L), eq(pageable)))
//                .thenReturn(new PageImpl<>(singletonList(entity)));
//
//        Page<ProductOrderModel> result = orderService.get(agentUser, pageable);
//
//        assertThat(result).isNotNull();
//        assertThat(result.getContent().get(0).getCreatedAt()).isNotNull();
//        assertThat(result.getContent().get(0).getCustomerName()).isEqualTo("customerName");
//
//        verify(repository).findByUser_id(eq(1L), eq(pageable));
//    }
//
//    @Test
//    void getByProductId_Admin() throws ValuePlusException {
//        User user = TestFixtures.getUser(RoleType.ADMIN);
//
//        when(repository.findByProduct_id(eq(1L), eq(pageable)))
//                .thenReturn(new PageImpl<>(singletonList(entity)));
//
//        Page<ProductOrderModel> result = orderService.getByProductId(1L, user, pageable);
//
//        assertThat(result).isNotNull();
//        assertThat(result.getContent().get(0).getCreatedAt()).isNotNull();
//        assertThat(result.getContent().get(0).getCustomerName()).isEqualTo("customerName");
//
//        verify(repository).findByProduct_id(eq(1L), eq(pageable));
//    }
//
//    @Test
//    void getByProductId_Agent() throws ValuePlusException {
//        when(repository.findByUser_idAndProduct_id(eq(1L), eq(1L), eq(pageable)))
//                .thenReturn(new PageImpl<>(singletonList(entity)));
//
//        Page<ProductOrderModel> result = orderService.getByProductId(1L, agentUser, pageable);
//
//        assertThat(result).isNotNull();
//        assertThat(result.getContent().get(0).getCreatedAt()).isNotNull();
//        assertThat(result.getContent().get(0).getCustomerName()).isEqualTo("customerName");
//
//        verify(repository).findByUser_idAndProduct_id(eq(1L), eq(1L), eq(pageable));
//    }
//
//    @Test
//    void filterOrders_successful() throws ValuePlusException {
//        when(productRepository.findById(eq(1L)))
//                .thenReturn(Optional.of(product));
//
//        when(repository.findAll(any(ProductOrderSpecification.class), eq(pageable)))
//                .thenReturn(new PageImpl<>(singletonList(entity)));
//
//        when(productOrderPaymentRepository.findByOrderSkuId(any()))
//                .thenReturn(Optional.of(new ProductOrderTransactions()));
//
//        Page<ProductOrderModel> result = orderService.filterProduct(
//                "",
//                1L,
//                "customerName",
//                null,
//                LocalDate.now(),
//                null,
//                pageable,
//                agentUser);
//
//        assertThat(result).isNotNull();
//        assertThat(result.getContent().get(0).getCreatedAt()).isNotNull();
//        assertThat(result.getContent().get(0).getCustomerName()).isEqualTo("customerName");
//
//        verify(productRepository).findById(eq(1L));
//        verify(repository).findAll(any(ProductOrderSpecification.class), eq(pageable));
//    }
//
//    @Test
//    void updateStatus_failed() {
//        when(repository.findByIdAndUser_id(anyLong(), anyLong()))
//                .thenReturn(Optional.empty());
//
//        assertThatThrownBy(() -> orderService.updateStatus(1L, OrderStatus.COMPLETED, agentUser))
//                .isInstanceOf(ValuePlusException.class)
//                .hasMessage("Order does not exist")
//                .hasFieldOrPropertyWithValue("httpStatus", HttpStatus.NOT_FOUND);
//
//        verify(repository).findByIdAndUser_id(eq(1L), eq(1L));
//    }
//
//
//    @Test
//    void cancelProductOrder() throws ValuePlusException {
//        User user = TestFixtures.mockUser();
//        entity.setStatus(OrderStatus.IN_PROGRESS);
//
//        when(repository.findByIdAndUser_id(anyLong(), anyLong()))
//                .thenReturn(Optional.of(entity));
//
//        assertThatThrownBy(() -> orderService.updateStatus(1L, OrderStatus.CANCELLED, agentUser))
//                .isInstanceOf(ValuePlusException.class)
//                .hasFieldOrPropertyWithValue("httpStatus", BAD_REQUEST)
//                .hasMessage("Only Pending ProductOrder can be cancelled");
//
//        verify(repository).findByIdAndUser_id(eq(1L), eq(1L));
//    }
//
//    @Test
//    void updateStatus_successful_Admin() throws ValuePlusException {
//        User user = TestFixtures.getUser(RoleType.ADMIN);
//
//        when(repository.findById(anyLong()))
//                .thenReturn(Optional.of(entity));
//        when(repository.save(any(ProductOrder.class)))
//                .then(i -> i.getArgument(0, ProductOrder.class));
//        when(walletService.creditWallet(
//                eq(entity.getUser()),
//                eq(BigDecimal.valueOf(9.00)),
//                eq("Credit from ProductOrder completion (id: 1)")))
//                .thenReturn(WalletModel.builder().build());
//
//        ProductOrderModel result = orderService.updateStatus(1L, OrderStatus.COMPLETED, user);
//        assertThat(result).isNotNull();
//        assertThat(result.getStatus()).isEqualTo(OrderStatus.COMPLETED);
//
//        verify(repository).findById(eq(1L));
//        verify(repository).save(any(ProductOrder.class));
//        verify(walletService).creditWallet(
//                eq(entity.getUser()),
//                ArgumentMatchers.eq(FunctionUtil.setScale(BigDecimal.valueOf(90.00))),
//                eq("Credit from ProductOrder completion (id: 1)"));
//    }
//
//    @Test
//    void updateStatus_successful_Agent() throws ValuePlusException {
//        when(repository.findByIdAndUser_id(anyLong(), anyLong()))
//                .thenReturn(Optional.of(entity));
//        when(repository.save(any(ProductOrder.class)))
//                .then(i -> i.getArgument(0, ProductOrder.class));
//
//        ProductOrderModel result = orderService.updateStatus(1L, OrderStatus.COMPLETED, agentUser);
//        assertThat(result).isNotNull();
//        assertThat(result.getStatus()).isEqualTo(OrderStatus.COMPLETED);
//
//        verify(repository).findByIdAndUser_id(eq(1L), eq(1L));
//        verify(repository).save(any(ProductOrder.class));
//    }
//
//    private ProductOrderModel orderModelFixture() {
//        return ProductOrderModel.builder()
//                .id(1L)
//                .customerName("customerName")
//                .address("address")
//                .quantity(10L)
//                .sellingPrice(BigDecimal.TEN)
//                .phoneNumber("09000000000")
//                .status(OrderStatus.IN_PROGRESS)
//                .productId(1L)
//                .build();
//    }
//}