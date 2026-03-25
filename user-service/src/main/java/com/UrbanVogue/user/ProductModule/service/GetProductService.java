package com.UrbanVogue.user.ProductModule.service;

import com.UrbanVogue.user.ProductModule.dto.GetProductDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Service
public class GetProductService {

    @Autowired
    private RestTemplate restTemplate;

    private final String ADMIN_URL = "http://localhost:8092/catalog";

    // Explore products
    public List<GetProductDTO> getProducts() {

        ResponseEntity<List<GetProductDTO>> response =
                restTemplate.exchange(
                        ADMIN_URL + "/getProducts",
                        org.springframework.http.HttpMethod.GET,
                        null,
                        new ParameterizedTypeReference<List<GetProductDTO>>() {}
                );

        return response.getBody();
    }

    // Product detail
    public Object getProductById(Long id) {
        return restTemplate.getForObject(
                ADMIN_URL + "/getProducts/" + id,
                Object.class
        );
    }
}