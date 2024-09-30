package com.anubhav.musicplayer.ui.activities

import android.content.Context
import android.view.View
import android.widget.MediaController
import com.anubhav.musicplayer.service.MediaPlayerService

class MyMediaController(context: Context) : MediaController(context) {
    override fun hide() {}
}

class MusicController(
    private val context: Context,
    private val anchorView: View
) : MediaController.MediaPlayerControl {

    private var service: MediaPlayerService? = null
    private var isServiceBound = false

    private var controller: MyMediaController? = null
    private var playbackPaused: Boolean = false
    private var paused: Boolean = false

    fun setService(service: MediaPlayerService) {
        this.service = service
    }

    fun isServiceBound(bound: Boolean) {
        this.isServiceBound = bound
    }

    fun hide() {
        controller?.hide()
    }

    fun show(timeout: Int? = null) = controller?.apply {
        if (timeout != null)
            show(timeout)
        else
            show()
    }

    fun onStart() {
        setController()
    }

    fun onResume() {
        if (paused) {
            setController()
            paused = false
        }
    }

    fun onPause() {
        paused = true
    }

    fun onDestroy() {
        service = null
    }

    private fun setController() {
        controller = MyMediaController(context).apply {
            setPrevNextListeners({ playNext() }, { playPrev() })
            setMediaPlayer(this@MusicController)
            setAnchorView(anchorView)
            isEnabled = true
        }
    }

    fun playNext() {
        service?.playNext()
        if (playbackPaused) {
            setController()
            playbackPaused = false
        }
        show(0)
    }

    fun playPrev() {
        service?.playPrev()
        if (playbackPaused) {
            setController()
            playbackPaused = false
        }
        show(0)
    }

    fun playSong(index: Int) {
        service?.setSong(index)
        service?.playSong()
        if (playbackPaused) {
            setController()
            playbackPaused = false
        }
        show(0)
    }

    fun shuffleSongs() {
        service?.setShuffle()
    }

    fun isShowing() = controller?.isShowing ?: false

    override fun canPause(): Boolean {
        return true
    }

    override fun canSeekBackward(): Boolean {
        return true
    }

    override fun canSeekForward(): Boolean {
        return true
    }

    override fun getAudioSessionId(): Int {
        return 1
    }

    override fun getBufferPercentage(): Int {
        val duration = service!!.getDuration()
        if (duration > 0) {
            return (service!!.getPosition() * 100) / (duration)
        }
        return 0
    }

    override fun getCurrentPosition(): Int {
        //if (service != null && isServiceBound && service!!.isPlaying())
        if (service != null && isServiceBound)
            return service!!.getPosition()
        else return 0
    }

    override fun start() {
        service?.go()
    }

    override fun pause() {
        playbackPaused = true
        service?.pausePlayer()
    }

    override fun seekTo(p0: Int) {
        service?.seek(p0)
    }

    override fun isPlaying(): Boolean {
        if (service != null && isServiceBound)
            return service!!.isPlaying()
        return false
    }

    override fun getDuration(): Int {
        //if (service != null && isServiceBound && service!!.isPlaying())
        if (service != null && isServiceBound)
            return service!!.getDuration()
        else return 0
    }

}