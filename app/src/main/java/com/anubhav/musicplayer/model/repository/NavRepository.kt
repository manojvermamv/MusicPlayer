package com.anubhav.musicplayer.model.repository

import android.util.Log
import com.anubhav.musicplayer.model.models.Contact
import com.anubhav.musicplayer.model.models.Music
import com.anubhav.musicplayer.model.models.User
import com.anubhav.musicplayer.model.retrofit.RetrofitClient
import com.anubhav.musicplayer.model.local.ContentDataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.awaitResponse

class NavRepository(private val source: com.anubhav.musicplayer.model.local.ContentDataSource) {

    private val api = RetrofitClient.apiInterface

    suspend fun fetchContacts(): List<Contact> {
        return withContext(Dispatchers.IO) {
            source.fetchContacts()
        }
    }

    suspend fun fetchMusic(): List<Music> {
        return withContext(Dispatchers.IO) {
            source.fetchMusic()
        }
    }

    suspend fun getUsersApiCall(): List<User> {
        return withContext(Dispatchers.IO) {
            makeApiCall { api.getUsers() } ?: emptyList()
        }
    }

}


suspend inline fun <reified T> makeApiCall(apiCall: () -> Call<T>): T? {
    return try {
        val response = apiCall().awaitResponse()
        if (response.isSuccessful) {
            response.body()
        } else {
            // Handle API error (e.g., non-200 status code)
            Log.e("DEBUG", "API error: ${response.code()} - ${response.message()}")
            null // Or throw an exception or return a default value
        }
    } catch (e: Exception) {
        // Handle network or other exceptions
        Log.e("DEBUG", "Error during API call: ${e.message}")
        null // Or throw an exception or return a default value
    }
}

fun <T> Call<T>.enqueueApiCall(
    onSuccess: (T) -> Unit,
    onFailure: (Throwable) -> Unit = { Log.v("RetrofitClient onFailure: ", it.message.toString()) }
) {
    enqueue(object : Callback<T> {
        override fun onFailure(call: Call<T>, t: Throwable) {
            onFailure(t)
        }

        override fun onResponse(call: Call<T>, response: Response<T>) {
            Log.v("RetrofitClient onResponse: ", response.body().toString())
            if (response.isSuccessful) {
                onSuccess(response.body()!!)
            } else {
                onFailure(Throwable("API call failed with status code ${response.code()}"))
            }
        }
    })
}