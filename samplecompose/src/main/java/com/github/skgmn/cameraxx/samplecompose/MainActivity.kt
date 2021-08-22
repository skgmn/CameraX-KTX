package com.github.skgmn.cameraxx.samplecompose

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.whenStarted
import com.github.skgmn.cameraxx.samplecompose.ui.theme.CameraXXTheme
import com.github.skgmn.startactivityx.*
import kotlinx.coroutines.launch

class MainActivity : FragmentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CameraXXTheme {
                MainScreen(
                    listenPermissionStatus(Manifest.permission.CAMERA),
                    onRequestCameraPermission = {
                        lifecycleScope.launch {
                            requestCameraPermission()
                        }
                    },
                    onTakePhoto = {
                        lifecycleScope.launch {
                            takePhoto()
                        }
                    }
                )
            }
        }

        lifecycleScope.launch {
            requestPermissions(Manifest.permission.CAMERA)
            viewModel.permissionsInitiallyRequested.value = true
        }
    }

    private suspend fun takePhoto() {
        val permissionRequest =
            PermissionRequest(listOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), true)
        val permissionResult = requestPermissions(permissionRequest)
        if (permissionResult.granted) {
            // ImageCapture seems not work well right after a permission is granted.
            // In this case recreate and rebind ImageCapture to workaround this issue.
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P &&
                permissionResult == GrantResult.JUST_GRANTED
            ) {
                viewModel.replaceImageCapture()
            }
            whenStarted {
                val uri = viewModel.takePhotoAsync().await()
                if (uri != null) {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, uri)
                        startActivity(intent)
                    } catch (e: ActivityNotFoundException) {
                        Toast.makeText(this@MainActivity, R.string.photo_saved, Toast.LENGTH_SHORT)
                            .show()
                    }
                }
            }
        } else {
            Toast.makeText(this@MainActivity, R.string.no_permissions, Toast.LENGTH_SHORT)
                .show()
        }
    }

    private suspend fun requestCameraPermission() {
        val permissionRequest =
            PermissionRequest(listOf(Manifest.permission.CAMERA), true)
        requestPermissions(permissionRequest)
    }
}