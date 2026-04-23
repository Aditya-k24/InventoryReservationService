CREATE TABLE inventory_event (
    id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    sku_id          uuid        NOT NULL REFERENCES sku(id),
    reservation_id  uuid        REFERENCES reservation(id),
    event_type      varchar(32) NOT NULL,
    delta_total     integer     NOT NULL DEFAULT 0,
    delta_available integer     NOT NULL DEFAULT 0,
    delta_reserved  integer     NOT NULL DEFAULT 0,
    metadata        text,
    created_at      timestamptz NOT NULL DEFAULT now()
);
