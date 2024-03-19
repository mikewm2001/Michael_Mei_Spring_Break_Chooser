package com.bignerdranch.android.michael_mei_spring_break_chooser

import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import kotlin.math.abs

class MainActivity : AppCompatActivity(), SensorEventListener {

    // UI and variable initialization
    private lateinit var translatedPhraseEditText: EditText
    private lateinit var languageListView: ListView
    private lateinit var selectedLanguage: String

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var lastTime: Long = 0
    private var lastUpdate: Long = 0
    private var lastX = 0.0f
    private var lastY = 0.0f
    private var lastZ = 0.0f
    private val SHAKE_THRESHOLD = 200

    private var mediaPlayer: MediaPlayer? = null

    private val TAG = "MainActivity"

    // Activity result launcher for speech recognition
    private val speechRecognitionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val data: Intent? = result.data
            // Extract recognized speech text from the intent
            val resultText =
                data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                    ?.get(0) ?: ""
            // Set recognized text to EditText
            translatedPhraseEditText.setText(resultText)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize UI elements
        translatedPhraseEditText = findViewById(R.id.translatedPhraseEditText)
        languageListView = findViewById(R.id.languageListView)

        // Array of supported languages
        val languages = arrayOf("English", "Spanish", "French", "Chinese", "Japanese", "Korean")

        // Adapter for populating language list view
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, languages)
        languageListView.adapter = adapter

        // Handle click events on language list items
        languageListView.setOnItemClickListener { _, _, position, _ ->
            // Get selected language
            val selectedLanguage = languages[position]
            // Prompt speech input in the selected language
            promptSpeechInput(selectedLanguage)
        }

        // Initiale sensor manager and accelerometer
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    }

    // Prompt speech input in the specified language
    private fun promptSpeechInput(language: String) {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, language)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Say something in $language")

        // Set language parameter based on the selected language
        when (language) {
            "English" -> intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en")
            "Spanish" -> intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es")
            "French" -> intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "fr")
            "Chinese" -> intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "zh-CN")
            "Japanese" -> intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ja")
            "Korean" -> intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ko")
        }
        // Launch speech recognition activity
        selectedLanguage = language
        speechRecognitionLauncher.launch(intent)
    }

    override fun onResume() {
        super.onResume()
        // Register sensor listener
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL)
    }

    override fun onPause() {
        super.onPause()
        // Unregister sensor listener
        sensorManager.unregisterListener(this)
    }

    // Handle snesor data change
    override fun onSensorChanged(event: SensorEvent?) {
        val curTime = System.currentTimeMillis()
        // Check if it's time to handle sensor data
        if (curTime - lastUpdate > 100) {
            val diffTime = curTime - lastTime
            // Check if it's time to handle sensor data
            if (diffTime > 100) {
                lastTime = curTime
                val x = event!!.values[0]
                val y = event.values[1]
                val z = event.values[2]

                val speed = abs(x + y + z - lastX - lastY - lastZ) / diffTime * 10000

                if (speed > SHAKE_THRESHOLD) {
                    // Create media player for the selected language
                    createMediaPlayer(selectedLanguage)
                    // Launch Google Maps with the vacation spot based on the selected language
                    launchGoogleMaps(selectedLanguage) }

                // Update last accelerometer values and last update time
                lastX = x
                lastY = y
                lastZ = z
                lastUpdate = curTime
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    // Launch Google Maps with the vacation spot based on the selected language
    private fun launchGoogleMaps(language: String) {
        // Get the vacation spot based on the selected language
        val vacationSpot = getVacationSpot(language)
        // Create a URI for the vacation spot
        val uri = Uri.parse("geo:0,0?q=$vacationSpot")
        // Create an intent to view the location on Google Maps
        val mapIntent = Intent(Intent.ACTION_VIEW, uri)
        // Set Google Maps package to ensure it opens in Google Maps app
        mapIntent.setPackage("com.google.android.apps.maps")
        // Check if there's an activity that can handle the intent
        if (mapIntent.resolveActivity(packageManager) != null) {
            // Start the activity to view the location on Google Maps
            startActivity(mapIntent)
        }
    }

    // Get the vacation spot based on the selected language
    private fun getVacationSpot(language: String): String {
        return when (language) {
            "English" -> "London"
            "Spanish" -> "Mexico City"
            "French" -> "Paris"
            "Chinese" -> "Beijing"
            "Japanese" -> "Tokyo"
            "Korean" -> "Seoul"
            else -> "Boston" // Default to Boston if language not recognized
        }
    }

    // Create media player for playing the greeting in the selected language
    private fun createMediaPlayer(language: String) {
        // Get the resource ID of the audio file based on the selected language
        val resourceId = when (language) {
            "English" -> R.raw.hello_english
            "Spanish" -> R.raw.hello_spanish
            "French" -> R.raw.hello_french
            "Chinese" -> R.raw.hello_chinese
            "Japanese" -> R.raw.hello_japanese
            "Korean" -> R.raw.hello_korean
            else -> R.raw.hello_english // Default to English if language not recognized
        }

        // Create media player with the selected audio file
        mediaPlayer = MediaPlayer.create(this, resourceId)
        // Play the audio
        mediaPlayer?.start()
    }
}