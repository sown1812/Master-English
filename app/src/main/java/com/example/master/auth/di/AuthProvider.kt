package com.example.master.auth.di

import com.google.firebase.auth.FirebaseAuth

interface AuthProvider {
    val firebaseAuth: FirebaseAuth
}

class FirebaseAuthProvider : AuthProvider {
    override val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
}
