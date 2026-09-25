package com.nexus.catalog.application.port.out;

import java.util.List;

public interface ProductImagePort {
    void saveAll(String productId, List<String> urls);
    List<String> findUrlsByProductId(String productId);
    void deleteByProductId(String productId);
}
