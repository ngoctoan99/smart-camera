package com.sanghm2.project_03_01.fragment

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.budiyev.android.codescanner.AutoFocusMode
import com.budiyev.android.codescanner.CodeScanner
import com.budiyev.android.codescanner.DecodeCallback
import com.budiyev.android.codescanner.ErrorCallback
import com.budiyev.android.codescanner.ScanMode
import com.sanghm2.project_03_01.databinding.FragmentQRScannerBinding
import java.util.regex.Pattern

class QRScannerFragment : Fragment() {
    private lateinit var binding : FragmentQRScannerBinding
    private lateinit var ScannerQr : CodeScanner
    private var textResult = ""
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentQRScannerBinding.inflate(LayoutInflater.from(context), container,false)
        if(ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED){
            ActivityCompat.requestPermissions(requireActivity(), arrayOf(android.Manifest.permission.CAMERA),123)
        }else {
            scanning()
        }
        binding.copyBtn.setOnClickListener {
            val clipboardManager = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboardManager.setPrimaryClip(ClipData.newPlainText("TextScanner", textResult))
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2)
                Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show()
        }
        return binding.root
    }

    private fun scanning() {
        ScannerQr = CodeScanner(requireContext(),binding.scannerqr)
        ScannerQr.camera = CodeScanner.CAMERA_BACK
        ScannerQr.formats = CodeScanner.ALL_FORMATS
        ScannerQr.autoFocusMode = AutoFocusMode.SAFE
        ScannerQr.scanMode = ScanMode.SINGLE
        ScannerQr.isAutoFocusEnabled = true
        ScannerQr.isFlashEnabled = false
        ScannerQr.decodeCallback = DecodeCallback {
            activity?.runOnUiThread {
                if(validationURL(it.text)){
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(it.text)))
                }else {
                    binding.resultScanner.visibility = View.VISIBLE
                    binding.copyBtn.visibility = View.VISIBLE
                    textResult = it.text
                    val result  = "Scanner Result : ${it.text}"
                    binding.resultScanner.text = result
                }
            }
        }
        ScannerQr.errorCallback = ErrorCallback {
            activity?.runOnUiThread {
                Toast.makeText(context, "Camera initialization error : ${it.message}", Toast.LENGTH_LONG).show()
            }
        }
        binding.scannerqr.setOnClickListener {
            ScannerQr.startPreview()
            binding.resultScanner.visibility = View.GONE
            binding.copyBtn.visibility = View.GONE
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(requestCode == 123){
            if(grantResults[0] == PackageManager.PERMISSION_GRANTED){
                Toast.makeText(context,"Camera permission granted ", Toast.LENGTH_SHORT).show()
                scanning()
            }else {
                Toast.makeText(context,"Camera permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if(::ScannerQr.isInitialized){
            ScannerQr.startPreview()
        }
    }

    override fun onPause() {
        if(::ScannerQr.isInitialized){
            ScannerQr.releaseResources()
        }
        super.onPause()
    }
    private fun validationURL(url: String?): Boolean {
        val regex = "[(http(s)?):\\/\\/(www\\.)?a-zA-Z0-9@:%._\\+~#=]{2,256}\\.[a-z]{2,6}\\b([-a-zA-Z0-9@:%_\\+.~#?&//=]*)"
        val p = Pattern.compile(regex)
        val m = p.matcher(url)
        return m.find()
    }
}