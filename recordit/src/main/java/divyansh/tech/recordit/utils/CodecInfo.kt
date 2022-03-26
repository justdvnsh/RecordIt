package divyansh.tech.recordit.utils

import android.content.Context
import android.content.Context.WINDOW_SERVICE
import android.content.res.Configuration
import android.content.res.Configuration.ORIENTATION_LANDSCAPE
import android.content.res.Configuration.ORIENTATION_PORTRAIT
import android.media.CamcorderProfile
import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.os.Build
import android.util.DisplayMetrics
import android.util.Range
import android.view.WindowManager
import androidx.annotation.RequiresApi


class CodecInfo {
    val maxSupportedWidth: Int
        get() {
            val recordingInfo = recordingInfo
            return recordingInfo.width
        }
    val maxSupportedHeight: Int
        get() {
            val recordingInfo = recordingInfo
            return recordingInfo.height
        }
    private val recordingInfo: RecordingInfo
        private get() {
            val displayMetrics = DisplayMetrics()
            val wm = context?.getSystemService(WINDOW_SERVICE) as WindowManager
            wm.defaultDisplay.getRealMetrics(displayMetrics)
            val displayWidth = displayMetrics.widthPixels
            val displayHeight = displayMetrics.heightPixels
            val displayDensity = displayMetrics.densityDpi
            val configuration: Configuration = context!!.resources.configuration
            val isLandscape = configuration.orientation === ORIENTATION_LANDSCAPE
            val camcorderProfile = CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH)
            val cameraWidth = camcorderProfile?.videoFrameWidth ?: -1
            val cameraHeight = camcorderProfile?.videoFrameHeight ?: -1
            val cameraFrameRate = camcorderProfile?.videoFrameRate ?: 30
            return calculateRecordingInfo(
                displayWidth, displayHeight, displayDensity, isLandscape,
                cameraWidth, cameraHeight, cameraFrameRate, 100
            )
        }
    private var context: Context? = null
    fun setContext(c: Context?) {
        context = c
    }

    class RecordingInfo(
        val width: Int,
        val height: Int,
        val frameRate: Int,
        val density: Int
    )

    // ALL PUBLIC METHODS
    // This is for testing purposes only
    // Select the default video encoder
    // This will only return the default video encoder, it does not mean that the encoder supports a particular set of parameters
    fun selectVideoCodec(mimeType: String?): MediaCodecInfo? {
        val result: MediaCodecInfo? = null
        // get the list of available codecs
        val numCodecs = MediaCodecList.getCodecCount()
        for (i in 0 until numCodecs) {
            val codecInfo = MediaCodecList.getCodecInfoAt(i)
            if (!codecInfo.isEncoder) {    // skipp decoder
                continue
            }
            val types = codecInfo.supportedTypes
            for (type in types) {
                if (type.equals(mimeType, ignoreCase = true)) {
                    return codecInfo
                }
            }
        }
        return result
    }

    private fun selectCodecByMime(mimeType: String): String {
        val numCodecs = MediaCodecList.getCodecCount()
        for (i in 0 until numCodecs) {
            val codecInfo = MediaCodecList.getCodecInfoAt(i)
            if (!codecInfo.isEncoder) {
                continue
            }
            val types = codecInfo.supportedTypes
            for (type in types) {
                if (type.equals(mimeType, ignoreCase = true)) {
                    return codecInfo.name
                }
            }
        }
        return "Mime not supported"
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private fun selectDefaultCodec(): MediaCodecInfo? {
        val result: MediaCodecInfo? = null
        // get the list of available codecs
        val numCodecs = MediaCodecList.getCodecCount()
        for (i in 0 until numCodecs) {
            val codecInfo = MediaCodecList.getCodecInfoAt(i)
            if (!codecInfo.isEncoder) {    // skipp decoder
                continue
            }
            val types = codecInfo.supportedTypes
            for (j in types.indices) {
                if (types[j].contains("video")) {
                    val codecCapabilities = codecInfo.getCapabilitiesForType(types[j])
                    val formatSup =
                        codecCapabilities.isFormatSupported(codecCapabilities.defaultFormat)
                    if (formatSup) {
                        return codecInfo
                    }
                }
            }
        }
        return result
    }

    // Get the default video encoder name
    // The default one will be returned first
    fun getDefaultVideoEncoderName(mimeType: String): String {
        var defaultEncoder = ""
        try {
            defaultEncoder = selectCodecByMime(mimeType)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return defaultEncoder
    }

    // Get the default video format
    // The default one will be returned first
    @get:RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    val defaultVideoFormat: String
        get() {
            var supported = ""
            try {
                val codecInfo = selectDefaultCodec()
                if (codecInfo != null) {
                    val types = codecInfo.supportedTypes
                    for (type in types) {
                        if (type.contains("video")) {
                            val codecCapabilities = codecInfo.getCapabilitiesForType(type)
                            val result = codecCapabilities.defaultFormat.toString()
                            return returnTypeFromMime(
                                result.substring(
                                    result.indexOf("=") + 1,
                                    result.indexOf(",")
                                )
                            )
                        }
                    }
                } else {
                    supported = "null"
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return supported
        }

    private fun returnTypeFromMime(mimeType: String): String {
        when (mimeType) {
            "video/MP2T" -> return "MPEG_2_TS"
            "video/mp4v-es" -> return "MPEG_4"
            "video/mp4v" -> return "MPEG_4"
            "video/mp4" -> return "MPEG_4"
            "video/avc" -> return "MPEG_4"
            "video/3gpp" -> return "THREE_GPP"
            "video/webm" -> return "WEBM"
            "video/x-vnd.on2.vp8" -> return "WEBM"
        }
        return ""
    }

    // Example usage - isSizeAndFramerateSupported(hbrecorder.getWidth(), hbrecorder.getHeight(), 30, "video/mp4", ORIENTATION_PORTRAIT);
    // int width - The width of the view to be recorder
    // int height - The height of the view to be recorder
    // String mimeType - for ex. video/mp4
    // int orientation - ORIENTATION_PORTRAIT or ORIENTATION_LANDSCAPE
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    fun isSizeAndFramerateSupported(
        width: Int,
        height: Int,
        fps: Int,
        mimeType: String?,
        orientation: Int
    ): Boolean {
        var supported = false
        try {
            val codecInfo = selectVideoCodec(mimeType)
            val types = codecInfo!!.supportedTypes
            for (type in types) {
                if (type.contains("video")) {
                    val codecCapabilities = codecInfo.getCapabilitiesForType(type)
                    val videoCapabilities = codecCapabilities.videoCapabilities

                    // Flip around the width and height in ORIENTATION_PORTRAIT because android's default orientation is ORIENTATION_LANDSCAPE
                    supported = if (ORIENTATION_PORTRAIT === orientation) {
                        videoCapabilities.areSizeAndRateSupported(height, width, fps.toDouble())
                    } else {
                        videoCapabilities.areSizeAndRateSupported(width, height, fps.toDouble())
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return supported
    }

    fun isMimeTypeSupported(mimeType: String?): Boolean {
        try {
            val codecInfo = selectVideoCodec(mimeType)
            val types = codecInfo!!.supportedTypes
            for (type in types) {
                if (type.contains("video")) {
                    //ignore
                }
            }
        } catch (e: Exception) {
            return false
        }
        return true
    }

    // Check if a particular size is supported
    // Provide width and height in Portrait mode, for example. isSizeSupported(1080, 1920, "video/mp4");
    // We do this because android's default orientation is landscape, so we have to flip around the width and height
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    fun isSizeSupported(width: Int, height: Int, mimeType: String?): Boolean {
        var supported = false
        try {
            val codecInfo = selectVideoCodec(mimeType)
            val types = codecInfo!!.supportedTypes
            for (type in types) {
                if (type.contains("video")) {
                    val codecCapabilities = codecInfo.getCapabilitiesForType(type)
                    val videoCapabilities = codecCapabilities.videoCapabilities
                    supported = videoCapabilities.isSizeSupported(height, width)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return supported
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    fun getMaxSupportedFrameRate(width: Int, height: Int, mimeType: String?): Double {
        var maxFPS = 0.0
        try {
            val codecInfo = selectVideoCodec(mimeType)
            val types = codecInfo!!.supportedTypes
            for (type in types) {
                if (type.contains("video")) {
                    val codecCapabilities = codecInfo.getCapabilitiesForType(type)
                    val videoCapabilities = codecCapabilities.videoCapabilities
                    val bit: Range<Double> =
                        videoCapabilities.getSupportedFrameRatesFor(height, width)
                    maxFPS = bit.upper
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return maxFPS
    }

    // Get the max supported bitrate for a particular mime type
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    fun getMaxSupportedBitrate(mimeType: String?): Int {
        var bitrate = 0
        try {
            val codecInfo = selectVideoCodec(mimeType)
            val types = codecInfo!!.supportedTypes
            for (type in types) {
                if (type.contains("video")) {
                    val codecCapabilities = codecInfo.getCapabilitiesForType(type)
                    val videoCapabilities = codecCapabilities.videoCapabilities
                    val bit: Range<Int> = videoCapabilities.bitrateRange
                    bitrate = bit.getUpper()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return bitrate
    }

    // Get supported video formats
    private var supportedVideoFormats: ArrayList<String> = ArrayList()
    fun getSupportedVideoFormats(): ArrayList<String> {
        val allFormats = arrayOf(
            "video/MP2T",
            "video/mp4v-es",
            "video/m4v",
            "video/mp4",
            "video/avc",
            "video/3gpp",
            "video/webm",
            "video/x-vnd.on2.vp8"
        )
        for (allFormat in allFormats) {
            checkSupportedVideoFormats(allFormat)
        }
        return supportedVideoFormats
    }

    private fun checkSupportedVideoFormats(mimeType: String) {
        // get the list of available codecs
        val numCodecs = MediaCodecList.getCodecCount()
        LOOP@ for (i in 0 until numCodecs) {
            val codecInfo = MediaCodecList.getCodecInfoAt(i)
            if (!codecInfo.isEncoder) {    // skipp decoder
                continue
            }
            val types = codecInfo.supportedTypes
            for (j in types.indices) {
                if (types[j].contains("video")) {
                    when (mimeType) {
                        "video/MP2T" -> {
                            supportedVideoFormats.add("MPEG_2_TS")
                            break@LOOP
                        }
                        "video/mp4v-es" -> {
                            if (!supportedVideoFormats.contains("MPEG_4")) {
                                supportedVideoFormats.add("MPEG_4")
                            }
                            break@LOOP
                        }
                        "video/mp4v" -> {
                            if (!supportedVideoFormats.contains("MPEG_4")) {
                                supportedVideoFormats.add("MPEG_4")
                            }
                            break@LOOP
                        }
                        "video/mp4" -> {
                            if (!supportedVideoFormats.contains("MPEG_4")) {
                                supportedVideoFormats.add("MPEG_4")
                            }
                            break@LOOP
                        }
                        "video/avc" -> {
                            if (!supportedVideoFormats.contains("MPEG_4")) {
                                supportedVideoFormats.add("MPEG_4")
                            }
                            break@LOOP
                        }
                        "video/3gpp" -> {
                            supportedVideoFormats.add("THREE_GPP")
                            break@LOOP
                        }
                        "video/webm" -> {
                            if (!supportedVideoFormats.contains("WEBM")) {
                                supportedVideoFormats.add("WEBM")
                            }
                            break@LOOP
                        }
                        "video/video/x-vnd.on2.vp8" -> {
                            if (!supportedVideoFormats.contains("WEBM")) {
                                supportedVideoFormats.add("WEBM")
                            }
                            break@LOOP
                        }
                    }
                }
            }
        }
    }

    // Get supported audio formats
    private var supportedAudioFormats: ArrayList<String> = ArrayList()
    fun getSupportedAudioFormats(): ArrayList<String> {
        val allFormats = arrayOf("audio/amr_nb", "audio/amr_wb", "audio/x-hx-aac-adts", "audio/ogg")
        for (allFormat in allFormats) {
            checkSupportedAudioFormats(allFormat)
        }
        return supportedAudioFormats
    }

    private fun checkSupportedAudioFormats(mimeType: String) {
        // get the list of available codecs
        val numCodecs = MediaCodecList.getCodecCount()
        LOOP@ for (i in 0 until numCodecs) {
            val codecInfo = MediaCodecList.getCodecInfoAt(i)
            if (!codecInfo.isEncoder) {    // skipp decoder
                continue
            }
            val types = codecInfo.supportedTypes
            label@ for (j in types.indices) {
                if (types[j].contains("audio")) {
                    when (mimeType) {
                        "audio/amr_nb" -> {
                            supportedAudioFormats.add("AMR_NB")
                            break@LOOP
                        }
                        "audio/amr_wb" -> {
                            supportedAudioFormats.add("AMR_WB")
                            break@LOOP
                        }
                        "audio/x-hx-aac-adts" -> {
                            supportedAudioFormats.add("AAC_ADTS")
                            break@LOOP
                        }
                        "audio/ogg" -> {
                            supportedAudioFormats.add("OGG")
                            break@LOOP
                        }
                    }
                }
            }
        }
    }

    // Get supported video mime types
    var mVideoMap: HashMap<String, String> = HashMap()
    val supportedVideoMimeTypes: HashMap<String, String>
        get() {
            checkIfSupportedVideoMimeTypes()
            return mVideoMap
        }

    private fun checkIfSupportedVideoMimeTypes() {
        // get the list of available codecs
        val numCodecs = MediaCodecList.getCodecCount()
        for (i in 0 until numCodecs) {
            val codecInfo = MediaCodecList.getCodecInfoAt(i)
            if (!codecInfo.isEncoder) {    // skipp decoder
                continue
            }
            val types = codecInfo.supportedTypes
            for (type in types) {
                if (type.contains("video")) {
                    mVideoMap[codecInfo.name] = type
                }
            }
        }
    }

    // Get supported audio mime types
    var mAudioMap: HashMap<String, String> = HashMap()
    val supportedAudioMimeTypes: HashMap<String, String>
        get() {
            checkIfSupportedAudioMimeTypes()
            return mAudioMap
        }

    private fun checkIfSupportedAudioMimeTypes() {
        // get the list of available codecs
        val numCodecs = MediaCodecList.getCodecCount()
        for (i in 0 until numCodecs) {
            val codecInfo = MediaCodecList.getCodecInfoAt(i)
            if (!codecInfo.isEncoder) {    // skipp decoder
                continue
            }
            val types = codecInfo.supportedTypes
            for (type in types) {
                if (type.contains("audio")) {
                    mAudioMap[codecInfo.name] = type
                }
            }
        }
    }

    companion object {
        fun calculateRecordingInfo(
            displayWidth: Int,
            displayHeight: Int,
            displayDensity: Int,
            isLandscapeDevice: Boolean,
            cameraWidth: Int,
            cameraHeight: Int,
            cameraFrameRate: Int,
            sizePercentage: Int
        ): RecordingInfo {
            // Scale the display size before any maximum size calculations.
            var displayWidth = displayWidth
            var displayHeight = displayHeight
            displayWidth = displayWidth * sizePercentage / 100
            displayHeight = displayHeight * sizePercentage / 100
            if (cameraWidth == -1 && cameraHeight == -1) {
                // No cameras. Fall back to the display size.
                return RecordingInfo(displayWidth, displayHeight, cameraFrameRate, displayDensity)
            }
            var frameWidth = if (isLandscapeDevice) cameraWidth else cameraHeight
            var frameHeight = if (isLandscapeDevice) cameraHeight else cameraWidth
            if (frameWidth >= displayWidth && frameHeight >= displayHeight) {
                // Frame can hold the entire display. Use exact values.
                return RecordingInfo(displayWidth, displayHeight, cameraFrameRate, displayDensity)
            }

            // Calculate new width or height to preserve aspect ratio.
            if (isLandscapeDevice) {
                frameWidth = displayWidth * frameHeight / displayHeight
            } else {
                frameHeight = displayHeight * frameWidth / displayWidth
            }
            return RecordingInfo(frameWidth, frameHeight, cameraFrameRate, displayDensity)
        }
    }
}