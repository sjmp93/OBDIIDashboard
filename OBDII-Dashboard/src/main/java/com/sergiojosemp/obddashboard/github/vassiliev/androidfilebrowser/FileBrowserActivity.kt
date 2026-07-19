package com.sergiojosemp.obddashboard.github.vassiliev.androidfilebrowser

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.os.Environment
import android.os.StatFs
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import com.sergiojosemp.obddashboard.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FilenameFilter
import java.util.Collections

public class FileBrowserActivity : Activity() {

    private val scope = CoroutineScope(Dispatchers.Main)

    public companion object {
        public const val INTENT_ACTION_SELECT_FILE: String = "ua.com.vassiliev.androidfilebrowser.SELECT_FILE_ACTION"
        public const val startDirectoryParameter: String = "path"
        public const val returnDirectoryParameter: String = ""
        public const val returnFileParameter: String = ""
        public const val showCannotReadParameter: String = ""
        public const val filterExtension: String = ""

        private const val LOGTAG = "F_PATH"
        private const val SELECT_DIRECTORY = 1
        private const val SELECT_FILE = 2

        @JvmStatic
        public fun getFreeSpace(path: String): Long {
            val stat = StatFs(path)
            return stat.availableBlocksLong.toLong() * stat.blockSizeLong.toLong()
        }

        @JvmStatic
        public fun formatBytes(bytes: Long): String {
            var retStr = ""
            var remaining = bytes

            if (remaining > 1073741824L) {
                val gbs = remaining / 1073741824L
                retStr += "$gbs GB "
                remaining -= gbs * 1073741824L
            }

            if (remaining > 1048576L) {
                val mbs = remaining / 1048576L
                retStr += "$mbs MB "
                remaining -= mbs * 1048576L
            }

            if (remaining > 1024L) {
                val kbs = remaining / 1024L
                retStr += "$kbs KB"
                remaining -= kbs * 1024L
            } else {
                retStr += "$remaining bytes"
            }

            return retStr
        }
    }

    private val pathDirsList: MutableList<String> = ArrayList()
    private val fileList: MutableList<Item> = ArrayList()
    private var path: File? = null
    private var chosenFile: String? = null
    private var adapter: ArrayAdapter<Item>? = null

    private var showHiddenFilesAndDirs: Boolean = true
    private var directoryShownIsEmpty: Boolean = false
    private var filterFileExtension: String? = null

    private var currentAction: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.file_browser_activity)

        val thisInt = intent
        currentAction = SELECT_DIRECTORY
        if (thisInt.action?.equals(INTENT_ACTION_SELECT_FILE, ignoreCase = true) == true) {
            Log.d(LOGTAG, "SELECT ACTION - SELECT FILE")
            currentAction = SELECT_FILE
        }

        showHiddenFilesAndDirs = thisInt.getBooleanExtra(showCannotReadParameter, true)
        filterFileExtension = thisInt.getStringExtra(filterExtension)

        setInitialDirectory()
        parseDirectoryPath()

        scope.launch {
            val items = withContext(Dispatchers.IO) {
                loadFileListInternal()
            }
            fileList.clear()
            fileList.addAll(items)

            createFileListAdapter()
            initializeButtons()
            initializeFileListView()
            updateCurrentDirectoryTextView()
            Log.d(LOGTAG, path?.absolutePath ?: "null")
        }
    }

    private fun setInitialDirectory() {
        val thisInt = intent
        val requestedStartDir = thisInt.getStringExtra(startDirectoryParameter)

        if (!requestedStartDir.isNullOrEmpty()) {
            val tempFile = File(requestedStartDir)
            if (tempFile.isDirectory) {
                path = tempFile
            }
        }

        if (path == null) {
            val extStorage = Environment.getExternalStorageDirectory()
            if (extStorage.isDirectory && extStorage.canRead()) {
                path = extStorage
            } else {
                path = File("/")
            }
        }
    }

    private fun parseDirectoryPath() {
        pathDirsList.clear()
        val pathString = path?.absolutePath ?: return
        val parts = pathString.split("/")
        for (part in parts) {
            pathDirsList.add(part)
        }
    }

    private fun initializeButtons() {
        findViewById<Button>(R.id.upDirectoryButton).setOnClickListener {
            Log.d(LOGTAG, "onclick for upDirButton")
            loadDirectoryUpAsync()
        }

        val selectFolderButton = findViewById<Button>(R.id.selectCurrentDirectoryButton)
        if (currentAction == SELECT_DIRECTORY) {
            selectFolderButton.setOnClickListener {
                Log.d(LOGTAG, "onclick for selectFolderButton")
                returnDirectoryFinishActivity()
            }
        } else {
            selectFolderButton.visibility = View.GONE
        }
    }

    private fun loadDirectoryUpAsync() {
        scope.launch {
            withContext(Dispatchers.IO) {
                val removedSegment = pathDirsList.removeAt(pathDirsList.size - 1)
                val currentPath = path?.toString() ?: return@withContext
                val lastIdx = currentPath.lastIndexOf(removedSegment)
                if (lastIdx > 0) {
                    path = File(currentPath.substring(0, lastIdx))
                }
            }

            loadFileListInternalOnMain()
        }
    }

    private fun loadFileListInternalOnMain() {
        scope.launch {
            val items = withContext(Dispatchers.IO) {
                loadFileListInternal()
            }
            fileList.clear()
            fileList.addAll(items)
            adapter?.notifyDataSetChanged()
            updateCurrentDirectoryTextView()
        }
    }

    private fun updateCurrentDirectoryTextView() {
        val curDirString = if (pathDirsList.isEmpty()) "/" else pathDirsList.joinToString("/") + "/"

        findViewById<Button>(R.id.upDirectoryButton).isEnabled = pathDirsList.isNotEmpty()

        val freeSpace = getFreeSpace(curDirString)
        var formattedSpaceString = formatBytes(freeSpace)
        if (freeSpace == 0L) {
            Log.d(LOGTAG, "NO FREE SPACE")
            if (!File(curDirString).canWrite()) {
                formattedSpaceString = "NON Writable"
            }
        }

        findViewById<Button>(R.id.selectCurrentDirectoryButton)
            .text = "Select\n[$formattedSpaceString]"

        findViewById<TextView>(R.id.currentDirectoryTextView)
            .text = "${getText(R.string.current_directory_text)}$curDirString"
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun initializeFileListView() {
        val lView = findViewById<ListView>(R.id.fileListView)
        val lParam = LinearLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT).apply {
            setMargins(15, 5, 15, 5)
        }
        lView.layoutParams = lParam
        lView.adapter = adapter

        lView.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            chosenFile = fileList[position].file
            val sel = File(path?.absolutePath + "/" + chosenFile)
            Log.d(LOGTAG, "Clicked: $chosenFile")

            if (sel.isDirectory) {
                if (sel.canRead()) {
                    pathDirsList.add(chosenFile!!)
                    path = File(sel.toString())
                    Log.d(LOGTAG, "Just reloading the list")
                    scope.launch {
                        val items = withContext(Dispatchers.IO) {
                            loadFileListInternal()
                        }
                        fileList.clear()
                        fileList.addAll(items)
                        adapter?.notifyDataSetChanged()
                        updateCurrentDirectoryTextView()
                        Log.d(LOGTAG, path?.absolutePath ?: "null")
                    }
                } else {
                    showToast("Path does not exist or cannot be read")
                }
            } else {
                Log.d(LOGTAG, "item clicked")
                if (!directoryShownIsEmpty) {
                    Log.d(LOGTAG, "File selected: $chosenFile")
                    returnFileFinishActivity(sel.absolutePath)
                }
            }
        }
    }

    private fun returnDirectoryFinishActivity() {
        val retIntent = Intent().apply {
            putExtra(returnDirectoryParameter, path?.absolutePath ?: "")
        }
        setResult(RESULT_OK, retIntent)
        finish()
    }

    private fun returnFileFinishActivity(filePath: String) {
        val retIntent = Intent().apply {
            putExtra(returnFileParameter, filePath)
        }
        setResult(RESULT_OK, retIntent)
        finish()
    }

    private fun loadFileListInternal(): List<Item> {
        try {
            path?.mkdirs()
        } catch (e: SecurityException) {
            Log.e(LOGTAG, "unable to write on the sd card ")
        }

        val currentPath = path ?: return emptyList()

        if (!currentPath.exists() || !currentPath.canRead()) {
            Log.e(LOGTAG, "path does not exist or cannot be read")
            return emptyList()
        }

        val filter = FilenameFilter { _, filename ->
            val sel = File(currentPath, filename)
            val showReadableFile = showHiddenFilesAndDirs || sel.canRead()

            when (currentAction) {
                SELECT_DIRECTORY -> sel.isDirectory && showReadableFile
                SELECT_FILE -> {
                    if (sel.isFile && filterFileExtension != null) {
                        showReadableFile && sel.name.endsWith(filterFileExtension!!)
                    } else {
                        showReadableFile
                    }
                }
                else -> true
            }
        }

        val fList = currentPath.list(filter) ?: return listOf(Item("Directory is empty", -1, canRead = false))

        if (fList.isEmpty()) {
            directoryShownIsEmpty = true
            return listOf(Item("Directory is empty", -1, canRead = false))
        }

        directoryShownIsEmpty = false
        val result: MutableList<Item> = ArrayList()

        for (fileName in fList) {
            val sel = File(currentPath, fileName)
            Log.d(LOGTAG, "File: $fileName readable: ${sel.canRead()}")

            var drawableID = R.drawable.file_icon
            if (sel.isDirectory) {
                drawableID = if (sel.canRead()) {
                    R.drawable.folder_icon
                } else {
                    R.drawable.folder_icon_light
                }
            }

            result.add(Item(fileName, drawableID, sel.canRead()))
        }

        Collections.sort(result, ItemFileNameComparator())
        return result
    }

    private fun createFileListAdapter() {
        adapter = object : ArrayAdapter<Item>(this, android.R.layout.select_dialog_item, android.R.id.text1, fileList) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent)
                val textView = view.findViewById<TextView>(android.R.id.text1)

                val drawableID = if (fileList[position].icon != -1) fileList[position].icon else 0
                textView.setCompoundDrawablesWithIntrinsicBounds(drawableID, 0, 0, 0)
                textView.ellipsize = null

                val dp3 = (3 * resources.displayMetrics.density + 0.5f).toInt()
                textView.compoundDrawablePadding = dp3

                textView.setOnClickListener { _ ->
                    val pathExtra = intent.extras?.getString("path")?.plus(textView.text.toString()) ?: ""
                    val chartIntent = Intent(applicationContext, com.sergiojosemp.obddashboard.activity.ChartActivity::class.java).apply {
                        putExtra("path", pathExtra)
                    }
                    startActivity(chartIntent)
                }

                return view
            }
        }
    }

    public data class Item(
        val file: String,
        val icon: Int,
        val canRead: Boolean
    ) {
        override fun toString(): String = file
    }

    private class ItemFileNameComparator : Comparator<Item> {
        override fun compare(lhs: Item, rhs: Item): Int {
            return lhs.file.lowercase().compareTo(rhs.file.lowercase())
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            Log.d(LOGTAG, "ORIENTATION_LANDSCAPE")
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            Log.d(LOGTAG, "ORIENTATION_PORTRAIT")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}
