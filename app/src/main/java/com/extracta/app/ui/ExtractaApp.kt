package com.extracta.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.extracta.app.feature.capture.CameraRoute
import com.extracta.app.feature.capture.CameraViewModel
import com.extracta.app.feature.details.DocumentDetailsScreen
import com.extracta.app.feature.details.DocumentDetailsViewModel
import com.extracta.app.feature.home.HomeScreen
import com.extracta.app.feature.home.HomeViewModel
import com.extracta.app.navigation.DOCUMENT_ID_ARGUMENT
import com.extracta.app.navigation.ExtractaDestination

@Composable
fun ExtractaApp(
    navController: NavHostController = rememberNavController(),
) {
    NavHost(
        navController = navController,
        startDestination = ExtractaDestination.Home.route,
    ) {
        composable(ExtractaDestination.Home.route) {
            val viewModel: HomeViewModel = hiltViewModel()
            val uiState = viewModel.uiState.collectAsStateWithLifecycle()
            val isImporting = viewModel.isImporting.collectAsStateWithLifecycle()
            val message = viewModel.message.collectAsStateWithLifecycle()
            val query = viewModel.query.collectAsStateWithLifecycle()
            val sortOrder = viewModel.sortOrder.collectAsStateWithLifecycle()

            HomeScreen(
                uiState = uiState.value,
                onScanClick = { navController.navigate(ExtractaDestination.Capture.route) },
                onDocumentClick = { documentId ->
                    navController.navigate(ExtractaDestination.DocumentDetails.route(documentId))
                },
                onImportImage = viewModel::importImage,
                onImportCancelled = viewModel::importCancelled,
                isImporting = isImporting.value,
                message = message.value,
                onMessageShown = viewModel::messageShown,
                query = query.value,
                onQueryChange = viewModel::updateQuery,
                sortOrder = sortOrder.value,
                onSortOrderChange = viewModel::updateSortOrder,
            )
        }
        composable(ExtractaDestination.Capture.route) {
            val viewModel: CameraViewModel = hiltViewModel()
            CameraRoute(
                viewModel = viewModel,
                onNavigateBack = navController::navigateUp,
                onDocumentSaved = { documentId ->
                    navController.navigate(ExtractaDestination.DocumentDetails.route(documentId)) {
                        popUpTo(ExtractaDestination.Capture.route) { inclusive = true }
                    }
                },
            )
        }
        composable(
            route = ExtractaDestination.DocumentDetails.route,
            arguments = listOf(
                navArgument(DOCUMENT_ID_ARGUMENT) { type = NavType.LongType },
            ),
        ) {
            val viewModel: DocumentDetailsViewModel = hiltViewModel()
            val uiState = viewModel.uiState.collectAsStateWithLifecycle()
            val deleted = viewModel.deleted.collectAsStateWithLifecycle()

            LaunchedEffect(deleted.value) {
                if (deleted.value) navController.navigateUp()
            }

            DocumentDetailsScreen(
                uiState = uiState.value,
                onNavigateBack = navController::navigateUp,
                onUpdateTitle = viewModel::updateTitle,
                onRetryOcr = viewModel::retryOcr,
                onDeleteDocument = viewModel::deleteDocument,
            )
        }
    }
}
