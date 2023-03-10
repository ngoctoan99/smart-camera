package com.sanghm2.project_03_01.fragment

import android.Manifest
import android.app.Activity
import android.app.ProgressDialog
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeler
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import com.sanghm2.project_03_01.R
import com.sanghm2.project_03_01.databinding.FragmentObjectRecognitionBinding

class ObjectRecognitionFragment : Fragment() {

    private lateinit var binding : FragmentObjectRecognitionBinding
    private lateinit var imageLabeler : ImageLabeler
    private lateinit var progressDialog : ProgressDialog

    private companion object{
        private const val  CAMERA_REQUEST_CODE = 100
        private const val  STORAGE_REQUEST_CODE = 101
    }
    private var  imageUri : Uri? = null
    private lateinit var cameraPermission : Array<String>
    private lateinit var storagePermission : Array<String>
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentObjectRecognitionBinding.inflate(LayoutInflater.from(context), container , false)
        progressDialog = ProgressDialog(context)
        progressDialog.setTitle("Please wait...")
        progressDialog.setCanceledOnTouchOutside(false)


        imageLabeler = ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS)

//        val imageLabelerOptions = ImageLabelerOptions.Builder()
//            .setConfidenceThreshold(0.8f)
//            .build()
//        imageLabeler =  ImageLabeling.getClient(imageLabelerOptions)

//        val bitmap1 = BitmapFactory.decodeResource(resources, R.drawable.cake)
//
//        val imageUri : Uri? = null

//        val bitmapDrawable = binding.imageTv.drawable as BitmapDrawable
//        val bitmap3 = bitmapDrawable.bitmap
        binding.btnRecognition.setOnClickListener {
            binding.resultTv.text = ""
            if(imageUri != null){
                val bitmap2 = MediaStore.Images.Media.getBitmap(requireContext().contentResolver , imageUri)
                labelImage(bitmap2)
            }else {
                showToast("Pick First Image")
            }
        }
        binding.imageTv.setOnClickListener {
            showInputImageDialog()
        }
        return binding.root
    }

    private fun labelImage(bitmap : Bitmap){
        progressDialog.setMessage("Preparing image...")
        progressDialog.show()
        val inputImage = InputImage.fromBitmap(bitmap,0)
        progressDialog.setMessage("Recognition Image...")
        imageLabeler.process(inputImage).addOnSuccessListener { imageLabels ->
            for(imageLabel in imageLabels){
                val text = imageLabel.text
                val confidence = imageLabel.confidence
                val index = imageLabel.index
                binding.resultTv.append("text: $text  \nconfidence: $confidence \nindex: $index\n\n")
            }
            progressDialog.dismiss()

        }.addOnFailureListener {
            progressDialog.dismiss()
            Toast.makeText(context , "$it",Toast.LENGTH_SHORT).show()
        }
    }

    private fun showInputImageDialog() {
        val popupMenu = PopupMenu(context, binding.imageTv)

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
        binding.layoutGlide.visibility = View.GONE
    }
    private val galleryActivityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){ result ->
        if(result.resultCode == Activity.RESULT_OK){
            val data = result.data
            imageUri = data!!.data
            binding.imageTv.setImageURI(imageUri)
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
        binding.layoutGlide.visibility = View.GONE
    }
    private val cameraActivityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){ result ->
        if(result.resultCode == Activity.RESULT_OK){
            binding.imageTv.setImageURI(imageUri)
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
        ActivityCompat.requestPermissions(context as Activity,storagePermission, STORAGE_REQUEST_CODE
        )
    }
    private fun requestCameraPermission(){
        ActivityCompat.requestPermissions(context as Activity,cameraPermission, CAMERA_REQUEST_CODE
        )
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
            STORAGE_REQUEST_CODE ->{
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