package com.UrbanVogue.admin.catalog.controller;

import com.UrbanVogue.admin.catalog.dto.CatalogProductDTO;
import com.UrbanVogue.admin.catalog.service.CatalogService;
import com.UrbanVogue.admin.addProduct.entity.Product;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/catalog")
public class CatalogController {

    @Autowired
    private CatalogService catalogService;

    // Explore section
    @GetMapping("/getProducts")
    public List<CatalogProductDTO> getProducts() {
        return catalogService.getProducts();
    }

    // Product detail
    @GetMapping("/getProducts/{id}")
    public Product getProductById(@PathVariable Long id) {
        return catalogService.getProductById(id);
    }
}