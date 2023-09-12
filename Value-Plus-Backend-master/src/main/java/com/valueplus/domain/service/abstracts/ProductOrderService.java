package com.valueplus.domain.service.abstracts;

import com.valueplus.app.exception.ValuePlusException;
import com.valueplus.domain.enums.OrderStatus;
import com.valueplus.domain.model.ProductOrderModel;
import com.valueplus.domain.model.ProductOrderResponse;
import com.valueplus.domain.model.SearchProductOrder;
import com.valueplus.flutterwave.service.CallbackResponse;
import com.valueplus.persistence.entity.Orders;
import com.valueplus.persistence.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

public interface ProductOrderService {

    List<ProductOrderModel> create(List<ProductOrderModel> order, User user) throws ValuePlusException;

    Orders get(Long id, User user) throws ValuePlusException;

    Orders updateStatus(Long id, OrderStatus status, User user) throws ValuePlusException;

    Orders changeStatus(String id, OrderStatus status, User user, Pageable pageable) throws ValuePlusException;

    Page<ProductOrderResponse> get(User user, Pageable pageable) throws ValuePlusException;

    Page<Orders> getAll(User user, Pageable pageable) throws ValuePlusException;

    Page<ProductOrderModel> getByProductId(Long productId, User vpUser, Pageable pageable) throws ValuePlusException;

    Page<ProductOrderModel> filterProduct(String skuId,
                                         Long productId,
                                         String customerName,
                                         OrderStatus status,
                                         LocalDate startDate,
                                         LocalDate endDate,
                                         Pageable pageable,
                                         User user) throws ValuePlusException;

    Page<Orders> filterProduct(String skuId,
                               String customerName,
                               OrderStatus status,
                               LocalDate startDate,
                               LocalDate endDate,
                               Pageable pageable,
                               User user);

    Page<ProductOrderModel> searchProduct(SearchProductOrder searchProductOrder,
                                          Pageable pageable,
                                          User user) throws ValuePlusException;

    Page<ProductOrderModel> findProductOrderBySkuId(String productSkuId, Pageable pageable);

    void verifyPaystackCall(String reference) throws ValuePlusException;

    void verifyFlwCall(CallbackResponse callbackResponse) throws ValuePlusException;
}
