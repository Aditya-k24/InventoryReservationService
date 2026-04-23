CREATE INDEX idx_reservation_pending_expiry
    ON reservation (expires_at)
    WHERE status = 'PENDING';
