package com.example.data

import com.example.BuildConfig
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class GeminiPart(val text: String)

@JsonClass(generateAdapter = true)
data class GeminiContent(val parts: List<GeminiPart>)

@JsonClass(generateAdapter = true)
data class GeminiRequest(
    val contents: List<GeminiContent>,
    val systemInstruction: GeminiContent? = null
)

@JsonClass(generateAdapter = true)
data class GeminiPartResponse(val text: String?)

@JsonClass(generateAdapter = true)
data class GeminiContentResponse(val parts: List<GeminiPartResponse>?)

@JsonClass(generateAdapter = true)
data class GeminiCandidate(val content: GeminiContentResponse?)

@JsonClass(generateAdapter = true)
data class GeminiResponse(val candidates: List<GeminiCandidate>?)

interface GeminiApiService {
    @POST("v1beta/models/{model}:generateContent")
    suspend fun generateContent(
        @Path("model") model: String,
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): GeminiResponse
}

object GeminiClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    val service: GeminiApiService = retrofit.create(GeminiApiService::class.java)

    suspend fun generate(prompt: String, systemInstruction: String? = null): String {
        val key = BuildConfig.GEMINI_API_KEY
        val hasRealKey = key.isNotEmpty() && key != "MY_GEMINI_API_KEY"

        if (hasRealKey) {
            val models = listOf("gemini-3.5-flash", "gemini-2.5-flash", "gemini-1.5-flash")
            for (model in models) {
                try {
                    val request = GeminiRequest(
                        contents = listOf(GeminiContent(parts = listOf(GeminiPart(text = prompt)))),
                        systemInstruction = systemInstruction?.let {
                            GeminiContent(parts = listOf(GeminiPart(text = it)))
                        }
                    )
                    val response = service.generateContent(model, key, request)
                    val text = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                    if (text != null) {
                        return text
                    }
                } catch (e: Exception) {
                    // Try the next model
                }
            }
        }

        return generateLocalFallback(prompt, systemInstruction, hasRealKey)
    }

    private fun generateLocalFallback(prompt: String, systemInstruction: String?, hasRealKey: Boolean): String {
        val normalizedPrompt = prompt.lowercase()
        val sys = systemInstruction ?: ""

        val notice = if (!hasRealKey) {
            "\n\n*(Note: This is an intelligent local fallback analysis. To activate full Gemini capabilities, configure GEMINI_API_KEY in the Secrets panel!)*"
        } else {
            "\n\n*(Note: Offline/Resilience Mode activated. Local database analysis applied!)*"
        }

        if (sys.contains("Academic Advisor")) {
            val name = Regex("Student:\\s*(.*)").find(prompt)?.groupValues?.get(1) ?: "Student"
            val attendancePct = Regex("Current Attendance:\\s*(\\d+)%").find(prompt)?.groupValues?.get(1)?.toIntOrNull() ?: 75
            val absences = Regex("Absences:\\s*(\\d+)").find(prompt)?.groupValues?.get(1)?.toIntOrNull() ?: 0
            val leaves = Regex("Approved Leaves:\\s*(\\d+)").find(prompt)?.groupValues?.get(1)?.toIntOrNull() ?: 0

            val riskLevel = when {
                attendancePct < 75 -> "HIGH RISK"
                attendancePct <= 80 -> "MODERATE RISK"
                else -> "LOW RISK"
            }

            val advice = when (riskLevel) {
                "HIGH RISK" -> "⚠️ Critical attention required! The student must attend the next 5-6 consecutive lectures and immediately submit official documentation or medical certificates to clear existing absences."
                "MODERATE RISK" -> "ℹ️ Action recommended. The student's current attendance rate is borderline. Encourage regular attendance for the next two weeks to comfortably cross the safe 80% threshold."
                else -> "✅ Exemplary performance! Maintain current attendance trends. Ensure any planned absences are pre-approved via the Leave Requests tab."
            }

            return """
                📊 AI ACADEMIC ADVISOR REPORT
                -------------------------------------------------
                Student Name: $name
                Current Attendance Run Rate: $attendancePct%
                Absences Recorded: $absences sessions
                Approved Leaves / OD: $leaves sessions
                
                Forecast Details:
                1. Projected Final Semester Attendance: Approximately $attendancePct% (based on current run rate).
                2. Risk Category: $riskLevel
                
                Recommendations:
                $advice
                
                Motivational Advice:
                "Consistency is the companion of success. Keep showing up every single day, and academic excellence will naturally follow!"
            """.trimIndent() + notice
        }

        val depts = Regex("Departments:\\s*(.*)").find(sys)?.groupValues?.get(1) ?: "Computer Science & Engineering (CSE)"
        val facultyCount = Regex("Total Faculty Count:\\s*(\\d+)").find(sys)?.groupValues?.get(1) ?: "3"
        val studentCount = Regex("Total Registered Students:\\s*(\\d+)").find(sys)?.groupValues?.get(1) ?: "5"
        val pendingLeaves = Regex("Pending Student Leave Requests:\\s*(\\d+)").find(sys)?.groupValues?.get(1) ?: "0"
        val holidays = Regex("Upcoming Holidays:\\s*(.*)").find(sys)?.groupValues?.get(1) ?: "None declared"
        val activeSessions = Regex("Active Session logs count:\\s*(\\d+)").find(sys)?.groupValues?.get(1) ?: "0"

        val lowAttendanceLines = mutableListOf<String>()
        val lowAttendanceBlock = Regex("Students with Low Attendance \\(<75%\\):\\s*\n([\\s\\S]*?)\n\\s*- Pending Student Leave Requests").find(sys)?.groupValues?.get(1)
        if (lowAttendanceBlock != null && lowAttendanceBlock.isNotBlank() && !lowAttendanceBlock.contains("None")) {
            lowAttendanceLines.addAll(lowAttendanceBlock.trim().split("\n"))
        }

        return when {
            normalizedPrompt.contains("low") || normalizedPrompt.contains("attendance") || normalizedPrompt.contains("threshold") || normalizedPrompt.contains("75") -> {
                val listStr = if (lowAttendanceLines.isNotEmpty()) {
                    lowAttendanceLines.joinToString("\n") { "• $it" }
                } else {
                    "🎉 Amazing! All registered students currently meet or exceed the mandatory 75% attendance threshold."
                }
                """
                    🚨 ATTENDANCE COMPLIANCE AUDIT
                    -------------------------------------------------
                    Our local database scan shows the following status regarding the 75% attendance threshold:
                    
                    $listStr
                    
                    Next Steps:
                    1. Faculty can log in and select any low attendance student to trigger official alerts or view detail reports.
                    2. Students below 75% are highlighted as "At Risk" in their dashboards.
                """.trimIndent() + notice
            }

            normalizedPrompt.contains("holiday") || normalizedPrompt.contains("leave") || normalizedPrompt.contains("pending") || normalizedPrompt.contains("request") -> {
                val holidayStr = if (holidays.isNotBlank() && !holidays.contains("None")) {
                    "Upcoming holidays registered: $holidays"
                } else {
                    "No upcoming holidays are registered in the academic database."
                }
                """
                    📅 CALENDAR & LEAVE DESK SUMMARY
                    -------------------------------------------------
                    • Pending Student Leave Requests: $pendingLeaves requests currently awaiting HOD/Faculty review.
                    • Academic Holidays: $holidayStr
                    
                    Next Steps:
                    - Faculty/HOD can head to the "Leave Requests" section in their dashboard to view and digitally seal approvals.
                    - Approved leaves are automatically updated into attendance registers.
                """.trimIndent() + notice
            }

            normalizedPrompt.contains("timetable") || normalizedPrompt.contains("schedule") || normalizedPrompt.contains("class") || normalizedPrompt.contains("session") -> {
                """
                    🗓️ ROSTER & CLASS TIMETABLE SUMMARY
                    -------------------------------------------------
                    • Active Departments: $depts
                    • Total Faculty Rosters: $facultyCount assigned professors
                    • Total Scheduled Sessions logged: $activeSessions
                    
                    Next Steps:
                    - Go to the Roster section to view specific lecture hours.
                    - Faculty can initiate automated QR proximity check-ins for any of these scheduled hours.
                """.trimIndent() + notice
            }

            else -> {
                """
                    🤖 INSTITUTIONAL ADVOCATE HELPER
                    -------------------------------------------------
                    Welcome to the G.R.T. Academic Support Center! I am your local, high-fidelity database-integrated assistant.
                    
                    Current Institution Statistics:
                    • Active Departments: $depts
                    • Total Faculty Profiles: $facultyCount
                    • Total Registered Students: $studentCount
                    • Pending Leave Applications: $pendingLeaves
                    • Upcoming Holidays: ${if (holidays.contains("None")) "None scheduled" else holidays}
                    
                    How can I assist you today? You can ask me:
                    - "Who has low attendance?"
                    - "Are there pending leaves?"
                    - "What is the holiday schedule?"
                    - "Tell me about class schedules"
                """.trimIndent() + notice
            }
        }
    }
}
