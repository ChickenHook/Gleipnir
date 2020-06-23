package org.gleipnir.app

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import org.gleipnir.app.extendableLoader.ApplicationPaths
import org.gleipnir.app.helpers.ApkZipHelper
import java.io.File
import java.io.InputStream
import java.net.URLConnection


class AppDetailsActivity : Activity() {

    companion object {
        const val EXTRA_APP = "extra_app";
        const val TAG = "AppDetailsActivity"
    }

    private val IMPORT_CHOOSER = 0
    private val EXPORT_CHOOSER = 1
    lateinit var mAppInfo: ApplicationInfo
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_app_details)
        intent?.let { it ->
            it.getParcelableExtra<ApplicationInfo>(EXTRA_APP)?.let { appInfo ->
                mAppInfo = appInfo
                findViewById<ImageView>(R.id.activity_app_details_icon)?.setImageDrawable(
                    packageManager.getApplicationIcon(appInfo.packageName)
                )
                findViewById<Button>(R.id.activity_app_details_export_data)?.setOnClickListener {
                    writeExport(appInfo)
                }
                findViewById<Button>(R.id.activity_app_details_share_data)?.setOnClickListener {
                    writeExport(appInfo)?.let {
                        shareFile(it)
                    } ?: kotlin.run {
                        showToast("Sharing failed!")
                    }
                }
                findViewById<Button>(R.id.activity_app_details_import_data)?.setOnClickListener {
                    chooseFile()
                }
            }

        } ?: kotlin.run {
            showToast("Error while gather app info")
        }
    }

    fun shareFile(file: File) {
        val intentShareFile = Intent(Intent.ACTION_SEND)

        intentShareFile.type = URLConnection.guessContentTypeFromName(file.name)
        intentShareFile.putExtra(
            Intent.EXTRA_STREAM,
            Uri.parse("content://" + file.absolutePath)
        )

        //if you need
        //intentShareFile.putExtra(Intent.EXTRA_SUBJECT,"Sharing File Subject);
        //intentShareFile.putExtra(Intent.EXTRA_TEXT, "Sharing File Description");


        //if you need
        //intentShareFile.putExtra(Intent.EXTRA_SUBJECT,"Sharing File Subject);
        //intentShareFile.putExtra(Intent.EXTRA_TEXT, "Sharing File Description");
        startActivity(Intent.createChooser(intentShareFile, "Share File"))
    }

    fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    fun writeExport(info: ApplicationInfo): File? {
        getExternalFilesDir(null)?.let {
            val output = File(it.absolutePath + File.separator + info.packageName + "_data.zip")
            val input = ApplicationPaths.buildDataDir(this, info.packageName)
            ApkZipHelper.zipIt(output, input)
            showToast("Exported to: ${output.absolutePath}")
            return output
        }
        return null
    }

    fun writeImport(info: ApplicationInfo, input: InputStream) {
        val dest = ApplicationPaths.buildDataDir(this, info.packageName)
        ApkZipHelper.unzip(input, dest)
        showToast("Imported to: ${dest.absolutePath}")
    }


    fun chooseFile() {
        try {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "*/*"
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            startActivityForResult(
                Intent.createChooser(intent, "Choose import zip"),
                IMPORT_CHOOSER
            )
        } catch (ex: ActivityNotFoundException) {
            // Potentially direct the user to the Market with a Dialog
            Toast.makeText(
                this, "Please install a File Manager.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }


    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent
    ) {
        when (requestCode) {
            IMPORT_CHOOSER -> {
                Log.i("Test", "Result URI " + data.data)
                val inStream: InputStream? = contentResolver.openInputStream(data.data!!)
                //val _data = inStream?.readBytes()
                inStream?.let {
                    writeImport(mAppInfo, inStream)
                } ?: kotlin.run {
                    showToast("Import failed!")
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }


}