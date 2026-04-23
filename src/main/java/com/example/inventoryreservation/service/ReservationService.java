package com.example.inventoryreservation.service;

import com.example.inventoryreservation.domain.*;
import com.example.inventoryreservation.dto.*;
import com.example.inventoryreservation.exception.InsufficientInventoryException;
import com.example.inventoryreservation.exception.InvalidReservationStateException;
import com.example.inventoryreservation.exception.ResourceNotFoundException;
import com.example.inventoryreservation.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final InventoryEventRepository inventoryEventRepository;
    private final AppUserRepository appUserRepository;

    public ReservationService(ReservationRepository reservationRepository,
                              InventoryItemRepository inventoryItemRepository,
                              InventoryEventRepository inventoryEventRepository,
                              AppUserRepository appUserRepository) {
        this.reservationRepository = reservationRepository;
        this.inventoryItemRepository = inventoryItemRepository;
        this.inventoryEventRepository = inventoryEventRepository;
        this.appUserRepository = appUserRepository;
    }

    @Transactional
    public CreateReservationResponse createReservation(String userEmail,
                                                       String idempotencyKey,
                                                       CreateReservationRequest request) {
        AppUser user = appUserRepository.findByEmail(userEmail)
            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userEmail));

        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            Optional<Reservation> existing = reservationRepository
                .findByCreatedByUserAndIdempotencyKey(user, idempotencyKey);
            if (existing.isPresent()) {
                return toCreateResponse(existing.get());
            }
        }

        List<String> skuCodes = request.items().stream()
            .map(ReservationItemRequest::skuCode)
            .sorted()
            .distinct()
            .toList();

        List<InventoryItem> lockedItems = inventoryItemRepository.findBySkuCodesWithLock(skuCodes);

        if (lockedItems.size() != skuCodes.size()) {
            Set<String> found = lockedItems.stream()
                .map(i -> i.getSku().getSkuCode())
                .collect(Collectors.toSet());
            String missing = skuCodes.stream().filter(c -> !found.contains(c)).findFirst().orElse("unknown");
            throw new ResourceNotFoundException("Inventory not found for SKU: " + missing);
        }

        Map<String, InventoryItem> itemByCode = lockedItems.stream()
            .collect(Collectors.toMap(i -> i.getSku().getSkuCode(), i -> i));

        for (ReservationItemRequest ri : request.items()) {
            InventoryItem inv = itemByCode.get(ri.skuCode());
            if (inv.getAvailableQty() < ri.quantity()) {
                throw new InsufficientInventoryException(ri.skuCode(), ri.quantity(), inv.getAvailableQty());
            }
        }

        Instant expiresAt = Instant.now().plus(request.ttlMinutes(), ChronoUnit.MINUTES);
        Reservation reservation = new Reservation(user, request.customerReference(), idempotencyKey, expiresAt);
        reservationRepository.save(reservation);

        for (ReservationItemRequest ri : request.items()) {
            InventoryItem inv = itemByCode.get(ri.skuCode());
            inv.reserve(ri.quantity());
            reservation.addItem(new ReservationItem(reservation, inv.getSku(), ri.quantity()));
            inventoryEventRepository.save(new InventoryEvent(
                inv.getSku(), reservation, "RESERVE",
                0, -ri.quantity(), ri.quantity(), null
            ));
        }

        return toCreateResponse(reservation);
    }

    @Transactional(readOnly = true)
    public ReservationResponse getReservation(UUID id) {
        Reservation r = reservationRepository.findByIdWithItems(id)
            .orElseThrow(() -> new ResourceNotFoundException("Reservation not found: " + id));
        return toGetResponse(r);
    }

    @Transactional
    public ConfirmReservationResponse confirmReservation(UUID id) {
        Reservation reservation = reservationRepository.findByIdWithItems(id)
            .orElseThrow(() -> new ResourceNotFoundException("Reservation not found: " + id));

        if (reservation.getStatus() != ReservationStatus.PENDING) {
            throw new InvalidReservationStateException(
                "Cannot confirm reservation in status: " + reservation.getStatus());
        }

        Instant now = Instant.now();
        if (now.isAfter(reservation.getExpiresAt())) {
            throw new InvalidReservationStateException("Cannot confirm an expired reservation");
        }

        List<String> skuCodes = reservation.getItems().stream()
            .map(ri -> ri.getSku().getSkuCode())
            .sorted()
            .toList();

        List<InventoryItem> lockedItems = inventoryItemRepository.findBySkuCodesWithLock(skuCodes);
        Map<String, InventoryItem> itemByCode = lockedItems.stream()
            .collect(Collectors.toMap(i -> i.getSku().getSkuCode(), i -> i));

        List<InventoryEffect> effects = new ArrayList<>();
        for (ReservationItem ri : reservation.getItems()) {
            InventoryItem inv = itemByCode.get(ri.getSku().getSkuCode());
            inv.confirm(ri.getQuantity());
            effects.add(new InventoryEffect(ri.getSku().getSkuCode(), -ri.getQuantity(), 0, -ri.getQuantity()));
            inventoryEventRepository.save(new InventoryEvent(
                ri.getSku(), reservation, "CONFIRM",
                -ri.getQuantity(), 0, -ri.getQuantity(), null
            ));
        }

        reservation.confirm(now);
        return new ConfirmReservationResponse(reservation.getId(), reservation.getStatus().name(), now, effects);
    }

    @Transactional
    public ReservationResponse cancelReservation(UUID id) {
        Reservation reservation = reservationRepository.findByIdWithItems(id)
            .orElseThrow(() -> new ResourceNotFoundException("Reservation not found: " + id));

        if (reservation.getStatus() != ReservationStatus.PENDING) {
            throw new InvalidReservationStateException(
                "Cannot cancel reservation in status: " + reservation.getStatus());
        }

        List<String> skuCodes = reservation.getItems().stream()
            .map(ri -> ri.getSku().getSkuCode())
            .sorted()
            .toList();

        List<InventoryItem> lockedItems = inventoryItemRepository.findBySkuCodesWithLock(skuCodes);
        Map<String, InventoryItem> itemByCode = lockedItems.stream()
            .collect(Collectors.toMap(i -> i.getSku().getSkuCode(), i -> i));

        for (ReservationItem ri : reservation.getItems()) {
            InventoryItem inv = itemByCode.get(ri.getSku().getSkuCode());
            inv.release(ri.getQuantity());
            inventoryEventRepository.save(new InventoryEvent(
                ri.getSku(), reservation, "CANCEL",
                0, ri.getQuantity(), -ri.getQuantity(), null
            ));
        }

        reservation.cancel(Instant.now());
        return toGetResponse(reservation);
    }

    @Transactional
    public void expireReservations() {
        List<Reservation> expired = reservationRepository.findPendingExpired(Instant.now());
        for (Reservation reservation : expired) {
            Reservation withItems = reservationRepository.findByIdWithItems(reservation.getId())
                .orElse(reservation);

            List<String> skuCodes = withItems.getItems().stream()
                .map(ri -> ri.getSku().getSkuCode())
                .sorted()
                .toList();

            if (!skuCodes.isEmpty()) {
                List<InventoryItem> lockedItems = inventoryItemRepository.findBySkuCodesWithLock(skuCodes);
                Map<String, InventoryItem> itemByCode = lockedItems.stream()
                    .collect(Collectors.toMap(i -> i.getSku().getSkuCode(), i -> i));

                for (ReservationItem ri : withItems.getItems()) {
                    InventoryItem inv = itemByCode.get(ri.getSku().getSkuCode());
                    if (inv != null) {
                        inv.release(ri.getQuantity());
                        inventoryEventRepository.save(new InventoryEvent(
                            ri.getSku(), withItems, "EXPIRE",
                            0, ri.getQuantity(), -ri.getQuantity(), null
                        ));
                    }
                }
            }
            withItems.expire();
        }
    }

    private CreateReservationResponse toCreateResponse(Reservation r) {
        List<ReservationItemResponse> items = r.getItems().stream()
            .map(ri -> new ReservationItemResponse(ri.getSku().getSkuCode(), ri.getQuantity()))
            .toList();
        String base = "/api/v1/reservations/" + r.getId();
        Map<String, String> links = new LinkedHashMap<>();
        links.put("self", base);
        if (r.getStatus() == ReservationStatus.PENDING) {
            links.put("confirm", base + "/confirm");
            links.put("cancel", base + "/cancel");
        }
        return new CreateReservationResponse(r.getId(), r.getStatus().name(),
            r.getCustomerReference(), r.getExpiresAt(), items, links);
    }

    private ReservationResponse toGetResponse(Reservation r) {
        List<ReservationItemResponse> items = r.getItems().stream()
            .map(ri -> new ReservationItemResponse(ri.getSku().getSkuCode(), ri.getQuantity()))
            .toList();
        String base = "/api/v1/reservations/" + r.getId();
        Map<String, String> links = new LinkedHashMap<>();
        links.put("self", base);
        if (r.getStatus() == ReservationStatus.PENDING) {
            links.put("confirm", base + "/confirm");
            links.put("cancel", base + "/cancel");
        }
        return new ReservationResponse(r.getId(), r.getStatus().name(),
            r.getCustomerReference(), r.getExpiresAt(),
            r.getConfirmedAt(), r.getCancelledAt(), items, links);
    }
}
