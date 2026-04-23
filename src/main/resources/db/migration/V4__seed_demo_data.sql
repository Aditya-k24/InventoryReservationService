INSERT INTO sku (id, sku_code, name, active)
VALUES
    (gen_random_uuid(), 'SKU-RED-CHAIR',  'Red Chair',   true),
    (gen_random_uuid(), 'SKU-LAMP-BLACK', 'Black Lamp',  true),
    (gen_random_uuid(), 'SKU-DESK-OAK',   'Oak Desk',    true)
ON CONFLICT (sku_code) DO NOTHING;

INSERT INTO inventory_item (id, sku_id, total_qty, available_qty, reserved_qty)
SELECT gen_random_uuid(), s.id, 25, 25, 0
FROM sku s
WHERE s.sku_code IN ('SKU-RED-CHAIR', 'SKU-LAMP-BLACK', 'SKU-DESK-OAK')
ON CONFLICT (sku_id) DO NOTHING;
