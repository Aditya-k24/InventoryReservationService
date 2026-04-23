package com.example.inventoryreservation.integration;

import com.example.inventoryreservation.AbstractIntegrationTest;
import com.example.inventoryreservation.TestDataHelper;
import com.example.inventoryreservation.dto.*;
import com.example.inventoryreservation.repository.ReservationRepository;
import com.example.inventoryreservation.service.ReservationService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ReservationIntegrationTest extends AbstractIntegrationTest {

    @Autowired TestRestTemplate restTemplate;
    @Autowired TestDataHelper dataHelper;
    @Autowired ReservationService reservationService;
    @Autowired ReservationRepository reservationRepository;
    @Autowired EntityManager entityManager;
    @Autowired TransactionTemplate transactionTemplate;

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
    void createReservation_reservesStockSuccessfully() {
        dataHelper.createInventory("SKU-CHAIR-R", "Red Chair", 10);

        CreateReservationRequest req = new CreateReservationRequest(
            "ORDER-001", 15, List.of(new ReservationItemRequest("SKU-CHAIR-R", 3)));

        ResponseEntity<CreateReservationResponse> response = operatorTemplate.postForEntity(
            "/api/v1/reservations", req, CreateReservationResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo("PENDING");
        assertThat(response.getBody().reservationId()).isNotNull();

        ResponseEntity<InventoryResponse> inv = operatorTemplate.getForEntity(
            "/api/v1/inventory/SKU-CHAIR-R", InventoryResponse.class);
        assertThat(inv.getBody().availableQuantity()).isEqualTo(7);
        assertThat(inv.getBody().reservedQuantity()).isEqualTo(3);
    }

    @Test
    void confirmReservation_deductsStockCorrectly() {
        dataHelper.createInventory("SKU-LAMP-C", "Lamp", 10);

        CreateReservationRequest req = new CreateReservationRequest(
            "ORDER-002", 15, List.of(new ReservationItemRequest("SKU-LAMP-C", 3)));
        ResponseEntity<CreateReservationResponse> created = operatorTemplate.postForEntity(
            "/api/v1/reservations", req, CreateReservationResponse.class);

        UUID id = created.getBody().reservationId();
        ResponseEntity<ConfirmReservationResponse> confirmed = operatorTemplate.postForEntity(
            "/api/v1/reservations/" + id + "/confirm", null, ConfirmReservationResponse.class);

        assertThat(confirmed.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(confirmed.getBody().status()).isEqualTo("CONFIRMED");

        ResponseEntity<InventoryResponse> inv = operatorTemplate.getForEntity(
            "/api/v1/inventory/SKU-LAMP-C", InventoryResponse.class);
        assertThat(inv.getBody().totalQuantity()).isEqualTo(7);
        assertThat(inv.getBody().availableQuantity()).isEqualTo(7);
        assertThat(inv.getBody().reservedQuantity()).isEqualTo(0);
    }

    @Test
    void cancelReservation_releasesStockCorrectly() {
        dataHelper.createInventory("SKU-DESK-CX", "Desk", 10);

        CreateReservationRequest req = new CreateReservationRequest(
            "ORDER-003", 15, List.of(new ReservationItemRequest("SKU-DESK-CX", 4)));
        ResponseEntity<CreateReservationResponse> created = operatorTemplate.postForEntity(
            "/api/v1/reservations", req, CreateReservationResponse.class);

        UUID id = created.getBody().reservationId();
        ResponseEntity<ReservationResponse> cancelled = operatorTemplate.postForEntity(
            "/api/v1/reservations/" + id + "/cancel", null, ReservationResponse.class);

        assertThat(cancelled.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(cancelled.getBody().status()).isEqualTo("CANCELLED");

        ResponseEntity<InventoryResponse> inv = operatorTemplate.getForEntity(
            "/api/v1/inventory/SKU-DESK-CX", InventoryResponse.class);
        assertThat(inv.getBody().totalQuantity()).isEqualTo(10);
        assertThat(inv.getBody().availableQuantity()).isEqualTo(10);
        assertThat(inv.getBody().reservedQuantity()).isEqualTo(0);
    }

    @Test
    void idempotentReservation_returnsSameReservationId() {
        dataHelper.createInventory("SKU-IDEM-1", "Idempotent Item", 10);

        CreateReservationRequest req = new CreateReservationRequest(
            "ORDER-IDEM", 15, List.of(new ReservationItemRequest("SKU-IDEM-1", 1)));

        HttpHeaders headers = new HttpHeaders();
        headers.set("Idempotency-Key", "idem-key-test-001");
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<CreateReservationRequest> entity = new HttpEntity<>(req, headers);

        ResponseEntity<CreateReservationResponse> first = operatorTemplate.exchange(
            "/api/v1/reservations", HttpMethod.POST, entity, CreateReservationResponse.class);
        ResponseEntity<CreateReservationResponse> second = operatorTemplate.exchange(
            "/api/v1/reservations", HttpMethod.POST, entity, CreateReservationResponse.class);

        assertThat(first.getBody().reservationId()).isEqualTo(second.getBody().reservationId());

        ResponseEntity<InventoryResponse> inv = operatorTemplate.getForEntity(
            "/api/v1/inventory/SKU-IDEM-1", InventoryResponse.class);
        assertThat(inv.getBody().reservedQuantity()).isEqualTo(1);
    }

    @Test
    void concurrentReservations_preventsOversell() throws InterruptedException {
        dataHelper.createInventory("SKU-CONCURRENT", "Concurrent Item", 1);

        CreateReservationRequest req = new CreateReservationRequest(
            "ORDER-CONCURRENT", 15, List.of(new ReservationItemRequest("SKU-CONCURRENT", 1)));

        int threads = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CyclicBarrier barrier = new CyclicBarrier(threads);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);

        List<Future<?>> futures = new java.util.ArrayList<>();
        for (int i = 0; i < threads; i++) {
            futures.add(executor.submit(() -> {
                try {
                    barrier.await();
                    ResponseEntity<String> response = operatorTemplate.postForEntity(
                        "/api/v1/reservations", req, String.class);
                    if (response.getStatusCode() == HttpStatus.CREATED) {
                        successCount.incrementAndGet();
                    } else if (response.getStatusCode() == HttpStatus.CONFLICT) {
                        conflictCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    conflictCount.incrementAndGet();
                }
                return null;
            }));
        }

        executor.shutdown();
        executor.awaitTermination(30, TimeUnit.SECONDS);

        assertThat(successCount.get()).isEqualTo(1);
        assertThat(conflictCount.get()).isEqualTo(1);

        ResponseEntity<InventoryResponse> inv = operatorTemplate.getForEntity(
            "/api/v1/inventory/SKU-CONCURRENT", InventoryResponse.class);
        assertThat(inv.getBody().availableQuantity()).isEqualTo(0);
        assertThat(inv.getBody().reservedQuantity()).isEqualTo(1);
    }

    @Test
    void expireReservation_releasesStock() {
        dataHelper.createInventory("SKU-EXPIRE-1", "Expire Item", 5);

        CreateReservationRequest req = new CreateReservationRequest(
            "ORDER-EXPIRE", 15, List.of(new ReservationItemRequest("SKU-EXPIRE-1", 2)));
        ResponseEntity<CreateReservationResponse> created = operatorTemplate.postForEntity(
            "/api/v1/reservations", req, CreateReservationResponse.class);

        UUID id = created.getBody().reservationId();

        // Back-date the expiry so the scheduler treats it as expired
        transactionTemplate.execute(status -> {
            entityManager.createQuery(
                    "UPDATE Reservation r SET r.expiresAt = :past WHERE r.id = :id")
                .setParameter("past", Instant.now().minus(1, ChronoUnit.HOURS))
                .setParameter("id", id)
                .executeUpdate();
            return null;
        });

        reservationService.expireReservations();

        ResponseEntity<InventoryResponse> inv = operatorTemplate.getForEntity(
            "/api/v1/inventory/SKU-EXPIRE-1", InventoryResponse.class);
        assertThat(inv.getBody().availableQuantity()).isEqualTo(5);
        assertThat(inv.getBody().reservedQuantity()).isEqualTo(0);
    }

    @Test
    void insufficientInventory_returns409() {
        dataHelper.createInventory("SKU-LOW-STOCK", "Low Stock Item", 2);

        CreateReservationRequest req = new CreateReservationRequest(
            "ORDER-TOO-BIG", 15, List.of(new ReservationItemRequest("SKU-LOW-STOCK", 5)));
        ResponseEntity<String> response = operatorTemplate.postForEntity(
            "/api/v1/reservations", req, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void getReservation_returnsReservationDetails() {
        dataHelper.createInventory("SKU-GET-RES", "Get Res Item", 10);

        CreateReservationRequest req = new CreateReservationRequest(
            "ORDER-GET", 15, List.of(new ReservationItemRequest("SKU-GET-RES", 1)));
        ResponseEntity<CreateReservationResponse> created = operatorTemplate.postForEntity(
            "/api/v1/reservations", req, CreateReservationResponse.class);

        UUID id = created.getBody().reservationId();
        ResponseEntity<ReservationResponse> response = operatorTemplate.getForEntity(
            "/api/v1/reservations/" + id, ReservationResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().reservationId()).isEqualTo(id);
        assertThat(response.getBody().status()).isEqualTo("PENDING");
        assertThat(response.getBody().items()).hasSize(1);
    }

    @Test
    void invalidRequest_returns400WithProblemDetail() {
        CreateReservationRequest req = new CreateReservationRequest("", 15,
            List.of(new ReservationItemRequest("SKU-ANY", 1)));
        ResponseEntity<String> response = operatorTemplate.postForEntity(
            "/api/v1/reservations", req, String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
}
