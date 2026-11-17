package com.example.airqualitymonitor.ui

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.airqualitymonitor.adapter.HistoryAdapter
import com.example.airqualitymonitor.data.AirQualityDao
import com.example.airqualitymonitor.data.AirQualityDatabase
import com.example.airqualitymonitor.databinding.ActivityHistoryBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHistoryBinding
    private lateinit var dao: AirQualityDao
    private lateinit var adapter: HistoryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.coordinatorLayoutHistory) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.appBarLayoutHistory.setPadding(0, systemBars.top, 0, 0)
            binding.rvHistory.setPadding(0, 8, 0, systemBars.bottom + 16)
            insets
        }

        dao = AirQualityDatabase.getDatabase(this).airQualityDao()

        setupRecyclerView()

        binding.btnBack.setOnClickListener {
            finish()
        }

        loadHistory()
    }

    private fun setupRecyclerView() {
        adapter = HistoryAdapter { readingToDelete ->
            lifecycleScope.launch {
                withContext(Dispatchers.IO) {
                    dao.delete(readingToDelete)
                }
                Toast.makeText(this@HistoryActivity, "Record deleted.", Toast.LENGTH_SHORT).show()
                loadHistory()
            }
        }
        binding.rvHistory.layoutManager = LinearLayoutManager(this)
        binding.rvHistory.adapter = adapter
    }

    private fun loadHistory() {
        lifecycleScope.launch {
            val readings = withContext(Dispatchers.IO) {
                dao.getAllReadings()
            }

            if (readings.isEmpty()) {
                binding.layoutEmptyHistory.visibility = View.VISIBLE
                binding.rvHistory.visibility = View.GONE
            } else {
                binding.layoutEmptyHistory.visibility = View.GONE
                binding.rvHistory.visibility = View.VISIBLE
                adapter.updateData(readings)
            }
        }
    }
}
