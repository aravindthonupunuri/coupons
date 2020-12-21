FROM docker.target.com/tap/alpine-openjdk11-build:latest
ENV JAVA_HEAP_PERCENT="70"
ADD backpack-registry-coupons-migration-consumer-app/build/distributions/backpack-registry-coupons-migration-consumer-app.tar /
ENTRYPOINT ["/backpack-registry-coupons-migration-consumer-app/bin/backpack-registry-coupons-migration-consumer-app"]
