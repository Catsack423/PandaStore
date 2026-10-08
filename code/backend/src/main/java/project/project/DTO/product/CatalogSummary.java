package project.project.DTO.product;

import java.math.BigDecimal;
import java.util.Map;

public record CatalogSummary(BigDecimal maxPrice, Map<Long, Long> categoryCounts) {
}
