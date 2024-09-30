package com.anubhav.musicplayer.model.retrofit

import com.anubhav.musicplayer.model.models.User
import retrofit2.Call
import retrofit2.http.GET

interface ApiInterface {

    @GET("users")
    fun getUsers(): Call<List<User>>

}