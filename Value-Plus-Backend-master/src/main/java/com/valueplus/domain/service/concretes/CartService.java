package com.valueplus.domain.service.concretes;

import com.valueplus.app.exception.CartItemNotExistException;
import com.valueplus.app.exception.ValuePlusException;
import com.valueplus.domain.model.AddToCartDto;
import com.valueplus.domain.model.CartDto;
import com.valueplus.domain.model.CartItemDto;
import com.valueplus.domain.service.abstracts.CartItems;
import com.valueplus.persistence.entity.Cart;
import com.valueplus.persistence.entity.Product;
import com.valueplus.persistence.entity.User;
import com.valueplus.persistence.repository.CartRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@Service
@Transactional
public class CartService implements CartItems {
    @Autowired
    private  CartRepository cartRepository;

    public CartService(){}

    public CartService(CartRepository cartRepository) {
        this.cartRepository = cartRepository;
    }

    public void addToCart(AddToCartDto addToCartDto, Product product, User user) throws ValuePlusException {
        ensureSellingPriceIsValid(addToCartDto.getPrice(),product, addToCartDto.isDiscounted());
        Cart cart = new Cart(product, addToCartDto.getQuantity(), user,addToCartDto.getPrice());
        cartRepository.save(cart);
    }


    public CartDto listCartItems(User user) {
        List<Cart> cartList = cartRepository.findAllByUserOrderByCreatedDateDesc(user);
        List<CartItemDto> cartItems = new ArrayList<>();
        for (Cart cart:cartList){
            CartItemDto cartItemDto = getDtoFromCart(cart);
            cartItems.add(cartItemDto);
        }
        BigDecimal totalCost = BigDecimal.ZERO;
        for (CartItemDto cartItemDto :cartItems){
            BigDecimal itemPrice = cartItemDto.getProduct().getPrice();
            BigDecimal productQuantity = BigDecimal.valueOf(cartItemDto.getQuantity());
            BigDecimal presentCost = itemPrice.multiply(productQuantity);
            totalCost = totalCost.add(presentCost);
        }
        return new CartDto(cartItems,totalCost);
    }


    public static CartItemDto getDtoFromCart(Cart cart) {
        return new CartItemDto(cart);
    }


    public void updateCartItem(AddToCartDto cartDto, User user,Product product){
        Cart cart = cartRepository.getOne(cartDto.getId());
        cart.setQuantity(cartDto.getQuantity());
        cart.setPrice(cartDto.getPrice());
        cartRepository.save(cart);
    }

    public void deleteCartItem(Long id,Long userId) throws CartItemNotExistException {
        if (!cartRepository.existsById(id))
            throw new CartItemNotExistException("Cart id is invalid : " + id);
        cartRepository.deleteById(id);

    }

    public void deleteCartItems(int userId) {
        cartRepository.deleteAll();
    }


    public void deleteUserCartItems(User user) {
        cartRepository.deleteByUser(user);
    }

    private void ensureSellingPriceIsValid(BigDecimal sellingPrice, Product product, Boolean discountStatus) throws ValuePlusException {
       if (discountStatus == true){

               if (product.getDiscountPrice().compareTo(sellingPrice) >0){
                   throw new ValuePlusException("Selling price must not be less than discount price", BAD_REQUEST);
               }
           }
       else
        if (product.getPrice().compareTo(sellingPrice) > 0) {
            throw new ValuePlusException("Selling price must not be less than product price", BAD_REQUEST);
        }
    }
}
