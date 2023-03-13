package com.sanghm2.project_03_01.fragment

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getSystemService
import com.google.zxing.BarcodeFormat
import com.google.zxing.WriterException
import com.google.zxing.qrcode.QRCodeWriter
import com.sanghm2.project_03_01.R
import com.sanghm2.project_03_01.databinding.FragmentQRGeneratorBinding
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

class QRGeneratorFragment : Fragment() {
    private lateinit var binding : FragmentQRGeneratorBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentQRGeneratorBinding.inflate(LayoutInflater.from(context),container,false)
        actionView()
        return binding.root
    }
    private fun actionView(){
        // click save image
        binding.imageQrcode.setOnClickListener {
            if(Build.VERSION.SDK_INT  >= Build.VERSION_CODES.M){
                if(ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
                    ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),100)
                }
                else {
                    saveImage()
                }
            }
            else {
                saveImage()
            }
        }
        binding.layoutGlide.visibility = View.GONE

        //click to generator qr code image
        binding.btngenerator.setOnClickListener {
            val data = binding.editQrcode.text.toString().trim()
            if(data.isEmpty()){
                Toast.makeText(context, "Please do not leave data blank", Toast.LENGTH_SHORT).show()
            }else {
                val write = QRCodeWriter()
                try {
                    val bitMatrix = write.encode(data, BarcodeFormat.QR_CODE, 512,512)
                    val width = bitMatrix.width
                    val height = bitMatrix.height
                    val bmp = Bitmap.createBitmap(width,height, Bitmap.Config.RGB_565)
                    for(x in 0 until width){
                        for (y in 0 until height){
                            bmp.setPixel(x,y, if(bitMatrix[x,y]) Color.BLACK else Color.WHITE)
                        }
                    }
                    binding.imageQrcode.setImageBitmap(bmp)
                    binding.editQrcode.setText("")
                    binding.layoutGlide.visibility = View.VISIBLE
                    hideKeyboard()
                }catch (e: WriterException){
                    e.printStackTrace()
                }
            }
        }
        binding.editQrcode.addTextChangedListener(object :TextWatcher{
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                binding.layoutGlide.visibility = View.GONE
            }

            override fun afterTextChanged(p0: Editable?) {

            }
        })
    }
    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if(requestCode == 100){
            if(grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                saveImage()
            }else {
                Toast.makeText(context, "Permission not granted", Toast.LENGTH_SHORT).show()
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    /// function save image to Download local memory
    private fun saveImage(){
        val timeStamp = System.currentTimeMillis()
        val externalStorageState = Environment.getExternalStorageState()
        if(externalStorageState.equals(Environment.MEDIA_MOUNTED)){
            val storageDirectory = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),"QR Generator")
            storageDirectory.mkdirs()
            val file = File(storageDirectory, "$timeStamp.png")
            try {
                val stream : OutputStream = FileOutputStream(file)
                val bitmap : Bitmap = (binding.imageQrcode.drawable as BitmapDrawable).bitmap
                bitmap.compress(Bitmap.CompressFormat.PNG,90,stream)
                stream.flush()
                stream.close()
                MediaStore.Images.Media.insertImage(requireActivity().contentResolver, file.absolutePath,file.name,file.name)
                Toast.makeText(context, "Save image successful ${Uri.parse(file.absolutePath)}", Toast.LENGTH_SHORT).show()
                binding.layoutGlide.visibility = View.GONE
            }catch (e : Exception){
                e.printStackTrace()
            }
        }else {
            Toast.makeText(context, "Fail to save image", Toast.LENGTH_SHORT).show()
        }
    }
    fun Fragment.hideKeyboard() {
        view?.let { activity?.hideKeyboard(it) }
    }

    fun Activity.hideKeyboard() {
        hideKeyboard(currentFocus ?: View(this))
    }

    fun Context.hideKeyboard(view: View) {
        val inputMethodManager = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
    }

}