package com.example.data.repository

import android.content.Context
import android.net.Uri
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream

class SharedSyncService(private val context: Context) {

    private val db = FirebaseFirestore.getInstance()
    private val client = OkHttpClient()

    // 1. FUNCIÓN PARA SUBIR LA IMAGEN GRATIS A CATBOX Y OBTENER LA URL
    suspend fun uploadImageToFreeHost(imageUri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            // Crear un archivo temporal con los bytes de la imagen seleccionada
             // Procesando archivo 
            val file = File(context.cacheDir, "temp_upload_image.jpg")
            context.contentResolver.openInputStream(imageUri)?.use { input ->
                FileOutputStream(file).use { output ->
                    input.copyTo(output)
                }
            }

            // Preparar el cuerpo de la petición Multipart exigida por Catbox
            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("reqtype", "fileupload")
                .addFormDataPart(
                    "fileToUpload", 
                    file.name, 
                    file.asRequestBody("image/jpeg".toMediaTypeOrNull())
                )
                .build()

            val request = Request.Builder()
                .url("https://catbox.moe/user/api.php")
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val imageUrl = response.body?.string()?.trim()
                    // Eliminamos el archivo temporal del teléfono
                    if (file.exists()) file.delete()
                    return@withContext imageUrl // Devuelve la URL directa de la foto
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext null
    }

    // 2. FUNCIÓN PARA GUARDAR EL ANUNCIO EN CLOUD FIRESTORE (TOTALMENTE GRATIS)
    suspend fun saveListingToFirestore(
        title: String, 
        price: String, 
        description: String, 
        phone: String, 
        province: String,
        imageUri: Uri?
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            var finalImageUrl = ""

            // Si el usuario seleccionó una foto, primero la subimos al hosting gratuito
            if (imageUri != null) {
                val uploadedUrl = uploadImageToFreeHost(imageUri)
                if (uploadedUrl != None && uploadedUrl!!.startsWith("http")) {
                    finalImageUrl = uploadedUrl
                }
            }

            // Estructuramos el anuncio para Firestore
            val listingData = hashMapOf(
                "title" to title,
                "price" to price,
                "description" to description,
                "phone" to phone,
                "province" to province,
                "imageUrl" to finalImageUrl,
                "timestamp" to System.currentTimeMillis()
            )

            // Guardamos el documento de texto puro en la colección "anuncios"
            db.collection("anuncios")
                .add(listingData)
                .await() // Espera de forma segura a que Firebase confirme el guardado
                
            return@withContext true
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext false
        }
    }
}
