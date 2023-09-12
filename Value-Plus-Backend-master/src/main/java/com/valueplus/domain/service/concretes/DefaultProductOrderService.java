package com.valueplus.domain.service.concretes;

import com.valueplus.app.config.audit.AuditEventPublisher;
import com.valueplus.app.exception.NotFoundException;
import com.valueplus.app.exception.ValuePlusException;
import com.valueplus.domain.enums.OrderStatus;
import com.valueplus.domain.mail.EmailService;
import com.valueplus.domain.model.*;
import com.valueplus.domain.service.abstracts.ProductOrderService;
import com.valueplus.domain.service.abstracts.WalletService;
import com.valueplus.flutterwave.model.ProductOrderTransactionResponseFlw;
import com.valueplus.flutterwave.service.CallbackResponse;
import com.valueplus.persistence.entity.*;
import com.valueplus.persistence.repository.*;
import com.valueplus.persistence.specs.OrdersSpecification;
import com.valueplus.persistence.specs.ProductOrderSpecification;
import com.valueplus.persistence.specs.SearchCriteria;
import com.valueplus.persistence.specs.SearchOperation;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.valueplus.domain.enums.ActionType.PRODUCT_ORDER_CREATE;
import static com.valueplus.domain.enums.EntityType.PRODUCT_ORDER;
import static com.valueplus.domain.util.FunctionUtil.setScale;
import static com.valueplus.domain.util.FunctionUtil.toDate;
import static com.valueplus.domain.util.MapperUtil.copy;
import static com.valueplus.domain.util.UserUtils.*;
import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@RequiredArgsConstructor
@Service
public class DefaultProductOrderService implements ProductOrderService {

    private final ProductOrderRepository repository;
    private final ProductRepository productRepository;
    private final WalletService walletService;
    private final EmailService emailService;
    private final UserRepository userRepository;
    private final AuditEventPublisher auditEvent;
    private final ProductOrderPaymentService productOrderPaymentService;
    private final ProductOrderPaymentRepository productOrderPaymentRepository;
    private final OrderRepository orderRepository;
    private final RegistrationService registrationService;
    private final Logger logger = LoggerFactory.getLogger(DefaultAuthenticationService.class);

    @Override
    public List<ProductOrderModel> create(List<ProductOrderModel> orders, User user) throws ValuePlusException {
        boolean anyRecordWithId = orders.stream()
                .anyMatch(order -> order.getId() != null);

        if (anyRecordWithId) {
            throw new ValuePlusException("Order items contains an order with id", BAD_REQUEST);
        }

        List<Long> products = extractProductIds(orders);
        Map<Long, Product> productMap = convertToMap(productRepository.findByIdIn(products));

        ensureAllProductExists(products, productMap);

        List<ProductOrder> productOrderList = convertToEntities(orders, productMap, user);

        var savedProductOrders = repository.saveAll(productOrderList);
        logger.info(productOrderList.get(0).getStatus().toString());
        LocalDateTime dateCreated = null;
        BigDecimal totalCostPrice = BigDecimal.ZERO;
        String customerName = null;
        String address = null;
        String phoneNumber = null;
        BigDecimal totalSellingPrice = BigDecimal.ZERO;
        BigDecimal commission = BigDecimal.ZERO;
        OrderStatus status = null;

        for (ProductOrder pr :productOrderList) {
            if (pr.getProduct().isDiscounted()){
                totalSellingPrice = totalSellingPrice.add(pr.getSellingPrice());
                totalCostPrice = totalCostPrice.add(pr.getProduct().getDiscountPrice().multiply(new BigDecimal(pr.getQuantity())));
                commission = commission.add((pr.getSellingPrice()).subtract(pr.getProduct().getDiscountPrice().multiply(new BigDecimal(pr.getQuantity()))));
            }else {
            totalSellingPrice = totalSellingPrice.add(pr.getSellingPrice());
            totalCostPrice = totalCostPrice.add(pr.getProduct().getPrice().multiply(new BigDecimal(pr.getQuantity())));
            commission = commission.add((pr.getSellingPrice()).subtract(pr.getProduct().getPrice().multiply(new BigDecimal(pr.getQuantity()))));}
            dateCreated = pr.getCreatedAt();
            customerName = pr.getCustomerName();
            status = pr.getStatus();
            phoneNumber = pr.getPhoneNumber();
            address = pr.getAddress();
        }
        logger.info("added values");
        Orders orders1 = Orders.builder()
                .customerName(customerName)
                .dateCreated(dateCreated)
                .commission(commission)
                .totalSellingPrice(totalSellingPrice)
                .totalCostPrice(totalCostPrice)
                .status(status)
                .skuId(savedProductOrders.get(0).getProductSkuId())
                .user(user)
                .customerAddress(address)
                .customerPhone(phoneNumber)
                .build();

        orderRepository.save(orders1);

        // Generating payment link for orders
        BigDecimal  totalAmount = productOrderList.stream()
                .map(ProductOrder::getSellingPrice)
                .reduce(new BigDecimal(0), BigDecimal::add);
        System.out.println("totalAmount = " + totalAmount);


        PaymentRequestDTO paymentRequestDTO = PaymentRequestDTO.builder()
                .email(user.getEmail())
                .amount(totalAmount)
                .orderSkuId(productOrderList.get(0).getProductSkuId())
                .paymentPlatform(orders.get(0).getPaymentPlatform())
                .build();

        productOrderPaymentService.productOrdersPayoutService(paymentRequestDTO);
        // end
        Set<User> users = new HashSet<>();
        User superAdmin = userRepository.findByEmail("vpadmin@gmail.com").get();
        User subAdmin = userRepository.findByEmail("elizabeth@nxt.ng").get();
        users.add(superAdmin);
        users.add(subAdmin);
        for (User record: users) {
            try{
                emailService.sendProductOrderCreationEmail(record, orders1);
            }catch (Exception e){
                logger.error(String.format("Error sending email { %s } :", e.getMessage()));
            }
        }

        return savedProductOrders.stream()
                .map(pOrder -> {
                    auditEvent.publish(new Object(), pOrder, PRODUCT_ORDER_CREATE, PRODUCT_ORDER);
                    return pOrder.toModel();
                })
                .collect(toList());
    }

    @Override
    public Orders get(Long id, User user) throws ValuePlusException {
        Orders productOder;
        if (isAgent(user) || isSuperAgent(user)) {
            productOder = getOrders(id,user);
        } else {
            productOder = getOrders(id);
        }
        return productOder;
    }

    @Override
    public Orders updateStatus(Long id, OrderStatus status, User user) throws ValuePlusException {
        try {
            List<ProductOrder> productOder;
            Orders orders;

            if (isAgent(user) || isSuperAgent(user)) {
                orders = getOrders(id,user);
                productOder = getOrderBySku(orders.getSkuId(),user);
            } else {
                orders = getOrders(id);
                productOder = getOrderBySku(orders.getSkuId());
            }

//            var oldObject = copy(productOder, ProductOrder.class);
            var oldObj = copy(orders,Orders.class);

            validateStatusUpdateRequest(status, orders);
            for (ProductOrder pO:productOder) {
                pO.setStatus(status);
            }

            orders.setStatus(status);

            List<ProductOrder> savedOrder = repository.saveAll(productOder);
            Orders savedOrders = orderRepository.save(orders);

            try{
                emailService.sendProductOrderStatusUpdate(savedOrders.getUser(), savedOrders);
            }catch (Exception e){
                logger.error(String.format("Error sending email { %s } :", e.getMessage()));
            }

            if (OrderStatus.COMPLETED.equals(status)) {
                logger.info(" before credit wallet");
                BigDecimal totalProfit = setScale(orders.getCommission());
                logger.info("after get commison");
                String description = "Credit from ProductOrder completion ".concat(orders.getSkuId());
                walletService.creditWallet(orders.getUser(), totalProfit, description);
                logger.info(" after credit wallet");
            }

//            auditEvent.publish(oldObj, savedOrder, PRODUCT_ORDER_STATUS_UPDATE, PRODUCT_ORDER);
            return savedOrders;
        } catch (Exception e) {
            if (e instanceof ValuePlusException) {
                throw (ValuePlusException) e;
            }
            throw new ValuePlusException(e.getMessage());
        }
    }

    @Override
    public Orders changeStatus(String skuId, OrderStatus orderStatus, User user, Pageable pageable) throws ValuePlusException{
        Page<ProductOrder> productOrders = repository.findAllByProductSkuId(skuId, pageable);
        productOrders.forEach(productOrder -> {
            try {
                validateStatusUpdateRequest(orderStatus, productOrder);
            } catch (ValuePlusException e) {
                e.printStackTrace();
            }
            productOrder.setStatus(orderStatus);});

        Orders orders =  orderRepository.findBySkuId(skuId).orElseThrow(() -> new NotFoundException("Order not found"));
        try {
            validateStatusUpdateRequest(orderStatus, orders);
            orders.setStatus(orderStatus);
            logger.info("after Validate");
            repository.saveAll(productOrders);
            orderRepository.save(orders);
        }catch (ValuePlusException e){
            e.printStackTrace();
        }



        return orders;

    }

    @Override
    public Page<ProductOrderResponse> get(User user, Pageable pageable) throws ValuePlusException {
        try {
            Page<ProductOrder> productOders;
            if (isAgent(user) || isSuperAgent(user)) {
                productOders = repository.findByUser_id(user.getId(),pageable);
            } else {
                productOders = repository.findAll(pageable);
            }
            var p=  productOders.map(ProductOrder::toModel);
            List<ProductOrderModel> productOrderModels = p.getContent();
            Set<String> userProductOrderSku = new HashSet<>();
            for (ProductOrderModel pro: productOrderModels) {
                if (pro.getOrderSkuId() != null){
                    userProductOrderSku.add(pro.getOrderSkuId());}
            }
            logger.info(userProductOrderSku.toString());
            List<ProductOrderResponse> productOrderResponses = new ArrayList<>();

            for (String sku:userProductOrderSku) {
                List<ProductOrderModel> productOrderModelList = productOrderModels.stream()
                        .filter(model -> sku.equals(model.getOrderSkuId()))
                        .collect(Collectors.toList());
                LocalDateTime dateCreated = null;
                Long orderId = null;
                BigDecimal totalCostPrice = BigDecimal.ZERO;
                String customerName = null;
                BigDecimal totalSellingPrice = BigDecimal.ZERO;
                BigDecimal commission = BigDecimal.ZERO;
                OrderStatus status = null;
                for (ProductOrderModel pr :productOrderModelList) {
                    totalSellingPrice = totalSellingPrice.add(new BigDecimal(String.valueOf(pr.getSellingPrice())));
                    totalCostPrice = totalCostPrice.add(new BigDecimal(String.valueOf(pr.getProductPrice())));
                    commission = commission.add(new BigDecimal(String.valueOf(pr.getTotalProfit())));
                    dateCreated = pr.getCreatedAt();
                    customerName = pr.getCustomerName();
                    status = pr.getStatus();
                    orderId = pr.getId();
                }

                ProductOrderResponse productOrderResponse =new ProductOrderResponse();
                productOrderResponse.setOrderId(orderId);
                productOrderResponse.setSkuId(sku);
                productOrderResponse.setDateCreated(dateCreated);
                productOrderResponse.setTotalCostPrice(totalCostPrice);
                productOrderResponse.setTotalSellingPrice(totalSellingPrice);
                productOrderResponse.setCommission(commission);
                productOrderResponse.setCustomerName(customerName);
                productOrderResponse.setStatus(status);
                productOrderResponses.add(productOrderResponse);

            }
            productOrderResponses.sort(Comparator.comparing(ProductOrderResponse::getDateCreated));
            Collections.reverse(productOrderResponses);
            Pageable paging = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());

            Page<ProductOrderResponse> page = new PageImpl<>(productOrderResponses,paging,productOrderResponses.size());

            return page;



        } catch (Exception e) {
            System.out.println(e.getMessage());
            throw new ValuePlusException("Error getting all orders", e);
        }
    }

    @Override
    public Page<Orders> getAll(User user, Pageable pageable) throws ValuePlusException {
        try {
            Page<Orders> productOders;
            if (isAgent(user) || isSuperAgent(user)) {
                productOders = orderRepository.findByUser_id(user.getId(),pageable);
            } else {
                productOders = orderRepository.findAll(pageable);
            }
            return productOders;
        }
        catch (Exception e) {
            throw new ValuePlusException("Error getting all orders", e);
        }
    }

    @Override
    public Page<ProductOrderModel> getByProductId(Long productId, User user, Pageable pageable) throws ValuePlusException {
        try {
            Page<ProductOrder> productOrders;
            if (isAgent(user) || isSuperAgent(user)) {
                productOrders = repository.findByUser_idAndProduct_id(user.getId(), productId, pageable);
            } else {
                productOrders = repository.findByProduct_id(productId, pageable);
            }
            return productOrders.map(ProductOrder::toModel);
        } catch (Exception e) {
            throw new ValuePlusException(format("Error getting orders by productId %d", productId), e);
        }
    }

    @Override
    public Page<ProductOrderModel> filterProduct(String skuId,
                                                 Long productId,
                                                 String customerName,
                                                 OrderStatus status,
                                                 LocalDate startDate,
                                                 LocalDate endDate,
                                                 Pageable pageable,
                                                 User user) {
        Product product = null;
        if (productId != null) {
            product = productRepository.findById(productId).orElse(null);
        }
        ProductOrderSpecification specification = buildSpecification(skuId, customerName, status, startDate, endDate, product, user);

        Page<ProductOrderModel> productOrderModels = repository.findAll(Specification.where(specification), pageable)
                .map(ProductOrder::toModel);

        ProductOrderTransactions transaction = productOrderPaymentRepository.findByOrderSkuId(skuId)
                .orElse(ProductOrderTransactions.builder().build());

        List<ProductOrderModel> productOrder = productOrderModels.getContent().stream()
                .map(productOrderModel -> {
                    productOrderModel.setPaymentLink(transaction.getAuthorization_url());
                    return productOrderModel;
                }).collect(Collectors.toList());

        return new PageImpl<>(productOrder, pageable, productOrderModels.getTotalElements());
    }

    @Override
    public Page<Orders> filterProduct(String skuId,
                                      String customerName,
                                      OrderStatus status,
                                      LocalDate startDate,
                                      LocalDate endDate,
                                      Pageable pageable,
                                      User user) {
        OrdersSpecification specification = buildOrdersSpecification(skuId, customerName, status, startDate, endDate, user);

        Page<Orders> orderModels = orderRepository.findAll(Specification.where(specification), pageable);

////        Page<ProductOrderModel> productOrderModels = repository.findAll(Specification.where(specification), pageable)
////                .map(ProductOrder::toModel);
//
//        ProductOrderTransactions transaction = productOrderPaymentRepository.findByOrderSkuId(skuId)
//                .orElse(ProductOrderTransactions.builder().build());
//
//        List<ProductOrderModel> productOrder = productOrderModels.getContent().stream()
//                .map(productOrderModel -> {
//                    productOrderModel.setPaymentLink(transaction.getAuthorization_url());
//                    return productOrderModel;
//                }).collect(Collectors.toList());

        return orderModels;
    }

    @Override
    public Page<ProductOrderModel> searchProduct(SearchProductOrder model, Pageable pageable, User user) throws ValuePlusException {
        var userFilter = user;
        Product product = null;
        if (model.getProductId() != null) {
            product = productRepository.findById(model.getProductId()).orElse(null);
        }
        if (model.getAgentId() != null && (!isAgent(user) || !isSuperAgent(user))) {
            userFilter = userRepository.findById(model.getAgentId())
                    .orElseThrow(() -> new ValuePlusException(format("Invalid agent id %d", model.getAgentId())));
        }

        ProductOrderSpecification specification = buildSpecification(null, model.getCustomerName(),
                model.getStatus(),
                toDate(model.getStartDate()),
                toDate(model.getEndDate()),
                product,
                userFilter);
        return repository.findAll(Specification.where(specification), pageable)
                .map(ProductOrder::toModel);
    }

    public Page<ProductOrderModel> findProductOrderBySkuId(String productSkuId, Pageable pageable){

        Page<ProductOrderModel> result = repository.findAllByProductSkuId(productSkuId, pageable).map(ProductOrder::toModel);

        ProductOrderTransactions transaction = productOrderPaymentRepository.findByOrderSkuId(productSkuId)
                .orElse(ProductOrderTransactions.builder().build());

        List<ProductOrderModel> productOrder = result.getContent().stream()
                .map(productOrderModel -> {
                    productOrderModel.setPaymentLink(transaction.getAuthorization_url());
                    return productOrderModel;
                }).collect(Collectors.toList());

        return new PageImpl<>(productOrder, pageable, result.getTotalElements());
    }

    private void validateStatusUpdateRequest(OrderStatus status, ProductOrder productOder) throws ValuePlusException {
        if (productOder.getStatus().equals(status)) {
            throw new ValuePlusException(format("ProductOrder status is presently %s", status), BAD_REQUEST);
        }
        if (OrderStatus.CANCELLED.equals(status) && !OrderStatus.PENDING.equals(productOder.getStatus())) {
            throw new ValuePlusException("Only Pending ProductOrder can be cancelled", BAD_REQUEST);
        }
    }

    private void validateStatusUpdateRequest(OrderStatus status, Orders productOder) throws ValuePlusException {
        if (productOder.getStatus().equals(status)) {
            throw new ValuePlusException(format("ProductOrder status is presently %s", status), BAD_REQUEST);
        }
        if (OrderStatus.CANCELLED.equals(status) && !OrderStatus.PENDING.equals(productOder.getStatus())) {
            throw new ValuePlusException("Only Pending ProductOrder can be cancelled", BAD_REQUEST);
        }
    }

    private List<ProductOrder> convertToEntities(List<ProductOrderModel> orders, Map<Long, Product> productMap, User user) throws ValuePlusException {
        List<ProductOrder> productOrderList = new ArrayList<>();
        String productId = generateProductOrderId();
        for (ProductOrderModel order : orders) {
            var product = productMap.get(order.getProductId());
            ensureSellingPriceIsValid(order, product);

            productOrderList.add(ProductOrder.fromModel(order, product, user)
                    .setStatus(OrderStatus.PENDING)
                    .setProductSkuId(productId)
                    .setProductImage(product.getImage()));
        }
        return productOrderList;
    }

    private void ensureSellingPriceIsValid(ProductOrderModel order, Product product) throws ValuePlusException {
        if (order.isDiscounted()){
            if (product.getDiscountPrice().compareTo(order.getSellingPrice()) >0){
                throw new ValuePlusException("Selling price must not be less than discount price", BAD_REQUEST);
            }
        }else
        if (product.getPrice().compareTo(order.getSellingPrice()) > 0) {
            throw new ValuePlusException("Selling price must not be less than product price", BAD_REQUEST);
        }
    }

    private List<Long> extractProductIds(List<ProductOrderModel> orders) {
        return orders.parallelStream()
                .map(ProductOrderModel::getProductId)
                .distinct()
                .collect(toList());
    }

    private void ensureAllProductExists(List<Long> products, Map<Long, Product> productMap) throws ValuePlusException {
        for (Long pid : products) {
            if (!productMap.containsKey(pid)) {
                throw new ValuePlusException(format("Product %d does not exist", pid), BAD_REQUEST);
            }
        }
    }

    private Map<Long, Product> convertToMap(Set<Product> products) {
        return products.stream()
                .collect(toMap(Product::getId, Function.identity()));
    }

    private ProductOrder getOrder(Long id, User user) throws ValuePlusException {
        return repository.findByIdAndUser_id(id, user.getId())
                .orElseThrow(() -> new ValuePlusException("Order does not exist", NOT_FOUND));
    }
    private List<ProductOrder> getOrderBySku(String skuId, User user) throws ValuePlusException {
        return repository.findByProductSkuIdAndUser_id(skuId, user.getId());
    }
    private List<ProductOrder> getOrderBySku(String skuId) throws ValuePlusException {
        return repository.findByProductSkuId(skuId);
    }
    private Orders getOrders(Long id, User user) throws ValuePlusException{
        return orderRepository.findByIdAndUser_id(id,user.getId())
                .orElseThrow(() -> new ValuePlusException("Order does not exist",NOT_FOUND));
    }
    private Orders getOrders(Long id) throws ValuePlusException{
        return orderRepository.findById(id)
                .orElseThrow(() -> new ValuePlusException("Order does not exist",NOT_FOUND));
    }
    private ProductOrder getOrder(Long id) throws ValuePlusException {
        return repository.findById(id)
                .orElseThrow(() -> new ValuePlusException("Order does not exist", NOT_FOUND));
    }

    @Override
    public void verifyPaystackCall(String reference) throws ValuePlusException {
        ProductOrderTransactionResponse response= productOrderPaymentService.verify(reference);

        if (response.getData().getStatus().equalsIgnoreCase("success")){
            ProductOrderTransactions transaction = productOrderPaymentRepository.findByReference(reference).orElse(null);
            Orders    orders = orderRepository.findBySkuId(transaction.getOrderSkuId()).orElse(null);

            List<Orders>orderList = orderRepository.findByUserAndStatus(orders.getUser(),OrderStatus.COMPLETED);
            if (orderList.isEmpty()){
                registrationService.updateCommission(orders.getUser());}
            updateStatus(orders.getId(),OrderStatus.COMPLETED,orders.getUser());
        }}

    @Override
    public void verifyFlwCall(CallbackResponse callbackResponse) throws ValuePlusException {
        ProductOrderTransactionResponseFlw response= productOrderPaymentService.verifyFlw(callbackResponse);

        if (response.getStatus().toString().equalsIgnoreCase("success")){
            ProductOrderTransactions transaction = productOrderPaymentRepository.findByReference(callbackResponse.getTrx_ref()).orElse(null);
            Orders orders = orderRepository.findBySkuId(transaction.getOrderSkuId()).orElse(null);

            List<Orders>orderList = orderRepository.findByUserAndStatus(orders.getUser(),OrderStatus.COMPLETED);
            if (orderList.isEmpty()){
                registrationService.updateCommission(orders.getUser());}
            updateStatus(orders.getId(),OrderStatus.COMPLETED,orders.getUser());
        }}

    private ProductOrderSpecification buildSpecification(String skuId,
                                                         String customerName,
                                                         OrderStatus status,
                                                         LocalDate startDate,
                                                         LocalDate endDate,
                                                         Product product,
                                                         User user) {
        ProductOrderSpecification specification = new ProductOrderSpecification();
        if (customerName != null) {
            specification.add(new SearchCriteria<>("customerName", customerName, SearchOperation.MATCH));
        }

        if (skuId != null) {
            specification.add(new SearchCriteria<>("productSkuId", skuId, SearchOperation.MATCH));
        }

        if (product != null) {
            specification.add(new SearchCriteria<>("product", product, SearchOperation.EQUAL));
        }

        if (status != null) {
            specification.add(new SearchCriteria<>("status", status, SearchOperation.EQUAL));
        }

        if (isAgent(user) || isSuperAgent(user)) {
            specification.add(new SearchCriteria<>("user", user, SearchOperation.EQUAL));
        }

        if (startDate != null) {
            specification.add(new SearchCriteria<>(
                    "createdAt",
                    startDate.atTime(LocalTime.MIN),
                    SearchOperation.GREATER_THAN_EQUAL));
        }

        if (endDate != null) {
            specification.add(new SearchCriteria<>("createdAt",
                    endDate.atTime(LocalTime.MAX),
                    SearchOperation.LESS_THAN_EQUAL));
        }
        return specification;
    }

    private OrdersSpecification buildOrdersSpecification(String skuId,
                                                         String customerName,
                                                         OrderStatus status,
                                                         LocalDate startDate,
                                                         LocalDate endDate,
                                                         User user) {
        OrdersSpecification specification = new OrdersSpecification();
        if (customerName != null) {
            specification.add(new SearchCriteria<>("customerName", customerName, SearchOperation.MATCH));
        }

        if (skuId != null) {
            specification.add(new SearchCriteria<>("skuId", skuId, SearchOperation.MATCH));
        }

        if (status != null) {
            specification.add(new SearchCriteria<>("status", status, SearchOperation.EQUAL));
        }

        if (isAgent(user) || isSuperAgent(user)) {
            specification.add(new SearchCriteria<>("user", user, SearchOperation.EQUAL));
        }

        if (startDate != null) {
            specification.add(new SearchCriteria<>(
                    "createdAt",
                    startDate.atTime(LocalTime.MIN),
                    SearchOperation.GREATER_THAN_EQUAL));
        }

        if (endDate != null) {
            specification.add(new SearchCriteria<>("createdAt",
                    endDate.atTime(LocalTime.MAX),
                    SearchOperation.LESS_THAN_EQUAL));
        }
        return specification;
    }

    private static String generateProductOrderId(){
        return "VPO-"+ UUID.randomUUID().toString().toUpperCase();

    }



//    public String getPaystackCallback(String response){
//        logger.info(response);
//
//    }
}