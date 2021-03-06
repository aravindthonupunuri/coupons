create table COUPONS (
                                            COUPON_CODE text not null,
                                            COUPON_TYPE text not null,
                                            REGISTRY_TYPE text not null,
                                            OFFER_ID text not null,
                                            COUPON_EXPIRY_DATE timestamp not null,
                                            CREATED_TS timestamp not null DEFAULT NOW(),
                                            UPDATED_TS timestamp not null DEFAULT NOW(),
                                            CONSTRAINT COUPONS_PK PRIMARY KEY (COUPON_CODE)
);

create table REGISTRY (
                                  REGISTRY_ID uuid not null,
                                  ALTERNATE_REGISTRY_ID text not null,
                                  REGISTRY_TYPE text not null,
                                  REGISTRY_STATUS text not null,
                                  REGISTRY_CREATED_TS timestamp not null,
                                  EVENT_DATE timestamp not null,
                                  COUPON_ASSIGNMENT_COMPLETE boolean not null,
                                  CREATED_TS timestamp not null DEFAULT NOW(),
                                  UPDATED_TS timestamp not null DEFAULT NOW(),
                                  CONSTRAINT REGISTRY_PK PRIMARY KEY (REGISTRY_ID)
);

create table REGISTRY_COUPONS (
                                        COUPON_CODE text,
                                        REGISTRY_ID uuid not null,
                                        COUPON_TYPE text not null,
                                        COUPON_REDEMPTION_STATUS text,
                                        COUPON_ISSUE_DATE timestamp,
                                        COUPON_EXPIRY_DATE timestamp,
                                        CREATED_TS timestamp not null DEFAULT NOW(),
                                        UPDATED_TS timestamp not null DEFAULT NOW(),
                                        CONSTRAINT REGISTRY_COUPONS_PK PRIMARY KEY (COUPON_CODE),
                                        CONSTRAINT FK_REGISTRY_COUPON
                                            FOREIGN KEY(REGISTRY_ID)
                                                REFERENCES REGISTRY(REGISTRY_ID)
                                                ON DELETE CASCADE
);

create table WELCOME_KITS (
                                  TCIN text not null,
                                  CREATED_TS timestamp not null DEFAULT NOW(),
                                  UPDATED_TS timestamp not null DEFAULT NOW(),
                                  CONSTRAINT WELCOME_KITS_PK PRIMARY KEY (TCIN)
);

CREATE INDEX coupons_registry_id_index
    ON REGISTRY_COUPONS(REGISTRY_ID);

