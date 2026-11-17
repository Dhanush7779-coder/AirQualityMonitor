# Air Quality Monitor (Android)

An Android application developed in Kotlin according to Question 1 requirements. The app provides real-time Air Quality Index (AQI) monitoring based on the user's current GPS location, dynamic category-based dashboard coloring, health advisories, a 24-hour hourly forecast, and Room database persistence for reading history.

---

## Features

1. **Firebase Google Authentication & Android Credential Manager:**
   - Sign in using Google accounts via modern `CredentialManager` (`GetGoogleIdOption`).
   - Authenticated with Firebase Authentication (`GoogleAuthProvider`).
   - Session maintenance and complete logout capability.

2. **Current Location Detection:**
   - Uses Google Play Services Fused Location Provider with the mandatory API:
     `getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null)`
   - Handles runtime permissions (`ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`) and graceful fallbacks.
   - Reverse geocoding offloaded to `Dispatchers.IO`.

3. **Live Air Quality Data:**
   - Integrates free Open-Meteo Air Quality API:
     `https://air-quality-api.open-meteo.com/v1/air-quality`
   - Real-time parameters: US AQI, PM2.5, PM10, Carbon Monoxide (CO).

4. **Dynamic Dashboard & Responsive Design:**
   - Dynamic card background based on US AQI categories using defined color resources:
     - 0–50: Good (Green)
     - 51–100: Moderate (Yellow)
     - 101–150: Unhealthy for Sensitive Groups (Orange)
     - 151–200: Unhealthy (Red)
     - 201–300: Very Unhealthy (Purple)
     - 301–500: Hazardous (Maroon)
   - Dynamic text contrast adjustment for high readability.
   - Full WindowInsets support (`fitsSystemWindows` / `ViewCompat.setOnApplyWindowInsetsListener`) preventing status bar and navigation bar overlap on modern Android devices.

5. **Next 24 Hours AQI Forecast:**
   - Horizontal `RecyclerView` with hourly AQI predictions and colored category badges.

6. **Room Database - Reading History:**
   - Offline local persistence storing date/time, AQI value, location, and key pollutant readings.
   - History screen (`HistoryActivity`) with RecyclerView.
   - Individual record deletion.

---

## Tech Stack & Architecture

- **Language:** Kotlin
- **UI:** XML Layouts with ViewBinding (Zero Jetpack Compose)
- **Database:** Android Room (`AirQualityDatabase`, `AirQualityDao`, `AirQualityEntity`)
- **Networking:** Retrofit 2 + Gson Converter Factory
- **Authentication:** Android Credential Manager + Firebase Authentication
- **Location:** Google Play Services Fused Location Provider Client
- **Minimum SDK:** 23 (Android 6.0)
- **Target / Compile SDK:** 37

---

## Project Structure

```
app/src/main/java/com/example/airqualitymonitor/
├── adapter/
│   ├── HistoryAdapter.kt
│   └── HourlyAdapter.kt
├── auth/
│   └── LoginActivity.kt
├── data/
│   ├── AirQualityDao.kt
│   ├── AirQualityDatabase.kt
│   └── AirQualityEntity.kt
├── location/
│   └── LocationHelper.kt
├── network/
│   ├── AirQualityResponse.kt
│   ├── ApiService.kt
│   └── RetrofitClient.kt
├── ui/
│   ├── HistoryActivity.kt
│   └── MainActivity.kt
└── util/
    └── AqiUtils.kt
```
