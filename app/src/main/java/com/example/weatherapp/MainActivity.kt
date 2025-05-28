package com.example.weatherapp

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.weatherapp.databinding.ActivityMainBinding
import com.example.weatherapp.datamodels.WeatherDataModel
import com.example.weatherapp.utils.Constants
import com.example.weatherapp.utils.Extensions.Companion.isNetworkAvailable
import com.example.weatherapp.utils.Extensions.Companion.requestNecessaryPermissions
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone


class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var mFusedLocationClient: FusedLocationProviderClient
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        val message = "Please Provide Necessary Permissions"
        val permissionList = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        if (!isLocationEnabled()) {
            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            startActivity(intent)
            Toast.makeText(this@MainActivity, "The location is not enabled", Toast.LENGTH_SHORT)
                .show()
        } else {
            this.requestNecessaryPermissions(permissionList, message) {
                requestLocationData()
                Log.d("Talha", "Permissions granted. Starting download...")
            }
        }


    }

    @SuppressLint("MissingPermission")
    private fun requestLocationData() {
        val locationRequest = com.google.android.gms.location.LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY, 1000
        ).build()
        mFusedLocationClient.requestLocationUpdates(locationRequest, object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                getLocationWeatherDetails(
                    locationResult.lastLocation?.latitude!!,
                    locationResult.lastLocation?.longitude!!
                )
            }
        }, Looper.myLooper())
    }

    private fun isLocationEnabled(): Boolean {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    private fun getLocationWeatherDetails(latitude: Double, longitude: Double) {
        if (isNetworkAvailable(this)) {
            val retrofit = Retrofit.Builder()
                .baseUrl(Constants.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            val serviceApi = retrofit.create(WeatherApiService::class.java)

            val call = serviceApi.getWeatherDetails(
                latitude, longitude,
                Constants.APP_ID,
                Constants.METRIC_UNIT
            )

            call.enqueue(object : Callback<WeatherDataModel> {
                @SuppressLint("SetTextI18n")
                override fun onResponse(
                    call: Call<WeatherDataModel>,
                    response: Response<WeatherDataModel>
                ) {
                    if (response.isSuccessful) {
                        val weather = response.body()
                        if (weather != null) {
                            for (i in weather.weather.indices) {
                                binding.textViewSunset.text = weather.sys?.sunset?.let {
                                    convertTime(
                                        it.toLong()
                                    )
                                }
                                binding.textViewSunrise.text = weather.sys?.sunrise?.let {
                                    convertTime(
                                        it.toLong()
                                    )
                                }
                                binding.textViewStatus.text = weather.weather[i].description
                                binding.textViewAddress.text = weather.name
                                binding.textViewTempMax.text =
                                    weather.main?.tempMax.toString() + " max"
                                binding.textViewTempMin.text =
                                    weather.main?.tempMin.toString() + " min"
                                binding.textViewTemp.text = weather.main?.temp.toString() + "°C"
                                binding.textViewHumidity.text = weather.main?.humidity.toString()
                                binding.textViewPressure.text = weather.main?.pressure.toString()
                                binding.textViewWind.text = weather.wind?.speed.toString()
                            }
                        }
                    } else {
                        Toast.makeText(
                            this@MainActivity,
                            "Something went wrong",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onFailure(call: Call<WeatherDataModel>, t: Throwable) {
                    Toast.makeText(this@MainActivity, t.toString(), Toast.LENGTH_SHORT).show()
                }

            })

        } else {
            Toast.makeText(this, "There's no internet connection", Toast.LENGTH_SHORT).show()
        }
    }

    private fun convertTime(time: Long): String {
        val date = Date(time * 1000L)
        val timeFormatted = SimpleDateFormat("HH:mm", Locale.UK)
        timeFormatted.timeZone = TimeZone.getDefault()
        return timeFormatted.format(date)
    }


}