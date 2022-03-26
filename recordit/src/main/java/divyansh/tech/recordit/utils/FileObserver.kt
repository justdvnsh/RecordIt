package divyansh.tech.recordit.utils

import android.app.Activity
import android.os.FileObserver
import divyansh.tech.recordit.listeners.BaseListener
import java.io.File
import java.util.*


internal class FileObserver(private val mPath: String, activity: Activity, ml: BaseListener) :
    FileObserver(mPath, ALL_EVENTS) {
    private var mObservers: MutableList<SingleFileObserver>? = null
    private val mMask: Int = ALL_EVENTS
    private val activity: Activity = activity
    private val ml: BaseListener = ml
    override fun startWatching() {
        if (mObservers != null) return
        mObservers = ArrayList()
        val stack = Stack<String>()
        stack.push(mPath)
        while (!stack.isEmpty()) {
            val parent = stack.pop()
            (mObservers as ArrayList<SingleFileObserver>).add(SingleFileObserver(parent, mMask))
            val path = File(parent)
            val files = path.listFiles() ?: continue
            for (f in files) {
                if (f.isDirectory && f.name != "." && f.name != "..") {
                    stack.push(f.path)
                }
            }
        }
        for (sfo in mObservers as ArrayList<SingleFileObserver>) {
            sfo.startWatching()
        }
    }

    override fun stopWatching() {
        if (mObservers == null) return
        for (sfo in mObservers!!) {
            sfo.stopWatching()
        }
        mObservers!!.clear()
        mObservers = null
    }

    override fun onEvent(event: Int, path: String?) {
        if (event == CLOSE_WRITE) {
            activity.runOnUiThread { ml.callback() }
        }
    }

    internal inner class SingleFileObserver(val mPath: String, mask: Int) :
        FileObserver(mPath, mask) {
        override fun onEvent(event: Int, path: String?) {
            val newPath = "$mPath/$path"
            this@FileObserver.onEvent(event, newPath)
        }
    }

}