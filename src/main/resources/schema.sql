-- ===============================
-- USERS
-- ===============================
DROP TABLE IF EXISTS "users" CASCADE;

CREATE TABLE "users"
(
    "id"                      UUID             NOT NULL,
    "created_at"              TIMESTAMPTZ      NOT NULL,
    "updated_at"              TIMESTAMPTZ      NOT NULL,
    "email"                   VARCHAR(100)     NOT NULL UNIQUE,
    "name"                    VARCHAR(20)      NOT NULL,
    "password"                VARCHAR(60)      NOT NULL,
    "gender"                  VARCHAR(20)      NULL,
    "birth_date"              DATE             NULL,
    "latitude"                DOUBLE PRECISION NULL,
    "longitude"               DOUBLE PRECISION NULL,
    "x"                       INTEGER          NULL,
    "y"                       INTEGER          NULL,
    "location_names"          VARCHAR(255)     NULL,
    "temperature_sensitivity" INTEGER          NOT NULL,
    "profile_image_url"       TEXT             NULL,
    "locked"                  BOOLEAN          NOT NULL,
    "role"                    VARCHAR(20)      NOT NULL,
    CONSTRAINT "PK_USERS" PRIMARY KEY ("id")
);

-- ===============================
-- FEEDS
-- ===============================
DROP TABLE IF EXISTS "feeds" CASCADE;

CREATE TABLE "feeds"
(
    "id"            UUID         NOT NULL,
    "weather_id"    UUID         NOT NULL,
    "author_id"     UUID         NOT NULL,
    "content"       VARCHAR(100) NOT NULL,
    "created_at"    TIMESTAMPTZ  NOT NULL,
    "updated_at"    TIMESTAMPTZ  NOT NULL,
    "comment_count" INTEGER      NOT NULL DEFAULT 0,
    "like_count"    BIGINT       NOT NULL DEFAULT 0,
    "liked_by_me"   BOOLEAN      NOT NULL DEFAULT FALSE,
    CONSTRAINT "PK_FEEDS" PRIMARY KEY ("id"),
    CONSTRAINT "FK_FEEDS_USERS" FOREIGN KEY ("author_id") REFERENCES "users" ("id") ON DELETE CASCADE
);

-- ===============================
-- COMMENTS
-- ===============================
DROP TABLE IF EXISTS "comments" CASCADE;

CREATE TABLE "comments"
(
    "id"         UUID         NOT NULL,
    "feed_id"    UUID         NOT NULL,
    "author_id"  UUID         NOT NULL,
    "content"    VARCHAR(100) NOT NULL,
    "created_at" TIMESTAMPTZ  NOT NULL,
    "updated_at" TIMESTAMPTZ  NOT NULL,
    CONSTRAINT "PK_COMMENTS" PRIMARY KEY ("id"),
    CONSTRAINT "FK_COMMENTS_FEEDS" FOREIGN KEY ("feed_id") REFERENCES "feeds" ("id") ON DELETE CASCADE,
    CONSTRAINT "FK_COMMENTS_USERS" FOREIGN KEY ("author_id") REFERENCES "users" ("id") ON DELETE CASCADE
);

-- ===============================
-- NOTIFICATIONS
-- ===============================
DROP TABLE IF EXISTS "notifications" CASCADE;

CREATE TABLE "notifications"
(
    "id"         UUID         NOT NULL,
    "user_id"    UUID         NOT NULL,
    "title"      VARCHAR(100) NOT NULL,
    "content"    VARCHAR(100) NOT NULL,
    "level"      VARCHAR(10)  NOT NULL,
    "created_at" TIMESTAMPTZ  NOT NULL,
    CONSTRAINT "PK_NOTIFICATIONS" PRIMARY KEY ("id"),
    CONSTRAINT "FK_NOTIFICATIONS_USERS" FOREIGN KEY ("user_id") REFERENCES "users" ("id") ON DELETE CASCADE,
    CONSTRAINT "CHK_NOTIFICATIONS_LEVEL" CHECK ("level" IN ('INFO', 'WARNING', 'ERROR'))
);

-- ===============================
-- DIRECT MESSAGES
-- ===============================
DROP TABLE IF EXISTS "direct_messages" CASCADE;

CREATE TABLE "direct_messages"
(
    "id"         UUID         NOT NULL,
    "sender"     UUID         NOT NULL,
    "receiver"   UUID         NOT NULL,
    "content"    VARCHAR(300) NOT NULL,
    "created_at" TIMESTAMPTZ  NOT NULL,
    CONSTRAINT "PK_DIRECT_MESSAGES" PRIMARY KEY ("id"),
    CONSTRAINT "FK_DM_SENDER" FOREIGN KEY ("sender") REFERENCES "users" ("id") ON DELETE CASCADE,
    CONSTRAINT "FK_DM_RECEIVER" FOREIGN KEY ("receiver") REFERENCES "users" ("id") ON DELETE CASCADE
);

-- ===============================
-- CLOTHES
-- ===============================
DROP TABLE IF EXISTS "clothes" CASCADE;

CREATE TABLE "clothes"
(
    "id"         UUID         NOT NULL,
    "created_at" TIMESTAMPTZ  NOT NULL,
    "updated_at" TIMESTAMPTZ  NOT NULL,
    "name"       VARCHAR(100) NOT NULL,
    "image_url"  TEXT         NULL,
    "type"       VARCHAR(20)  NOT NULL,
    "owner_id"   UUID         NOT NULL,
    CONSTRAINT "PK_CLOTHES" PRIMARY KEY ("id"),
    CONSTRAINT "FK_CLOTHES_USERS" FOREIGN KEY ("owner_id") REFERENCES "users" ("id") ON DELETE CASCADE,
    CONSTRAINT "CHK_CLOTHES_TYPE" CHECK ("type" IN ('TOP', 'BOTTOM'))
);

-- ===============================
-- CLOTHES ATTRIBUTE DEFINITIONS
-- ===============================
DROP TABLE IF EXISTS "clothes_attribute_defs" CASCADE;

CREATE TABLE "clothes_attribute_defs"
(
    "id"         UUID        NOT NULL,
    "created_at" TIMESTAMPTZ NOT NULL,
    "updated_at" TIMESTAMPTZ NOT NULL,
    "name"       VARCHAR(50) NOT NULL UNIQUE,
    CONSTRAINT "PK_CLOTHES_ATTRIBUTE_DEFS" PRIMARY KEY ("id")
);

-- ===============================
-- ATTRIBUTE OPTIONS
-- ===============================
DROP TABLE IF EXISTS "attribute_options" CASCADE;

CREATE TABLE "attribute_options"
(
    "id"            UUID        NOT NULL,
    "created_at"    TIMESTAMPTZ NOT NULL,
    "updated_at"    TIMESTAMPTZ NOT NULL,
    "value"         VARCHAR(50) NOT NULL,
    "definition_id" UUID        NOT NULL,
    CONSTRAINT "PK_ATTRIBUTE_OPTIONS" PRIMARY KEY ("id"),
    CONSTRAINT "FK_OPTIONS_DEF" FOREIGN KEY ("definition_id") REFERENCES "clothes_attribute_defs" ("id") ON DELETE CASCADE
);

-- ===============================
-- CLOTHES ATTRIBUTES
-- ===============================
DROP TABLE IF EXISTS "clothes_attributes" CASCADE;

CREATE TABLE "clothes_attributes"
(
    "id"            UUID        NOT NULL,
    "created_at"    TIMESTAMPTZ NOT NULL,
    "updated_at"    TIMESTAMPTZ NOT NULL,
    "option_value"  VARCHAR(50) NOT NULL,
    "clothes_id"    UUID        NOT NULL,
    "definition_id" UUID        NOT NULL,
    CONSTRAINT "PK_CLOTHES_ATTRIBUTES" PRIMARY KEY ("id"),
    CONSTRAINT "FK_CLOTHES_ATTR_CLOTHES" FOREIGN KEY ("clothes_id") REFERENCES "clothes" ("id") ON DELETE CASCADE,
    CONSTRAINT "FK_CLOTHES_ATTR_DEF" FOREIGN KEY ("definition_id") REFERENCES "clothes_attribute_defs" ("id") ON DELETE CASCADE
);

-- ===============================
-- FOLLOWS
-- ===============================
DROP TABLE IF EXISTS "follows" CASCADE;

CREATE TABLE "follows"
(
    "id"           UUID        NOT NULL,
    "follower_id"  UUID        NOT NULL,
    "following_id" UUID        NOT NULL,
    "created_at"   TIMESTAMPTZ NOT NULL,
    CONSTRAINT "PK_FOLLOWS" PRIMARY KEY ("id"),
    CONSTRAINT "FK_FOLLOWS_FOLLOWER" FOREIGN KEY ("follower_id") REFERENCES "users" ("id") ON DELETE CASCADE,
    CONSTRAINT "FK_FOLLOWS_FOLLOWING" FOREIGN KEY ("following_id") REFERENCES "users" ("id") ON DELETE CASCADE
);

-- ===============================
-- FEED LIKES
-- ===============================
DROP TABLE IF EXISTS "feed_likes" CASCADE;

CREATE TABLE "feed_likes"
(
    "id"         UUID        NOT NULL,
    "user_id"    UUID        NOT NULL,
    "feed_id"    UUID        NOT NULL,
    "created_at" TIMESTAMPTZ NOT NULL,
    CONSTRAINT "PK_FEED_LIKES" PRIMARY KEY ("id"),
    CONSTRAINT "FK_FEED_LIKES_FEED" FOREIGN KEY ("feed_id") REFERENCES "feeds" ("id") ON DELETE CASCADE,
    CONSTRAINT "FK_FEED_LIKES_USER" FOREIGN KEY ("user_id") REFERENCES "users" ("id") ON DELETE CASCADE
);

-- ===============================
-- FEED CLOTHES
-- ===============================
DROP TABLE IF EXISTS "feed_clothes" CASCADE;

CREATE TABLE "feed_clothes"
(
    "id"         UUID        NOT NULL,
    "feed_id"    UUID        NOT NULL,
    "clothes_id" UUID        NOT NULL,
    "created_at" TIMESTAMPTZ NOT NULL,
    CONSTRAINT "PK_FEED_CLOTHES" PRIMARY KEY ("id"),
    CONSTRAINT "FK_FEED_CLOTHES_FEED" FOREIGN KEY ("feed_id") REFERENCES "feeds" ("id") ON DELETE CASCADE,
    CONSTRAINT "FK_FEED_CLOTHES_CLOTHES" FOREIGN KEY ("clothes_id") REFERENCES "clothes" ("id") ON DELETE CASCADE
);

-- ===============================
-- WEATHER (ENUM → CHECK 로 처리)
-- ===============================
DROP TABLE IF EXISTS "weathers" CASCADE;

CREATE TABLE "weathers"
(
    "id"                        UUID             NOT NULL,
    "user_id"                   UUID             NOT NULL,
    "forecasted_at"             TIMESTAMPTZ      NOT NULL,
    "forecast_at"               TIMESTAMPTZ      NOT NULL,
    "sky_status"                VARCHAR(20)      NOT NULL,
    "precipitation_type"        VARCHAR(20)      NOT NULL,
    "precipitation_amount"      DOUBLE PRECISION NULL,
    "precipitation_probability" DOUBLE PRECISION NOT NULL,
    "temperature_current"       DOUBLE PRECISION NOT NULL,
    "temperature_compared"      DOUBLE PRECISION NULL,
    "temperature_min"           DOUBLE PRECISION NULL,
    "temperature_max"           DOUBLE PRECISION NULL,
    "wind_speed"                DOUBLE PRECISION NULL,
    "wind_speed_word"           VARCHAR(20)      NULL,
    "humidity_current"          DOUBLE PRECISION NULL,
    "humidity_compared"         DOUBLE PRECISION NULL,
    "created_at"                TIMESTAMPTZ      NOT NULL,
    CONSTRAINT "PK_WEATHERS" PRIMARY KEY ("id"),
    CONSTRAINT "FK_WEATHERS_USER" FOREIGN KEY ("user_id") REFERENCES "users" ("id") ON DELETE CASCADE,
    CONSTRAINT "CHK_WEATHER_SKY" CHECK ("sky_status" IN ('CLEAR', 'MOSTLY_CLOUDY', 'CLOUDY')),
    CONSTRAINT "CHK_WEATHER_PRECIP" CHECK ("precipitation_type" IN
                                           ('NONE', 'RAIN', 'RAIN_SNOW', 'SNOW', 'SHOWER')),
    CONSTRAINT "CHK_WEATHER_WIND" CHECK ("wind_speed_word" IN ('WEAK', 'MODERATE', 'STRONG'))
);

-- ===============================
-- RECOMMENDS
-- ===============================
DROP TABLE IF EXISTS "recommends" CASCADE;

CREATE TABLE "recommends"
(
    "id"         UUID        NOT NULL,
    "created_at" TIMESTAMPTZ NOT NULL,
    "user_id"    UUID        NOT NULL,
    "weather_id" UUID        NOT NULL,
    CONSTRAINT "PK_RECOMMENDS" PRIMARY KEY ("id"),
    CONSTRAINT "FK_RECOMMENDS_USER" FOREIGN KEY ("user_id") REFERENCES "users" ("id") ON DELETE CASCADE,
    CONSTRAINT "FK_RECOMMENDS_WEATHER" FOREIGN KEY ("weather_id") REFERENCES "weathers" ("id") ON DELETE CASCADE
);

-- ===============================
-- RECOMMENDATION_CLOTHES
-- ===============================
DROP TABLE IF EXISTS "recommendation_clothes" CASCADE;

CREATE TABLE "recommendation_clothes"
(
    "id"           UUID        NOT NULL,
    "created_at"   TIMESTAMPTZ NOT NULL,
    "recommend_id" UUID        NOT NULL,
    "clothes_id"   UUID        NOT NULL,
    CONSTRAINT "PK_RECOMMENDATION_CLOTHES" PRIMARY KEY ("id"),
    CONSTRAINT "FK_RECOMMENDATION_RECOMMEND" FOREIGN KEY ("recommend_id") REFERENCES "recommends" ("id") ON DELETE CASCADE,
    CONSTRAINT "FK_RECOMMENDATION_CLOTHES" FOREIGN KEY ("clothes_id") REFERENCES "clothes" ("id") ON DELETE CASCADE
);

-- ===============================
-- FOLLOWS UNIQUE 추가 (중복 팔로우 방지)
-- ===============================
ALTER TABLE "follows"
ADD CONSTRAINT "UK_FOLLOWS_FOLLOWER_FOLLOWING"
UNIQUE ("follower_id", "following_id");