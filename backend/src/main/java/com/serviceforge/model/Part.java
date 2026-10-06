package com.serviceforge.model;

public class Part {

    private final Long id;
    private String sku;
    private String name;
    private int totalQuantity;

    public Part(Long id, String sku, String name) {
        this(id, sku, name, 0);
    }

    public Part(Long id, String sku, String name, int totalQuantity) {
        this.id = id;
        this.sku = sku;
        this.name = name;
        this.totalQuantity = totalQuantity;
    }

    public Long getId() { return id; }
    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getTotalQuantity() { return totalQuantity; }
    public void setTotalQuantity(int totalQuantity) { this.totalQuantity = totalQuantity; }
}
