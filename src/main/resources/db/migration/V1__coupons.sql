-- Schemat tabeli coupons.
--
-- Kod kuponu jest zapisywany w formie znormalizowanej (lower case) wymuszanej
-- na "value" w CouponCode w warstwie domeny, więc zwykły UNIQUE na kolumnie
-- daje unikalność case-insensitive bez potrzeby indeksu funkcyjnego lower(code).
--
-- "created_at" jest TIMESTAMP WITH TIME ZONE — Postgres trzyma to wewnętrznie jako
-- UTC, co odpowiada modelowi Instant w domenie.
--
-- Ograniczenia CHECK powielają niezmienniki agregatu na poziomie bazy (nawet bezpośredni INSERT
-- z konsoli SQL nie wprowadzi rekordu w nielegalny stan)

CREATE TABLE coupons (
    id              UUID         PRIMARY KEY,
    code            VARCHAR(64)  NOT NULL UNIQUE,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    max_usages      INTEGER      NOT NULL,
    current_usages  INTEGER      NOT NULL DEFAULT 0,
    country         VARCHAR(2)   NOT NULL,
    CONSTRAINT chk_coupons_max_usages_positive    CHECK (max_usages > 0),
    CONSTRAINT chk_coupons_current_usages_nonneg  CHECK (current_usages >= 0),
    CONSTRAINT chk_coupons_current_within_max     CHECK (current_usages <= max_usages)
);
