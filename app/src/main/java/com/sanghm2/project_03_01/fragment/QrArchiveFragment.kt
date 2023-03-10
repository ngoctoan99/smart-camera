package com.sanghm2.project_03_01.fragment

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.sanghm2.project_03_01.AdapterImageArchive
import com.sanghm2.project_03_01.R
import com.sanghm2.project_03_01.databinding.FragmentQrArchiveBinding
import com.sanghm2.project_03_01.model.ModelImage
import java.io.File

class QrArchiveFragment : Fragment() {
    private lateinit var binding : FragmentQrArchiveBinding
    private lateinit var imageArrayList : ArrayList<ModelImage>
    private lateinit var adapterImage : AdapterImageArchive
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentQrArchiveBinding.inflate(LayoutInflater.from(context),container,false)
        loadImage()
        return binding.root
    }

    private fun loadImage(){
        imageArrayList = ArrayList()
        adapterImage = AdapterImageArchive(requireContext(),imageArrayList)
        binding.qrRv.adapter = adapterImage
        val folder = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),"QR Generator")
        if(folder.exists()){
            val files = folder.listFiles()
            if(files != null && files.isEmpty()){
                binding.emptyList.visibility = View.VISIBLE
                binding.qrRv.visibility = View.GONE
            }else {
                for(fileEntry in files){
                    binding.emptyList.visibility = View.GONE
                    binding.qrRv.visibility = View.VISIBLE
                    val uri = Uri.fromFile(fileEntry)
                    val model = ModelImage(fileEntry,uri)
                    imageArrayList.add(model)
                    Log.d("toandatafile",model.toString())
                    adapterImage.notifyItemInserted(imageArrayList.size)
                }
            }
        }else {
            binding.emptyList.visibility = View.VISIBLE
            binding.qrRv.visibility = View.GONE
        }
    }

}