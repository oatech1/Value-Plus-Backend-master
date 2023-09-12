package com.valueplus.app.controller;

import com.valueplus.app.exception.ApiResponse;
import com.valueplus.app.exception.CartItemNotExistException;
import com.valueplus.app.exception.ProductNotExistException;
import com.valueplus.app.exception.ValuePlusException;
import com.valueplus.domain.model.AddToCartDto;
import com.valueplus.domain.model.CartDto;
import com.valueplus.domain.service.concretes.CartService;
import com.valueplus.domain.service.concretes.DefaultProductService;
import com.valueplus.domain.util.UserUtils;
import com.valueplus.persistence.entity.Product;
import com.valueplus.persistence.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/cart")
public class CartController {
    @Autowired
    private CartService cartService;

    @Autowired
    private DefaultProductService productService;


    @PostMapping("/add")
    public ResponseEntity<ApiResponse> addToCart(@RequestBody AddToCartDto addToCartDto) throws ProductNotExistException, ValuePlusException {
        Product product = productService.getProduct(addToCartDto.getProductId());
        User user = UserUtils.getLoggedInUser();
        cartService.addToCart(addToCartDto, product, user);
        return new ResponseEntity<>(new ApiResponse(true, "Added to cart"), HttpStatus.CREATED);

    }

    @GetMapping("/")
    public ResponseEntity<CartDto> getCartItems()  {
        User user = UserUtils.getLoggedInUser();
        CartDto cartDto = cartService.listCartItems(user);
        return new ResponseEntity<>(cartDto, HttpStatus.OK);
    }

    @PutMapping("/update/{cartItemId}")
    public ResponseEntity<ApiResponse> updateCartItem(@RequestBody @Valid AddToCartDto cartDto) throws ProductNotExistException, ValuePlusException {
        User user = UserUtils.getLoggedInUser();
        Product product = productService.getProduct(cartDto.getProductId());
        cartService.updateCartItem(cartDto, user, product);
        return new ResponseEntity<>(new ApiResponse(true, "Product has been updated"), HttpStatus.OK);
    }

    @DeleteMapping("/delete/{cartItemId}")
    public ResponseEntity<ApiResponse> deleteCartItem(@PathVariable("cartItemId") Long itemID) throws CartItemNotExistException {
        User user = UserUtils.getLoggedInUser();
        cartService.deleteCartItem(itemID, user.getId());
        return new ResponseEntity<>(new ApiResponse(true, "Item has been removed"), HttpStatus.OK);
    }


    @DeleteMapping("/clear-cart")
    public ResponseEntity<ApiResponse>clearCart()throws CartItemNotExistException{
        User user = UserUtils.getLoggedInUser();
        cartService.deleteUserCartItems(user);
        return new ResponseEntity<>(new ApiResponse(true,"Cart has been cleared"),HttpStatus.OK);
    }
}
