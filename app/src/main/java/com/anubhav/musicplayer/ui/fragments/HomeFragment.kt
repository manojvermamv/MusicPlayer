package com.anubhav.musicplayer.ui.fragments

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.core.view.MenuProvider
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.anubhav.musicplayer.R
import com.anubhav.musicplayer.databinding.FragmentHomeBinding
import com.anubhav.musicplayer.model.models.MusicAction
import com.anubhav.musicplayer.model.models.MusicActionType
import com.anubhav.musicplayer.ui.adapter.MusicRvAdapter
import com.anubhav.musicplayer.ui.common.BaseFragment
import com.anubhav.musicplayer.utils.goToSettings
import com.anubhav.musicplayer.utils.isAllPermissionsGranted
import com.anubhav.musicplayer.utils.observe
import com.anubhav.musicplayer.utils.registerForMultiplePermissionsResult
import com.anubhav.musicplayer.utils.requestPermission
import com.anubhav.musicplayer.utils.showToast
import com.anubhav.musicplayer.viewmodel.MainActivityViewModel
import org.koin.androidx.viewmodel.ext.android.activityViewModel


class HomeFragment : BaseFragment<FragmentHomeBinding>(FragmentHomeBinding::inflate), MenuProvider {

    private val viewModel by activityViewModel<MainActivityViewModel>()

    private val relationalMsg = "This app needs notifications and media permissions to play music."
    private val permissions by lazy {
        mutableListOf<String>().apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
            add(
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    Manifest.permission.READ_MEDIA_AUDIO
                } else {
                    Manifest.permission.READ_EXTERNAL_STORAGE
                }
            )
        }.toList()
    }

    private val permissionsLauncher = registerForMultiplePermissionsResult { results ->
        if (results.isAllPermissionsGranted()) {
            onPermissionGranted()
        } else {
            requireContext().goToSettings()
            showToast("This app needs notifications and media permissions! Go to settings and grant all permissions.")
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setSupportActionBar(binding.toolbar)
        setTitle(getString(R.string.app_name))
        setDisplayHomeAsUpEnabled()

        requireActivity().addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)

        binding.songRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.songRecyclerView.setHasFixedSize(true)
        binding.actionShuffle.setOnClickListener { shuffleSongs() }

        requestPermission(permissions, relationalMsg, permissionsLauncher, this::onPermissionGranted)
        fetchMusic()
    }

    private fun fetchMusic() {
        viewModel.triggerProgressBar(true)
        observe(viewModel.musicLiveData) {
            viewModel.triggerProgressBar(false)
            binding.songRecyclerView.adapter = MusicRvAdapter(ArrayList(it)) { action ->
                viewModel.triggerMusicAction(action)
            }
        }
    }

    private fun shuffleSongs() {
        viewModel.triggerMusicAction(MusicAction(type = MusicActionType.SHUFFLE))
    }

    private fun stopSongs() {
        viewModel.triggerMusicAction(MusicAction(type = MusicActionType.EXIT))
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.main_actions, menu)
    }

    override fun onMenuItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_refresh -> viewModel.refreshMusic()
            R.id.action_stop -> stopSongs()
        }
        return false
    }

    private fun onPermissionGranted() {
        viewModel.refreshMusic()
    }

}