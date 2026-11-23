package com.example.utils

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.location.LocationManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.provider.ContactsContract
import android.provider.OpenableColumns
import android.util.Base64
import java.io.ByteArrayOutputStream
import java.io.File

object MediaAndDeviceHelpers {

    fun compressImageUriToBase64(
        context: Context,
        uri: Uri,
        maxDimension: Int = 512,
        quality: Int = 70
    ): String? = try {
        val stream = context.contentResolver.openInputStream(uri) ?: return null
        val original = stream.use { BitmapFactory.decodeStream(it) } ?: return null
        val scale = minOf(
            maxDimension.toFloat() / original.width.coerceAtLeast(1),
            maxDimension.toFloat() / original.height.coerceAtLeast(1),
            1f
        )
        val w = (original.width * scale).toInt().coerceAtLeast(1)
        val h = (original.height * scale).toInt().coerceAtLeast(1)
        val resized = if (scale < 1f) Bitmap.createScaledBitmap(original, w, h, true) else original
        val out = ByteArrayOutputStream()
        resized.compress(Bitmap.CompressFormat.JPEG, quality, out)
        Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
    } catch (_: Exception) {
        null
    }

    fun readDocumentMeta(context: Context, uri: Uri): Pair<String, String> {
        var name = "Document"
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (idx >= 0 && cursor.moveToFirst()) {
                name = cursor.getString(idx) ?: "Document"
            }
        }
        val mime = context.contentResolver.getType(uri) ?: "application/octet-stream"
        return name to mime
    }

    fun readDeviceContacts(context: Context): List<Pair<String, String>> {
        val results = mutableListOf<Pair<String, String>>()
        try {
            val cursor = context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER
                ),
                null,
                null,
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
            )
            cursor?.use {
                val nameIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                while (it.moveToNext() && results.size < 150) {
                    val name = if (nameIdx >= 0) it.getString(nameIdx) ?: "" else ""
                    val num = if (numIdx >= 0) it.getString(numIdx) ?: "" else ""
                    if (name.isNotBlank() && num.isNotBlank()) {
                        results.add(name to num)
                    }
                }
            }
        } catch (_: Exception) {
        }
        return results
    }

    @SuppressLint("MissingPermission")
    fun getLastKnownLocationText(context: Context): String? = try {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        val providers = lm?.getProviders(true) ?: emptyList()
        var best: android.location.Location? = null
        for (p in providers) {
            val loc = lm?.getLastKnownLocation(p) ?: continue
            if (best == null || loc.accuracy < best.accuracy) {
                best = loc
            }
        }
        best?.let { "%.5f, %.5f".format(it.latitude, it.longitude) }
    } catch (_: Exception) {
        null
    }

    class VoiceNoteRecorder(private val context: Context) {
        private var recorder: MediaRecorder? = null
        private var outputFile: File? = null

        fun start(): Boolean = try {
            val file = File(context.cacheDir, "smile_vn_${System.currentTimeMillis()}.m4a")
            outputFile = file
            @Suppress("DEPRECATION")
            val r = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                MediaRecorder()
            }
            r.setAudioSource(MediaRecorder.AudioSource.MIC)
            r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            r.setAudioEncodingBitRate(32000)
            r.setAudioSamplingRate(16000)
            r.setOutputFile(file.absolutePath)
            r.prepare()
            r.start()
            recorder = r
            true
        } catch (_: Exception) {
            stopAndDiscard()
            false
        }

        fun stopAndEncodeBase64(): String? = try {
            recorder?.stop()
            recorder?.release()
            recorder = null
            val bytes = outputFile?.takeIf { it.exists() }?.readBytes()
            outputFile?.delete()
            outputFile = null
            if (bytes != null && bytes.size <= 240_000) {
                Base64.encodeToString(bytes, Base64.NO_WRAP)
            } else {
                null
            }
        } catch (_: Exception) {
            stopAndDiscard()
            null
        }

        fun stopAndDiscard() {
            runCatching { recorder?.stop() }
            runCatching { recorder?.release() }
            recorder = null
            outputFile?.delete()
            outputFile = null
        }
    }

    fun playBase64Audio(context: Context, base64: String, onFinished: () -> Unit): MediaPlayer? = try {
        val bytes = Base64.decode(base64, Base64.NO_WRAP)
        val tmp = File(context.cacheDir, "smile_play_${System.currentTimeMillis()}.m4a")
        tmp.writeBytes(bytes)
        MediaPlayer().apply {
            setDataSource(tmp.absolutePath)
            setOnCompletionListener {
                it.release()
                tmp.delete()
                onFinished()
            }
            prepare()
            start()
        }
    } catch (_: Exception) {
        onFinished()
        null
    }
}
