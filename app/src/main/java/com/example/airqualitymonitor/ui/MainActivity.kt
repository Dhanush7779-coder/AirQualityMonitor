package com.example.airqualitymonitor.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.airqualitymonitor.R
import com.example.airqualitymonitor.adapter.HourlyAdapter
import com.example.airqualitymonitor.auth.LoginActivity
import com.example.airqualitymonitor.data.AirQualityDao
import com.example.airqualitymonitor.data.AirQualityDatabase
import com.example.airqualitymonitor.data.AirQualityEntity
import com.example.airqualitymonitor.databinding.ActivityMainBinding
import com.example.airqualitymonitor.location.LocationHelper
import com.example.airqualitymonitor.network.AirQualityResponse
import com.example.airqualitymonitor.network.HourlyForecastItem
import com.example.airqualitymonitor.network.RetrofitClient
import com.example.airqualitymonitor.util.AqiCategory
import com.example.airqualitymonitor.util.AqiUtils
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var locationHelper: LocationHelper
    private lateinit var dao: AirQualityDao
    private lateinit var hourlyAdapter: HourlyAdapter

    private var currentAqiEntity: AirQualityEntity? = null
    private var lastLatitude: Double = 17.7041
    private var lastLongitude: Double = 83.2977
    private var currentLocationName: String = "Detecting location..."

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

        if (fineGranted || coarseGranted) {
            binding.cardPermissionBanner.visibility = View.GONE
            fetchLocationAndAirQuality()
        } else {
            binding.cardPermissionBanner.visibility = View.VISIBLE
            // Fallback to default coordinates so the user still sees functional data
            Toast.makeText(
                this,
                "Location permission denied. Showing default location AQI.",
                Toast.LENGTH_LONG
            ).show()
            fetchAirQuality(lastLatitude, lastLongitude, "Default Location (Permission Denied)")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Prevent top toolbar from overlapping with system status bar and camera notch
        ViewCompat.setOnApplyWindowInsetsListener(binding.coordinatorLayout) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.appBarLayout.setPadding(0, systemBars.top, 0, 0)
            binding.nestedScrollView.setPadding(0, 0, 0, systemBars.bottom)
            insets
        }

        auth = FirebaseAuth.getInstance()
        locationHelper = LocationHelper(this)
        dao = AirQualityDatabase.getDatabase(this).airQualityDao()

        setupUserHeader()
        setupHourlyRecyclerView()
        setupListeners()

        checkPermissionAndLoad()
    }

    private fun setupUserHeader() {
        val user = auth.currentUser
        val displayName = user?.displayName ?: user?.email ?: "User"
        binding.tvUserWelcome.text = "Welcome, $displayName"
    }

    private fun setupHourlyRecyclerView() {
        hourlyAdapter = HourlyAdapter()
        binding.rvHourlyForecast.layoutManager = LinearLayoutManager(
            this,
            LinearLayoutManager.HORIZONTAL,
            false
        )
        binding.rvHourlyForecast.adapter = hourlyAdapter
    }

    private fun setupListeners() {
        binding.btnRefresh.setOnClickListener {
            checkPermissionAndLoad()
        }

        binding.btnRetry.setOnClickListener {
            checkPermissionAndLoad()
        }

        binding.btnGrantPermission.setOnClickListener {
            requestLocationPermissions()
        }

        binding.btnViewHistory.setOnClickListener {
            val intent = Intent(this, HistoryActivity::class.java)
            startActivity(intent)
        }

        binding.btnLogout.setOnClickListener {
            performLogout()
        }

        binding.btnSaveReading.setOnClickListener {
            saveCurrentReading()
        }
    }

    private fun checkPermissionAndLoad() {
        if (locationHelper.hasLocationPermission()) {
            binding.cardPermissionBanner.visibility = View.GONE
            fetchLocationAndAirQuality()
        } else {
            requestLocationPermissions()
        }
    }

    private fun requestLocationPermissions() {
        locationPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    private fun fetchLocationAndAirQuality() {
        showLoading(true, getString(R.string.location_loading))

        lifecycleScope.launch {
            val location = locationHelper.getCurrentLocation()
            if (location != null) {
                lastLatitude = location.latitude
                lastLongitude = location.longitude
                currentLocationName = locationHelper.getCityName(lastLatitude, lastLongitude)
            } else {
                // If device GPS is disabled or unavailable (e.g. emulators), notify and fallback
                currentLocationName = "Default Coordinates (GPS Unavailable)"
                Toast.makeText(
                    this@MainActivity,
                    "Live GPS fix unavailable. Using default coordinates.",
                    Toast.LENGTH_SHORT
                ).show()
            }

            fetchAirQuality(lastLatitude, lastLongitude, currentLocationName)
        }
    }

    private fun fetchAirQuality(latitude: Double, longitude: Double, locationName: String) {
        showLoading(true, getString(R.string.data_loading))

        lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.apiService.getAirQuality(
                        latitude = latitude,
                        longitude = longitude
                    )
                }

                showLoading(false)
                bindAirQualityData(response, locationName)

            } catch (e: Exception) {
                showLoading(false)
                showError("Error connecting to Open-Meteo API: ${e.localizedMessage ?: "Unknown error"}")
            }
        }
    }

    private fun bindAirQualityData(response: AirQualityResponse, locationName: String) {
        val current = response.current
        if (current == null || current.usAqi == null) {
            showError("Air quality readings are currently unavailable for this location.")
            return
        }

        binding.layoutError.visibility = View.GONE
        binding.layoutDashboardContent.visibility = View.VISIBLE

        val aqiValue = current.usAqi
        val aqiCategory = AqiUtils.getCategory(aqiValue)
        val pm25 = current.pm25 ?: 0.0
        val pm10 = current.pm10 ?: 0.0
        val co = current.carbonMonoxide ?: 0.0

        // Location & Time
        binding.tvCurrentLocation.text = locationName
        val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        val formattedTime = dateFormat.format(Date())
        binding.tvLastUpdated.text = "Last updated: $formattedTime"

        // AQI Value & Category Badge
        binding.tvAqiValue.text = aqiValue.toString()
        binding.tvAqiCategoryBadge.text = aqiCategory.title

        // Dynamic Dashboard Color based on AQI category
        val categoryColor = ContextCompat.getColor(this, aqiCategory.colorRes)
        binding.cardAqiStatus.setCardBackgroundColor(categoryColor)

        // Health Advisory
        binding.tvHealthAdvice.text = aqiCategory.advice

        // Text contrast depending on background
        val textColor = if (aqiCategory.isDarkBackground) {
            ContextCompat.getColor(this, R.color.white)
        } else {
            ContextCompat.getColor(this, R.color.black)
        }
        binding.tvAqiTitle.setTextColor(textColor)
        binding.tvAqiValue.setTextColor(textColor)
        binding.tvAqiUnit.setTextColor(textColor)
        binding.tvHealthAdviceTitle.setTextColor(textColor)
        binding.tvHealthAdvice.setTextColor(textColor)

        // Pollutants
        binding.tvPm25Value.text = String.format(Locale.US, "%.1f", pm25)
        binding.tvPm10Value.text = String.format(Locale.US, "%.1f", pm10)
        binding.tvCoValue.text = String.format(Locale.US, "%.1f", co)

        // Next 24 Hours Forecast
        processHourlyForecast(response.hourly, current.time)

        // Cache reading entity for saving to Room
        currentAqiEntity = AirQualityEntity(
            dateTime = formattedTime,
            aqi = aqiValue,
            category = aqiCategory.title,
            location = locationName,
            pm25 = pm25,
            pm10 = pm10,
            carbonMonoxide = co
        )
    }

    private fun processHourlyForecast(hourly: com.example.airqualitymonitor.network.HourlyAirQuality?, currentTime: String) {
        if (hourly == null || hourly.time.isNullOrEmpty() || hourly.usAqi.isNullOrEmpty()) {
            return
        }

        val times = hourly.time
        val aqis = hourly.usAqi

        // Find starting index matching current hour or 0
        var startIndex = 0
        val currentPrefix = if (currentTime.length >= 13) currentTime.substring(0, 13) else ""
        if (currentPrefix.isNotEmpty()) {
            val foundIndex = times.indexOfFirst { it.startsWith(currentPrefix) }
            if (foundIndex != -1) {
                startIndex = foundIndex
            }
        }

        val forecastList = mutableListOf<HourlyForecastItem>()
        for (i in startIndex until (startIndex + 24).coerceAtMost(times.size.coerceAtMost(aqis.size))) {
            val hourTime = times[i]
            val aqiVal = aqis[i] ?: continue
            forecastList.add(HourlyForecastItem(time = hourTime, aqi = aqiVal))
        }

        hourlyAdapter.updateData(forecastList)
    }

    private fun saveCurrentReading() {
        val entity = currentAqiEntity
        if (entity == null) {
            Toast.makeText(this, "No air quality data available to save.", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                dao.insert(entity)
            }
            Toast.makeText(this@MainActivity, getString(R.string.saved_successfully), Toast.LENGTH_SHORT).show()
        }
    }

    private fun performLogout() {
        auth.signOut()
        lifecycleScope.launch {
            try {
                val credentialManager = CredentialManager.create(this@MainActivity)
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
            } catch (_: Exception) {}

            val intent = Intent(this@MainActivity, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    private fun showLoading(isLoading: Boolean, message: String = "") {
        if (isLoading) {
            binding.layoutLoading.visibility = View.VISIBLE
            binding.tvLoadingStatus.text = message
            binding.layoutDashboardContent.visibility = View.GONE
            binding.layoutError.visibility = View.GONE
        } else {
            binding.layoutLoading.visibility = View.GONE
        }
    }

    private fun showError(message: String) {
        binding.layoutLoading.visibility = View.GONE
        binding.layoutDashboardContent.visibility = View.GONE
        binding.layoutError.visibility = View.VISIBLE
        binding.tvErrorMessage.text = message
    }
}
