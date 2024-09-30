package com.anubhav.musicplayer.ui.fragments

import android.content.ComponentName
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.view.View
import android.widget.SeekBar
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.anubhav.musicplayer.R
import com.anubhav.musicplayer.databinding.FragmentMusicPlayerBinding
import com.anubhav.musicplayer.ui.common.BaseFragment
import com.anubhav.musicplayer.viewmodel.MainActivityViewModel
import com.anubhav.musicplayer.model.models.Music
import com.anubhav.musicplayer.service.PlaybackService
import com.anubhav.musicplayer.service.PlaybackService.Companion.SEEK_BACKWARD
import com.anubhav.musicplayer.service.PlaybackService.Companion.SEEK_FORWARD
import com.anubhav.musicplayer.utils.showToast
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.squareup.picasso.Picasso
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.activityViewModel
import java.util.Locale

class MusicPlayerFragment : BaseFragment<FragmentMusicPlayerBinding>(FragmentMusicPlayerBinding::inflate) {

    private val viewModel by activityViewModel<MainActivityViewModel>()

    private val data by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arguments?.getParcelable("data", Music::class.java)
        } else {
            arguments?.getParcelable("data") as? Music
        }
    }

    private lateinit var controller: MediaController
    private var mediaControllerFuture: ListenableFuture<MediaController>? = null

    private var duration: Int = 0

    // Elapsed Time
    private var stopwatchStartTime: Long = 0
    private var elapsedTime: Long = 0
    private var isStopwatchRunning: Boolean = false
    private var currentSongPosition: Long = 0L

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setSupportActionBar(binding.toolbar)
        setTitle(getString(R.string.app_name), data?.name)
        setDisplayHomeAsUpEnabled()

        initializeMediaController()
        setupUIControls()
    }

    private fun initializeMediaController() {
        val sessionToken = SessionToken(requireContext(), ComponentName(requireContext(), PlaybackService::class.java))
        mediaControllerFuture = MediaController.Builder(requireContext(), sessionToken).buildAsync()
        mediaControllerFuture?.apply {
            addListener(Runnable {
                controller = get()
                if (controller.isConnected) controller.stop()
                updateUIWithMediaController(controller)
                // Ensure media is played appropriately based on state
                log("INITIAL STATE = ${controller.playbackState}")
                handlePlaybackBasedOnState()
            }, MoreExecutors.directExecutor())
        }
    }

    private fun handlePlaybackBasedOnState() {
        if (controller.playbackState == Player.STATE_IDLE || controller.playbackState == Player.STATE_ENDED) {
            playMedia()
        } else if (controller.playbackState == Player.STATE_READY || controller.playbackState == Player.STATE_BUFFERING) {
            updateUIWithPlayback()
        }
    }

    private fun updateUIWithPlayback() {
        hideBuffering()
        updatePlayPauseButton(controller.playWhenReady)
        if (controller.playWhenReady) {
            val currentPosition = controller.currentPosition.toInt() / 1000
            binding.seekbar.progress = currentPosition
            binding.time.text = getTimeString(currentPosition)
            binding.duration.text = getTimeString(controller.duration.toInt() / 1000)
        }
    }

    private fun playMedia() {
        val music = data ?: return
        val mediaItem = MediaItem.Builder()
            .setMediaId(music.id.toString())
            .setMediaMetadata(createMediaMetadata(music.name, music.artist, music.artist, music.coverUri))
            .setRequestMetadata(createRequestMetadata(music.getMusicUri()))
            .build()

        /*val musicUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-4.mp3"
        val coverUrl = "https://i.pinimg.com/736x/4b/02/1f/4b021f002b90ab163ef41aaaaa17c7a4.jpg"
        val mediaItem = MediaItem.Builder()
            .setMediaId(musicUrl)
            .setMediaMetadata(createMediaMetadata("SoundHelix-Song-4", "", "SoundHelix", coverUrl.toUri()))
            .setRequestMetadata(createRequestMetadata(musicUrl.toUri()))
            .build()*/

        controller.setMediaItem(mediaItem)
        controller.prepare()
        controller.play()
    }

    private fun createRequestMetadata(musicUri: Uri): MediaItem.RequestMetadata {
        return MediaItem.RequestMetadata.Builder()
            .setMediaUri(musicUri)
            .build()
    }

    private fun createMediaMetadata(title: String, artist: String, album: String, coverUri: Uri? = null): MediaMetadata {
        return MediaMetadata.Builder()
            .setFolderType(MediaMetadata.FOLDER_TYPE_ALBUMS)
            .setArtworkUri(coverUri)
            .setDisplayTitle(title)
            .setArtist(artist)
            .setAlbumTitle(album)
            .build()
    }

    private fun updateUIWithMediaController(controller: MediaController) {
        controller.addListener(object : Player.Listener {

            override fun onPositionDiscontinuity(
                oldPosition: Player.PositionInfo, newPosition: Player.PositionInfo, reason: Int
            ) {
                val currentPosition = controller.currentPosition.toInt() / 1000
                binding.seekbar.progress = currentPosition
                binding.time.text = getTimeString(currentPosition)
                binding.duration.text = getTimeString(controller.duration.toInt() / 1000)
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_BUFFERING -> {
                        showBuffering()
                        pauseStopwatch()
                    }
                    Player.STATE_READY -> {
                        hideBuffering()
                        updatePlayPauseButton(controller.playWhenReady)
                        if (controller.playWhenReady){
                            startStopwatch()
                        }
                    }

                    Player.STATE_ENDED -> handlePlaybackEnded()
                    Player.STATE_IDLE -> {
                        pauseStopwatch()
                    }
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                updatePlayPauseButton(isPlaying)

                if (isPlaying) {
                    startStopwatch()
                } else {
                    pauseStopwatch()
                }

                duration = controller.duration.toInt() / 1000
                binding.seekbar.max = duration
                binding.time.text = "0:00"
                binding.duration.text = getTimeString(duration)
            }
        })

        updatePlayPauseButton(controller.playWhenReady)

        lifecycleScope.launch {
            while (isActive) {
                val currentPosition = controller.currentPosition.toInt() / 1000
                binding.seekbar.progress = currentPosition
                binding.time.text = getTimeString(currentPosition)
                binding.duration.text = getTimeString(duration)
                delay(1000)
            }
        }
    }

    private fun triggerProgressBar(visibility: Boolean) {
        binding.progressBar.isVisible = visibility
        binding.btnPlayPause.isVisible = !visibility
    }

    private fun setupUIControls() {
        binding.btnPlayPause.setOnClickListener {
            if (controller.playWhenReady) {
                controller.pause()
            } else {
                controller.play()
            }
        }

        binding.seekbar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) controller.seekTo(progress.toLong() * 1000)
                binding.time.text = getTimeString(progress)
                binding.duration.text = getTimeString(duration)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                pauseStopwatch()
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                if (controller.playWhenReady) {
                    startStopwatch()
                }
            }
        })

        binding.btnSkipForward.setOnClickListener {
            currentSongPosition = controller.currentPosition
            if (currentSongPosition + SEEK_FORWARD <= controller.duration) {
                controller.seekTo(currentSongPosition + SEEK_FORWARD)
            } else {
                controller.seekTo(controller.duration)
            }
        }

        binding.btnSkipBack.setOnClickListener {
            currentSongPosition = controller.currentPosition
            if (currentSongPosition - SEEK_BACKWARD >= 0) {
                controller.seekTo(currentSongPosition - SEEK_BACKWARD)
            } else {
                controller.seekTo(0)
            }
        }
    }

    private fun startStopwatch() {
        if (!isStopwatchRunning) {
            stopwatchStartTime = SystemClock.elapsedRealtime() - elapsedTime
            isStopwatchRunning = true
        }
    }

    private fun pauseStopwatch() {
        if (isStopwatchRunning) {
            elapsedTime = SystemClock.elapsedRealtime() - stopwatchStartTime
            isStopwatchRunning = false
        }
    }

    private fun reportListenTime() {
        if (isStopwatchRunning) {
            elapsedTime = SystemClock.elapsedRealtime() - stopwatchStartTime
        }
        // Convert milliseconds to seconds
        val listenedTimeInSeconds = elapsedTime / 1000

        showToast("Total listened time: $listenedTimeInSeconds seconds")
        log("Total listened time: $listenedTimeInSeconds seconds")
    }

    private fun updatePlayPauseButton(isPlaying: Boolean) {
        binding.btnPlayPause.setImageResource(if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play)
        if (isPlaying) {
            Picasso.get().load(data?.coverUri).error(R.drawable.img_music).into(binding.ivAlbum)
        }
    }

    private fun getTimeString(seconds: Int): String {
        val mins = seconds / 60
        val secs = seconds % 60
        return String.format(Locale.getDefault(), "%02d:%02d", mins, secs)
    }

    private fun showBuffering() { triggerProgressBar(true) }

    private fun hideBuffering() { triggerProgressBar(false) }

    private fun handlePlaybackEnded() {
        binding.btnPlayPause.setImageResource(R.drawable.ic_play)
        binding.seekbar.progress = 0
        binding.time.text = getTimeString(duration)
        binding.duration.text = getTimeString(duration)
        triggerProgressBar(false)
        pauseStopwatch()
        reportListenTime()
    }

    override fun onDestroyView() {
        super.onDestroyView()
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaControllerFuture?.let {
            MediaController.releaseFuture(it)
        }
    }

    private fun log(message: String) {
        Log.e("=====[MediaPlayer]=====", message)
    }

}