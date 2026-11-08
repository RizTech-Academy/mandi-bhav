package com.riztechacademy.mandibhav.ui

import android.content.ActivityNotFoundException
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import coil3.compose.AsyncImage
import com.riztechacademy.mandibhav.data.SaleSlipRepository
import com.riztechacademy.mandibhav.model.CommodityPrice
import com.riztechacademy.mandibhav.model.SaleSlip
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.IOException
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private val slipDateFormat = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH)

/** What the sale slip section shows. */
data class SaleSlipUiState(
    val slip: SaleSlip? = null,
    val isSaving: Boolean = false,
    /** A one-time message: shown, then cleared with [SaleSlipViewModel.messageShown]. */
    val message: String? = null,
)

/** The sale slip photo for one commodity at one market. */
@HiltViewModel(assistedFactory = SaleSlipViewModel.Factory::class)
class SaleSlipViewModel @AssistedInject constructor(
    @Assisted("market") private val market: String,
    @Assisted("commodity") private val commodity: String,
    private val repository: SaleSlipRepository,
) : ViewModel() {

    private val saving = MutableStateFlow(false)
    private val message = MutableStateFlow<String?>(null)

    val uiState: StateFlow<SaleSlipUiState> =
        combine(repository.slip(market, commodity), saving, message) { slip, isSaving, text ->
            SaleSlipUiState(slip, isSaving, text)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SaleSlipUiState())

    /** Copies the picture at [source] in as the slip for the price of [date]. */
    fun attach(date: LocalDate, source: Uri) {
        saving.value = true
        viewModelScope.launch {
            try {
                repository.attach(market, commodity, date, source)
            } catch (e: IOException) {
                message.value = "Could not read that picture. Try another one."
            } finally {
                saving.value = false
            }
        }
    }

    fun remove() {
        viewModelScope.launch { repository.remove(market, commodity) }
    }

    suspend fun newCameraCapture(): Uri = repository.newCameraCapture()

    fun messageShown() {
        message.value = null
    }

    @AssistedFactory
    interface Factory {
        fun create(@Assisted("market") market: String, @Assisted("commodity") commodity: String): SaleSlipViewModel
    }
}

/**
 * The farmer's own photo of her sale slip for [price]'s commodity and
 * market, to read against the modal price.
 *
 * Neither way in needs a permission. The photo picker hands the app the one
 * picture the user chose, and the camera app writes its photo into a file
 * this app offers it through a FileProvider: the camera app needs the
 * camera permission, and Mandi Bhav does not.
 */
@Composable
fun SaleSlipSection(
    price: CommodityPrice,
    modifier: Modifier = Modifier,
    viewModel: SaleSlipViewModel = hiltViewModel<SaleSlipViewModel, SaleSlipViewModel.Factory>(
        creationCallback = { factory -> factory.create(price.market, price.commodity) },
    ),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val hasCamera = remember { context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY) }

    val pickPhoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        // null: the user backed out of the picker.
        if (uri != null) viewModel.attach(price.date, uri)
    }

    // Saved, not remembered: while the camera app is in front, Android may
    // kill this app's process, and the result then arrives in a new one.
    // A plain remember would have forgotten where the photo was written.
    var pendingCapture by rememberSaveable { mutableStateOf<Uri?>(null) }
    val takePhoto = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
        val capture = pendingCapture
        pendingCapture = null
        if (saved && capture != null) viewModel.attach(price.date, capture)
    }

    var cameraMissing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Your sale slip", style = MaterialTheme.typography.titleMedium)
        val slip = uiState.slip
        if (slip == null) {
            Text(
                "Keep a photo of the slip from your commission agent, to compare what you were paid " +
                    "with the modal price. It stays on this phone.",
                style = MaterialTheme.typography.bodyMedium,
            )
        } else {
            AsyncImage(
                model = slip.file,
                contentDescription = "Your sale slip for ${slip.commodity} at ${slip.market}",
                contentScale = ContentScale.Fit,
                placeholder = ColorPainter(MaterialTheme.colorScheme.surfaceVariant),
                error = ColorPainter(MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(4f / 3f)
                    .clip(RoundedCornerShape(12.dp)),
            )
            Text(
                "Attached to the price of ${slipDateFormat.format(slip.date)}.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        when {
            uiState.isSaving -> Text("Saving the photo…", style = MaterialTheme.typography.bodyMedium)
            uiState.message != null -> Text(
                uiState.message.orEmpty(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
            cameraMissing -> Text(
                "There is no camera app on this phone. Choose a photo instead.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (hasCamera) {
                OutlinedButton(onClick = {
                    viewModel.messageShown()
                    scope.launch {
                        // Off the main thread: StrictMode caught the folder and
                        // the FileProvider's path check reading the disk on it.
                        val target = viewModel.newCameraCapture()
                        pendingCapture = target
                        try {
                            takePhoto.launch(target)
                        } catch (e: ActivityNotFoundException) {
                            pendingCapture = null
                            cameraMissing = true
                        }
                    }
                }) { Text("Take photo") }
            }
            OutlinedButton(onClick = {
                viewModel.messageShown()
                pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }) { Text("Choose photo") }
            if (slip != null) {
                TextButton(onClick = viewModel::remove) { Text("Remove") }
            }
        }
    }
}
