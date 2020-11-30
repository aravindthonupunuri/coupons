package com.tgt.backpackregistrycoupons.test

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class DataProvider {

    static Logger LOG = LoggerFactory.getLogger(DataProvider)

    static ObjectMapper objectMapper = new ObjectMapper()

    static checkHeaders = { headers ->
        def headerOk = !headers.getAuthorization().get().isEmpty() && headers.get('X-B3-TraceId') != null &&
            headers.get('X-B3-SpanId') != null && headers.get('X-B3-ParentSpanId') != null &&
            headers.get('X-B3-Sampled') != null
        if (!headerOk) {
            LOG.error("checkHeaders failed: " +
                "authHeader: ${headers.getAuthorization().get()} X-B3-TraceId: ${headers.get('X-B3-TraceId')} X-B3-SpanId: ${headers.get('X-B3-SpanId')} X-B3-ParentSpanId: ${headers.get('X-B3-ParentSpanId')} X-B3-Sampled: ${headers.get('X-B3-Sampled')}"
            )
        }
        headerOk
    }

    static getTokenResponse() {
        return [
            "access_token" : "test-token",
            "token_type" : "Bearer",
            "expires_in" : "259200",
            "scope" : "openid"
        ]
    }

    static getHeaders(profileId, includeDebug = true) {
        def headers = ["X-Tgt-Auth-Source": "gsp", "profile_id": profileId, "x-api-id": UUID.randomUUID().toString(), "Authorization" : UUID.randomUUID().toString()]
        if (includeDebug) {
            headers.put("x-lists-debug", "true")
        }
        return headers
    }
}
