CREATE SCHEMA IF NOT EXISTS pt;

CREATE TYPE pt.adjustment_status AS ENUM ('PENDING', 'CAPTURED', 'CANCELLED');

CREATE TABLE pt.adjustment
(
    id              BIGSERIAL                   NOT NULL,
    event_id        BIGINT                      NOT NULL,
    shop_id         CHARACTER VARYING           NOT NULL,
    party_id        CHARACTER VARYING           NOT NULL,
    invoice_id      CHARACTER VARYING           NOT NULL,
    payment_id      CHARACTER VARYING           NOT NULL,
    adjustment_id   CHARACTER VARYING           NOT NULL,
    status          pt.adjustment_status        NOT NULL DEFAULT 'PENDING',
    created_at      TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    captured_at     TIMESTAMP WITHOUT TIME ZONE,
    payout_id       CHARACTER VARYING,
    amount          BIGINT                               DEFAULT 0,
    CONSTRAINT adjustment_pkey PRIMARY KEY (id)
);

CREATE INDEX adjustment_payout_id_idx ON pt.adjustment using btree (payout_id);

CREATE UNIQUE INDEX IF NOT EXISTS adjustment_ukey ON pt.adjustment USING btree (invoice_id, payment_id, adjustment_id);

CREATE TYPE pt.chargeback_category AS ENUM ('fraud', 'dispute', 'authorisation', 'processing_error');

CREATE TYPE pt.chargeback_stage AS ENUM ('chargeback', 'pre_arbitration', 'arbitration');

CREATE TYPE pt.chargeback_status AS ENUM ('PENDING', 'SUCCEEDED', 'REJECTED', 'CANCELLED');

CREATE TABLE pt.chargeback
(
    id                 BIGSERIAL                   NOT NULL,
    event_id           BIGINT                      NOT NULL,
    shop_id            CHARACTER VARYING           NOT NULL,
    party_id           CHARACTER VARYING           NOT NULL,
    invoice_id         CHARACTER VARYING           NOT NULL,
    payment_id         CHARACTER VARYING           NOT NULL,
    chargeback_id      CHARACTER VARYING           NOT NULL,
    payout_id          CHARACTER VARYING,
    status             pt.chargeback_status        NOT NULL DEFAULT 'PENDING',
    created_at         TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    amount             BIGINT                      NOT NULL,
    currency_code      CHARACTER VARYING           NOT NULL,
    levy_amount        BIGINT                      NOT NULL,
    levy_currency_code CHARACTER VARYING           NOT NULL,
    fee                BIGINT                      NOT NULL DEFAULT 0,
    chargeback_stage   pt.chargeback_stage         NOT NULL,
    succeeded_at       TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT chargeback_pkey PRIMARY KEY (id)
);

CREATE INDEX chargeback_payout_id_idx ON pt.chargeback USING btree (payout_id);

CREATE UNIQUE INDEX IF NOT EXISTS chargeback_ukey ON pt.chargeback USING btree (invoice_id, payment_id, chargeback_id);

CREATE TABLE pt.invoice
(
    id             CHARACTER VARYING           NOT NULL,
    party_id       CHARACTER VARYING           NOT NULL,
    shop_id        CHARACTER VARYING           NOT NULL,
    created_at     TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    party_revision BIGINT,
    CONSTRAINT invoice_pkey PRIMARY KEY (id)
);

CREATE TYPE pt.payment_status AS ENUM ('PENDING', 'CAPTURED', 'CANCELLED');

CREATE TABLE pt.payment
(
    id                BIGSERIAL                   NOT NULL,
    event_id          BIGINT                      NOT NULL,
    invoice_id        CHARACTER VARYING           NOT NULL,
    payment_id        CHARACTER VARYING           NOT NULL,
    party_id          CHARACTER VARYING           NOT NULL,
    shop_id           CHARACTER VARYING           NOT NULL,
    provider_id       INTEGER,
    status            pt.payment_status           NOT NULL DEFAULT 'PENDING',
    payout_id         CHARACTER VARYING,
    amount            BIGINT                      NOT NULL,
    provider_fee      BIGINT,
    fee               BIGINT,
    external_fee      BIGINT,
    currency_code     CHARACTER VARYING           NOT NULL,
    captured_at       TIMESTAMP WITHOUT TIME ZONE,
    created_at        TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    guarantee_deposit BIGINT                               DEFAULT 0,
    terminal_id       INTEGER,
    domain_revision   BIGINT,
    party_revision    BIGINT,
    CONSTRAINT payment_pkey PRIMARY KEY (id)
);

CREATE INDEX payment_invoice_id_idx ON pt.payment USING BTREE (invoice_id);

CREATE INDEX payment_payout_id_idx ON pt.payment using btree (payout_id);

CREATE UNIQUE INDEX IF NOT EXISTS payment_ukey ON pt.payment USING btree (invoice_id, payment_id);

CREATE TYPE pt.refund_status AS ENUM ('PENDING', 'SUCCEEDED', 'FAILED');

CREATE TABLE pt.refund
(
    id              BIGSERIAL,
    event_id        BIGINT                      NOT NULL,
    shop_id         CHARACTER VARYING           NOT NULL,
    party_id        CHARACTER VARYING           NOT NULL,
    invoice_id      CHARACTER VARYING           NOT NULL,
    payment_id      CHARACTER VARYING           NOT NULL,
    refund_id       CHARACTER VARYING           NOT NULL,
    status          pt.refund_status            NOT NULL DEFAULT 'PENDING',
    created_at      TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    reason          CHARACTER VARYING,
    amount          BIGINT                      NOT NULL,
    fee             BIGINT                      NOT NULL,
    payout_id       CHARACTER VARYING,
    succeeded_at    TIMESTAMP WITHOUT TIME ZONE,
    domain_revision BIGINT,
    currency_code   CHARACTER VARYING           NOT NULL,
    CONSTRAINT refund_pkey PRIMARY KEY (id)
);

CREATE INDEX refund_payout_id_idx ON pt.refund using btree (payout_id);

CREATE UNIQUE INDEX IF NOT EXISTS refund_ukey ON pt.refund USING btree (invoice_id, payment_id, refund_id);

CREATE TABLE pt.shop_meta
(
    party_id                             CHARACTER VARYING           NOT NULL,
    shop_id                              CHARACTER VARYING           NOT NULL,
    wtime                                TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    last_payout_created_at               TIMESTAMP WITHOUT TIME ZONE,
    calendar_id                          INTEGER,
    scheduler_id                         INTEGER,
    has_payment_institution_acc_pay_tool BOOLEAN                     NOT NULL,
    CONSTRAINT shop_meta_pkey PRIMARY KEY (party_id, shop_id)
);