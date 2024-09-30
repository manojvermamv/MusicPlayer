package com.anubhav.musicplayer.utils

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.view.KeyEvent
import android.view.Window
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer


/**
 * Usage: We use it very simply, you only need to add this function to your OnCreate() method
 *
 * onBackPressed {
 *     // Do your job
 *     // Return "false" if you want to system perform back
 *     // Return "true" if you want to override the back functionality
 *     true
 * }
 * */
fun Activity.onBackPressed(callback: (() -> Boolean)) {
    (this as? FragmentActivity)?.onBackPressedDispatcher?.addCallback(
        this,
        object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (!callback()) {
                    remove()
                    performBackPressed()
                }
            }
        })
}

fun Activity.performBackPressed() {
    (this as? FragmentActivity)?.onBackPressedDispatcher?.onBackPressed()
}

fun Fragment.onBackPressed(callback: (() -> Boolean)) {
    requireActivity().onBackPressedDispatcher.addCallback(
        viewLifecycleOwner,
        object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (!callback()) {
                    remove()
                    performBackPressed()
                }
            }
        })
}

fun Fragment.performBackPressed() {
    requireActivity().onBackPressedDispatcher.onBackPressed()
}

fun Dialog.onBackPressed(callback: (() -> Boolean)) {
    setOnKeyListener { _, keyCode, event ->
        if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
            callback()
        } else {
            false
        }
    }
}


/**
 * Register activities result
 * */
fun <I, O> Activity.registerForActivityResult(
    contract: ActivityResultContract<I, O>,
    callback: ActivityResultCallback<O>
) = (this as ComponentActivity).registerForActivityResult(contract, callback)


fun Fragment.registerForActivityResult(callback: ((Int, Intent?) -> Unit)): ActivityResultLauncher<Intent> {
    return registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        callback.invoke(result.resultCode, result.data)
    }
}

fun AppCompatActivity.registerForActivityResult(callback: ((Int, Intent?) -> Unit)): ActivityResultLauncher<Intent> {
    return registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        callback.invoke(result.resultCode, result.data)
    }
}

fun Fragment.registerForActivityResultOnSuccess(callback: ((Intent) -> Unit)): ActivityResultLauncher<Intent> {
    return registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK && result?.data != null) {
            callback.invoke(result.data!!)
        }
    }
}

fun AppCompatActivity.registerForActivityResultOnSuccess(callback: ((Intent) -> Unit)): ActivityResultLauncher<Intent> {
    return registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK && result?.data != null) {
            callback.invoke(result.data!!)
        }
    }
}

/**
 * Launch file chooser
 * */
fun ActivityResultLauncher<Intent>.launchFileChooser(
    type: String,
    title: String = "Choose file",
    multipleAllowed: Boolean = false
) {
    // show file chooser
    val intent = Intent(Intent.ACTION_GET_CONTENT)
    intent.type = type
    intent.addCategory(Intent.CATEGORY_OPENABLE)
    intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, multipleAllowed)
    launch(Intent.createChooser(intent, title))
}

/**
 * Changes the foreground color of the status bars to light or dark so that the items on the bar
 * can be read clearly.
 *
 * @param window Window that hosts the status bars
 * @param isLight `true` to make the foreground color light
 */
fun setLightStatusBar(window: Window, isLight: Boolean) {
    val insetsController = WindowCompat.getInsetsController(window, window.decorView)
    insetsController.isAppearanceLightStatusBars = isLight
}

/**
 * Changes the foreground color of the navigation bars to light or dark so that the items on the
 * bar can be read clearly.
 *
 * @param window Window that hosts the status bars
 * @param isLight `true` to make the foreground color light.
 */
fun setLightNavigationBar(window: Window, isLight: Boolean) {
    val insetsController = WindowCompat.getInsetsController(window, window.decorView)
    insetsController.isAppearanceLightNavigationBars = isLight
}


/**
 * Helper extensions
 * */
fun Context.showToast(msg: String) {
    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}

fun Fragment.showToast(msg: String) {
    Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
}


/**
 * ArchComponentExt
 * */
fun <T> LifecycleOwner.observe(liveData: LiveData<T>, action: (t: T) -> Unit) {
    liveData.observe(this, Observer { it?.let { t -> action(t) } })
}

fun <T> LifecycleOwner.observeEvent(
    liveData: LiveData<SingleEvent<T>>,
    action: (t: SingleEvent<T>) -> Unit
) {
    liveData.observe(this, Observer { it?.let { t -> action(t) } })
}