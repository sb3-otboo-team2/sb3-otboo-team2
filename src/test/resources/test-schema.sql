-- H2 데이터베이스용 스키마 (PostgreSQL에서 H2로 변환)

-- ===============================
-- USERS
-- ===============================
DROP TABLE IF EXISTS users CASCADE;

CREATE TABLE users
(
    id                      UUID             NOT NULL,
    created_at              TIMESTAMP        NOT NULL,
    updated_at              TIMESTAMP        NOT NULL,
    email                   VARCHAR(100)     NOT NULL UNIQUE,
    name                    VARCHAR(20)      NOT NULL,
    password                VARCHAR(60)      NOT NULL,
    gender                  VARCHAR(20)      NULL,
    birth_date              DATE             NULL,
    latitude                DOUBLE PRECISION NULL,
    longitude               DOUBLE PRECISION NULL,
    x                       INTEGER          NULL,
    y                       INTEGER          NULL,
    location_names          VARCHAR(255)     NULL,
    temperature_sensitivity INTEGER          NOT NULL,
    profile_image_url       TEXT             NULL,
    locked                  BOOLEAN          NOT NULL,
    role                    VARCHAR(20)      NOT NULL,
    provider_id             VARCHAR(255)     NULL,
    provider                VARCHAR(255)     NULL,
    PRIMARY KEY (id)
);

-- ===============================
-- CLOTHES
-- ===============================
DROP TABLE IF EXISTS clothes CASCADE;

CREATE TABLE clothes
(
    id         UUID         NOT NULL,
    created_at TIMESTAMP    NOT NULL,
    updated_at TIMESTAMP    NOT NULL,
    name       VARCHAR(100) NOT NULL,
    image_url  TEXT         NULL,
    type       VARCHAR(20)  NOT NULL,
    owner_id   UUID         NOT NULL,
    PRIMARY KEY (id),
    FOREIGN KEY (owner_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT chk_clothes_type CHECK (type IN
                                       ('TOP', 'BOTTOM', 'DRESS', 'OUTER', 'UNDERWEAR', 'ACCESSORY',
                                        'SHOES', 'SOCKS', 'HAT', 'BAG', 'SCARF', 'ETC'))
);

-- ===============================
-- CLOTHES ATTRIBUTE DEFINITIONS
-- ===============================
DROP TABLE IF EXISTS clothes_attribute_defs CASCADE;

CREATE TABLE clothes_attribute_defs
(
    id         UUID        NOT NULL,
    created_at TIMESTAMP   NOT NULL,
    updated_at TIMESTAMP   NOT NULL,
    name       VARCHAR(50) NOT NULL UNIQUE,
    PRIMARY KEY (id)
);

-- ===============================
-- ATTRIBUTE OPTIONS
-- ===============================
DROP TABLE IF EXISTS attribute_options CASCADE;

CREATE TABLE attribute_options
(
    id            UUID        NOT NULL,
    created_at    TIMESTAMP   NOT NULL,
    updated_at    TIMESTAMP   NOT NULL,
    value         VARCHAR(50) NOT NULL,
    definition_id UUID        NOT NULL,
    PRIMARY KEY (id),
    FOREIGN KEY (definition_id) REFERENCES clothes_attribute_defs (id) ON DELETE CASCADE,
    UNIQUE (definition_id, value)
);

-- ===============================
-- CLOTHES ATTRIBUTES
-- ===============================
DROP TABLE IF EXISTS clothes_attributes CASCADE;

CREATE TABLE clothes_attributes
(
    id            UUID        NOT NULL,
    created_at    TIMESTAMP   NOT NULL,
    updated_at    TIMESTAMP   NOT NULL,
    option_value  VARCHAR(50) NOT NULL,
    clothes_id    UUID        NOT NULL,
    definition_id UUID        NOT NULL,
    PRIMARY KEY (id),
    FOREIGN KEY (clothes_id) REFERENCES clothes (id) ON DELETE CASCADE,
    FOREIGN KEY (definition_id) REFERENCES clothes_attribute_defs (id) ON DELETE CASCADE
);