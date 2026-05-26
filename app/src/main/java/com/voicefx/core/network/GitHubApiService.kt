package com.voicefx.core.network

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GitHubApiService @Inject constructor() {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .build()

    private val apiBase = "https://api.github.com"
    private var token: String = ""
    private var owner: String = ""
    private var repo: String = "voicefx"

    fun configure(token: String, owner: String, repo: String) {
        this.token = token
        this.owner = owner
        this.repo = repo
    }

    fun isConfigured(): Boolean = token.isNotBlank() && owner.isNotBlank()

    private fun authHeaders(): Map<String, String> {
        return mapOf(
            "Authorization" to "Bearer $token",
            "Accept" to "application/vnd.github.v3+json",
            "User-Agent" to "VoiceFX-Android"
        )
    }

    data class Release(
        val id: Long,
        val uploadUrl: String,
        val htmlUrl: String
    )

    data class Asset(
        val id: Long,
        val name: String,
        val downloadUrl: String,
        val size: Long
    )

    data class WorkflowDispatchResult(
        val success: Boolean,
        val message: String = ""
    )

    fun createDraftRelease(tag: String, name: String): Result<Release> {
        return try {
            val json = JSONObject().apply {
                put("tag_name", tag)
                put("name", name)
                put("draft", true)
                put("generate_release_notes", false)
            }
            val url = "$apiBase/repos/$owner/$repo/releases"
            val response = executePost(url, json.toString())
            val body = JSONObject(response)
            Result.success(Release(
                id = body.getLong("id"),
                uploadUrl = body.getString("upload_url"),
                htmlUrl = body.getString("html_url")
            ))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun uploadAsset(uploadUrl: String, fileName: String, data: ByteArray, mimeType: String): Result<Asset> {
        return try {
            val url = uploadUrl.replace("{?name,label}", "?name=$fileName")
            val mediaType = mimeType.toMediaType()
            val body = data.toRequestBody(mediaType)
            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $token")
                .addHeader("Accept", "application/vnd.github.v3+json")
                .addHeader("User-Agent", "VoiceFX-Android")
                .post(body)
                .build()
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: throw IOException("Empty response")
            if (!response.isSuccessful) {
                throw IOException("Upload failed: $responseBody")
            }
            val json = JSONObject(responseBody)
            Result.success(Asset(
                id = json.getLong("id"),
                name = json.getString("name"),
                downloadUrl = json.getString("browser_download_url"),
                size = json.getLong("size")
            ))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun triggerWorkflow(eventType: String, payload: Map<String, Any>): Result<WorkflowDispatchResult> {
        return try {
            val jsonPayload = JSONObject()
            val clientPayload = JSONObject()
            payload.forEach { (key, value) ->
                when (value) {
                    is String -> clientPayload.put(key, value)
                    is Number -> clientPayload.put(key, value)
                    is Boolean -> clientPayload.put(key, value)
                }
            }
            jsonPayload.put("event_type", eventType)
            jsonPayload.put("client_payload", clientPayload)

            val url = "$apiBase/repos/$owner/$repo/dispatches"
            executePost(url, jsonPayload.toString())
            Result.success(WorkflowDispatchResult(true))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getReleaseAssets(releaseId: Long): Result<List<Asset>> {
        return try {
            val url = "$apiBase/repos/$owner/$repo/releases/$releaseId/assets"
            val response = executeGet(url)
            val jsonArray = JSONArray(response)
            val assets = mutableListOf<Asset>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                assets.add(Asset(
                    id = obj.getLong("id"),
                    name = obj.getString("name"),
                    downloadUrl = obj.getString("browser_download_url"),
                    size = obj.getLong("size")
                ))
            }
            Result.success(assets)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun downloadAsset(downloadUrl: String): Result<ByteArray> {
        return try {
            val request = Request.Builder()
                .url(downloadUrl)
                .addHeader("Authorization", "Bearer $token")
                .addHeader("Accept", "application/octet-stream")
                .addHeader("User-Agent", "VoiceFX-Android")
                .get()
                .build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                throw IOException("Download failed: ${response.code}")
            }
            Result.success(response.body?.bytes() ?: throw IOException("Empty body"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun deleteRelease(releaseId: Long): Result<Unit> {
        return try {
            val url = "$apiBase/repos/$owner/$repo/releases/$releaseId"
            executeDelete(url)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun deleteAsset(assetId: Long): Result<Unit> {
        return try {
            val url = "$apiBase/repos/$owner/$repo/releases/assets/$assetId"
            executeDelete(url)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getWorkflowRunStatus(sessionId: String): Result<String> {
        return try {
            val url = "$apiBase/repos/$owner/$repo/actions/runs?event=repository_dispatch&per_page=5"
            val response = executeGet(url)
            val json = JSONObject(response)
            val runs = json.getJSONArray("workflow_runs")
            for (i in 0 until runs.length()) {
                val run = runs.getJSONObject(i)
                val payload = run.optJSONObject("display_title") ?: continue
                if (payload.optString("session_id") == sessionId) {
                    return Result.success(run.getString("conclusion").ifEmpty { "pending" })
                }
            }
            Result.success("unknown")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun executePost(url: String, jsonBody: String): String {
        val mediaType = "application/json".toMediaType()
        val body = jsonBody.toRequestBody(mediaType)
        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $token")
            .addHeader("Accept", "application/vnd.github.v3+json")
            .addHeader("User-Agent", "VoiceFX-Android")
            .post(body)
            .build()
        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: throw IOException("Empty response")
        if (!response.isSuccessful) {
            throw IOException("POST $url failed (${response.code}): $responseBody")
        }
        return responseBody
    }

    private fun executeGet(url: String): String {
        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $token")
            .addHeader("Accept", "application/vnd.github.v3+json")
            .addHeader("User-Agent", "VoiceFX-Android")
            .get()
            .build()
        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: throw IOException("Empty response")
        if (!response.isSuccessful) {
            throw IOException("GET $url failed (${response.code}): $responseBody")
        }
        return responseBody
    }

    private fun executeDelete(url: String) {
        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $token")
            .addHeader("Accept", "application/vnd.github.v3+json")
            .addHeader("User-Agent", "VoiceFX-Android")
            .delete()
            .build()
        val response = client.newCall(request).execute()
        if (!response.isSuccessful) {
            val body = response.body?.string() ?: ""
            throw IOException("DELETE $url failed (${response.code}): $body")
        }
    }
}
