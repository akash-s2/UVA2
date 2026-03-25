

package com.UrbanVogue.user.OrderModule.client;

import com.UrbanVogue.user.OrderModule.dto.ProductResponseDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class ProductClient {

    @Value("${internal.api.key:uva-internal-secret-2026}")
    private String internalApiKey;

    @Value("${service.admin.url}")
    private String adminServiceUrl;

    @Autowired
    private RestTemplate restTemplate;

    //  COMMON METHODE for extracting the tokens
    private String getToken() {
        Object credentials = SecurityContextHolder.getContext()
                .getAuthentication()
                .getCredentials();
        System.out.println(" FORWARDING TOKEN: " + credentials);
        if (credentials != null) {
            return credentials.toString();
        }
        return null;
    }

    //  COMMON HEADER BUILDERs
    private HttpHeaders getHeaders() {
        HttpHeaders headers = new HttpHeaders();

        String token = getToken();

        if (token != null) {
            headers.set("Authorization", "Bearer " + token);
        }

        headers.set("X-Internal-Api-Key", internalApiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    //  FETCHing the  PRODUCT
    public ProductResponseDTO getProduct(Long productId, Integer quantity) {

        String url = adminServiceUrl + "/internal/products/" + productId + "?qty=" + quantity;

        HttpEntity<Void> entity = new HttpEntity<>(getHeaders());

        ResponseEntity<ProductResponseDTO> response =
                restTemplate.exchange(
                        url,
                        HttpMethod.GET,
                        entity,
                        ProductResponseDTO.class
                );

        return response.getBody();
    }

    //Reduce stock

    public void reduceStock(Long productId, Integer quantity) {

        String url = adminServiceUrl + "/internal/products/reduce/"
                + productId + "?qty=" + quantity;

        HttpEntity<Void> entity = new HttpEntity<>(getHeaders());

        restTemplate.exchange(
                url,
                HttpMethod.PUT,
                entity,
                Void.class
        );
    }
}
