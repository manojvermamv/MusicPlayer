package com.anubhav.musicplayer.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.anubhav.musicplayer.R
import com.anubhav.musicplayer.model.models.Music
import com.anubhav.musicplayer.ui.activities.MainActivity
import kotlin.random.Random

class MediaPlayerService : Service(), MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener, MediaPlayer.OnCompletionListener {

    private lateinit var mediaPlayer: MediaPlayer
    private lateinit var songs: ArrayList<Music>

    private var shuffle = false
    private var songPosition = 0
    private val musicBinder: IBinder = MusicBinder(this)
    private val random: Random by lazy { Random.Default }

    private var currentSong: Music? = null
    private val notificationId = 110
    private val channelId = "my_channel_id"

    override fun onCreate() {
        super.onCreate()
        songPosition = 0
        mediaPlayer = MediaPlayer()
        initMusicPlayer()
    }

    override fun onDestroy() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            stopForeground(true)
        }
    }

    override fun onBind(intent: Intent?): IBinder {
        return musicBinder
    }

    override fun onUnbind(intent: Intent?): Boolean {
        mediaPlayer.stop()
        mediaPlayer.release()
        return false
    }

    override fun onPrepared(player: MediaPlayer?) {
        player?.start()

        fun createNotificationChannel(channelId: String, channelName: String): String {
            lateinit var notificationChannel: NotificationChannel
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                notificationChannel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT)
                notificationChannel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                manager.createNotificationChannel(notificationChannel)
            }
            return channelId
        }
        val pendingIntent: PendingIntent = Intent(this, MainActivity::class.java).let { intent ->
                PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        }
        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .setContentTitle("Playing")
            .setContentText(currentSong?.name ?: "")
            .setContentIntent(pendingIntent)
            .setTicker(currentSong?.name ?: "")
            .build()
        createNotificationChannel(channelId, getString(R.string.app_name))
        startForeground(notificationId, notification)
    }

    override fun onError(mp: MediaPlayer?, p1: Int, p2: Int): Boolean {
        mp?.reset()
        return false
    }

    override fun onCompletion(mp: MediaPlayer?) {
        if (mediaPlayer.currentPosition > 0) {
            mp?.reset()
            playNext()
        }
    }

    private fun initMusicPlayer() {
        mediaPlayer.setWakeMode(applicationContext, PowerManager.PARTIAL_WAKE_LOCK)
        val audioAttributes = AudioAttributes.Builder()
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .build()
        mediaPlayer.setAudioAttributes(audioAttributes)
        mediaPlayer.setOnPreparedListener(this)
        mediaPlayer.setOnCompletionListener(this)
        mediaPlayer.setOnErrorListener(this)
    }

    fun getPosition(): Int {
        return mediaPlayer.currentPosition
    }
    fun getDuration(): Int {
        return mediaPlayer.duration
    }
    fun isPlaying(): Boolean {
        return mediaPlayer.isPlaying
    }
    fun pausePlayer() {
        mediaPlayer.pause()
    }
    fun seek(position: Int) {
        mediaPlayer.seekTo(position)
    }
    fun go() {
        mediaPlayer.start()
    }

    fun playSong() {
        mediaPlayer.reset()
        currentSong = songs[songPosition]
        val musicUri = currentSong?.getMusicUri() ?: return
        try {
            mediaPlayer.setDataSource(applicationContext, musicUri)
        } catch (e: Exception) {
            Log.e("MUSIC SERVICE", "Error setting data source", e)
        }
        mediaPlayer.prepare()
    }

    fun playPrev() {
        songPosition--
        if (songPosition < 0) songPosition = songs.size - 1
        playSong()
    }

    fun playNext() {
        if (shuffle) {
            var newSong = songPosition
            while (newSong == songPosition) {
                newSong = random.nextInt(songs.size)
            }
            songPosition = newSong
        } else {
            songPosition++
            if (songPosition >= songs.size) songPosition = 0
        }
        playSong()
    }

    fun setShuffle() {
        shuffle = !shuffle
    }

    fun setList(music: ArrayList<Music>) {
        songs = music
    }

    fun setSong(songIndex: Int) {
        songPosition = songIndex
    }

    class MusicBinder(private val service: MediaPlayerService) : Binder() {
        val getService: MediaPlayerService
            get() = service
    }

}