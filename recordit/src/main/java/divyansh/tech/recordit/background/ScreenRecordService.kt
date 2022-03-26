package divyansh.tech.recordit.background

import android.R
import android.app.*
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.*
import android.util.Log
import androidx.annotation.RequiresApi
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.graphics.Color;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import divyansh.tech.recordit.utils.CodecInfo
import divyansh.tech.recordit.utils.Constants.ERROR_KEY
import divyansh.tech.recordit.utils.Constants.ERROR_REASON_KEY
import divyansh.tech.recordit.utils.Constants.MAX_FILE_SIZE_KEY
import divyansh.tech.recordit.utils.Constants.MAX_FILE_SIZE_REACHED_ERROR
import divyansh.tech.recordit.utils.Constants.NO_SPECIFIED_MAX_SIZE
import divyansh.tech.recordit.utils.Constants.ON_COMPLETE
import divyansh.tech.recordit.utils.Constants.ON_COMPLETE_KEY
import divyansh.tech.recordit.utils.Constants.ON_START
import divyansh.tech.recordit.utils.Constants.ON_START_KEY
import divyansh.tech.recordit.utils.Constants.SETTINGS_ERROR
import java.io.FileDescriptor
import java.text.SimpleDateFormat
import java.util.*


class ScreenRecordService : Service() {
    private var maxFileSize = NO_SPECIFIED_MAX_SIZE
    private var hasMaxFileBeenReached = false
    private var mScreenWidth = 0
    private var mScreenHeight = 0
    private var mScreenDensity = 0
    private var mResultCode = 0
    private var mResultData: Intent? = null
    private var isVideoHD = false
    private var isAudioEnabled = false
    private var path: String? = null
    private var mMediaProjection: MediaProjection? = null
    private var mMediaRecorder: MediaRecorder? = null
    private var mVirtualDisplay: VirtualDisplay? = null
    private var name: String? = null
    private var audioBitrate = 0
    private var audioSamplingRate = 0
    private var audioSourceAsInt = 0
    private var videoEncoderAsInt = 0
    private var isCustomSettingsEnabled = false
    private var videoFrameRate = 0
    private var videoBitrate = 0
    private var outputFormatAsInt = 0
    private var orientationHint = 0
    private var returnedUri: Uri? = null
    private var mIntent: Intent? = null

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        val pauseResumeAction = intent.action
        //Pause Recording
        if (pauseResumeAction != null && pauseResumeAction == "pause") {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                pauseRecording()
            }
        } else if (pauseResumeAction != null && pauseResumeAction == "resume") {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                resumeRecording()
            }
        } else {
            //Get intent extras
            hasMaxFileBeenReached = false
            mIntent = intent
            maxFileSize = intent.getLongExtra(MAX_FILE_SIZE_KEY, NO_SPECIFIED_MAX_SIZE.toLong()).toInt()
            val notificationSmallIcon = intent.getByteArrayExtra("notificationSmallBitmap")
            val notificationSmallVector = intent.getIntExtra("notificationSmallVector", 0)
            var notificationTitle = intent.getStringExtra("notificationTitle")
            var notificationDescription = intent.getStringExtra("notificationDescription")
            var notificationButtonText = intent.getStringExtra("notificationButtonText")
            orientationHint = intent.getIntExtra("orientation", 400)
            mResultCode = intent.getIntExtra("code", -1)
            mResultData = intent.getParcelableExtra("data")
            mScreenWidth = intent.getIntExtra("width", 0)
            mScreenHeight = intent.getIntExtra("height", 0)
            if (intent.getStringExtra("mUri") != null) {
                returnedUri = Uri.parse(intent.getStringExtra("mUri"))
            }
            if (mScreenHeight == 0 || mScreenWidth == 0) {
                val codecInfo = CodecInfo()
                codecInfo.setContext(this)
                mScreenHeight = codecInfo.maxSupportedHeight
                mScreenWidth = codecInfo.maxSupportedWidth
            }
            mScreenDensity = intent.getIntExtra("density", 1)
            isVideoHD = intent.getBooleanExtra("quality", true)
            isAudioEnabled = intent.getBooleanExtra("audio", true)
            path = intent.getStringExtra("path")
            name = intent.getStringExtra("fileName")
            val audioSource = intent.getStringExtra("audioSource")
            val videoEncoder = intent.getStringExtra("videoEncoder")
            videoFrameRate = intent.getIntExtra("videoFrameRate", 30)
            videoBitrate = intent.getIntExtra("videoBitrate", 40000000)
            audioSource?.let { setAudioSourceAsInt(it) }
            videoEncoder?.let { setvideoEncoderAsInt(it) }
            filePath = name
            audioBitrate = intent.getIntExtra("audioBitrate", 128000)
            audioSamplingRate = intent.getIntExtra("audioSamplingRate", 44100)
            val outputFormat = intent.getStringExtra("outputFormat")
            outputFormat?.let { setOutputFormatAsInt(it) }
            isCustomSettingsEnabled = intent.getBooleanExtra("enableCustomSettings", false)

            //Set notification notification button text if developer did not
            if (notificationButtonText == null) {
                notificationButtonText = "STOP RECORDING"
            }
            //Set notification bitrate if developer did not
            if (audioBitrate == 0) {
                audioBitrate = 128000
            }
            //Set notification sampling rate if developer did not
            if (audioSamplingRate == 0) {
                audioSamplingRate = 44100
            }
            //Set notification title if developer did not
            if (notificationTitle == null || notificationTitle == "") {
                notificationTitle = "Recording your screen"
            }
            //Set notification description if developer did not
            if (notificationDescription == null || notificationDescription == "") {
                notificationDescription = "Drag down to stop the recording"
            }

            //Notification
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channelId = "001"
                val channelName = "RecordChannel"
                val channel =
                    NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_NONE)
                channel.lightColor = Color.BLUE
                channel.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
                val manager =
                    getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager?
                if (manager != null) {
                    manager.createNotificationChannel(channel)
                    val notification: Notification
                    val myIntent = Intent(this, NotificationReceiver::class.java)
                    val pendingIntent: PendingIntent
                    pendingIntent = if (Build.VERSION.SDK_INT >= 31) {
                        PendingIntent.getBroadcast(this, 0, myIntent, PendingIntent.FLAG_IMMUTABLE)
                    } else {
                        PendingIntent.getBroadcast(this, 0, myIntent, 0)
                    }
                    val action: Notification.Action = Notification.Action.Builder(
                        Icon.createWithResource(this, R.drawable.presence_video_online),
                        notificationButtonText,
                        pendingIntent
                    ).build()
                    notification = when {
                        notificationSmallIcon != null -> {
                            val bmp = BitmapFactory.decodeByteArray(
                                notificationSmallIcon,
                                0,
                                notificationSmallIcon.size
                            )
                            //Modify notification badge
                            Notification.Builder(applicationContext, channelId).setOngoing(
                                true
                            ).setSmallIcon(Icon.createWithBitmap(bmp))
                                .setContentTitle(notificationTitle)
                                .setContentText(notificationDescription).addAction(action).build()
                        }
                        notificationSmallVector != 0 -> {
                            Notification.Builder(applicationContext, channelId).setOngoing(
                                true
                            ).setSmallIcon(notificationSmallVector).setContentTitle(notificationTitle)
                                .setContentText(notificationDescription).addAction(action).build()
                        }
                        else -> {
                            //Modify notification badge
                            Notification.Builder(applicationContext, channelId).setOngoing(
                                true
                            ).setSmallIcon(R.drawable.stat_notify_chat).setContentTitle(notificationTitle)
                                .setContentText(notificationDescription).addAction(action).build()
                        }
                    }
                    startForeground(101, notification)
                }
            } else {
                startForeground(101, Notification())
            }
            if (returnedUri == null) {
                if (path == null) {
                    path =
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
                            .toString()
                }
            }

            //Init MediaRecorder
            try {
                initRecorder()
            } catch (e: Exception) {
                val receiver: ResultReceiver = intent.getParcelableExtra(BUNDLED_LISTENER)!!
                val bundle = Bundle()
                bundle.putString(ERROR_REASON_KEY, Log.getStackTraceString(e))
                if (receiver != null) {
                    receiver.send(Activity.RESULT_OK, bundle)
                }
            }

            //Init MediaProjection
            try {
                initMediaProjection()
            } catch (e: Exception) {
                val receiver: ResultReceiver = intent.getParcelableExtra(BUNDLED_LISTENER)!!
                val bundle = Bundle()
                bundle.putString(ERROR_REASON_KEY, Log.getStackTraceString(e))
                if (receiver != null) {
                    receiver.send(Activity.RESULT_OK, bundle)
                }
            }

            //Init VirtualDisplay
            try {
                initVirtualDisplay()
            } catch (e: Exception) {
                val receiver: ResultReceiver = intent.getParcelableExtra(BUNDLED_LISTENER)!!
                val bundle = Bundle()
                bundle.putString(ERROR_REASON_KEY, Log.getStackTraceString(e))
                if (receiver != null) {
                    receiver.send(Activity.RESULT_OK, bundle)
                }
            }
            mMediaRecorder!!.setOnErrorListener(MediaRecorder.OnErrorListener { mediaRecorder, what, extra ->
                if (what == 268435556 && hasMaxFileBeenReached) {
                    // Benign error b/c recording is too short and has no frames. See SO: https://stackoverflow.com/questions/40616466/mediarecorder-stop-failed-1007
                    return@OnErrorListener
                }
                val receiver: ResultReceiver = intent.getParcelableExtra(BUNDLED_LISTENER)!!
                val bundle = Bundle()
                bundle.putInt(ERROR_KEY, SETTINGS_ERROR)
                bundle.putString(ERROR_REASON_KEY, what.toString())
                if (receiver != null) {
                    receiver.send(Activity.RESULT_OK, bundle)
                }
            })
            mMediaRecorder!!.setOnInfoListener { mr, what, extra ->
                if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED) {
                    hasMaxFileBeenReached = true
                    Log.i(
                        TAG,
                        java.lang.String.format(
                            Locale.US,
                            "onInfoListen what : %d | extra %d",
                            what,
                            extra
                        )
                    )
                    val receiver: ResultReceiver =
                        intent.getParcelableExtra(BUNDLED_LISTENER)!!
                    val bundle = Bundle()
                    bundle.putInt(ERROR_KEY, MAX_FILE_SIZE_REACHED_ERROR)
                    bundle.putString(ERROR_REASON_KEY, "File size max has been reached.")
                    if (receiver != null) {
                        receiver.send(Activity.RESULT_OK, bundle)
                    }
                }
            }

            //Start Recording
            try {
                mMediaRecorder!!.start()
                val receiver: ResultReceiver = intent.getParcelableExtra(BUNDLED_LISTENER)!!
                val bundle = Bundle()
                bundle.putInt(ON_START_KEY, ON_START)
                if (receiver != null) {
                    receiver.send(Activity.RESULT_OK, bundle)
                }
            } catch (e: Exception) {
                // From the tests I've done, this can happen if another application is using the mic or if an unsupported video encoder was selected
                val receiver: ResultReceiver = intent.getParcelableExtra(BUNDLED_LISTENER)!!
                val bundle = Bundle()
                bundle.putInt(ERROR_KEY, SETTINGS_ERROR)
                bundle.putString(ERROR_REASON_KEY, Log.getStackTraceString(e))
                if (receiver != null) {
                    receiver.send(Activity.RESULT_OK, bundle)
                }
            }
        }
        return Service.START_STICKY
    }

    //Pause Recording
    @RequiresApi(api = Build.VERSION_CODES.N)
    private fun pauseRecording() {
        mMediaRecorder!!.pause()
    }

    //Resume Recording
    @RequiresApi(api = Build.VERSION_CODES.N)
    private fun resumeRecording() {
        mMediaRecorder!!.resume()
    }

    //Set output format as int based on what developer has provided
    //It is important to provide one of the following and nothing else.
    private fun setOutputFormatAsInt(outputFormat: String) {
        outputFormatAsInt = when (outputFormat) {
            "DEFAULT" -> 0
            "THREE_GPP" -> 1
            "AMR_NB" -> 3
            "AMR_WB" -> 4
            "AAC_ADTS" -> 6
            "MPEG_2_TS" -> 8
            "WEBM" -> 9
            "OGG" -> 11
            "MPEG_4" -> 2
            else -> 2
        }
    }

    //Set video encoder as int based on what developer has provided
    //It is important to provide one of the following and nothing else.
    private fun setvideoEncoderAsInt(encoder: String) {
        when (encoder) {
            "DEFAULT" -> videoEncoderAsInt = 0
            "H263" -> videoEncoderAsInt = 1
            "H264" -> videoEncoderAsInt = 2
            "MPEG_4_SP" -> videoEncoderAsInt = 3
            "VP8" -> videoEncoderAsInt = 4
            "HEVC" -> videoEncoderAsInt = 5
        }
    }

    //Set audio source as int based on what developer has provided
    //It is important to provide one of the following and nothing else.
    private fun setAudioSourceAsInt(audioSource: String) {
        when (audioSource) {
            "DEFAULT" -> audioSourceAsInt = 0
            "MIC" -> audioSourceAsInt = 1
            "VOICE_UPLINK" -> audioSourceAsInt = 2
            "VOICE_DOWNLINK" -> audioSourceAsInt = 3
            "VOICE_CALL" -> audioSourceAsInt = 4
            "CAMCODER" -> audioSourceAsInt = 5
            "VOICE_RECOGNITION" -> audioSourceAsInt = 6
            "VOICE_COMMUNICATION" -> audioSourceAsInt = 7
            "REMOTE_SUBMIX" -> audioSourceAsInt = 8
            "UNPROCESSED" -> audioSourceAsInt = 9
            "VOICE_PERFORMANCE" -> audioSourceAsInt = 10
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private fun initMediaProjection() {
        mMediaProjection =
            (Objects.requireNonNull(getSystemService(Context.MEDIA_PROJECTION_SERVICE)) as MediaProjectionManager).getMediaProjection(
                mResultCode,
                mResultData!!
            )
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Throws(Exception::class)
    private fun initRecorder() {
        val formatter = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.getDefault())
        val curDate = Date(System.currentTimeMillis())
        val curTime: String = formatter.format(curDate).replace(" ", "")
        var videoQuality = "HD"
        if (!isVideoHD) {
            videoQuality = "SD"
        }
        if (name == null) {
            name = videoQuality + curTime
        }
        filePath = "$path/$name.mp4"
        fileName = "$name.mp4"
        mMediaRecorder = MediaRecorder()
        if (isAudioEnabled) {
            mMediaRecorder!!.setAudioSource(audioSourceAsInt)
        }
        mMediaRecorder!!.setVideoSource(MediaRecorder.VideoSource.SURFACE)
        mMediaRecorder!!.setOutputFormat(outputFormatAsInt)
        if (orientationHint != 400) {
            mMediaRecorder!!.setOrientationHint(orientationHint)
        }
        if (isAudioEnabled) {
            mMediaRecorder!!.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            mMediaRecorder!!.setAudioEncodingBitRate(audioBitrate)
            mMediaRecorder!!.setAudioSamplingRate(audioSamplingRate)
        }
        mMediaRecorder!!.setVideoEncoder(videoEncoderAsInt)
        if (returnedUri != null) {
            try {
                val contentResolver: ContentResolver = getContentResolver()
                val inputPFD: FileDescriptor =
                    Objects.requireNonNull(contentResolver.openFileDescriptor(returnedUri!!, "rw"))!!.fileDescriptor
                mMediaRecorder!!.setOutputFile(inputPFD)
            } catch (e: Exception) {
                val receiver: ResultReceiver = mIntent!!.getParcelableExtra(BUNDLED_LISTENER)!!
                val bundle = Bundle()
                bundle.putString(ERROR_REASON_KEY, Log.getStackTraceString(e))
                if (receiver != null) {
                    receiver.send(Activity.RESULT_OK, bundle)
                }
            }
        } else {
            mMediaRecorder!!.setOutputFile(filePath)
        }
        mMediaRecorder!!.setVideoSize(mScreenWidth, mScreenHeight)
        if (!isCustomSettingsEnabled) {
            if (!isVideoHD) {
                mMediaRecorder!!.setVideoEncodingBitRate(12000000)
                mMediaRecorder!!.setVideoFrameRate(30)
            } else {
                mMediaRecorder!!.setVideoEncodingBitRate(5 * mScreenWidth * mScreenHeight)
                mMediaRecorder!!.setVideoFrameRate(60) //after setVideoSource(), setOutFormat()
            }
        } else {
            mMediaRecorder!!.setVideoEncodingBitRate(videoBitrate)
            mMediaRecorder!!.setVideoFrameRate(videoFrameRate)
        }

        // Catch approaching file limit
        if (maxFileSize > NO_SPECIFIED_MAX_SIZE) {
            mMediaRecorder!!.setMaxFileSize(maxFileSize.toLong()) // in bytes
        }
        mMediaRecorder!!.prepare()
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private fun initVirtualDisplay() {
        mVirtualDisplay = mMediaProjection!!.createVirtualDisplay(
            TAG,
            mScreenWidth,
            mScreenHeight,
            mScreenDensity,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            mMediaRecorder!!.surface,
            null,
            null
        )
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    override fun onDestroy() {
        super.onDestroy()
        resetAll()
        callOnComplete()
    }

    private fun callOnComplete() {
        if (mIntent != null) {
            val receiver: ResultReceiver = mIntent!!.getParcelableExtra(BUNDLED_LISTENER)!!
            val bundle = Bundle()
            bundle.putString(ON_COMPLETE_KEY, ON_COMPLETE)
            if (receiver != null) {
                receiver.send(Activity.RESULT_OK, bundle)
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private fun resetAll() {
        stopForeground(true)
        if (mVirtualDisplay != null) {
            mVirtualDisplay!!.release()
            mVirtualDisplay = null
        }
        if (mMediaRecorder != null) {
            mMediaRecorder!!.setOnErrorListener(null)
            mMediaRecorder!!.reset()
        }
        if (mMediaProjection != null) {
            mMediaProjection!!.stop()
            mMediaProjection = null
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    companion object {
        private const val TAG = "ScreenRecordService"

        //Return the output file path as string
        var filePath: String? = null
            private set

        //Return the name of the output file
        var fileName: String? = null
            private set
        const val BUNDLED_LISTENER = "listener"

    }
}