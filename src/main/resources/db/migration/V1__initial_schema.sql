CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE app_user (
    id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    email           varchar(255) NOT NULL UNIQUE,
    password_hash   varchar(255) NOT NULL,
    role            varchar(32)  NOT NULL,
    created_at      timestamptz  NOT NULL DEFAULT now()
);

CREATE TABLE sku (
    id          uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    sku_code    varchar(64)  NOT NULL UNIQUE,
    name        varchar(255) NOT NULL,
    active      boolean      NOT NULL DEFAULT true,
    created_at  timestamptz  NOT NULL DEFAULT now()
);

CREATE TABLE inventory_item (
    id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    sku_id          uuid    NOT NULL UNIQUE REFERENCES sku(id),
    total_qty       integer NOT NULL DEFAULT 0,
    available_qty   integer NOT NULL DEFAULT 0,
    reserved_qty    integer NOT NULL DEFAULT 0,
    updated_at      timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT chk_inventory_non_negative
        CHECK (total_qty >= 0 AND available_qty >= 0 AND reserved_qty >= 0),
    CONSTRAINT chk_inventory_balance
        CHECK (total_qty = available_qty + reserved_qty)
);

CREATE TABLE reservation (
    id                  uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    created_by_user_id  uuid         NOT NULL REFERENCES app_user(id),
    customer_reference  varchar(128) NOT NULL,
    status              varchar(32)  NOT NULL,
    idempotency_key     varchar(128),
    expires_at          timestamptz  NOT NULL,
    confirmed_at        timestamptz,
    cancelled_at        timestamptz,
    created_at          timestamptz  NOT NULL DEFAULT now(),
    CONSTRAINT uk_reservation_idempotency
        UNIQUE (created_by_user_id, idempotency_key)
);

CREATE TABLE reservation_item (
    id              uuid    PRIMARY KEY DEFAULT gen_random_uuid(),
    reservation_id  uuid    NOT NULL REFERENCES reservation(id),
    sku_id          uuid    NOT NULL REFERENCES sku(id),
    quantity        integer NOT NULL,
    CONSTRAINT chk_reservation_item_qty_positive CHECK (quantity > 0)
);
