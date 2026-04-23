package com.example.inventoryreservation.integration;

import com.example.inventoryreservation.AbstractIntegrationTest;
import com.example.inventoryreservation.TestDataHelper;
import com.example.inventoryreservation.domain.AppUser;
import com.example.inventoryreservation.dto.AdjustInventoryRequest;
import com.example.inventoryreservation.dto.InventoryResponse;
import com.example.inventoryreservation.repository.InventoryItemRepository;
import com.example.inventoryreservation.repository.SkuRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.annotation.DirtiesContext;

import static org.assertj.core.api.Assertions.assertThat;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class InventoryIntegrationTest extends AbstractIntegrationTest {

    @Autowired TestRestTemplate restTemplate;
    @Autowired TestDataHelper dataHelper;
    @Autowired SkuRepository skuRepository;
    @Autowired InventoryItemRepository inventoryItemRepository;

    private TestRestTemplate adminTemplate;
    private TestRestTemplate operatorTemplate;

    @BeforeEach
    void setUp() {
        dataHelper.createAdmin();
        dataHelper.createOperator();
        adminTemplate = restTemplate.withBasicAuth("admin@example.com", "changeme");
        operatorTemplate = restTemplate.withBasicAuth("ops@example.com", "changeme");
    }

    @Test
    void adjustInventory_createsStockForNewSku() {
        dataHelper.createInventory("SKU-TEST-ADJ", "Test Adjust", 0);

        AdjustInventoryRequest req = new AdjustInventoryRequest("SKU-TEST-ADJ", 10, "initial stock");
        ResponseEntity<InventoryResponse> response = adminTemplate.postForEntity(
            "/api/v1/inventory/adjustments", req, InventoryResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().totalQuantity()).isEqualTo(10);
        assertThat(response.getBody().availableQuantity()).isEqualTo(10);
    }

    @Test
    void getInventory_returnsCurrentStock() {
        dataHelper.createInventory("SKU-GET-TEST", "Get Test", 25);

        ResponseEntity<InventoryResponse> response = operatorTemplate.getForEntity(
            "/api/v1/inventory/SKU-GET-TEST", InventoryResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().skuCode()).isEqualTo("SKU-GET-TEST");
        assertThat(response.getBody().totalQuantity()).isEqualTo(25);
    }

    @Test
    void adjustInventory_forbiddenForOperator() {
        AdjustInventoryRequest req = new AdjustInventoryRequest("SKU-FORBIDDEN", 10, "test");
        ResponseEntity<String> response = operatorTemplate.postForEntity(
            "/api/v1/inventory/adjustments", req, String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void unauthenticatedRequest_returns401() {
        ResponseEntity<String> response = restTemplate.getForEntity(
            "/api/v1/inventory/ANY-SKU", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void actuatorHealth_isPublic() {
        ResponseEntity<String> response = restTemplate.getForEntity(
            "/actuator/health", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
