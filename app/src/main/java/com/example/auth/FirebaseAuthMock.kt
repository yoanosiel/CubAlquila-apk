package com.example.auth

import com.parse.ParseUser
import java.lang.Exception

class FirebaseAuth {
    companion object {
        @JvmStatic fun getInstance(): FirebaseAuth = FirebaseAuth()
    }
    val currentUser: FirebaseUser? = if (ParseUser.getCurrentUser() != null) FirebaseUser() else null

    fun signInWithEmailAndPassword(email: String, pass: String): FakeTask {
        val task = FakeTask()
        ParseUser.logInInBackground(email, pass) { _, e -> task.execute(e) }
        return task
    }
    fun createUserWithEmailAndPassword(email: String, pass: String): FakeTask {
        val task = FakeTask()
        val user = ParseUser()
        user.username = email
        user.email = email
        user.setPassword(pass)
        user.signUpInBackground { e -> task.execute(e) }
        return task
    }
    fun signInWithCredential(cred: Any?): FakeTask {
        val task = FakeTask()
        task.execute(Exception("Google Login no está disponible en Cuba sin VPN. Usa correo."))
        return task
    }
}

class FirebaseUser

class FakeTask {
    var isSuccessful: Boolean = false
    var exception: Exception? = null
    private var listener: ((FakeTask) -> Unit)? = null
    private var executed = false
    fun execute(e: Exception?) {
        isSuccessful = (e == null)
        exception = e
        executed = true
        listener?.invoke(this)
    }
    fun addOnCompleteListener(l: (FakeTask) -> Unit) {
        listener = l
        if (executed) l(this)
    }
}

class GoogleAuthProvider {
    companion object {
        @JvmStatic fun getCredential(idToken: String, accessToken: String?) = Any()
    }
}
