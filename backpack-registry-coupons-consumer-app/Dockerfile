FROM docker.target.com/tap/alpine-openjdk11-build:latest
ENV JAVA_HEAP_PERCENT="70"
ADD backpack-registry-coupons-consumer-app/build/distributions/backpack-registry-coupons-consumer-app.tar /
ENTRYPOINT ["/backpack-registry-coupons-consumer-app/bin/backpack-registry-coupons-consumer-app"]
