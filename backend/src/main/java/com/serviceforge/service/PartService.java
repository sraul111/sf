package com.serviceforge.service;

import com.serviceforge.data.MockDataStore;
import com.serviceforge.model.Part;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

@Service
public class PartService {

    private final MockDataStore dataStore;

    public PartService(MockDataStore dataStore) {
        this.dataStore = dataStore;
    }

    public List<Part> getAllParts() { return dataStore.getAllParts(); }

    public Optional<Part> findPartBySku(String sku) {
        return dataStore.findPartBySku(sku);
    }

    public synchronized Part reserve(String sku, int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be a positive integer");
        }
        Part part = dataStore.findPartBySku(sku)
                .orElseThrow(() -> new NoSuchElementException("Unknown SKU: " + sku));
        if (part.getTotalQuantity() < quantity) {
            throw new IllegalStateException("Insufficient stock for SKU: " + sku + " (requested " + quantity + ", available " + part.getTotalQuantity() + ")");
        }
        part.setTotalQuantity(part.getTotalQuantity() - quantity);
        return part;
    }

    public synchronized Part release(String sku, int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be a positive integer");
        }
        Part part = dataStore.findPartBySku(sku)
                .orElseThrow(() -> new NoSuchElementException("Unknown SKU: " + sku));
        part.setTotalQuantity(part.getTotalQuantity() + quantity);
        return part;
    }
}
