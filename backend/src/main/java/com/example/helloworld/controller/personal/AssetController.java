package com.example.helloworld.controller.personal;

import com.example.helloworld.dto.common.ApiResponse;
import com.example.helloworld.entity.personal.Asset;
import com.example.helloworld.service.personal.AssetService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/personal/assets")
@CrossOrigin(origins = "*")
public class AssetController {

    @Autowired
    private AssetService assetService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Asset>>> getAllAssets() {
        List<Asset> assets = assetService.getAllAssets();
        return ResponseEntity.ok(ApiResponse.ok(assets));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Asset>> getAssetById(@PathVariable Long id) {
        return assetService.getAssetById(id)
                .map(a -> ResponseEntity.ok(ApiResponse.ok(a)))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.fail("資產不存在")));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Asset>> createAsset(@RequestBody Asset asset) {
        Asset saved = assetService.saveAsset(asset);
        return ResponseEntity.ok(ApiResponse.ok(saved));
    }

    @Autowired
    private ObjectMapper objectMapper;

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Asset>> updateAsset(@PathVariable Long id, @RequestBody JsonNode jsonNode) {
        return assetService.getAssetById(id)
                .map(existing -> {
                    if (jsonNode.has("stockCode")) {
                        existing.setStockCode(jsonNode.get("stockCode").asText(null));
                    }
                    if (jsonNode.has("assetType")) {
                        existing.setAssetType(jsonNode.get("assetType").asText(null));
                    }
                    if (jsonNode.has("name")) {
                        existing.setName(jsonNode.get("name").asText(null));
                    }
                    if (jsonNode.has("currency")) {
                        existing.setCurrency(jsonNode.get("currency").asText(null));
                    }
                    if (jsonNode.has("purchaseDate")) {
                        JsonNode purchaseDateNode = jsonNode.get("purchaseDate");
                        if (purchaseDateNode.isNull() || purchaseDateNode.asText().isEmpty()) {
                            existing.setPurchaseDate(null);
                        } else {
                            existing.setPurchaseDate(java.time.LocalDate.parse(purchaseDateNode.asText()));
                        }
                    }
                    if (jsonNode.has("quantity")) {
                        JsonNode quantityNode = jsonNode.get("quantity");
                        if (!quantityNode.isNull()) {
                            existing.setQuantity(new BigDecimal(quantityNode.asText()));
                        }
                    }
                    if (jsonNode.has("cost")) {
                        JsonNode costNode = jsonNode.get("cost");
                        if (!costNode.isNull()) {
                            existing.setCost(new BigDecimal(costNode.asText()));
                        }
                    }
                    if (jsonNode.has("unitPrice")) {
                        JsonNode unitPriceNode = jsonNode.get("unitPrice");
                        if (!unitPriceNode.isNull()) {
                            existing.setUnitPrice(new BigDecimal(unitPriceNode.asText()));
                        }
                    }
                    if (jsonNode.has("currentPrice")) {
                        JsonNode currentPriceNode = jsonNode.get("currentPrice");
                        if (!currentPriceNode.isNull()) {
                            existing.setCurrentPrice(new BigDecimal(currentPriceNode.asText()));
                        }
                    }
                    if (jsonNode.has("member")) {
                        existing.setMember(jsonNode.get("member").asText(null));
                    }
                    if (jsonNode.has("category")) {
                        existing.setCategory(jsonNode.get("category").asText(null));
                    }
                    if (jsonNode.has("orderIndex")) {
                        JsonNode orderIndexNode = jsonNode.get("orderIndex");
                        if (!orderIndexNode.isNull()) {
                            existing.setOrderIndex(orderIndexNode.asInt());
                        }
                    }
                    if (jsonNode.has("updatedByUid")) {
                        existing.setUpdatedByUid(jsonNode.get("updatedByUid").asText(null));
                    }
                    return ResponseEntity.ok(ApiResponse.ok(assetService.saveAsset(existing)));
                })
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.fail("資產不存在")));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteAsset(@PathVariable Long id) {
        assetService.deleteAsset(id);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
