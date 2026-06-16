package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.firebase.firestore.FirebaseFirestore

// 1. Estructura simple para la oferta
data class Oferta(
    val titulo: String = "",
    val descripcion: String = "",
    val precio: String = "",
    val contacto: String = "",
    val fecha: Long = System.currentTimeMillis()
)

// 2. Clase Principal para mostrar la pantalla
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

// 3. Diseño visual básico Modo Libre
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

// 4. Función directa para publicar en Firestore
fun publicarOfertaDirecto(titulo: String, descripcion: String, precio: String, contacto: String) {
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
