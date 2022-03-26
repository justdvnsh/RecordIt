package divyansh.tech.recordit.listeners

interface RecordItListener {
    fun onRecordingStarted()
    fun onRecordingCompleted()
    fun onRecordingError(errorCode: Int, message: String)
}