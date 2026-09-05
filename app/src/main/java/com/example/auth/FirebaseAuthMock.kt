package com.example.auth

import java.lang.Exception

class FirebaseAuth {
    companion object {
        @JvmStatic fun getInstance(): FirebaseAuth = FirebaseAuth()
    }
    val currentUser: FirebaseUser? = null

    fun signInWithEmailAndPassword(email: String, pass: String): FakeTask =
        FakeTask().apply { execute(if (email.isBlank() || pass.isBlank()) Exception("Correo o contraseña vacíos") else null) }

    fun createUserWithEmailAndPassword(email: String, pass: String): FakeTask =
        FakeTask().apply { execute(if (email.isBlank() || pass.length < 6) Exception("Datos de registro no válidos") else null) }

    fun signInWithCredential(cred: Any?): FakeTask =
        FakeTask().apply { execute(Exception("Inicio de sesión con Google no está configurado en esta versión.")) }
}

class FirebaseUser

class FakeTask {
    var isSuccessful: Boolean = false
    var exception: Exception? = null
    private var listener: ((FakeTask) -> Unit)? = null
    private var executed = false

    fun execute(e: Exception?) {
        isSuccessful = e == null
        exception = e
        executed = true
        listener?.invoke(this)
    }

    fun addOnCompleteListener(activity: Any, l: (FakeTask) -> Unit) {
        listener = l
        if (executed) l(this)
    }

    fun addOnCompleteListener(l: (FakeTask) -> Unit) {
        listener = l
        if (executed) l(this)
    }
}

class GoogleAuthProvider {
    companion object {
        @JvmStatic fun getCredential(idToken: String?, accessToken: String?) = Any()
    }
}
