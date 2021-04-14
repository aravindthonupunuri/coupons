package com.tgt.graphana_builder

import com.tgt.grafana_builder.GrafanaBuilder
import com.tgt.grafana_builder.GrafanaBuilderConfig
import spock.lang.Specification

class GrafanaBuilderCouponsAppUnitTest extends Specification {

    def "build backpack-registry-coupons-app grafana dashboard"() {
        given:
        def moduleDir = System.getProperty("user.dir")

        def metricsAlert = new GrafanaBuilderConfig.MetricsAlert(
            prodTapApplication: "backpackregistrycoupons",
            prodTapCluster: "backpackregistrycoupons",
            notificationUids: [ "FQW_lvBZk", "roC6asiMz", "7GGtGwmMz" ],
            cpuUsageThreshold: "75",
            memUsageThreshold: "75",
            server500countThreshold: 25
        )

        GrafanaBuilderConfig grafanaBuilderConfig = new GrafanaBuilderConfig(
            tapDashboardJsonFile: "${moduleDir}/src/test/unit/resources/tap-dashboard.json",
            apiServerSpecBasePath: "/registries_coupons/v1",
            apiServerSpecPath: "${moduleDir}/api-specs/backpack-registry-coupons-v1.yml",
            needResiliencePanel: false,
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
