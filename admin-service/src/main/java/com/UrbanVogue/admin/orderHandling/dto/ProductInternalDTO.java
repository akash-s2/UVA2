package com.UrbanVogue.admin.orderHandling.dto;

public class ProductInternalDTO {
    private Double price;
    private  Integer numberOfPieces;
    private  String name;

    public ProductInternalDTO(Double price, Integer qty,String name) {
        this.price = price;
        this.numberOfPieces = qty;
        this.name=name;
    }

    // Getters & Setters
    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }

    public Integer  getNumberOfPieces() { return numberOfPieces; }
    public void setNumberOfPieces(Integer qty) { this.numberOfPieces = qty; }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}