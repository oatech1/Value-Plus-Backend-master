package com.valueplus.domain.model;


import com.valueplus.persistence.entity.Cart;
import com.valueplus.persistence.entity.Product;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

public class CartItemDto {
    private Long id;
    private @NotNull Integer quantity;
    private @NotNull Product product;
    private @NotNull BigDecimal price;
    public CartItemDto() {
    }

    public CartItemDto(Cart cart) {
        this.setId(cart.getId());
        this.setQuantity(cart.getQuantity());
        this.setProduct(cart.getProduct());
        this.setPrice(cart.getPrice());
    }

    @Override
    public String toString() {
        return "CartItemDto{" +
                "id=" + id +
                ", quantity=" + quantity +
                ", product=" + product.getName() +
                ", price=" + price +
                '}';
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public BigDecimal getPrice() {return price;}

    public void setPrice(BigDecimal price) {this.price = price;}
}