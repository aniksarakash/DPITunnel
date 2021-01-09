package ru.evgeniy.dpitunnel.fragment

import android.Manifest
import android.content.ContentResolver
import android.content.ContentValues
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.text.method.LinkMovementMethod
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.gun0912.tedpermission.PermissionListener
import com.gun0912.tedpermission.TedPermission
import ru.evgeniy.dpitunnel.R
import java.io.File
import java.io.InputStream
import java.io.OutputStream

class SNISlide: Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_slide_sni, container, false)

        val sniSlideText: TextView = view.findViewById(R.id.sni_overview_text)
        sniSlideText.movementMethod = LinkMovementMethod.getInstance()

        val saveCertButon: Button = view.findViewById(R.id.save_certificate_button)
        saveCertButon.setOnClickListener{
            saveFileToExternalStorage(
                    if(Build.VERSION.SDK_INT < Build.VERSION_CODES.Q)
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    else
                        null,
                    requireContext().getFilesDir().toString() + File.separator + "rootCA.crt",
                    "rootCA.crt")
        }

        return view
    }

    private fun saveFileToExternalStorage(directory: File?, filePath: String, targetFileName: String){
        var uri: Uri? = null
        var contentResolver: ContentResolver? = null
        val values = ContentValues()
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q){
            contentResolver = activity?.contentResolver;

            values.put(MediaStore.Files.FileColumns.MIME_TYPE, "application/x-x509-ca-cert")
            values.put(MediaStore.Files.FileColumns.DATE_ADDED, System.currentTimeMillis() / 1000)
            values.put(MediaStore.Files.FileColumns.TITLE, targetFileName)
            values.put(MediaStore.Files.FileColumns.DISPLAY_NAME, targetFileName)
            values.put(MediaStore.Files.FileColumns.DATE_TAKEN, System.currentTimeMillis())
            values.put(MediaStore.Files.FileColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            values.put(MediaStore.Files.FileColumns.IS_PENDING, 1)
            uri = contentResolver?.insert(MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY), values)
        }

        val baseFile = File(filePath)
        if(baseFile.exists()){

            if(Build.VERSION.SDK_INT < Build.VERSION_CODES.Q){
                val permissionListener: PermissionListener = object : PermissionListener {
                    override fun onPermissionGranted() {
                        // If ok save certificate
                        directory?.let {
                            val targetFile = File(it.path+File.separator+targetFileName)
                            baseFile.copyTo(targetFile)
                        }
                        Toast.makeText(context,
                                String.format(getString(R.string.saved_to_downloads), targetFileName),
                                Toast.LENGTH_LONG).show()
                    }

                    override fun onPermissionDenied(deniedPermissions: List<String>) {
                        // If not ok show warning
                        Toast.makeText(context, getString(R.string.please_grant_permissions), Toast.LENGTH_LONG).show()
                    }
                }
                TedPermission.with(context)
                        .setPermissionListener(permissionListener)
                        .setPermissions(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        .check()
            }else{
                uri?.let {
                    val outputStream: OutputStream? = contentResolver?.openOutputStream(uri)
                    outputStream?.let{
                        val inputStream: InputStream = File(filePath).inputStream()
                        inputStream.copyTo(outputStream, 1024)
                    }
                    values.clear()
                    values.put(MediaStore.Files.FileColumns.IS_PENDING, 0)
                    contentResolver?.update(uri, values, null, null)

                    Toast.makeText(context,
                            String.format(getString(R.string.saved_to_downloads), targetFileName),
                            Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}