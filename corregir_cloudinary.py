import os

print("\n🚀 --- APLICANDO CORRECCIÓN DE KOTLIN ---")
cloud_name = "mdmhprpj"
upload_preset = "dsjpuc7j"

# Función para encontrar el corchete de cierre exacto
def encontrar_cierre(texto, inicio):
    contador = 0
    for i in range(inicio, len(texto)):
        if texto[i] == '{': contador += 1
        elif texto[i] == '}':
            contador -= 1
            if contador == 0: return i
    return -1

# Buscar el archivo Kotlin
archivo_objetivo = None
for root, dirs, files in os.walk('app/src/main/java'):
    for f in files:
        if f.endswith('.kt'):
            ruta = os.path.join(root, f)
            with open(ruta, 'r', encoding='utf-8') as archivo:
                if 'fun uploadImage' in archivo.read():
                    archivo_objetivo = ruta
                    break
    if archivo_objetivo: break

if archivo_objetivo:
    with open(archivo_objetivo, 'r', encoding='utf-8') as archivo:
        contenido = archivo.read()
    
    # Encontrar la función
    firma = "fun uploadImage("
    inicio_func = contenido.find(firma)
    
    if inicio_func != -1:
        inicio_corchete = contenido.find("{", inicio_func)
        fin_corchete = encontrar_cierre(contenido, inicio_corchete)
        
        # Nuevo código con parseo de texto simple (sin Regex)
        nuevo_codigo = f"""fun uploadImage(imageBytes: ByteArray): String? {{
    val client = okhttp3.OkHttpClient()
    val imageBody = okhttp3.RequestBody.create(okhttp3.MediaType.parse("image/jpeg"), imageBytes)
    val requestBody = okhttp3.MultipartBody.Builder()
        .setType(okhttp3.MultipartBody.FORM)
        .addFormDataPart("file", "image.jpg", imageBody)
        .addFormDataPart("upload_preset", "{upload_preset}")
        .build()

    val request = okhttp3.Request.Builder()
        .url("https://api.cloudinary.com/v1_1/{cloud_name}/image/upload")
        .post(requestBody)
        .build()

    return try {{
        client.newCall(request).execute().use {{ response ->
            val bodyStr = response.body?.string()
            if (response.isSuccessful && bodyStr != null) {{
                // Usamos substring para cortar el texto de forma segura y evitar errores de compilador
                val url = bodyStr.substringAfter("\\"secure_url\\":\\"").substringBefore("\\"")
                if (url != bodyStr) url else null
            }} else {{
                null
            }}
        }}
    }} catch (e: Exception) {{
        null
    }}
}}"""
        
        # Reemplazar e inyectar
        contenido_final = contenido[:inicio_func] + nuevo_codigo + contenido[fin_corchete+1:]
        
        with open(archivo_objetivo, 'w', encoding='utf-8') as archivo:
            archivo.write(contenido_final)
            
        print(f"\n✅ ¡Éxito! El código fue corregido de forma segura en: {archivo_objetivo}")
    else:
        print("\n❌ No se encontró la función uploadImage en el archivo.")
else:
    print("\n❌ No se encontró ningún archivo Kotlin con la función de subida de imágenes.")
