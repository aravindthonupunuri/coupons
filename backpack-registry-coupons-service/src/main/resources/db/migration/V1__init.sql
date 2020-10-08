create table REGISTRY_COUPONS (
                                            COUPON_ID text not null,
                                            COUPON_TYPE text not null,
                                            REGISTRY_TYPE text not null,
                                            OFFER_ID text not null,
                                            COUPON_EXPIRY_DATE timestamp not null,
                                            CONSTRAINT REGISTRY_COUPONS_PK PRIMARY KEY (COUPON_ID)
);

CREATE UNIQUE INDEX REGISTRY_COUPONS_IDX1 ON REGISTRY_COUPONS (COUPON_ID, COUPON_TYPE, REGISTRY_TYPE);
