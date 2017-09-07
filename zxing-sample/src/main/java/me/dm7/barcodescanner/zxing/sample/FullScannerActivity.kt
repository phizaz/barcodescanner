package me.dm7.barcodescanner.zxing.sample

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.support.v4.app.DialogFragment
import android.support.v4.app.FragmentManager
import android.support.v4.view.MenuItemCompat
import android.util.Log
import android.util.Pair
import android.view.Menu
import android.view.MenuItem
import android.view.ViewGroup
import android.widget.Toast

import com.google.zxing.BarcodeFormat
import com.google.zxing.Result

import java.util.ArrayList
import java.util.Date

import me.dm7.barcodescanner.zxing.ZXingScannerView
import java.text.SimpleDateFormat

class FullScannerActivity : BaseScannerActivity(), MessageDialogFragment.MessageDialogListener, ZXingScannerView.ResultHandler, FormatSelectorDialogFragment.FormatSelectorDialogListener, CameraSelectorDialogFragment.CameraSelectorDialogListener {
    private var mScannerView: ZXingScannerView? = null
    private var mFlash: Boolean = false
    private var mAutoFocus: Boolean = false
    private var mSelectedIndices: ArrayList<Int>? = null
    private var mCameraId = -1
    private var handler: Handler? = null

    private var results = ArrayList<Pair<String, Date>>()

    public override fun onCreate(state: Bundle?) {
        super.onCreate(state)

        // setup the configurations
        if (state != null) {
            mFlash = state.getBoolean(FLASH_STATE, false)
            mAutoFocus = state.getBoolean(AUTO_FOCUS_STATE, true)
            mSelectedIndices = state.getIntegerArrayList(SELECTED_FORMATS)
            mCameraId = state.getInt(CAMERA_ID, -1)
        } else {
            mFlash = false
            mAutoFocus = true
            mSelectedIndices = null
            mCameraId = -1
        }

        setContentView(R.layout.activity_simple_scanner)
        setupToolbar()

        val contentFrame = findViewById(R.id.content_frame) as ViewGroup
        mScannerView = ZXingScannerView(this)
        setupFormats()
        contentFrame.addView(mScannerView)
        handler = Handler()
    }

    public override fun onResume() {
        super.onResume()
        mScannerView!!.setResultHandler(this)
        mScannerView!!.startCamera(mCameraId)
        mScannerView!!.flash = mFlash
        mScannerView!!.setAutoFocus(mAutoFocus)
    }

    public override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(FLASH_STATE, mFlash)
        outState.putBoolean(AUTO_FOCUS_STATE, mAutoFocus)
        outState.putIntegerArrayList(SELECTED_FORMATS, mSelectedIndices)
        outState.putInt(CAMERA_ID, mCameraId)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        var menuItem: MenuItem

        if (mFlash) {
            menuItem = menu.add(Menu.NONE, R.id.menu_flash, 0, R.string.flash_on)
        } else {
            menuItem = menu.add(Menu.NONE, R.id.menu_flash, 0, R.string.flash_off)
        }
        MenuItemCompat.setShowAsAction(menuItem, MenuItem.SHOW_AS_ACTION_NEVER)


        if (mAutoFocus) {
            menuItem = menu.add(Menu.NONE, R.id.menu_auto_focus, 0, R.string.auto_focus_on)
        } else {
            menuItem = menu.add(Menu.NONE, R.id.menu_auto_focus, 0, R.string.auto_focus_off)
        }
        MenuItemCompat.setShowAsAction(menuItem, MenuItem.SHOW_AS_ACTION_NEVER)

        // disable: format option force using only COBRA
//        menuItem = menu.add(Menu.NONE, R.id.menu_formats, 0, R.string.formats)
//        MenuItemCompat.setShowAsAction(menuItem, MenuItem.SHOW_AS_ACTION_NEVER)

        menuItem = menu.add(Menu.NONE, R.id.menu_camera_selector, 0, R.string.select_camera)
        MenuItemCompat.setShowAsAction(menuItem, MenuItem.SHOW_AS_ACTION_NEVER)

        menuItem = menu.add(Menu.NONE, R.id.menu_attendance_list, 0, R.string.attendance_list)
        MenuItemCompat.setShowAsAction(menuItem, MenuItem.SHOW_AS_ACTION_NEVER)

        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle presses on the action bar items
        when (item.itemId) {
            R.id.menu_flash -> {
                mFlash = !mFlash
                if (mFlash) {
                    item.setTitle(R.string.flash_on)
                } else {
                    item.setTitle(R.string.flash_off)
                }
                mScannerView!!.flash = mFlash
                return true
            }
            R.id.menu_auto_focus -> {
                mAutoFocus = !mAutoFocus
                if (mAutoFocus) {
                    item.setTitle(R.string.auto_focus_on)
                } else {
                    item.setTitle(R.string.auto_focus_off)
                }
                mScannerView!!.setAutoFocus(mAutoFocus)
                return true
            }
            R.id.menu_formats -> {
                val fragment = FormatSelectorDialogFragment.newInstance(this, mSelectedIndices)
                fragment.show(supportFragmentManager, "format_selector")
                return true
            }
            R.id.menu_camera_selector -> {
                mScannerView!!.stopCamera()
                val cFragment = CameraSelectorDialogFragment.newInstance(this, mCameraId)
                cFragment.show(supportFragmentManager, "camera_selector")
                return true
            }
            R.id.menu_attendance_list -> {
                val intent = Intent(applicationContext, AttendanceListActivity::class.java)
                startActivity(intent)
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    override fun handleResult(rawResult: Result) {
        try {
            val notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val r = RingtoneManager.getRingtone(applicationContext, notification)
            r.play()
        } catch (e: Exception) {
        }

        appendResults(rawResult.text, Date())
        copyResults()
        showMessageDialog("Copied ${results.size} rows to clipboard")
    }

    private fun appendResults(barcode: String, date: Date) {
        results.add(Pair(barcode, date))
    }

    private fun copyResults() {
        val dformat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")

        val str = StringBuilder()
        str.append("<html><body><table>")
        for (res in results) {
            str.append("<tr>")
            str.append("<td>${res.first}</td>")
            str.append("<td>${getStudentID(res.first)}</td>")
            str.append("<td>${dformat.format(res.second)}</td>")
            str.append("</tr>")
        }
        str.append("</table></body></html>")

        val html = str.toString()
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val data = ClipData.newHtmlText("html", html, html)
        clipboard.setPrimaryClip(data)
    }

    private fun getStudentID(barcode: String): String {
        return barcode.substring(5, 13)
    }

    fun showMessageDialog(message: String) {
        handler!!.post({
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        })

        handler!!.postDelayed({
            mScannerView!!.resumeCameraPreview(this)
        }, 1000)

//        val fragment = MessageDialogFragment.newInstance("Scan Results", message, this)
//        fragment.show(supportFragmentManager, "scan_results")
    }

    fun closeMessageDialog() {
        closeDialog("scan_results")
    }

    fun closeFormatsDialog() {
        closeDialog("format_selector")
    }

    fun closeDialog(dialogName: String) {
        val fragmentManager = supportFragmentManager
        val fragment = fragmentManager.findFragmentByTag(dialogName) as DialogFragment
        fragment?.dismiss()
    }

    override fun onDialogPositiveClick(dialog: DialogFragment) {
        // Resume the camera
        mScannerView!!.resumeCameraPreview(this)
    }

    override fun onFormatsSaved(selectedIndices: ArrayList<Int>) {
        mSelectedIndices = selectedIndices
        setupFormats()
    }

    override fun onCameraSelected(cameraId: Int) {
        mCameraId = cameraId
        mScannerView!!.startCamera(mCameraId)
        mScannerView!!.flash = mFlash
        mScannerView!!.setAutoFocus(mAutoFocus)
    }

    fun setupFormats() {
        val formats = ArrayList<BarcodeFormat>()

//        if (mSelectedIndices == null || mSelectedIndices!!.isEmpty()) {
//            mSelectedIndices = ArrayList<Int>()
//            for (i in ZXingScannerView.ALL_FORMATS.indices) {
//                mSelectedIndices!!.add(i)
//            }
//        }

        // force using only 1: CODBRA
        for (index in listOf(1)) {
            formats.add(ZXingScannerView.ALL_FORMATS[index])
        }
        if (mScannerView != null) {
            mScannerView!!.setFormats(formats)
        }
    }

    public override fun onPause() {
        super.onPause()
        mScannerView!!.stopCamera()
        closeMessageDialog()
        closeFormatsDialog()
    }

    companion object {

        private val TAG = "FullScannerActivity"

        private val FLASH_STATE = "FLASH_STATE"
        private val AUTO_FOCUS_STATE = "AUTO_FOCUS_STATE"
        private val SELECTED_FORMATS = "SELECTED_FORMATS"
        private val CAMERA_ID = "CAMERA_ID"
    }
}
