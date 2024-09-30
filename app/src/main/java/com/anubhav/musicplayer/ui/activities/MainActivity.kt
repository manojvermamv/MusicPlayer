package com.anubhav.musicplayer.ui.activities

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupActionBarWithNavController
import com.anubhav.musicplayer.R
import com.anubhav.musicplayer.databinding.ActivityMainBinding
import com.anubhav.musicplayer.model.models.MusicAction
import com.anubhav.musicplayer.model.models.MusicActionType
import com.anubhav.musicplayer.service.MediaPlayerService
import com.anubhav.musicplayer.ui.common.BaseActivity
import com.anubhav.musicplayer.utils.SingleEvent
import com.anubhav.musicplayer.utils.observe
import com.anubhav.musicplayer.utils.observeEvent
import com.anubhav.musicplayer.utils.onBackPressed
import com.anubhav.musicplayer.utils.setLightStatusBar
import com.anubhav.musicplayer.viewmodel.MainActivityViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import kotlin.system.exitProcess


class MainActivity : BaseActivity<ActivityMainBinding>(ActivityMainBinding::inflate) {

    private val viewModel by viewModel<MainActivityViewModel>()

    private lateinit var navController: NavController

    private var musicService: MediaPlayerService? = null
    private var playIntent: Intent? = null
    private val controller: MusicController by lazy { MusicController(this, binding.bottomView) }

    private val musicConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            val binder = service as MediaPlayerService.MusicBinder
            musicService = binder.getService
            controller.setService(musicService!!)
            controller.isServiceBound(true)
            viewModel.refreshMusic()
        }
        override fun onServiceDisconnected(name: ComponentName) {
            controller.isServiceBound(false)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setLightStatusBar(window, true)
        onBackPressed {
            navController.currentBackStackEntry?.destination?.let {
                if (it.id == R.id.homeFragment) finish()
                else navController.popBackStack()
            } ?: finish()
            true
        }

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController
        setupActionBarWithNavController(navController)
        navHostController()

        observeEvent(viewModel.triggerProgressBar, ::triggerProgressBar)
        observeEvent(viewModel.triggerMusicAction, ::triggerMusicAction)
        observe(viewModel.musicLiveData) { musicService?.setList(ArrayList(it)) }
    }

    override fun onStart() {
        super.onStart()
        controller.onStart()
        if (playIntent == null) {
            playIntent = Intent(this, MediaPlayerService::class.java)
            bindService(playIntent!!, musicConnection, BIND_AUTO_CREATE)
            startService(playIntent)
        }
    }

    override fun onResume() {
        super.onResume()
        controller.onResume()
    }

    override fun onPause() {
        super.onPause()
        controller.onPause()
    }

    override fun onDestroy() {
        stopService(playIntent)
        musicService = null
        controller.onDestroy()
        super.onDestroy()
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }

    private fun navHostController() {
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.musicPlayerFragment -> hideBottomBar()
                R.id.homeFragment -> showBottomBar()
                else -> showBottomBar()
            }
        }
    }

    private fun exitPlayer() {
        stopService(playIntent)
        musicService = null
        exitProcess(0)
    }

    private fun showBottomBar() { }

    private fun hideBottomBar() { }

    private fun triggerProgressBar(event: SingleEvent<Boolean>) {
        event.getContentIfNotHandled()?.let {
            binding.progressBar.isVisible = it
        }
    }

    private fun triggerMusicAction(event: SingleEvent<MusicAction>) {
        event.getContentIfNotHandled()?.let {
            if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                when (it.type) {
                    MusicActionType.PLAY -> controller.playSong(it.index)
                    MusicActionType.PAUSE -> controller.pause()
                    MusicActionType.NEXT -> controller.playNext()
                    MusicActionType.PREVIOUS -> controller.playPrev()
                    MusicActionType.SHUFFLE -> controller.shuffleSongs()
                    MusicActionType.EXIT -> exitPlayer()
                    else -> navController.navigate(R.id.action_homeFragment_to_musicPlayerFragment, Bundle().apply {
                        putParcelable("data", it.music)
                    })
                }
            }
        }
    }

}