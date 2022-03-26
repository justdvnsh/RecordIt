package divyansh.tech.recordit

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.net.Uri
import android.os.FileObserver
import android.util.DisplayMetrics
import divyansh.tech.recordit.listeners.BaseListener
import divyansh.tech.recordit.listeners.RecordItListener
import divyansh.tech.recordit.utils.Constants.NO_SPECIFIED_MAX_SIZE


class RecordIt: BaseListener{

    private val mScreenWidth = 0
    private val mScreenHeight = 0
    private var mScreenDensity = 0
    private val context: Context? = null
    private val resultCode = 0
    private var isAudioEnabled = true
    private var isVideoHDEnabled = true
    private val activity: Activity? = null
    private var outputPath: String? = null
    private var fileName: String? = null
    private val notificationTitle: String? = null
    private val notificationDescription: String? = null
    private val notificationButtonText: String? = null
    private var audioBitrate = 0
    private var audioSamplingRate = 0
    private val observer: FileObserver? = null
    private val hbRecorderListener: RecordItListener? = null
    private val byteArray: ByteArray = byteArrayOf()
    private val vectorDrawable = 0
    private var audioSource = "MIC"
    private var videoEncoder = "DEFAULT"
    private var enableCustomSettings = false
    private var videoFrameRate = 30
    private var videoBitrate = 40000000
    private var outputFormat = "DEFAULT"
    private var orientation = 0
    private var maxFileSize = NO_SPECIFIED_MAX_SIZE // Default no max size

    var wasOnErrorCalled = false
    var service: Intent? = null
    var isPaused = false
    var isMaxDurationSet = false
    var maxDuration = 0
    var mUri: Uri? = null
    var mWasUriSet = false

    init {
        setScreenDensity()
    }

    fun setOrientationHint(orientationDegree: Int) {
        orientation = orientationDegree
    }

    fun setOutputPath(path: String) {
        outputPath = path
    }

    fun setOutputUri(uri: Uri) {
        mUri = uri
    }

    /*Set max duration in seconds */
    @JvmName("setMaxDuration1")
    fun setMaxDuration(seconds: Int) {
        isMaxDurationSet = true
        maxDuration = seconds * 1000
    }

    /*Set max file size in kb*/
    fun setMaxFileSize(fileSize: Long) {
        maxFileSize = fileSize.toInt()
    }

    fun wasUriSet(): Boolean {
        return mWasUriSet
    }

    /*Set file name*/
    fun setFileName(fileName: String?) {
        this.fileName = fileName
    }

    /*Set audio bitrate*/
    fun setAudioBitrate(audioBitrate: Int) {
        this.audioBitrate = audioBitrate
    }

    /*Set audio sample rate*/
    fun setAudioSamplingRate(audioSamplingRate: Int) {
        this.audioSamplingRate = audioSamplingRate
    }

    /*Enable/Disable audio*/
    fun isAudioEnabled(bool: Boolean) {
        isAudioEnabled = bool
    }

    /*Set Audio Source*/ //MUST BE ONE OF THE FOLLOWING - https://developer.android.com/reference/android/media/MediaRecorder.AudioSource.html
    fun setAudioSource(source: String) {
        audioSource = source
    }

    /*Enable/Disable HD recording*/
    fun recordHDVideo(bool: Boolean) {
        isVideoHDEnabled = bool
    }

    /*Set Video Encoder*/ //MUST BE ONE OF THE FOLLOWING - https://developer.android.com/reference/android/media/MediaRecorder.VideoEncoder.html
    fun setVideoEncoder(encoder: String) {
        videoEncoder = encoder
    }

    //Enable Custom Settings
    fun enableCustomSettings() {
        enableCustomSettings = true
    }

    //Set Video Frame Rate
    fun setVideoFrameRate(fps: Int) {
        videoFrameRate = fps
    }

    //Set Video BitRate
    fun setVideoBitrate(bitrate: Int) {
        videoBitrate = bitrate
    }

    //Set Output Format
    //MUST BE ONE OF THE FOLLOWING - https://developer.android.com/reference/android/media/MediaRecorder.OutputFormat.html
    fun setOutputFormat(format: String) {
        outputFormat = format
    }

    // Set screen densityDpi
    private fun setScreenDensity() {
        val metrics: DisplayMetrics = Resources.getSystem().displayMetrics
        mScreenDensity = metrics.densityDpi
    }

    override fun callback() {
    }
}