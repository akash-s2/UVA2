package com.UrbanVogue.user.ProductModule.controller;

import com.UrbanVogue.user.ProductModule.dto.GetProductDTO;
import com.UrbanVogue.user.ProductModule.service.GetProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/user/getProducts")
public class GetProductController {

    @Autowired
    private GetProductService getProductService;

    // Explore section
    @GetMapping
    public List<GetProductDTO> getProducts() {
        return getProductService.getProducts();
    }

    // Product detail
    @GetMapping("/{id}")
    public Object getProductById(@PathVariable Long id) {
        return getProductService.getProductById(id);
    }
}