package info.androidhive.biometricauthentication.utils

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import info.androidhive.biometricauthentication.R

object BiometricUtils {
    const val AUTHENTICATORS = BIOMETRIC_STRONG or DEVICE_CREDENTIAL

    fun createBiometricPrompt(
        activity: AppCompatActivity,
        callback: BiometricAuthCallback
    ): BiometricPrompt {
        val executor = ContextCompat.getMainExecutor(activity)

        val promptCallback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationError(errCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errCode, errString)
                callback.onAuthenticationError(errCode, errString)
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                callback.onAuthenticationFailed()
            }

            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                callback.onAuthenticationSucceeded(result)
            }
        }
        return BiometricPrompt(activity, executor, promptCallback)
    }

    fun enrollBiometric(context: Context) {
        val canAuthenticate = BiometricManager.from(context).canAuthenticate(AUTHENTICATORS)
    }

    fun canAuthenticate(context: Context) = BiometricManager.from(context)
        .canAuthenticate(AUTHENTICATORS) == BiometricManager.BIOMETRIC_SUCCESS

    fun isEnrolledPending(context: Context) = BiometricManager.from(context)
        .canAuthenticate(AUTHENTICATORS) == BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED

    fun createPromptInfo(activity: AppCompatActivity): BiometricPrompt.PromptInfo =
        BiometricPrompt.PromptInfo.Builder().apply {
            setTitle(activity.getString(R.string.prompt_info_title))
            setSubtitle(activity.getString(R.string.prompt_info_subtitle))
            setAllowedAuthenticators(AUTHENTICATORS)
            setConfirmationRequired(false)
        }.build()

    interface BiometricAuthCallback {
        fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult)
        fun onAuthenticationFailed()
        fun onAuthenticationError(errCode: Int, errString: CharSequence)
    }
}