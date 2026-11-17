package com.example.airqualitymonitor.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import androidx.lifecycle.lifecycleScope
import com.example.airqualitymonitor.R
import com.example.airqualitymonitor.databinding.ActivityLoginBinding
import com.example.airqualitymonitor.ui.MainActivity
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var credentialManager: CredentialManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        auth = FirebaseAuth.getInstance()
        credentialManager = CredentialManager.create(this)

        // Maintain existing session: If already signed in, go straight to MainActivity
        if (auth.currentUser != null) {
            navigateToMain()
            return
        }

        binding.btnGoogleSignIn.setOnClickListener {
            initiateGoogleSignIn()
        }
    }

    private fun initiateGoogleSignIn() {
        val serverClientId = try {
            getString(R.string.default_web_client_id)
        } catch (e: Exception) {
            ""
        }

        if (serverClientId.isEmpty()) {
            Toast.makeText(
                this,
                "Web Client ID missing from google-services.json",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        // GetSignInWithGoogleOption ensures the Google account chooser bottom-sheet always opens
        val signInWithGoogleOption = GetSignInWithGoogleOption.Builder(serverClientId)
            .build()

        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(serverClientId)
            .setAutoSelectEnabled(false)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(signInWithGoogleOption)
            .addCredentialOption(googleIdOption)
            .build()

        binding.progressBar.visibility = View.VISIBLE
        binding.btnGoogleSignIn.isEnabled = false

        lifecycleScope.launch {
            try {
                val response = credentialManager.getCredential(
                    request = request,
                    context = this@LoginActivity
                )
                handleCredential(response.credential)
            } catch (e: GetCredentialCancellationException) {
                Toast.makeText(this@LoginActivity, "Sign-in cancelled", Toast.LENGTH_SHORT).show()
            } catch (e: NoCredentialException) {
                Toast.makeText(
                    this@LoginActivity,
                    "No Google credentials found. Please check that a Google account is added in device Settings.",
                    Toast.LENGTH_LONG
                ).show()
            } catch (e: Exception) {
                Toast.makeText(
                    this@LoginActivity,
                    "Sign-in failed: ${e.localizedMessage ?: "Unknown error"}",
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                binding.progressBar.visibility = View.GONE
                binding.btnGoogleSignIn.isEnabled = true
            }
        }
    }

    private fun handleCredential(credential: androidx.credentials.Credential) {
        if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            try {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val idToken = googleIdTokenCredential.idToken
                val authCredential = GoogleAuthProvider.getCredential(idToken, null)

                binding.progressBar.visibility = View.VISIBLE
                auth.signInWithCredential(authCredential)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Signed in successfully!", Toast.LENGTH_SHORT).show()
                        navigateToMain()
                    }
                    .addOnFailureListener { e ->
                        binding.progressBar.visibility = View.GONE
                        Toast.makeText(
                            this,
                            "Firebase Authentication failed: ${e.localizedMessage}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
            } catch (e: Exception) {
                binding.progressBar.visibility = View.GONE
                Toast.makeText(
                    this,
                    "Error parsing Google credentials: ${e.localizedMessage}",
                    Toast.LENGTH_LONG
                ).show()
            }
        } else {
            Toast.makeText(this, "Unexpected credential type", Toast.LENGTH_SHORT).show()
        }
    }

    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}
