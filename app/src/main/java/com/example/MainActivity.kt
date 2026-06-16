package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
// Importación tradicional y a prueba de fallos
import com.google.firebase.firestore.FirebaseFirestore

// 1. Tu estructura simple para la oferta
data class Oferta(
    val titulo: String = "",
    val descripcion: String = "",
    val precio: String = "",
    val contacto: String = "",
    val fecha: Long = System.currentTimeMillis()
)

// 2. La clase Principal
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    PantallaTablon()
                }
            }
        }
    }
}

// 3. Un diseño visual básico para arrancar
@Composable
fun PantallaTablon() {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(text = "CubAlquila", style = MaterialTheme.typography.headlineLarge)
        Text(text = "Modo Libre - Sin Autenticación")
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(onClick = {
            publicarOfertaDirecto("Casa de Prueba", "Descripción", "100", "555-0000")
        }) {
            Text("Probar Conexión (Publicar)")
        }
    }
}

// 4. Tu función directa para publicar
fun publicarOfertaDirecto(titulo: String, descripcion: String, precio: String, contacto: String) {
    // Usamos getInstance() que no depende de extensiones
    val db = FirebaseFirestore.getInstance()
    val nuevaOferta = Oferta(titulo, descripcion, precio, contacto)

    db.collection("ofertas")
        .add(nuevaOferta)
        .addOnSuccessListener {
            println("Oferta publicada con éxito")
        }
        .addOnFailureListener { e ->
            println("Error al publicar: $e")
        }
}
