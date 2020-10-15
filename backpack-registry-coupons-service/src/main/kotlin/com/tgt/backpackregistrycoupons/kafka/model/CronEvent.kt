package com.tgt.guestnotifications.kafka.model

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.tgt.lists.lib.api.util.EventType
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.Month

data class CronEvent(
    @JsonSerialize(using = LocalDateTimeSerializer::class)
    @JsonDeserialize(using = LocalDateTimeDeserializer::class)
    @JsonProperty("event_date_time")
    val eventDateTime: LocalDateTime,

    @JsonProperty("time_zone")
    val timeZone: String,

    @JsonProperty("event_interval_mins")
    val eventIntervalMins: Long,

    @JsonProperty("minute_block_of_hour")
    val minuteBlockOfHour: Long,

    @JsonProperty("hour_of_day")
    val hourOfDay: Int,

    @JsonProperty("day_of_week")
    val dayOfWeek: DayOfWeek,

    @JsonProperty("day_of_month")
    val dayOfMonth: Int,

    @JsonProperty("month_of_year")
    val monthOfYear: Month,

    @JsonProperty("retry_state")
    var retryState: String? = null
) {
    companion object {
        // jacksonObjectMapper() returns a normal ObjectMapper with the KotlinModule registered
        private val jsonMapper: ObjectMapper = jacksonObjectMapper().setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE)

        @JvmStatic
        fun getEventType(): EventType {
            return "CRON-EVENT"
        }

        @JvmStatic
        fun deserialize(byteArray: ByteArray): CronEvent {
            return jsonMapper.readValue<CronEvent>(byteArray, CronEvent::class.java)
        }
    }
}
