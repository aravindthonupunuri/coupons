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
                                            CONSTRAINT REGISTRY_COUPONS_PK PRIMARY KEY (COUPON_CODE)
);
