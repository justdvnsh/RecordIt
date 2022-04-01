package divyansh.tech.recordit

import android.R
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaScannerConnection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.CheckBox
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.Manifest;
import android.content.Context;
import android.content.res.Configuration.ORIENTATION_PORTRAIT
import android.net.Uri;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import divyansh.tech.recordit.listeners.RecordItListener
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;


class MainActivity : AppCompatActivity(), RecordItListener {
    private var hasPermissions = false

    //Declare HBRecorder
    private var hbRecorder: RecordIt? = null

    //Start/Stop Button
    private var startbtn: Button? = null

    //HD/SD quality
    private var radioGroup: RadioGroup? = null

    //Should record/show audio/notification
    private var recordAudioCheckBox: CheckBox? = null

    //Reference to checkboxes and radio buttons
    var wasHDSelected = true
    var isAudioEnabled = true

    //Should custom settings be used
    var custom_settings_switch: SwitchCompat? = null

    // Max file size in K
    private val maxFileSizeInK: EditText? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initViews()
        setOnClickListeners()
        setRadioGroupCheckListener()
        setRecordAudioCheckBoxListener()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            //Init HBRecorder
            hbRecorder = RecordIt(this, this)

            //When the user returns to the application, some UI changes might be necessary,
            //check if recording is in progress and make changes accordingly
            if (hbRecorder.isBusyRecording()) {
                startbtn.setText(R.string.stop_recording)
            }
        }

        // Examples of how to use the HBRecorderCodecInfo class to get codec info
        val hbRecorderCodecInfo = HBRecorderCodecInfo()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val mWidth: Int = hbRecorder.getDefaultWidth()
            val mHeight: Int = hbRecorder.getDefaultHeight()
            val mMimeType = "video/avc"
            val mFPS = 30
            if (hbRecorderCodecInfo.isMimeTypeSupported(mMimeType)) {
                val defaultVideoEncoder: String =
                    hbRecorderCodecInfo.getDefaultVideoEncoderName(mMimeType)
                val isSizeAndFramerateSupported: Boolean =
                    hbRecorderCodecInfo.isSizeAndFramerateSupported(
                        mWidth,
                        mHeight,
                        mFPS,
                        mMimeType,
                        ORIENTATION_PORTRAIT
                    )
                Log.e(
                    "EXAMPLE",
                    "THIS IS AN EXAMPLE OF HOW TO USE THE (HBRecorderCodecInfo) TO GET CODEC INFO:"
                )
                Log.e(
                    "HBRecorderCodecInfo",
                    "defaultVideoEncoder for ($mMimeType) -> $defaultVideoEncoder"
                )
                Log.e(
                    "HBRecorderCodecInfo",
                    "MaxSupportedFrameRate -> " + hbRecorderCodecInfo.getMaxSupportedFrameRate(
                        mWidth,
                        mHeight,
                        mMimeType
                    )
                )
                Log.e(
                    "HBRecorderCodecInfo",
                    "MaxSupportedBitrate -> " + hbRecorderCodecInfo.getMaxSupportedBitrate(mMimeType)
                )
                Log.e(
                    "HBRecorderCodecInfo",
                    "isSizeAndFramerateSupported @ Width = $mWidth Height = $mHeight FPS = $mFPS -> $isSizeAndFramerateSupported"
                )
                Log.e(
                    "HBRecorderCodecInfo",
                    "isSizeSupported @ Width = " + mWidth + " Height = " + mHeight + " -> " + hbRecorderCodecInfo.isSizeSupported(
                        mWidth,
                        mHeight,
                        mMimeType
                    )
                )
                Log.e(
                    "HBRecorderCodecInfo",
                    "Default Video Format = " + hbRecorderCodecInfo.getDefaultVideoFormat()
                )
                val supportedVideoMimeTypes: HashMap<String, String> =
                    hbRecorderCodecInfo.getSupportedVideoMimeTypes()
                for ((key, value) in supportedVideoMimeTypes.entrySet()) {
                    Log.e(
                        "HBRecorderCodecInfo",
                        "Supported VIDEO encoders and mime types : $key -> $value"
                    )
                }
                val supportedAudioMimeTypes: HashMap<String, String> =
                    hbRecorderCodecInfo.getSupportedAudioMimeTypes()
                for ((key, value) in supportedAudioMimeTypes.entrySet()) {
                    Log.e(
                        "HBRecorderCodecInfo",
                        "Supported AUDIO encoders and mime types : $key -> $value"
                    )
                }
                val supportedVideoFormats: ArrayList<String> =
                    hbRecorderCodecInfo.getSupportedVideoFormats()
                for (j in 0 until supportedVideoFormats.size()) {
                    Log.e(
                        "HBRecorderCodecInfo",
                        "Available Video Formats : " + supportedVideoFormats[j]
                    )
                }
            } else {
                Log.e("HBRecorderCodecInfo", "MimeType not supported")
            }
        }
    }

    //Create Folder
    //Only call this on Android 9 and lower (getExternalStoragePublicDirectory is deprecated)
    //This can still be used on Android 10> but you will have to add android:requestLegacyExternalStorage="true" in your Manifest
    private fun createFolder() {
        val f1 = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
            "HBRecorder"
        )
        if (!f1.exists()) {
            if (f1.mkdirs()) {
                Log.i("Folder ", "created")
            }
        }
    }

    //Init Views
    private fun initViews() {
        startbtn = findViewById(R.id.button_start)
        radioGroup = findViewById(R.id.radio_group)
        recordAudioCheckBox = findViewById(R.id.audio_check_box)
        custom_settings_switch = findViewById(R.id.custom_settings_switch)
    }

    //Start Button OnClickListener
    private fun setOnClickListeners() {
        startbtn.setOnClickListener(object : OnClickListener() {
            fun onClick(v: View?) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    //first check if permissions was granted
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        if (checkSelfPermission(
                                Manifest.permission.RECORD_AUDIO,
                                PERMISSION_REQ_ID_RECORD_AUDIO
                            )
                        ) {
                            hasPermissions = true
                        }
                    } else {
                        if (checkSelfPermission(
                                Manifest.permission.RECORD_AUDIO,
                                PERMISSION_REQ_ID_RECORD_AUDIO
                            ) && checkSelfPermission(
                                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                PERMISSION_REQ_ID_WRITE_EXTERNAL_STORAGE
                            )
                        ) {
                            hasPermissions = true
                        }
                    }
                    if (hasPermissions) {
                        //check if recording is in progress
                        //and stop it if it is
                        if (hbRecorder.isBusyRecording()) {
                            hbRecorder.stopScreenRecording()
                            startbtn.setText(R.string.start_recording)
                        } else {
                            startRecordingScreen()
                        }
                    }
                } else {
                    showLongToast("This library requires API 21>")
                }
            }
        })
    }

    //Check if HD/SD Video should be recorded
    private fun setRadioGroupCheckListener() {
        radioGroup!!.setOnCheckedChangeListener { radioGroup, checkedId ->
            if (checkedId == R.id.hd_button) {
                //Ser HBRecorder to HD
                wasHDSelected = true
            } else if (checkedId == R.id.sd_button) {
                //Ser HBRecorder to SD
                wasHDSelected = false
            }
        }
    }

    //Check if audio should be recorded
    private fun setRecordAudioCheckBoxListener() {
        recordAudioCheckBox!!.setOnCheckedChangeListener { compoundButton, isChecked -> //Enable/Disable audio
            isAudioEnabled = isChecked
        }
    }

    fun HBRecorderOnStart() {
        Log.e("HBRecorder", "HBRecorderOnStart called")
    }

    //Listener for when the recording is saved successfully
    //This will be called after the file was created
    fun HBRecorderOnComplete() {
        startbtn.setText(R.string.start_recording)
        showLongToast("Saved Successfully")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            //Update gallery depending on SDK Level
            if (hbRecorder.wasUriSet()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    updateGalleryUri()
                } else {
                    refreshGalleryFile()
                }
            } else {
                refreshGalleryFile()
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private fun refreshGalleryFile() {
        MediaScannerConnection.scanFile(
            this, arrayOf(hbRecorder.getFilePath()), null
        ) { path, uri ->
            Log.i("ExternalStorage", "Scanned $path:")
            Log.i("ExternalStorage", "-> uri=$uri")
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    private fun updateGalleryUri() {
        contentValues!!.clear()
        contentValues!!.put(MediaStore.Video.Media.IS_PENDING, 0)
        contentResolver.update(mUri, contentValues, null, null)
    }

    fun HBRecorderOnError(errorCode: Int, reason: String?) {
        // Error 38 happens when
        // - the selected video encoder is not supported
        // - the output format is not supported
        // - if another app is using the microphone

        //It is best to use device default
        if (errorCode == SETTINGS_ERROR) {
            showLongToast(getString(R.string.settings_not_supported_message))
        } else if (errorCode == MAX_FILE_SIZE_REACHED_ERROR) {
            showLongToast(getString(R.string.max_file_size_reached_message))
        } else {
            showLongToast(getString(R.string.general_recording_error_message))
            Log.e("HBRecorderOnError", reason)
        }
        startbtn.setText(R.string.start_recording)
    }

    //Start recording screen
    //It is important to call it like this
    //hbRecorder.startScreenRecording(data); should only be called in onActivityResult
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private fun startRecordingScreen() {
        if (custom_settings_switch!!.isChecked) {
            //WHEN SETTING CUSTOM SETTINGS YOU MUST SET THIS!!!
            hbRecorder.enableCustomSettings()
            customSettings()
            val mediaProjectionManager =
                getSystemService<Any>(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            val permissionIntent = mediaProjectionManager?.createScreenCaptureIntent()
            startActivityForResult(permissionIntent, SCREEN_RECORD_REQUEST_CODE)
        } else {
            quickSettings()
            val mediaProjectionManager =
                getSystemService<Any>(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            val permissionIntent = mediaProjectionManager?.createScreenCaptureIntent()
            startActivityForResult(permissionIntent, SCREEN_RECORD_REQUEST_CODE)
        }
        startbtn.setText(R.string.stop_recording)
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private fun customSettings() {
        val prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)

        //Is audio enabled
        val audio_enabled = prefs.getBoolean("key_record_audio", true)
        hbRecorder.isAudioEnabled(audio_enabled)

        //Audio Source
        val audio_source = prefs.getString("key_audio_source", null)
        if (audio_source != null) {
            when (audio_source) {
                "0" -> hbRecorder.setAudioSource("DEFAULT")
                "1" -> hbRecorder.setAudioSource("CAMCODER")
                "2" -> hbRecorder.setAudioSource("MIC")
            }
        }

        //Video Encoder
        val video_encoder = prefs.getString("key_video_encoder", null)
        if (video_encoder != null) {
            when (video_encoder) {
                "0" -> hbRecorder.setVideoEncoder("DEFAULT")
                "1" -> hbRecorder.setVideoEncoder("H264")
                "2" -> hbRecorder.setVideoEncoder("H263")
                "3" -> hbRecorder.setVideoEncoder("HEVC")
                "4" -> hbRecorder.setVideoEncoder("MPEG_4_SP")
                "5" -> hbRecorder.setVideoEncoder("VP8")
            }
        }

        //NOTE - THIS MIGHT NOT BE SUPPORTED SIZES FOR YOUR DEVICE
        //Video Dimensions
        val video_resolution = prefs.getString("key_video_resolution", null)
        if (video_resolution != null) {
            when (video_resolution) {
                "0" -> hbRecorder.setScreenDimensions(426, 240)
                "1" -> hbRecorder.setScreenDimensions(640, 360)
                "2" -> hbRecorder.setScreenDimensions(854, 480)
                "3" -> hbRecorder.setScreenDimensions(1280, 720)
                "4" -> hbRecorder.setScreenDimensions(1920, 1080)
            }
        }

        //Video Frame Rate
        val video_frame_rate = prefs.getString("key_video_fps", null)
        if (video_frame_rate != null) {
            when (video_frame_rate) {
                "0" -> hbRecorder.setVideoFrameRate(60)
                "1" -> hbRecorder.setVideoFrameRate(50)
                "2" -> hbRecorder.setVideoFrameRate(48)
                "3" -> hbRecorder.setVideoFrameRate(30)
                "4" -> hbRecorder.setVideoFrameRate(25)
                "5" -> hbRecorder.setVideoFrameRate(24)
            }
        }

        //Video Bitrate
        val video_bit_rate = prefs.getString("key_video_bitrate", null)
        if (video_bit_rate != null) {
            when (video_bit_rate) {
                "1" -> hbRecorder.setVideoBitrate(12000000)
                "2" -> hbRecorder.setVideoBitrate(8000000)
                "3" -> hbRecorder.setVideoBitrate(7500000)
                "4" -> hbRecorder.setVideoBitrate(5000000)
                "5" -> hbRecorder.setVideoBitrate(4000000)
                "6" -> hbRecorder.setVideoBitrate(2500000)
                "7" -> hbRecorder.setVideoBitrate(1500000)
                "8" -> hbRecorder.setVideoBitrate(1000000)
            }
        }

        //Output Format
        val output_format = prefs.getString("key_output_format", null)
        if (output_format != null) {
            when (output_format) {
                "0" -> hbRecorder.setOutputFormat("DEFAULT")
                "1" -> hbRecorder.setOutputFormat("MPEG_4")
                "2" -> hbRecorder.setOutputFormat("THREE_GPP")
                "3" -> hbRecorder.setOutputFormat("WEBM")
            }
        }
    }

    //Get/Set the selected settings
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private fun quickSettings() {
        hbRecorder.setAudioBitrate(128000)
        hbRecorder.setAudioSamplingRate(44100)
        hbRecorder.recordHDVideo(wasHDSelected)
        hbRecorder.isAudioEnabled(isAudioEnabled)
        //Customise Notification
        hbRecorder.setNotificationSmallIcon(R.drawable.icon)
        //hbRecorder.setNotificationSmallIconVector(R.drawable.ic_baseline_videocam_24);
        hbRecorder.setNotificationTitle(getString(R.string.stop_recording_notification_title))
        hbRecorder.setNotificationDescription(getString(R.string.stop_recording_notification_message))
    }

    // Example of how to set the max file size
    /*@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void setRecorderMaxFileSize() {
        String s = maxFileSizeInK.getText().toString();
        long maxFileSizeInKilobytes;
        try {
            maxFileSizeInKilobytes = Long.parseLong(s);
        } catch (NumberFormatException e) {
            maxFileSizeInKilobytes = 0;
        }
        hbRecorder.setMaxFileSize(maxFileSizeInKilobytes * 1024); // Convert to bytes
    }*/
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id: Int = item.getItemId()
        if (id == R.id.action_settings) {
            // launch settings activity
            startActivity(Intent(this@MainActivity, SettingsActivity::class.java))
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    //Check if permissions was granted
    private fun checkSelfPermission(permission: String, requestCode: Int): Boolean {
        if (ContextCompat.checkSelfPermission(
                this,
                permission
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(permission), requestCode)
            return false
        }
        return true
    }

    //Handle permissions
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            PERMISSION_REQ_ID_RECORD_AUDIO -> if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                checkSelfPermission(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    PERMISSION_REQ_ID_WRITE_EXTERNAL_STORAGE
                )
            } else {
                hasPermissions = false
                showLongToast("No permission for " + Manifest.permission.RECORD_AUDIO)
            }
            PERMISSION_REQ_ID_WRITE_EXTERNAL_STORAGE -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                hasPermissions = true
                startRecordingScreen()
            } else {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    hasPermissions = true
                    //Permissions was provided
                    //Start screen recording
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        startRecordingScreen()
                    }
                } else {
                    hasPermissions = false
                    showLongToast("No permission for " + Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }
            else -> {}
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (requestCode == SCREEN_RECORD_REQUEST_CODE) {
                if (resultCode == RESULT_OK) {
                    //Set file path or Uri depending on SDK version
                    setOutputPath()
                    //Start screen recording
                    hbRecorder.startScreenRecording(data, resultCode, this)
                }
            }
        }
    }

    //For Android 10> we will pass a Uri to HBRecorder
    //This is not necessary - You can still use getExternalStoragePublicDirectory
    //But then you will have to add android:requestLegacyExternalStorage="true" in your Manifest
    //IT IS IMPORTANT TO SET THE FILE NAME THE SAME AS THE NAME YOU USE FOR TITLE AND DISPLAY_NAME
    var resolver: ContentResolver? = null
    var contentValues: ContentValues? = null
    var mUri: Uri? = null
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private fun setOutputPath() {
        val filename = generateFileName()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            resolver = contentResolver
            contentValues = ContentValues()
            contentValues!!.put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/" + "HBRecorder")
            contentValues!!.put(MediaStore.Video.Media.TITLE, filename)
            contentValues!!.put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
            contentValues!!.put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
            mUri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues)
            //FILE NAME SHOULD BE THE SAME
            hbRecorder.setFileName(filename)
            hbRecorder.setOutputUri(mUri)
        } else {
            createFolder()
            hbRecorder.setOutputPath(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
                    .toString() + "/HBRecorder"
            )
        }
    }

    //Generate a timestamp to be used as a file name
    private fun generateFileName(): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.getDefault())
        val curDate = Date(System.currentTimeMillis())
        return formatter.format(curDate).replace(" ", "")
    }

    //Show Toast
    private fun showLongToast(msg: String) {
        Toast.makeText(applicationContext, msg, Toast.LENGTH_LONG).show()
    }

    //drawable to byte[]
    private fun drawable2ByteArray(@DrawableRes drawableId: Int): ByteArray {
        val icon = BitmapFactory.decodeResource(resources, drawableId)
        val stream = ByteArrayOutputStream()
        icon.compress(Bitmap.CompressFormat.PNG, 100, stream)
        return stream.toByteArray()
    }

    companion object {
        //Permissions
        private const val SCREEN_RECORD_REQUEST_CODE = 777
        private const val PERMISSION_REQ_ID_RECORD_AUDIO = 22
        private const val PERMISSION_REQ_ID_WRITE_EXTERNAL_STORAGE =
            PERMISSION_REQ_ID_RECORD_AUDIO + 1
    }
}