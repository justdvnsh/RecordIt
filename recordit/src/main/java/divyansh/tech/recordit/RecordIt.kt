package divyansh.tech.recordit

import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.*
import android.util.DisplayMetrics
import android.util.Log
import androidx.annotation.DrawableRes
import androidx.annotation.RequiresApi
import divyansh.tech.recordit.background.ScreenRecordService
import divyansh.tech.recordit.listeners.BaseListener
import divyansh.tech.recordit.listeners.RecordItListener
import divyansh.tech.recordit.utils.CodecInfo
import divyansh.tech.recordit.utils.Constants.ERROR_KEY
import divyansh.tech.recordit.utils.Constants.ERROR_REASON_KEY
import divyansh.tech.recordit.utils.Constants.GENERAL_ERROR
import divyansh.tech.recordit.utils.Constants.MAX_FILE_SIZE_KEY
import divyansh.tech.recordit.utils.Constants.NO_SPECIFIED_MAX_SIZE
import divyansh.tech.recordit.utils.Constants.ON_COMPLETE_KEY
import divyansh.tech.recordit.utils.Constants.ON_START_KEY
import divyansh.tech.recordit.utils.Countdown
import divyansh.tech.recordit.utils.FileObserver
import java.io.ByteArrayOutputStream
import java.io.File


class RecordIt: BaseListener{

    private var mScreenWidth = 0
    private var mScreenHeight = 0
    private var mScreenDensity = 0
    private val context: Context? = null
    private var resultCode = 0
    private var isAudioEnabled = true
    private var isVideoHDEnabled = true
    private var activity: Activity? = null
    private var outputPath: String? = null
    private var fileName: String? = null
    private var notificationTitle: String? = null
    private var notificationDescription: String? = null
    private var notificationButtonText: String? = null
    private var audioBitrate = 0
    private var audioSamplingRate = 0
    private var observer: FileObserver? = null
    private val recorderListener: RecordItListener? = null
    private var byteArray: ByteArray = byteArrayOf()
    private var vectorDrawable = 0
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

    //Get default width
    fun getDefaultWidth(): Int {
        val codecInfo = CodecInfo()
        codecInfo.setContext(context)
        return codecInfo.maxSupportedWidth
    }

    //Get default height
    fun getDefaultHeight(): Int {
        val codecInfo = CodecInfo()
        codecInfo.setContext(context)
        return codecInfo.maxSupportedHeight
    }

    //Set Custom Dimensions (NOTE - YOUR DEVICE MIGHT NOT SUPPORT THE SIZE YOU PASS IT)
    fun setScreenDimensions(heightInPX: Int, widthInPX: Int) {
        mScreenHeight = heightInPX
        mScreenWidth = widthInPX
    }

    /*Get file path including file name and extension*/
    fun getFilePath(): String? {
        return ScreenRecordService.filePath
    }

    /*Get file name and extension*/
    fun getFileName(): String? {
        return ScreenRecordService.fileName
    }

    /*Start screen recording*/
    fun startScreenRecording(data: Intent, resultCode: Int, activity: Activity?) {
        this.resultCode = resultCode
        this.activity = activity
        startService(data)
    }

    /*Stop screen recording*/
    fun stopScreenRecording() {
        val service = Intent(context, ScreenRecordService::class.java)
        context!!.stopService(service)
    }

    /*Pause screen recording*/
    @RequiresApi(api = Build.VERSION_CODES.N)
    fun pauseScreenRecording() {
        if (service != null) {
            isPaused = true
            service!!.action = "pause"
            context!!.startService(service)
        }
    }

    /*Pause screen recording*/
    @RequiresApi(api = Build.VERSION_CODES.N)
    fun resumeScreenRecording() {
        if (service != null) {
            isPaused = false
            service!!.action = "resume"
            context!!.startService(service)
        }
    }

    /*Check if video is paused*/
    fun isRecordingPaused(): Boolean {
        return isPaused
    }

    /*Check if recording is in progress*/
    fun isBusyRecording(): Boolean {
        val manager = context!!.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        if (manager != null) {
            for (service in manager.getRunningServices(Int.MAX_VALUE)) {
                if (ScreenRecordService::class.java.name == service.service.className) {
                    return true
                }
            }
        }
        return false
    }

    /*Change notification icon Drawable*/
    fun setNotificationSmallIcon(@DrawableRes drawable: Int) {
        val icon = BitmapFactory.decodeResource(context!!.resources, drawable)
        val stream = ByteArrayOutputStream()
        icon.compress(Bitmap.CompressFormat.PNG, 100, stream)
        byteArray = stream.toByteArray()
    }

    /*Change notification icon using Vector Drawable*/
    fun setNotificationSmallIconVector(@DrawableRes VectorDrawable: Int) {
        vectorDrawable = VectorDrawable
    }

    /*Change notification icon using byte[]*/
    fun setNotificationSmallIcon(bytes: ByteArray) {
        byteArray = bytes
    }

    /*Set notification title*/
    fun setNotificationTitle(Title: String) {
        notificationTitle = Title
    }

    /*Set notification description*/
    fun setNotificationDescription(Description: String) {
        notificationDescription = Description
    }

    fun setNotificationButtonText(string: String) {
        notificationButtonText = string
    }

    /*Start recording service*/
    private fun startService(data: Intent) {
        try {
            if (!mWasUriSet) {
                if (outputPath != null) {
                    val file = File(outputPath)
                    val parent: String = file.parent
                    observer = FileObserver(parent, activity!!, this@RecordIt)
                } else {
                    observer = FileObserver(
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
                            .toString(), activity!!, this@RecordIt
                    )
                }
                observer!!.startWatching()
            }
            service = Intent(context, ScreenRecordService::class.java)
            if (mWasUriSet) {
                service!!.putExtra("mUri", mUri.toString())
            }
            service!!.putExtra("code", resultCode)
            service!!.putExtra("data", data)
            service!!.putExtra("audio", isAudioEnabled)
            service!!.putExtra("width", mScreenWidth)
            service!!.putExtra("height", mScreenHeight)
            service!!.putExtra("density", mScreenDensity)
            service!!.putExtra("quality", isVideoHDEnabled)
            service!!.putExtra("path", outputPath)
            service!!.putExtra("fileName", fileName)
            service!!.putExtra("orientation", orientation)
            service!!.putExtra("audioBitrate", audioBitrate)
            service!!.putExtra("audioSamplingRate", audioSamplingRate)
            service!!.putExtra("notificationSmallBitmap", byteArray)
            service!!.putExtra("notificationSmallVector", vectorDrawable)
            service!!.putExtra("notificationTitle", notificationTitle)
            service!!.putExtra("notificationDescription", notificationDescription)
            service!!.putExtra("notificationButtonText", notificationButtonText)
            service!!.putExtra("enableCustomSettings", enableCustomSettings)
            service!!.putExtra("audioSource", audioSource)
            service!!.putExtra("videoEncoder", videoEncoder)
            service!!.putExtra("videoFrameRate", videoFrameRate)
            service!!.putExtra("videoBitrate", videoBitrate)
            service!!.putExtra("outputFormat", outputFormat)
            service!!.putExtra(
                ScreenRecordService.BUNDLED_LISTENER,
                object : ResultReceiver(Handler()) {
                    override fun onReceiveResult(resultCode: Int, resultData: Bundle) {
                        super.onReceiveResult(resultCode, resultData)
                        if (resultCode == Activity.RESULT_OK) {
                            val errorListener = resultData.getString(ERROR_REASON_KEY)
                            val onComplete = resultData.getString(ON_COMPLETE_KEY)
                            val onStartCode = resultData.getInt(ON_START_KEY)
                            val errorCode = resultData.getInt(ERROR_KEY)
                            if (errorListener != null) {
                                //Stop countdown if it was set
                                stopCountDown()
                                if (!mWasUriSet) {
                                    observer!!.stopWatching()
                                }
                                wasOnErrorCalled = true
                                if (errorCode > 0) {
                                    recorderListener!!.onRecordingError(errorCode, errorListener)
                                } else {
                                    recorderListener!!.onRecordingError(
                                        GENERAL_ERROR,
                                        errorListener
                                    )
                                }
                                try {
                                    val mService = Intent(context, ScreenRecordService::class.java)
                                    context!!.stopService(mService)
                                } catch (e: Exception) {
                                    // Can be ignored
                                }
                            } else if (onComplete != null) {
                                //Stop countdown if it was set
                                stopCountDown()
                                //OnComplete for when Uri was passed
                                if (mWasUriSet && !wasOnErrorCalled) {
                                    recorderListener!!.onRecordingCompleted()
                                }
                                wasOnErrorCalled = false
                            } else if (onStartCode != 0) {
                                recorderListener!!.onRecordingStarted()
                                //Check if max duration was set and start count down
                                if (isMaxDurationSet) {
                                    startCountdown()
                                }
                            }
                        }
                    }
                })
            // Max file size
            service!!.putExtra(MAX_FILE_SIZE_KEY, maxFileSize)
            context!!.startService(service)
        } catch (e: Exception) {
            recorderListener!!.onRecordingError(0, Log.getStackTraceString(e))
        }
    }

    /*CountdownTimer for when max duration is set*/
    var countDown: Countdown? = null
    private fun startCountdown() {
        countDown = object : Countdown(maxDuration.toLong(), 1000, 0) {
            override fun onTick(timeLeft: Long) {
                // Could add a callback to provide the time to the user
                // Will add if users request this
            }

            override fun onFinished() {
                onTick(0)
                // Since the timer is running on a different thread
                // UI chances should be called from the UI Thread
                activity!!.runOnUiThread {
                    try {
                        stopScreenRecording()
                        observer!!.stopWatching()
                        recorderListener!!.onRecordingCompleted()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }

            override fun onStopCalled() {
                // Currently unused, but might be helpful in the future
            }
        }
        (countDown as Countdown).start()
    }

    private fun stopCountDown() {
        if (countDown != null) {
            countDown!!.stop()
        }
    }

    /*Complete callback method*/
    override fun callback() {
        observer!!.stopWatching()
        recorderListener!!.onRecordingCompleted()
    }
}