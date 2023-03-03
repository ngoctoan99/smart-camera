package com.sanghm2.project_03_01.screen

import android.Manifest
import android.app.Activity
import android.app.ProgressDialog
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
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
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.text.TextRecognizer
import com.sanghm2.project_03_01.R
import com.sanghm2.project_03_01.databinding.FragmentCountFaceBinding

class CountFaceFragment : Fragment() {
     private lateinit var binding : FragmentCountFaceBinding
     private lateinit var detector : FaceDetector
    private companion object{
        private const val  SCALING_FATOR = 10
        private const val TAG  = "FACE_DETECT_TAG"
        private const val  CAMERA_REQUEST_CODE = 100
        private const val  STORAGE_REQUEST_CODE = 101
    }
    private var  imageUri : Uri? = null
    private var position : Int = 0
    private lateinit var cameraPermission : Array<String>
    private lateinit var storagePermission : Array<String>
    private lateinit var progressDialog : ProgressDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
         binding  = FragmentCountFaceBinding.inflate(layoutInflater, container, false)

        cameraPermission = arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        storagePermission = arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        progressDialog = ProgressDialog(context)
        progressDialog.setTitle("Please wait")
        progressDialog.setCanceledOnTouchOutside(false)
        binding.imageDetectLl.visibility = View.GONE
        binding.originalIv.setOnClickListener {
            showInputImageDialog()
        }
        val realTimeFdo = FaceDetectorOptions.Builder()
            .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .build()

        detector = FaceDetection.getClient(realTimeFdo) ;
        // image from drawable
        val bitmap1 = BitmapFactory.decodeResource(resources , R.drawable.newtwoperson);
//        // image from ImageView
//        val bitmapDrawable  = original_iv.drawable as BitmapDrawable
//        val bitmap2 = bitmapDrawable.bitmap
//        //image from URI
//        val imageUri : Uri?  = null


        binding.detectFacebtn.setOnClickListener {
            try {
                val bitmap3 = MediaStore.Images.Media.getBitmap(requireContext().contentResolver,imageUri)
                anlyzePhoto(bitmap3)
            }catch (e : Exception){
                Log.e(TAG, "onCreate: " ,e)
            }
        }
        return binding.root
    }

    private fun anlyzePhoto(bitmap : Bitmap){
        Log.d(TAG, "analyzePhoto: ")
        val smallerBitmap = Bitmap.createScaledBitmap(
            bitmap,
            bitmap.width / SCALING_FATOR,
            bitmap.height / SCALING_FATOR,
            false
        )
        val inputImage = InputImage.fromBitmap(smallerBitmap,0)
        detector.process(inputImage).addOnSuccessListener {faces ->
            Log.d(TAG, "anlyzePhoto: Successfully detected face ... ")
            Toast.makeText(context, "Face Detected ...", Toast.LENGTH_SHORT).show()

            for (face in faces){
                val rect  = face.boundingBox
                rect.set(rect.left * SCALING_FATOR,
                    rect.top * (SCALING_FATOR -1) ,
                    rect.right * (SCALING_FATOR),
                    rect.bottom * SCALING_FATOR + 90)
            }
            checkVisibleButton(faces.size)
            Log.d(TAG, "anlyzePhoto: Number of faces   ${faces.size}")
            val string  = "Have ${faces.size} face in the picture"
            binding.countFaceTv.text = string

            cropDetectedFace(bitmap,faces,position)

            binding.imageDetectLl.visibility = View.VISIBLE
            binding.prevFace.setOnClickListener {
                if(position > 0 ){
                    position -= 1
                    cropDetectedFace(bitmap,faces,position)
                    checkVisibleButton(faces.size)
                }
            }
            binding.nextFace.setOnClickListener {
                if(position < (faces.size - 1) ){
                    position += 1
                    cropDetectedFace(bitmap,faces,position)
                    checkVisibleButton(faces.size)
                }
            }
        }.addOnFailureListener { e->
            Log.e(TAG, "anlyzePhoto: ", e)
            Toast.makeText(context, "Failed due to  ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun cropDetectedFace(bitmap: Bitmap, faces : List<Face>, position : Int){
        if(faces.isNotEmpty()){
            val rect = faces[position].boundingBox
            val x = Math.max(rect.left,0)
            val y = Math.max(rect.top , 0)
            val width = rect.width()
            val heigth = rect.height()

            val croppedBitmap = Bitmap.createBitmap(
                bitmap,
                x,
                y,
                if(x + width > bitmap.width) bitmap.width - x else width ,
                if(y + heigth > bitmap.height) bitmap.height - y else heigth
            )
            binding.croppedIv.setImageBitmap(croppedBitmap)
        }
        else {
            Toast.makeText(context,"Don't  detect the face",Toast.LENGTH_SHORT).show()
        }

    }
    private fun showInputImageDialog() {
        val popupMenu = PopupMenu(context, binding.originalIv)

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
            binding.originalIv.setImageURI(imageUri)
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
            binding.originalIv.setImageURI(imageUri)
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
    private fun checkVisibleButton(number : Int){
        if(position == 0){
            binding.prevFace.visibility = View.GONE
            binding.nextFace.visibility = View.VISIBLE
        }else if(position == number -1){
            binding.nextFace.visibility = View.GONE
            binding.prevFace.visibility = View.VISIBLE
        }else if(number == 1 ){
            binding.prevFace.visibility = View.GONE
            binding.nextFace.visibility = View.GONE
        }else{
            binding.prevFace.visibility = View.VISIBLE
            binding.nextFace.visibility = View.VISIBLE
        }
    }

}