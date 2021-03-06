DROP TABLE IF EXISTS REGULATORY_REGIONS;

CREATE TABLE REGULATORY_REGIONS (
    CHROMOSOME SMALLINT,
    START INTEGER,
    "end" INTEGER,
    FEATURE_TYPE VARCHAR(200)
);
CREATE INDEX RR1 ON REGULATORY_REGIONS (CHROMOSOME, START, "end");
