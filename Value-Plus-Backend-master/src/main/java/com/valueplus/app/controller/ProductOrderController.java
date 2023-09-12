package com.valueplus.app.controller;

import com.valueplus.app.exception.ValuePlusException;
import com.valueplus.domain.enums.OrderStatus;
import com.valueplus.domain.model.ProductOrderModel;
import com.valueplus.domain.model.SearchProductOrder;
import com.valueplus.domain.service.abstracts.ProductOrderService;
import com.valueplus.domain.service.concretes.ProductOrderPaymentService;
import com.valueplus.domain.util.UserUtils;
import com.valueplus.flutterwave.service.CallbackResponse;
import com.valueplus.persistence.entity.Orders;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

import static com.valueplus.domain.util.FunctionUtil.toDate;
import static org.springframework.data.domain.Sort.Direction.DESC;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping(path = "v1/product-orders", produces = MediaType.APPLICATION_JSON_VALUE)
public class ProductOrderController {

    private final ProductOrderService productOrderService;
    private final ProductOrderPaymentService orderPaymentService;

    @PostMapping
    @ResponseStatus(HttpStatus.OK)
    public List<ProductOrderModel> create(@Valid @RequestBody List<ProductOrderModel> orders) throws ValuePlusException {
        return productOrderService.create(orders, UserUtils.getLoggedInUser());
    }

    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public Orders get(@PathVariable("id") Long id) throws ValuePlusException {
        return productOrderService.get(id, UserUtils.getLoggedInUser());
    }

    @GetMapping("/product/{id}")
    @ResponseStatus(HttpStatus.OK)
    public Page<ProductOrderModel> getByProductId(@PathVariable("id") Long productId,
                                                  @PageableDefault(sort = "id", direction = DESC) Pageable pageable) throws ValuePlusException {
        return productOrderService.getByProductId(productId, UserUtils.getLoggedInUser(), pageable);
    }

    @GetMapping("/filter-orders")
    @ResponseStatus(HttpStatus.OK)
    public Page<ProductOrderModel> searchProduct(@RequestParam(value = "customerName", required = false) String customerName,
                                                 @RequestParam(value = "productId", required = false) Long productId,
                                                 @RequestParam(value = "status", required = false) OrderStatus status,
                                                 @RequestParam(value = "startDate", required = false) String startDate,
                                                 @RequestParam(value = "endDate", required = false) String endDate,
                                                 @RequestParam(value = "orderSkuId", required = false) String skuId,
                                                 @PageableDefault(sort = "id", direction = DESC) Pageable pageable) throws ValuePlusException {

        return productOrderService.filterProduct(
                skuId,
                productId,
                customerName,
                status,
                toDate(startDate),
                toDate(endDate),
                pageable,
                UserUtils.getLoggedInUser());
    }

    @GetMapping("/filter")
    @ResponseStatus(HttpStatus.OK)
    public Page<Orders> searchProduct(@RequestParam(value = "customerName", required = false) String customerName,
                                                 @RequestParam(value = "status", required = false) OrderStatus status,
                                                 @RequestParam(value = "startDate", required = false) String startDate,
                                                 @RequestParam(value = "endDate", required = false) String endDate,
                                                 @RequestParam(value = "orderSkuId", required = false) String skuId,
                                                 @PageableDefault(sort = "id", direction = DESC) Pageable pageable)  {

        return productOrderService.filterProduct(
                skuId,
                customerName,
                status,
                toDate(startDate),
                toDate(endDate),
                pageable,
                UserUtils.getLoggedInUser());
    }

    @GetMapping("/sku-id")
    @ResponseStatus(HttpStatus.OK)
    public Page<ProductOrderModel> searchProduct(@RequestParam(value = "key", required = false) String skuId,  @PageableDefault(sort = "id", direction = DESC) Pageable pageable){
        return productOrderService.findProductOrderBySkuId(skuId, pageable);
    }

    @PostMapping("/searches")
    @ResponseStatus(HttpStatus.OK)
    public Page<ProductOrderModel> filterProduct(@Valid @RequestBody SearchProductOrder searchProductOrder,
                                                 @PageableDefault(sort = "id", direction = DESC) Pageable pageable) throws ValuePlusException {

        return productOrderService.searchProduct(
                searchProductOrder,
                pageable,
                UserUtils.getLoggedInUser());
    }

    @PostMapping("/{id}/cancel")
    @ResponseStatus(HttpStatus.OK)
    public Orders cancelOrder(@PathVariable("id") Long orderId) throws ValuePlusException {
        return productOrderService.updateStatus(orderId, OrderStatus.CANCELLED, UserUtils.getLoggedInUser());
    }

    @GetMapping("/{skuId}/cancel")
    @ResponseStatus(HttpStatus.OK)
    public Orders cancelOrderBySkuId(@PathVariable String skuId, Pageable pageable) throws ValuePlusException {
        return productOrderService.changeStatus(skuId, OrderStatus.CANCELLED, UserUtils.getLoggedInUser(), pageable);
    }

    @PreAuthorize("hasAuthority('UPDATE_PRODUCT_ORDER_STATUS')")
    @PostMapping("/{id}/status/{status}/update")
    @ResponseStatus(HttpStatus.OK)
    public Orders updateStatus(@PathVariable("id") Long orderId,
                                          @PathVariable("status") OrderStatus status) throws ValuePlusException {
        return productOrderService.updateStatus(orderId, status, UserUtils.getLoggedInUser());
    }

    @GetMapping("")
    @ResponseStatus(HttpStatus.OK)
    public Page<Orders> getAllOrders(@PageableDefault(sort = "id", direction = DESC) Pageable pageable) throws ValuePlusException {
        return productOrderService.getAll(UserUtils.getLoggedInUser(), pageable);
    }

    @GetMapping("/verify-paystack-callback")
    @ResponseStatus(HttpStatus.OK)
    public void verifyPaystackCallback(@RequestParam String reference) throws ValuePlusException {
        log.info(reference.concat( " this is the refernce"));
        productOrderService.verifyPaystackCall(reference);
    }

    @GetMapping("/verify/flutterwave-callback")
    @ResponseStatus(HttpStatus.OK)
    public void verifyFlutterwaveCallback(@RequestParam String status, @RequestParam String tx_ref, @RequestParam String transaction_id) throws ValuePlusException {
        log.info(tx_ref.concat( " this is the reference"));
        CallbackResponse response = new CallbackResponse(status, tx_ref,transaction_id);
        productOrderService.verifyFlwCall(response);
    }
}
