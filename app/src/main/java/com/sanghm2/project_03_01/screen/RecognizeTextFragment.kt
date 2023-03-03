package com.sanghm2.project_03_01.screen

import android.Manifest
import android.R.attr.label
import android.app.Activity
import android.app.ProgressDialog
import android.content.*
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore

import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getSystemService
import androidx.fragment.app.Fragment
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.sanghm2.project_03_01.R
import com.sanghm2.project_03_01.databinding.FragmentRecognizeTextBinding


class RecognizeTextFragment : Fragment() {
    private lateinit var binding :FragmentRecognizeTextBinding
    private companion object {
        private const val  CAMERA_REQUEST_CODE = 100
        private const val  STORAGE_REQUEST_CODE = 101
    }

    private var  imageUri : Uri? = null
    private lateinit var cameraPermission : Array<String>
    private lateinit var storagePermission : Array<String>
    private lateinit var textRecognize : TextRecognizer
    private lateinit var progressDialog : ProgressDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        binding = FragmentRecognizeTextBinding.inflate(LayoutInflater.from(context),container,false)
        cameraPermission = arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        storagePermission = arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        progressDialog = ProgressDialog(context)
        progressDialog.setTitle("Please wait")
        progressDialog.setCanceledOnTouchOutside(false)

        textRecognize = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        binding.takePictureBtn.setOnClickListener {
            showInputImageDialog()
        }

        binding.recognizeTextBtn.setOnClickListener {
            if(imageUri == null){
                showToast("Pick Image First...")
            }else {
                recognizeTextFromImage()
            }
        }
        binding.finishBtn.setOnClickListener {
            binding.recognizeTextEdit.setText("")
            binding.imageIv.setImageResource(R.drawable.ic_baseline_image_24)
            imageUri = null
        }
        binding.copyBtn.setOnClickListener {
           if(binding.recognizeTextEdit.text.toString().trim().isNotEmpty()){
               val clipboardManager = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
               clipboardManager.setPrimaryClip(ClipData.newPlainText("", binding.recognizeTextEdit.text.toString().trim()))
               if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2)
                   Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show()
           }else {
               Toast.makeText(context,"Empty",Toast.LENGTH_SHORT).show()
           }
        }
        return binding.root
    }

    private fun recognizeTextFromImage() {
        progressDialog.setMessage("Preparing Image...")
        progressDialog.show()
        try {
            val  inputImage = InputImage.fromFilePath(requireContext(),imageUri!!)
            progressDialog.setMessage("Recognize text...")
            val textTaskResult = textRecognize.process(inputImage)
                .addOnSuccessListener {
                    val recognizeText  = it.text
                    binding.recognizeTextEdit.setText(recognizeText)
                    progressDialog.dismiss()
                }.addOnFailureListener {
                    progressDialog.dismiss()
                    showToast("Failed to recognize text due to  ${it.message}")
                }
        }catch (e: Exception){
            showToast("Fail to prepare image due to ${e.message}")
        }
    }

    private fun showInputImageDialog() {
        val popupMenu = PopupMenu(context, binding.takePictureBtn)

        popupMenu.menu.add(Menu.NONE , 1,1,"CAMERA")
        popupMenu.menu.add(Menu.NONE , 2,2,"GALLERY")
        popupMenu.show()
        popupMenu.setOnMenuItemClickListener { menuItem->

            val id = menuItem.itemId
            if(id == 1){
                if(checkCameraPermission()){
                    pickImageCamera()
                }else {
                    requestCameraPermission()
                }
            }else if(id == 2){
                if(checkStoragePermission()){
                    pickImageGallery()
                }
                else {
                    requestStoragePermission()
                }
            }
            return@setOnMenuItemClickListener true
        }
    }


    private fun pickImageGallery(){
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        galleryActivityResultLauncher.launch(intent)
    }
    private val galleryActivityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){ result ->
        if(result.resultCode == Activity.RESULT_OK){
            val data = result.data
            imageUri = data!!.data
            binding.imageIv.setImageURI(imageUri)
        }else {

        }
    }
    private fun pickImageCamera(){
        val values = ContentValues()
        values.put(MediaStore.Images.Media.TITLE , "Sample Title")
        values.put(MediaStore.Images.Media.DESCRIPTION , "Sample Description")
        imageUri =
            activity?.contentResolver?.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,values)
        val intent  = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent.putExtra(MediaStore.EXTRA_OUTPUT,imageUri)
        cameraActivityResultLauncher.launch(intent)
    }
    private val cameraActivityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){ result ->
        if(result.resultCode == Activity.RESULT_OK){
            binding.imageIv.setImageURI(imageUri)
        }else {
            showToast("Cancelled....")
        }

    }
    private fun checkStoragePermission() : Boolean {
        return context?.let { ContextCompat.checkSelfPermission(it, Manifest.permission.WRITE_EXTERNAL_STORAGE) } == PackageManager.PERMISSION_GRANTED
    }
    private fun checkCameraPermission(): Boolean {
        val cameraResult = context?.let { ContextCompat.checkSelfPermission(it, Manifest.permission.CAMERA) } == PackageManager.PERMISSION_GRANTED
        val storageResult = context?.let { ContextCompat.checkSelfPermission(it, Manifest.permission.WRITE_EXTERNAL_STORAGE) } == PackageManager.PERMISSION_GRANTED
        return cameraResult && storageResult
    }

    private fun requestStoragePermission(){
        ActivityCompat.requestPermissions(context as Activity,storagePermission, STORAGE_REQUEST_CODE)
    }
    private fun requestCameraPermission(){
        ActivityCompat.requestPermissions(context as Activity,cameraPermission, CAMERA_REQUEST_CODE)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when(requestCode){
            CAMERA_REQUEST_CODE ->{
                if(grantResults.isNotEmpty()){
                    val cameraAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED
                    val storageAccepted = grantResults[1] == PackageManager.PERMISSION_GRANTED
                    if(cameraAccepted && storageAccepted){
                        pickImageCamera()
                    }else {
                        showToast("Camera & Storage permission are required...")
                    }
                }
            }
            STORAGE_REQUEST_CODE->{
                if(grantResults.isNotEmpty()){
                    val storageAccepted  = grantResults[0] == PackageManager.PERMISSION_GRANTED
                    if(storageAccepted){
                        pickImageGallery()
                    }else {
                        showToast("Storage permission are required...")
                    }
                }
            }
        }
    }
    private fun showToast(message: String){
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

}