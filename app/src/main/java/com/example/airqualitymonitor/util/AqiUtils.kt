package com.example.airqualitymonitor.util

import androidx.annotation.ColorRes
import com.example.airqualitymonitor.R

enum class AqiCategory(
    val title: String,
    val advice: String,
    @get:ColorRes val colorRes: Int,
    @get:ColorRes val lightColorRes: Int,
    val isDarkBackground: Boolean
) {
    GOOD(
        title = "Good",
        advice = "Enjoy outdoor activities.",
        colorRes = R.color.aqi_good,
        lightColorRes = R.color.aqi_good_light,
        isDarkBackground = true
    ),
    MODERATE(
        title = "Moderate",
        advice = "Air quality is acceptable.",
        colorRes = R.color.aqi_moderate,
        lightColorRes = R.color.aqi_moderate_light,
        isDarkBackground = false
    ),
    UNHEALTHY_SENSITIVE(
        title = "Unhealthy for Sensitive Groups",
        advice = "Sensitive people should reduce prolonged outdoor activity.",
        colorRes = R.color.aqi_sensitive,
        lightColorRes = R.color.aqi_sensitive_light,
        isDarkBackground = true
    ),
    UNHEALTHY(
        title = "Unhealthy",
        advice = "Reduce outdoor activity.",
        colorRes = R.color.aqi_unhealthy,
        lightColorRes = R.color.aqi_unhealthy_light,
        isDarkBackground = true
    ),
    VERY_UNHEALTHY(
        title = "Very Unhealthy",
        advice = "Avoid prolonged outdoor exposure.",
        colorRes = R.color.aqi_very_unhealthy,
        lightColorRes = R.color.aqi_very_unhealthy_light,
        isDarkBackground = true
    ),
    HAZARDOUS(
        title = "Hazardous",
        advice = "Stay indoors and avoid outdoor activities.",
        colorRes = R.color.aqi_hazardous_maroon,
        lightColorRes = R.color.aqi_hazardous_light,
        isDarkBackground = true
    )
}

object AqiUtils {
    fun getCategory(aqi: Int): AqiCategory {
        return when {
            aqi <= 50 -> AqiCategory.GOOD
            aqi <= 100 -> AqiCategory.MODERATE
            aqi <= 150 -> AqiCategory.UNHEALTHY_SENSITIVE
            aqi <= 200 -> AqiCategory.UNHEALTHY
            aqi <= 300 -> AqiCategory.VERY_UNHEALTHY
            else -> AqiCategory.HAZARDOUS
        }
    }
}
