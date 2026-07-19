package com.github.pires.obd.reader

import java.util.HashMap

data class ObdReading(
    var latitude: Double = 0.0,
    var longitude: Double = 0.0,
    var altitude: Double = 0.0,
    var timestamp: Float = 0f,
    var vin: String? = null,
    var readingsInput: Map<String, String>? = null
) {
    val readings: MutableMap<String, String> = HashMap(readingsInput ?: emptyMap())

    override fun toString(): String {
        return "lat:$latitude;" +
                "long:$longitude;" +
                "alt:$altitude;" +
                "vin:$vin;" +
                "readings:${readings.toString().substring(10).replace("}", "").replace(",", ";")}"
    }
}
