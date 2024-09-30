package com.anubhav.musicplayer.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder


/**
 * Register permissions result
 * */
fun Fragment.registerForPermissionResult(callback: ((Boolean) -> Unit)): ActivityResultLauncher<String> {
    return registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        callback(isGranted)
    }
}

fun AppCompatActivity.registerForPermissionResult(callback: ((Boolean) -> Unit)): ActivityResultLauncher<String> {
    return registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        callback(isGranted)
    }
}

fun Fragment.registerForMultiplePermissionsResult(callback: ((MutableList<PermissionResult>) -> Unit)): ActivityResultLauncher<Array<String>> {
    return registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        callback.invoke(permissions.toMutableList())
    }
}

fun AppCompatActivity.registerForMultiplePermissionsResult(callback: ((MutableList<PermissionResult>) -> Unit)): ActivityResultLauncher<Array<String>> {
    return registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        callback.invoke(permissions.toMutableList())
    }
}

fun MutableList<PermissionResult>.isAllPermissionsGranted() = (size == filter { it.granted }.size)

private fun Map<String, Boolean>.toMutableList(): MutableList<PermissionResult> {
    val results = mutableListOf<PermissionResult>()
    forEach { (permission, isGranted) ->
        results.add(PermissionResult(permission, isGranted))
    }
    return results
}

data class PermissionResult(
    val permission: String,
    val granted: Boolean
)


/**
 * Permission extensions
 * */
fun Context.showPermissionRationaleDialog(
    message: String = "",
    onPositiveClick: () -> Unit
) {
    MaterialAlertDialogBuilder(this)
        .setTitle("Permission Required")
        .setMessage(message.ifBlank { "This app requires certain permissions to function properly. Please grant all permissions." })
        .setNegativeButton("No") { dialog, _ -> dialog.dismiss() }
        .setPositiveButton("Yes") { _, _ -> onPositiveClick() }
        .show()
}

fun Activity.requestPermission(
    permission: String,
    relationalMsg: String = "",
    resultLauncher: ActivityResultLauncher<String>? = null,
    onGranted: () -> Unit = {}
) = when {
    ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED -> { onGranted() }
    shouldShowRequestPermissionRationale(permission) -> {
        showPermissionRationaleDialog(relationalMsg) {
            resultLauncher?.launch(permission)
        }
    }
    else -> resultLauncher?.launch(permission)
}

fun Fragment.requestPermission(
    permission: String,
    relationalMsg: String = "",
    resultLauncher: ActivityResultLauncher<String>? = null,
    onGranted: () -> Unit = {}
) = (activity as? AppCompatActivity)?.requestPermission(permission, relationalMsg, resultLauncher, onGranted)


fun Activity.requestPermission(
    permissions: List<String>,
    relationalMsg: String = "",
    resultLauncher: ActivityResultLauncher<Array<String>>? = null,
    onGranted: () -> Unit = {}
) {
    val notGrantedPermissions = permissions.filter {
        ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
    }
    when {
        notGrantedPermissions.isEmpty() -> { onGranted() }
        notGrantedPermissions.any { shouldShowRequestPermissionRationale(it) } -> {
            showPermissionRationaleDialog(relationalMsg) {
                resultLauncher?.launch(notGrantedPermissions.toTypedArray())
            }
        }
        else -> resultLauncher?.launch(notGrantedPermissions.toTypedArray())
    }
}

fun Fragment.requestPermission(
    permissions: List<String>,
    relationalMsg: String = "",
    resultLauncher: ActivityResultLauncher<Array<String>>? = null,
    onGranted: () -> Unit = {}
) = (activity as? AppCompatActivity)?.requestPermission(permissions, relationalMsg, resultLauncher, onGranted)


fun Context.goToSettings() {
    try {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.data = Uri.fromParts("package", packageName, null)
        startActivity(intent)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}