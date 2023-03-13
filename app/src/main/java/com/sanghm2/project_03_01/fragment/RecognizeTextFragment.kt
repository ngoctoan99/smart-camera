package com.sanghm2.project_03_01.fragment

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.ProgressDialog
import android.content.*
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.util.Log

import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.sanghm2.project_03_01.R
import com.sanghm2.project_03_01.databinding.FragmentRecognizeTextBinding
import com.sanghm2.project_03_01.model.ModelLanguage
import java.util.*
import kotlin.collections.ArrayList


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
    private var languageArrayList : ArrayList<ModelLanguage>?= null
    private var sourceLanguageCode = "en"
    private var sourceLanguageTitle = "Tiếng Anh"
    private var targetLanguageCode = "vi"
    private var targetLanguageTitle = "Tiếng Việt"
    private lateinit var translatorOptions : TranslatorOptions

    private lateinit var  translator : Translator

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        requireActivity().overridePendingTransition(R.anim.slide_in_left,R.anim.slide_out_left)
        binding = FragmentRecognizeTextBinding.inflate(LayoutInflater.from(context),container,false)
        initView()
        loadAvailableLanguages()
        actionView()
        return binding.root
    }

    private fun loadAvailableLanguages() {
        languageArrayList = ArrayList()
        val languageCodeList = TranslateLanguage.getAllLanguages()
        for (languageCode in languageCodeList){
            val languageTitle = Locale(languageCode).displayLanguage
            val modelLanguage = ModelLanguage(languageCode, languageTitle)
            languageArrayList!!.add(modelLanguage)
        }
    }

    private fun actionView() {
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
            binding.translateTv.text = ""
            binding.sourceLanguageChooseBtn.text = "Choose"
            binding.targetLanguageChooseBtn.text = "Choose"
            binding.translationLl.visibility = View.GONE
            binding.translateLl.visibility = View.GONE
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
        binding.sourceLanguageChooseBtn.setOnClickListener {
            sourceLanguageChoose()
        }
        binding.targetLanguageChooseBtn.setOnClickListener {
            targetLanguageChoose()
        }
        binding.translateBtn.setOnClickListener {
            validateData()
        }
        binding.translationLl.setOnClickListener{
            binding.translationLl.visibility = View.GONE
            binding.translateLl.visibility = View.VISIBLE
        }
        binding.recognizeTextEdit.addTextChangedListener(object : TextWatcher{
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                binding.translationLl.visibility = View.VISIBLE
                binding.translateLl.visibility = View.GONE
                languageRecognition(binding.recognizeTextEdit.text.toString().trim())
            }
            override fun afterTextChanged(p0: Editable?) {
            }

        })
    }
    private var sourceLanguageText = ""
    private fun validateData() {
        sourceLanguageText = binding.recognizeTextEdit.text.toString().trim()

        if (sourceLanguageText.isEmpty()){
            showToast("Empty")
        }else {
            startTranslation()
        }
    }

    private fun startTranslation() {
        progressDialog.setMessage("Processing language model...")
        progressDialog.show()
        translatorOptions = TranslatorOptions.Builder()
            .setSourceLanguage(sourceLanguageCode)
            .setTargetLanguage(targetLanguageCode)
            .build()
        translator = Translation.getClient(translatorOptions)
        val downloadConditions  = DownloadConditions.Builder()
            .requireWifi()
            .build()

        translator.downloadModelIfNeeded(downloadConditions).addOnSuccessListener {
            progressDialog.setMessage("Translating...")
            translator.translate(sourceLanguageText).addOnSuccessListener {translatedText ->
                progressDialog.dismiss()
                binding.translateTv.text = translatedText
            }.addOnFailureListener {
                progressDialog.dismiss()
                showToast("${it.message}")
            }
        }.addOnFailureListener {
            progressDialog.dismiss()
            showToast("${it.message}")
        }
    }

    private fun initView(){
        cameraPermission = arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        storagePermission = arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        progressDialog = ProgressDialog(context)
        progressDialog.setTitle("Please wait")
        progressDialog.setCanceledOnTouchOutside(false)
        textRecognize = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    private fun sourceLanguageChoose(){
        val popupMenu = PopupMenu(context , binding.sourceLanguageChooseBtn)

        for(i in languageArrayList!!.indices){
            popupMenu.menu.add(Menu.NONE, i, i, languageArrayList!![i].languageTitle)
        }
        popupMenu.show()

        popupMenu.setOnMenuItemClickListener { menuItem ->
            val position  = menuItem.itemId
            sourceLanguageCode = languageArrayList!![position].languageCode
            sourceLanguageTitle = languageArrayList!![position].languageTitle

            binding.sourceLanguageChooseBtn.text = sourceLanguageTitle


            false
        }
    }
    private fun targetLanguageChoose(){
        val popupMenu = PopupMenu(context , binding.targetLanguageChooseBtn)

        for(i in languageArrayList!!.indices){
            popupMenu.menu.add(Menu.NONE, i, i, languageArrayList!![i].languageTitle)
        }
        popupMenu.show()

        popupMenu.setOnMenuItemClickListener { menuItem ->
            val position  = menuItem.itemId
            targetLanguageCode = languageArrayList!![position].languageCode
            targetLanguageTitle = languageArrayList!![position].languageTitle

            binding.targetLanguageChooseBtn.text = targetLanguageTitle


            false
        }
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
    private fun languageRecognition(text: String) {
        val languageIdentifier = LanguageIdentification.getClient()
        languageIdentifier.identifyPossibleLanguages(text)
            .addOnSuccessListener { identifiedLanguages ->
                for (identifiedLanguage in identifiedLanguages) {
//                    val language = identifiedLanguage.languageTag
                    val language = identifiedLanguages[0].languageTag
                    val confidence = identifiedLanguage.confidence
                    Log.d("translatetext", "$language $confidence")
                    for (i in 0 until languageArrayList!!.size){
                        if(languageArrayList!![i].languageCode == language){
                            binding.sourceLanguageChooseBtn.text = languageArrayList!![i].languageTitle
                            sourceLanguageCode = languageArrayList!![i].languageCode
                        }
                    }
                }
            }
            .addOnFailureListener {
                Log.d("translatetexterror",it.toString())
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

    override fun onResume() {
        super.onResume()
        binding.recognizeTextEdit.setText("")
        binding.translationLl.visibility = View.GONE
    }
}