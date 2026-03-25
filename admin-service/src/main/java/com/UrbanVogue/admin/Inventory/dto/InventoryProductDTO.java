package com.UrbanVogue.admin.Inventory.dto;

public class InventoryProductDTO {
    private Long id;
    private String name;
    private Long numberOfPieces;

    // getters & setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Long getNumberOfPieces() { return numberOfPieces; }
    public void setNumberOfPieces(Long numberOfPieces) { this.numberOfPieces = numberOfPieces; }
}