package com.tgt.grafana_builder

import spock.lang.Specification

class GrafanaBuilderCouponsPromoConsumerUnitTest extends Specification {

    def "build backpack-registry-coupons-promo-consumer-app grafana dashboard"() {
        given:
        def moduleDir = System.getProperty("user.dir")

        def metricsAlert = new GrafanaBuilderConfig.MetricsAlert(
            prodTapApplication: "backpackregistrycouponspromoconsumer",
            prodTapCluster: "backpackregistrycouponspromoconsumer",
            notificationUids: [ "FQW_lvBZk", "roC6asiMz", "7GGtGwmMz" ],
            cpuUsageThreshold: "75",
            memUsageThreshold: "75",
            server500countThreshold: 25
        )

        def kafkaConsumers = [
            new GrafanaBuilderConfig.KafkaConsumer(
                title: "Registry Coupons Promo Consumer",
                metricsName: "msgbus_consumer_event",
                isDlqConsumer: false,
                devEnvironment: new GrafanaBuilderConfig.KafkaConsumerEnvironment(
                    topic: "promo-coupon-redemption-notifications-v2",
                    consumerGroup: "promo-msg-consumer-dev",
                    ttcCluster: "ost-ttc-test-app",
                    tteCluster: "ost-ttce-test-app"
                ),
                stageEnvironment: new GrafanaBuilderConfig.KafkaConsumerEnvironment(
                    topic: "promo-coupon-redemption-notifications-v2",
                    consumerGroup: "promo-msg-consumer-stage",
                    ttcCluster: "ost-ttc-test-app",
                    tteCluster: "ost-ttce-test-app"
                ),
                prodEnvironment: new GrafanaBuilderConfig.KafkaConsumerEnvironment(
                    topic: "promo-coupon-redemption-notifications-v2",
                    consumerGroup: "promo-msg-consumer-prod",
                    ttcCluster: "ost-ttc-prod-app",
                    tteCluster: "ost-ttce-prod-app"
                )
            ),
            new GrafanaBuilderConfig.KafkaConsumer(
                title: "Registry Coupons Promo DLQ Consumer",
                metricsName: "msgbus_consumer_event",
                isDlqConsumer: true,
                devEnvironment: new GrafanaBuilderConfig.KafkaConsumerEnvironment(
                    topic: "registry-internal-data-bus-dev-dlq",
                    consumerGroup: "backpack-registry-coupons-promo-bus-dev-dlq-consumer",
                    ttcCluster: "ost-ttc-test-app",
                    tteCluster: "ost-ttce-test-app"
                ),
                stageEnvironment: new GrafanaBuilderConfig.KafkaConsumerEnvironment(
                    topic: "registry-internal-data-bus-stage-dlq",
                    consumerGroup: "backpack-registry-coupons-promo-data-bus-stage-dlq-consumer",
                    ttcCluster: "ost-ttc-test-app",
                    tteCluster: "ost-ttce-test-app"
                ),
                prodEnvironment: new GrafanaBuilderConfig.KafkaConsumerEnvironment(
                    topic: "registry-internal-data-bus-prod-dlq",
                    consumerGroup: "backpack-registry-coupons-promo-data-bus-prod-dlq-consumer",
                    ttcCluster: "ost-ttc-prod-app",
                    tteCluster: "ost-ttce-prod-app"
                )
            )
        ]

        def kafkaProducers = [
            new GrafanaBuilderConfig.KafkaProducer(
                title: "Registry msgbus DLQ Producer",
                metricsName: "msgbus_producer_event",
                isDlqProducer: true
            )
        ]

        GrafanaBuilderConfig grafanaBuilderConfig = new GrafanaBuilderConfig(
            tapDashboardJsonFile: "${moduleDir}/src/test/unit/resources/tap-dashboard.json",
            needResiliencePanel: false,
            kafkaConsumers: kafkaConsumers,
            kafkaProducers: kafkaProducers,
            metricsAlert: metricsAlert,
            cassandra: false,
            postgres: true
        )

        GrafanaBuilder grafanaBuilder = new GrafanaBuilder(grafanaBuilderConfig)

        when:
        def success = grafanaBuilder.buildPanels()

        then:
        success
    }
}
