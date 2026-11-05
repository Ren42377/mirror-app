package com.ren42377.mirror

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ren42377.mirror.data.RecordingCacheManager
import com.ren42377.mirror.ui.MirrorViewModel
import com.ren42377.mirror.ui.camera.CameraScreen
import com.ren42377.mirror.ui.preview.PhotoPreviewScreen
import com.ren42377.mirror.ui.preview.VideoPreviewScreen
import com.ren42377.mirror.ui.theme.MirrorTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MirrorViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            RecordingCacheManager.clearAllRecordings(applicationContext)
        }
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        setContent {
            MirrorTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MirrorNavHost(viewModel = viewModel)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing) {
            viewModel.clearCaptureResult()
            RecordingCacheManager.clearAllRecordings(applicationContext)
        }
    }
}

private object Routes {
    const val CAMERA = "camera"
    const val PHOTO_PREVIEW = "photo_preview"
    const val VIDEO_PREVIEW = "video_preview"
}

@Composable
private fun MirrorNavHost(viewModel: MirrorViewModel) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.CAMERA,
        modifier = Modifier.fillMaxSize(),
        enterTransition = {
            fadeIn(animationSpec = tween(260, easing = FastOutSlowInEasing)) +
                scaleIn(
                    initialScale = 0.94f,
                    animationSpec = tween(260, easing = FastOutSlowInEasing)
                )
        },
        exitTransition = {
            fadeOut(animationSpec = tween(200, easing = FastOutSlowInEasing))
        },
        popEnterTransition = {
            fadeIn(animationSpec = tween(240, easing = FastOutSlowInEasing))
        },
        popExitTransition = {
            fadeOut(animationSpec = tween(220, easing = FastOutSlowInEasing)) +
                scaleOut(
                    targetScale = 0.94f,
                    animationSpec = tween(220, easing = FastOutSlowInEasing)
                )
        }
    ) {
        composable(Routes.CAMERA) {
            CameraScreen(
                viewModel = viewModel,
                onNavigateToPhotoPreview = {
                    navController.navigate(Routes.PHOTO_PREVIEW) {
                        launchSingleTop = true
                    }
                },
                onNavigateToVideoPreview = {
                    navController.navigate(Routes.VIDEO_PREVIEW) {
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(Routes.PHOTO_PREVIEW) {
            PhotoPreviewScreen(
                viewModel = viewModel,
                onClose = {
                    navController.popBackStack(Routes.CAMERA, inclusive = false)
                }
            )
        }

        composable(Routes.VIDEO_PREVIEW) {
            VideoPreviewScreen(
                viewModel = viewModel,
                onClose = {
                    navController.popBackStack(Routes.CAMERA, inclusive = false)
                }
            )
        }
    }
}
