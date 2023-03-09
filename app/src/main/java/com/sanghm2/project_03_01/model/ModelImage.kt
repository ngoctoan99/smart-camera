package com.sanghm2.project_03_01.model

import android.net.Uri
import java.io.File

class ModelImage (var file : File, var imageUri : Uri){
    override fun toString(): String {
        return "ModelImage(file=$file, imageUri=$imageUri)"
    }
}
