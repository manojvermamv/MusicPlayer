package com.anubhav.musicplayer.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.liveData
import androidx.lifecycle.switchMap
import com.anubhav.musicplayer.model.models.MusicAction
import com.anubhav.musicplayer.utils.SingleEvent
import com.anubhav.musicplayer.model.repository.NavRepository

class MainActivityViewModel(
    context: Application,
    private val navRepository: NavRepository,
) : AndroidViewModel(context) {

    private val _contactsRefreshTrigger = MutableLiveData<Unit>()
    private val _musicRefreshTrigger = MutableLiveData<Unit>()

    /**
     * LiveData functionality
     * */
    val contactsLiveData = _contactsRefreshTrigger.switchMap {
        liveData { emit(navRepository.fetchContacts()) }
    }

    val musicLiveData = _musicRefreshTrigger.switchMap {
        liveData { emit(navRepository.fetchMusic()) }
    }

    /**
     * Refresh functionality
     * */
    fun refreshContacts() {
        _contactsRefreshTrigger.value = Unit
    }

    fun refreshMusic() {
        _musicRefreshTrigger.value = Unit
    }

    /**
     * Fetch data from api request
     * */
    var getUsers = liveData {
        emit(navRepository.getUsersApiCall())
    }

    /**
     * UI actions as event, user action is single one time event, Shouldn't be multiple time consumption
     */

    private val triggerProgressBarPrivate = MutableLiveData<SingleEvent<Boolean>>()
    val triggerProgressBar: LiveData<SingleEvent<Boolean>> get() = triggerProgressBarPrivate

    fun triggerProgressBar(show: Boolean) {
        triggerProgressBarPrivate.value = SingleEvent(show)
    }

    private val triggerMusicActionPrivate = MutableLiveData<SingleEvent<MusicAction>>()
    val triggerMusicAction: LiveData<SingleEvent<MusicAction>> get() = triggerMusicActionPrivate

    fun triggerMusicAction(action: MusicAction) {
        triggerMusicActionPrivate.value = SingleEvent(action)
    }

}