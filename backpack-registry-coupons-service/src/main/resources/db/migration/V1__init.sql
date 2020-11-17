create table COUPONS (
                                            COUPON_CODE text not null,
                                            COUPON_TYPE text not null,
                                            REGISTRY_TYPE text not null,
                                            OFFER_ID text not null,
                                            COUPON_EXPIRY_DATE timestamp not null,
                                            CONSTRAINT COUPONS_PK PRIMARY KEY (COUPON_CODE)
);

CREATE UNIQUE INDEX COUPONS_IDX1 ON COUPONS (COUPON_CODE, COUPON_TYPE, REGISTRY_TYPE);

create table REGISTRY_COUPONS (
                                            REGISTRY_ID uuid not null,
                                            COUPON_TYPE text not null,
                                            REGISTRY_TYPE text not null,
                                            REGISTRY_STATUS text not null,
                                            REGISTRY_CREATED_TS timestamp not null,
                                            EVENT_DATE timestamp not null,
                                            COUPON_CODE text,
                                            COUPON_NOTIFIED boolean not null,
                                            COUPON_REDEMPTION_STATUS text,
                                            COUPON_ISSUE_DATE timestamp,
                                            COUPON_EXPIRY_DATE timestamp,
                                            CREATED_USER text not null,
                                            UPDATED_USER text not null,
                                            CREATED_TS timestamp not null DEFAULT NOW(),
                                            UPDATED_TS timestamp not null DEFAULT NOW(),
                                            CONSTRAINT REGISTRY_COUPONS_PK PRIMARY KEY (REGISTRY_ID, COUPON_TYPE)
);

CREATE UNIQUE INDEX REGISTRY_COUPONS_IDX1 ON REGISTRY_COUPONS (REGISTRY_ID, COUPON_TYPE);
