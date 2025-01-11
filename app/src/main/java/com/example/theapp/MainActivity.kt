package com.example.theapp

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private lateinit var sharedPreferences: SharedPreferences

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        sharedPreferences = getSharedPreferences("Metro", MODE_PRIVATE)

        val metroStationsLine1 = arrayOf(
            "El Marg Elgadidah", "El Marg", "Ezbet El Nakhl", "Ain Shams", "El Matareyya", "Helmeyet El Zaitoun",
            "Hadayeq El Zaitoun", "Saray El Qobba", "Hammamat El Qobba", "Kobri El Qobba", "Manshiet El Sadr",
            "El Demerdash", "Ghamra", "El Shohadaa", "Orabi", "Nasser", "Sadat", "Saad Zaghloul",
            "El Sayeda Zeinab", "El Malek El Saleh", "Mar Girgis", "El Zahraa", "Dar El Salam",
            "Hadayeq El Maadi", "Maadi", "Sakanat El Maadi", "Tora El Balad", "Kozzika", "Tora El Asmant",
            "El Maasara", "Hadayeq Helwan", "Wadi Hof", "Helwan University", "Ain Helwan", "Helwan"
        )
        val metroStationsLine2 = arrayOf(
            "El Moneeb", "Sakiat Mekki", "Omm El Misryeen", "El Giza", "Faisal", "Cairo University",
            "Bohooth", "Dokki", "Opera", "Sadat", "Mohamed Naguib", "Attaba", "El Shohadaa", "Massara",
            "Rod El Farag", "St. Teresa", "Khalafawy", "Mezallat", "Koliet El Zeraa", "Shubra El Kheima"
        )
        val metroStationsLine3 = arrayOf(
            "Adly Mansour", "El Haykestep", "Omar Ibn El Khattab", "Qobaa", "Hesham Barakat", "El Nozha",
            "Nadi El Shams", "Alf Maskan", "Heliopolis", "Haroun", "Al Ahram", "Koleyet El Banat",
            "Stadium", "Fair Zone", "Abbassia", "Abdou Pasha", "El Geish", "Bab El Shaaria", "Attaba",
            "Nasser", "Maspero", "Safaa Hijazy", "Kit Kat", "Sudan", "Imbaba", "El Bohy", "El Qawmia",
            "Ring Road", "Rod El Farag", "Tawfikia", "Wadi El Nile", "Gamet El Dowel", "Boulak El Dakrour"
        )
        val allStations = arrayOf("please select ") + (metroStationsLine1 + metroStationsLine2 + metroStationsLine3).distinct()
        val graph = buildMetroGraph(listOf(metroStationsLine1, metroStationsLine2, metroStationsLine3))
        val lines = listOf(metroStationsLine1, metroStationsLine2, metroStationsLine3)
        val spinnerStart = findViewById<Spinner>(R.id.spinnerStart)
        val spinnerDestination = findViewById<Spinner>(R.id.spinnerDestination)
        val buttonRoute = findViewById<Button>(R.id.buttonRoute)
        val textViewResult = findViewById<TextView>(R.id.textViewResult)
        val timeTextView = findViewById<TextView>(R.id.timeTextView)
        val priceTextView = findViewById<TextView>(R.id.priceTextView)
        val directionTextView = findViewById<TextView>(R.id.directionTextView)
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, allStations)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerStart.adapter = adapter
        spinnerDestination.adapter = adapter
        buttonRoute.isEnabled = false

        val updateButtonState = {
            buttonRoute.isEnabled = spinnerStart.selectedItemPosition > 0 &&
                    spinnerDestination.selectedItemPosition > 0
        }
        fun createOnItemSelectedListener(action: () -> Unit) = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) = action()
            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }

        spinnerStart.onItemSelectedListener = createOnItemSelectedListener(updateButtonState)
        spinnerDestination.onItemSelectedListener = createOnItemSelectedListener(updateButtonState)


        buttonRoute.setOnClickListener {
            val startStation = spinnerStart.selectedItem?.toString()
            val endStation = spinnerDestination.selectedItem?.toString()

            when {
                startStation == null || endStation == null -> {
                    textViewResult.text = "Please select start and destination stations."
                    timeTextView.text = "Time: 0"
                    priceTextView.text = "Price: nothing"
                    directionTextView.text = ""
                    buttonRoute.isEnabled = false
                }
                startStation == endStation -> {
                    textViewResult.text = "You are in this station, don't go anywhere!"
                    timeTextView.text = "Time: 0"
                    priceTextView.text = "Price: nothing"
                    directionTextView.text = ""
                    buttonRoute.isEnabled = true
                }
                else -> {
                    val shortestRoute = findShortestPathBFS(graph, startStation, endStation)
                    if (shortestRoute.isEmpty()) {
                        textViewResult.text = "No route found between $startStation and $endStation."
                        timeTextView.text = ""
                        priceTextView.text = ""
                        directionTextView.text = ""
                    } else {
                        val stationCount = shortestRoute.size - 1
                        val timeTaken = stationCount * 2
                        val ticketPrice = when (stationCount) {
                            in 1..9 -> 8
                            in 10..16 -> 10
                            in 17..23 -> 15
                            else -> 20
                        }
                        val directions = determineDirections(lines, shortestRoute)

                        textViewResult.text = "Route:         ${shortestRoute.joinToString(" -> ")}"
                        timeTextView.text = "Time: $timeTaken minutes"
                        priceTextView.text = "Price: $ticketPrice EGP"
                        directionTextView.text = "Directions: $directions"


                        saveRouteToPreferences(startStation, endStation, shortestRoute, directions, timeTaken, ticketPrice)
                    }
                    buttonRoute.isEnabled = true
                }
            }
        }
    }

    private fun buildMetroGraph(lines: List<Array<String>>): Map<String, List<String>> {
        val graph = mutableMapOf<String, MutableList<String>>()
        for (line in lines) {
            for (i in line.indices) {
                graph.putIfAbsent(line[i], mutableListOf())
                if (i > 0) graph[line[i]]!!.add(line[i - 1])
                if (i < line.size - 1) graph[line[i]]!!.add(line[i + 1])
            }
        }
        return graph
    }

    private fun findShortestPathBFS(graph: Map<String, List<String>>, start: String, end: String): List<String> {
        val queue = ArrayDeque<List<String>>()
        val visited = mutableSetOf<String>()
        queue.add(listOf(start))

        while (queue.isNotEmpty()) {
            val path = queue.removeFirst()
            val station = path.last()

            if (station == end) return path

            if (station !in visited) {
                visited.add(station)
                graph[station]?.forEach { neighbor -> queue.add(path + neighbor) }
            }
        }
        return emptyList()
    }

    private fun determineDirections(lines: List<Array<String>>, route: List<String>): String {
        val steps = mutableListOf<String>()

        var currentLine: Array<String>? = null
        var previousStation: String? = null
        var startStation = route.first()

        for (i in route.indices) {
            val station = route[i]
            val line = lines.find { station in it }

            if (!line.contentEquals(currentLine)) {
                if (currentLine != null) {
                    val currentLineName = "${currentLine.first()} - ${currentLine.last()}"
                    steps.add("Take $currentLineName line from $startStation to $previousStation.")
                }
                currentLine = line
                startStation = station
            }
            previousStation = station
        }

        if (currentLine != null ) {
            val currentLineName = "${currentLine.first()} - ${currentLine.last()}"
            steps.add("Take $currentLineName line from $startStation to $previousStation.")
        }

        return steps.joinToString("\n")
    }


    private fun saveRouteToPreferences(startStation: String?, endStation: String?, route: List<String>, directions: String, time: Int, price: Int) {
        val editor = sharedPreferences.edit()
        editor.putString("start_station", startStation)
        editor.putString("end_station", endStation)
        editor.putString("route", route.joinToString(" -> "))
        editor.putString("directions", directions)
        editor.putInt("time", time)
        editor.putInt("price", price)
        editor.apply()
    }
}

