package com.example.meteoxiaomi

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class Hour(val time: String, val temp: Int, val wind: Int, val precip: Int, val code: Int)
private data class WeatherData(
    val place: String, val temp: Int, val feels: Int, val description: String,
    val wind: Int, val humidity: Int, val hours: List<Hour>
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MeteoApp() }
    }
}

@Composable
private fun MeteoApp() {
    var cityInput by remember { mutableStateOf("Kyiv") }
    var city by remember { mutableStateOf("Kyiv") }
    var weather by remember { mutableStateOf<WeatherData?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    var refresh by remember { mutableIntStateOf(0) }

    LaunchedEffect(city, refresh) {
        loading = true
        error = null
        try {
            weather = loadWeather(city)
        } catch (e: Exception) {
            error = "Не удалось загрузить погоду. Проверь интернет и попробуй ещё раз."
        } finally {
            loading = false
        }
    }

    val w = weather
    Column(
        Modifier.fillMaxSize().background(Color(0xFFF2F5FA)).padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(18.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Meteo", fontSize = 27.sp, fontWeight = FontWeight.Bold, color = Color(0xFF18243A))
                Text("Погода по часам", fontSize = 13.sp, color = Color(0xFF65748B))
            }
            TextButton(onClick = { refresh++ }) { Text("Обновить") }
        }
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = cityInput, onValueChange = { cityInput = it },
                modifier = Modifier.weight(1f), singleLine = true,
                label = { Text("Город") }, shape = RoundedCornerShape(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            Button(onClick = { if (cityInput.isNotBlank()) city = cityInput.trim() },
                shape = RoundedCornerShape(16.dp), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 17.dp)
            ) { Text("Найти") }
        }
        Spacer(Modifier.height(14.dp))
        if (loading && w == null) {
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (w != null) {
            Column(Modifier.fillMaxSize().padding(bottom = 12.dp).verticalScroll(rememberScrollState())) {
                Box(
                    Modifier.fillMaxWidth().background(
                        Brush.verticalGradient(listOf(Color(0xFF5A9CF6), Color(0xFF82C6F7))),
                        RoundedCornerShape(30.dp)
                    ).padding(22.dp)
                ) {
                    Column {
                        Text(w.place, color = Color.White.copy(alpha = .95f), fontSize = 18.sp, fontWeight = FontWeight.Medium)
                        Text(SimpleDateFormat("EEEE, d MMMM", Locale("ru")).format(Date()).replaceFirstChar { it.uppercase() },
                            color = Color.White.copy(alpha = .85f), fontSize = 13.sp)
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(weatherEmoji(w.hours.firstOrNull()?.code ?: 0), fontSize = 54.sp)
                            Spacer(Modifier.width(18.dp))
                            Column {
                                Text("${w.temp}°", fontSize = 58.sp, fontWeight = FontWeight.Light, color = Color.White)
                                Text(w.description, fontSize = 16.sp, color = Color.White)
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        Text("Ощущается как ${w.feels}°", color = Color.White.copy(alpha = .92f))
                        Spacer(Modifier.height(14.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            MiniMetric("Ветер", "${w.wind} км/ч")
                            MiniMetric("Влажность", "${w.humidity}%")
                            MiniMetric("Порывы", "${(w.wind * 1.5).toInt()} км/ч")
                        }
                    }
                }
                Spacer(Modifier.height(18.dp))
                Text("Почасовой прогноз", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF18243A))
                Text("Температура · ветер · осадки", fontSize = 13.sp, color = Color(0xFF65748B))
                Spacer(Modifier.height(10.dp))
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    w.hours.take(24).forEach { hour ->
                        Column(
                            Modifier.width(84.dp).background(Color.White, RoundedCornerShape(22.dp)).padding(vertical = 14.dp, horizontal = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(hour.time, fontSize = 13.sp, color = Color(0xFF65748B))
                            Spacer(Modifier.height(8.dp))
                            Text(weatherEmoji(hour.code), fontSize = 25.sp)
                            Text("${hour.temp}°", fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF18243A))
                            Spacer(Modifier.height(9.dp))
                            Text("↗ ${hour.wind}", fontSize = 13.sp, color = Color(0xFF2878D0))
                            Text("км/ч", fontSize = 10.sp, color = Color(0xFF65748B))
                            Spacer(Modifier.height(7.dp))
                            Text("💧 ${hour.precip}%", fontSize = 12.sp, color = Color(0xFF2878D0))
                        }
                    }
                }
                Spacer(Modifier.height(18.dp))
                Text("На что обратить внимание", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF18243A))
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    InfoCard("Ветер", "${w.wind} км/ч", "Скорость сейчас", Modifier.weight(1f))
                    InfoCard("Влажность", "${w.humidity}%", "Относительная", Modifier.weight(1f))
                }
                Spacer(Modifier.height(10.dp))
                Text("Данные: Open-Meteo · бесплатно, без ключа API", fontSize = 11.sp, color = Color(0xFF7C8798))
                error?.let { Text(it, color = Color.Red, modifier = Modifier.padding(top = 8.dp)) }
            }
        } else {
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Text(error ?: "Загружаю погоду…", color = Color(0xFF65748B))
            }
        }
    }
}

@Composable
private fun MiniMetric(title: String, value: String) {
    Column {
        Text(title, color = Color.White.copy(alpha = .8f), fontSize = 12.sp)
        Text(value, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun InfoCard(title: String, value: String, sub: String, modifier: Modifier = Modifier) {
    Column(modifier.background(Color.White, RoundedCornerShape(22.dp)).padding(16.dp)) {
        Text(title, fontSize = 13.sp, color = Color(0xFF65748B))
        Spacer(Modifier.height(8.dp))
        Text(value, fontSize = 24.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF18243A))
        Text(sub, fontSize = 11.sp, color = Color(0xFF8A95A5))
    }
}

private suspend fun loadWeather(city: String): WeatherData = withContext(Dispatchers.IO) {
    val encoded = URLEncoder.encode(city, "UTF-8")
    val geo = JSONObject(URL("https://geocoding-api.open-meteo.com/v1/search?name=$encoded&count=1&language=ru&format=json").readText())
    val results = geo.optJSONArray("results") ?: throw Exception("City not found")
    if (results.length() == 0) throw Exception("City not found")
    val place = results.getJSONObject(0)
    val lat = place.getDouble("latitude")
    val lon = place.getDouble("longitude")
    val displayName = place.optString("name", city) + (place.optString("country", "").let { if (it.isNotBlank()) ", $it" else "" })
    val url = "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon&current=temperature_2m,relative_humidity_2m,apparent_temperature,weather_code,wind_speed_10m&hourly=temperature_2m,precipitation_probability,weather_code,wind_speed_10m&forecast_days=2&timezone=auto"
    val root = JSONObject(URL(url).readText())
    val current = root.getJSONObject("current")
    val hourly = root.getJSONObject("hourly")
    val times = hourly.getJSONArray("time")
    val temps = hourly.getJSONArray("temperature_2m")
    val winds = hourly.getJSONArray("wind_speed_10m")
    val precips = hourly.getJSONArray("precipitation_probability")
    val codes = hourly.getJSONArray("weather_code")
    val now = current.getString("time").take(13)
    var start = 0
    for (i in 0 until times.length()) {
        if (times.getString(i).startsWith(now)) { start = i; break }
        if (times.getString(i) <= current.getString("time")) start = i
    }
    val hours = (start until minOf(start + 24, times.length())).map { i ->
        Hour(times.getString(i).takeLast(5), temps.getDouble(i).toInt(), winds.getDouble(i).toInt(),
            precips.getInt(i), codes.getInt(i))
    }
    WeatherData(displayName, current.getDouble("temperature_2m").toInt(),
        current.getDouble("apparent_temperature").toInt(), describe(current.getInt("weather_code")),
        current.getDouble("wind_speed_10m").toInt(), current.getInt("relative_humidity_2m"), hours)
}

private fun describe(code: Int): String = when (code) {
    0 -> "Ясно"
    1 -> "Преимущественно ясно"
    2 -> "Переменная облачность"
    3 -> "Пасмурно"
    45, 48 -> "Туман"
    51, 53, 55 -> "Морось"
    56, 57 -> "Ледяная морось"
    61, 63, 65 -> "Дождь"
    66, 67 -> "Ледяной дождь"
    71, 73, 75, 77 -> "Снег"
    80, 81, 82 -> "Ливень"
    85, 86 -> "Снегопад"
    95, 96, 99 -> "Гроза"
    else -> "Погодные условия"
}

private fun weatherEmoji(code: Int): String = when (code) {
    0 -> "☀️"
    1, 2 -> "🌤️"
    3 -> "☁️"
    45, 48 -> "🌫️"
    in 51..67 -> "🌧️"
    in 71..77 -> "🌨️"
    in 80..82 -> "🌦️"
    in 85..86 -> "❄️"
    in 95..99 -> "⛈️"
    else -> "🌤️"
}
