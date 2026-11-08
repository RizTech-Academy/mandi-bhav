package com.riztechacademy.mandibhav.data

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.riztechacademy.mandibhav.data.local.SaleSlipDao
import com.riztechacademy.mandibhav.data.local.SaleSlipEntity
import com.riztechacademy.mandibhav.model.SaleSlip
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.IOException
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/** The photo of the last sale slip for each commodity at each market. */
interface SaleSlipRepository {

    /** The slip for [commodity] at [market], or null if there is none. */
    fun slip(market: String, commodity: String): Flow<SaleSlip?>

    /**
     * Copies the picture at [source] into the app's own storage and makes it
     * the slip for [commodity] at [market], replacing any before it.
     * [source] is read once, now: a Uri from the photo picker or another app
     * only grants access for a while, so the app never keeps the Uri itself.
     * Throws [IOException] if the picture cannot be read.
     */
    suspend fun attach(market: String, commodity: String, date: LocalDate, source: Uri)

    suspend fun remove(market: String, commodity: String)

    /**
     * A new, empty file for the camera app to write a photo into, as a
     * content:// Uri the app can grant to it. The file is in the cache: if
     * the user backs out of the camera, it is thrown away later. Suspends:
     * making the folder and the Uri both touch the disk.
     */
    suspend fun newCameraCapture(): Uri
}

/**
 * Slips in Room, pictures as files under `files/slips/` in the app's private
 * storage: no other app can read them, they need no permission, and they
 * go when the app is uninstalled. They are not in the backup rules, so they
 * stay on this phone.
 */
class FileSaleSlipRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val dao: SaleSlipDao,
) : SaleSlipRepository {

    private val slipsDir get() = File(context.filesDir, "slips")
    private val cameraDir get() = File(context.cacheDir, "camera")

    override fun slip(market: String, commodity: String): Flow<SaleSlip?> =
        dao.observe(market, commodity).map { row ->
            row?.let { SaleSlip(it.market, it.commodity, it.date, File(slipsDir, it.fileName)) }
        }
            // filesDir checks the folder exists, on the disk: not on the main thread.
            .flowOn(Dispatchers.IO)

    override suspend fun attach(market: String, commodity: String, date: LocalDate, source: Uri) =
        withContext(Dispatchers.IO) {
            slipsDir.mkdirs()
            val copy = File(slipsDir, "${UUID.randomUUID()}.jpg")
            try {
                val input = context.contentResolver.openInputStream(source)
                    ?: throw IOException("Could not open $source")
                input.use { from -> copy.outputStream().use { to -> from.copyTo(to) } }
                if (copy.length() == 0L) throw IOException("$source was empty")
            } catch (e: IOException) {
                copy.delete()
                throw e
            } catch (e: SecurityException) {
                // The grant to read the Uri has gone: report it as a failed read.
                copy.delete()
                throw IOException("No longer allowed to read $source", e)
            }
            val before = dao.get(market, commodity)
            dao.upsert(SaleSlipEntity(market, commodity, date, copy.name))
            before?.let { File(slipsDir, it.fileName).delete() }
            // Whatever the camera wrote has been copied, or was abandoned.
            cameraDir.listFiles()?.forEach { it.delete() }
            Unit
        }

    override suspend fun remove(market: String, commodity: String) = withContext(Dispatchers.IO) {
        val before = dao.get(market, commodity) ?: return@withContext
        dao.delete(market, commodity)
        File(slipsDir, before.fileName).delete()
        Unit
    }

    override suspend fun newCameraCapture(): Uri = withContext(Dispatchers.IO) {
        cameraDir.mkdirs()
        val file = File(cameraDir, "capture-${System.currentTimeMillis()}.jpg")
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }
}
