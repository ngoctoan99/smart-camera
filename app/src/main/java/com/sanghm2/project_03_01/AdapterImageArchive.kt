package com.sanghm2.project_03_01

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Environment
import android.text.format.DateFormat
import android.util.Log
import android.view.*
import android.widget.PopupMenu
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.sanghm2.project_03_01.activity.FullScreenImage
import com.sanghm2.project_03_01.databinding.RowImageBinding
import com.sanghm2.project_03_01.model.ModelImage
import java.io.File
import java.util.*
import kotlin.collections.ArrayList


class AdapterImageArchive(
    private var context: Context,
    private var imageList: ArrayList<ModelImage>,
) : RecyclerView.Adapter<AdapterImageArchive.HolderImage>() {
    private lateinit var binding : RowImageBinding
    inner class HolderImage(itemView : View) : RecyclerView.ViewHolder(itemView){
        var image = binding.imageStorageIv
        var nameTv = binding.nameImageTv
        var dateTv = binding.dateImageTv
        var moreBtn = binding.moreBtn
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HolderImage {
        binding = RowImageBinding.inflate(LayoutInflater.from(context),parent,false)
        return HolderImage(binding.root)
    }

    override fun onBindViewHolder(holder: HolderImage, position: Int) {
        val model  = imageList[position]
        val imageUri = model.imageUri
        val name = model.file.name
        val file = File(model.file.toString())
        val lastModDate = Date(file.lastModified())
        val timeStamp  =lastModDate.time
        val namefile = model.file.name
        binding.dateImageTv.text = formatTimeStamp(timeStamp)
        binding.nameImageTv.text = name
        Glide.with(context).load(imageUri).placeholder(R.drawable.ic_baseline_image_24).into(holder.image)

        holder.itemView.setOnClickListener {
            val intent = Intent(context , FullScreenImage::class.java)
            intent.putExtra("uri","${model.imageUri}")
            Log.d("datamodel", model.toString())
            context.startActivity(intent)
        }

        holder.moreBtn.setOnClickListener {
            showDialog(namefile,holder)
            Log.d("namefile",namefile)
        }
    }

    private fun showDialog(namefile: String, holder: HolderImage) {
        val popupMenu = PopupMenu(context, holder.itemView,Gravity.END)
        popupMenu.menu.add(Menu.NONE , 1,1,"Share")
        popupMenu.menu.add(Menu.NONE , 2,2,"Delete")
        popupMenu.show()
        popupMenu.setOnMenuItemClickListener { menuItem->
            val id = menuItem.itemId
            if(id == 1){
                shareImageAndText(namefile)
            }else if(id == 2){
                deleteImage(namefile, holder)
            }
            return@setOnMenuItemClickListener false
        }
    }
    private fun formatTimeStamp(timeStamp: Long): String {
        val cal = Calendar.getInstance(Locale.ENGLISH)
        cal.timeInMillis = timeStamp
        return DateFormat.format("dd/MM/yyyy HH:mm:ss aa", cal).toString()
    }
    private fun shareImageAndText(namefile: String) {
        val file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),"QR Generator"+"/${namefile}" )
        val bmpUri = FileProvider.getUriForFile(context, "com.sanghm2.project_03_01.fileprovider", file)
        val intent = Intent().apply {
            this.action = Intent.ACTION_SEND
            this.putExtra(Intent.EXTRA_STREAM, bmpUri)
            this.type = "image/png"
        }
        context.startActivity(Intent.createChooser(intent,"Share Image"))
    }
    @SuppressLint("NotifyDataSetChanged")
    private fun deleteImage(namefile: String, holder: HolderImage){
        imageList.removeAt(holder.position)
        notifyItemRemoved(holder.position)
        val file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),"QR Generator"+"/${namefile}" )
        file.delete()
    }
    override fun getItemCount(): Int {
        return imageList.size
    }

}