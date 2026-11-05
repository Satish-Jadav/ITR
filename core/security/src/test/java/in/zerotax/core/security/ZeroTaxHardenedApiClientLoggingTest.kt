package `in`.zerotax.core.security

import `in`.zerotax.core.security.network.ApiDiagnosticRecord
import `in`.zerotax.core.security.network.ZeroTaxHardenedApiClient
import okhttp3.Call
import okhttp3.EventListener
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.net.UnknownHostException
import javax.net.ssl.SSLPeerUnverifiedException

class ZeroTaxHardenedApiClientLoggingTest {

    private lateinit var mockServer: MockWebServer

    @BeforeEach
    fun setUp() {
        mockServer = MockWebServer()
        mockServer.start()
        ZeroTaxHardenedApiClient.clearDiagnostics()
    }

    @AfterEach
    fun tearDown() {
        mockServer.shutdown()
        ZeroTaxHardenedApiClient.clearDiagnostics()
    }

    @Test
    fun testSuccessfulApiCallRecordsDiagnostics() {
        mockServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{"status":"healthy","ay":"2026-27"}""")
        )

        val serverUrl = mockServer.url("/api/v1/compliance/portal-health").toString()
        ZeroTaxHardenedApiClient.apiBaseUrl = mockServer.url("/").toString().removeSuffix("/")

        val client = ZeroTaxHardenedApiClient.createHardenedOkHttpClient()
        val request = Request.Builder().url(serverUrl).get().build()

        client.newCall(request).execute().use { response ->
            assertTrue(response.isSuccessful)
            assertEquals(200, response.code)
        }

        val records = ZeroTaxHardenedApiClient.recentDiagnostics
        assertTrue(records.isNotEmpty())
        val last = records.last()
        assertEquals("GET", last.method)
        assertEquals(200, last.httpCode)
        assertTrue(last.isSuccessful)
        assertNull(last.errorClass)
    }

    @Test
    fun testHttp404RecordsDiagnosticHint() {
        mockServer.enqueue(
            MockResponse()
                .setResponseCode(404)
                .setBody("""{"error":"Resource not found"}""")
        )

        val serverUrl = mockServer.url("/api/v1/nonexistent").toString()
        ZeroTaxHardenedApiClient.apiBaseUrl = mockServer.url("/").toString().removeSuffix("/")

        val client = ZeroTaxHardenedApiClient.createHardenedOkHttpClient()
        val request = Request.Builder().url(serverUrl).get().build()

        client.newCall(request).execute().use { response ->
            assertFalse(response.isSuccessful)
            assertEquals(404, response.code)
        }

        val lastFailure = ZeroTaxHardenedApiClient.getLastFailure()
        assertNotNull(lastFailure)
        assertEquals(404, lastFailure?.httpCode)
        assertFalse(lastFailure!!.isSuccessful)
        assertTrue(lastFailure.diagnosticHint!!.contains("Not Found"))
    }

    @Test
    fun testHttp500RecordsDiagnosticHint() {
        mockServer.enqueue(
            MockResponse()
                .setResponseCode(500)
                .setBody("""{"error":"Internal server crash"}""")
        )

        val serverUrl = mockServer.url("/api/v1/crash").toString()
        ZeroTaxHardenedApiClient.apiBaseUrl = mockServer.url("/").toString().removeSuffix("/")

        val client = ZeroTaxHardenedApiClient.createHardenedOkHttpClient()
        val request = Request.Builder().url(serverUrl).get().build()

        client.newCall(request).execute().use { response ->
            assertFalse(response.isSuccessful)
            assertEquals(500, response.code)
        }

        val lastFailure = ZeroTaxHardenedApiClient.getLastFailure()
        assertNotNull(lastFailure)
        assertEquals(500, lastFailure?.httpCode)
        assertFalse(lastFailure!!.isSuccessful)
        assertTrue(lastFailure.diagnosticHint!!.contains("Internal Server Error"))
    }

    @Test
    fun testManualDiagnosticRecording() {
        val customRecord = ApiDiagnosticRecord(
            method = "GET",
            url = "https://api.zerotax.in/api/v1/statutory/timeline",
            httpCode = null,
            durationMs = 45L,
            isSuccessful = false,
            errorClass = "UnknownHostException",
            errorMessage = "Unable to resolve host api.zerotax.in",
            diagnosticHint = "DNS lookup failed for 'api.zerotax.in'. The domain does not resolve or the device is offline."
        )

        ZeroTaxHardenedApiClient.recordDiagnostic(customRecord)

        val failure = ZeroTaxHardenedApiClient.getLastFailure()
        assertNotNull(failure)
        assertEquals("UnknownHostException", failure?.errorClass)
        assertTrue(failure!!.diagnosticHint!!.contains("DNS lookup failed"))
    }
}
