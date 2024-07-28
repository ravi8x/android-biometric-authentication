package info.androidhive.biometricauthentication

import android.app.Activity
import android.content.Intent
import android.hardware.biometrics.BiometricManager.Authenticators.BIOMETRIC_STRONG
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.provider.Settings.EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricPrompt
import info.androidhive.biometricauthentication.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity(), BiometricUtils.BiometricAuthCallback {
    private val TAG = "MainActivity"
    private lateinit var binding: ActivityMainBinding
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var lockScreenFragment: LockScreenFragment
    private val authenticators = BIOMETRIC_STRONG

    private val enrollBiometricRequestLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                showBiometricPrompt()
            } else {
                Log.e(TAG, "Error enrolling in biometric ${it.resultCode}")
                //showSnackBar("Failed to enroll in biometric")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        lockScreenFragment = LockScreenFragment.newInstance()

        binding.content.btnUnlock.setOnClickListener {
            showBiometricPrompt()
        }

        biometricPrompt = BiometricUtils.createBiometricPrompt(this, this)

        // check if biometric is enrolled or not
        if (BiometricUtils.canAuthenticate(applicationContext)) {
            // biometric is supported
            binding.content.btnUnlock.text = getString(R.string.unlock)
        } else if (BiometricUtils.isEnrolledPending(applicationContext)) {
            // biometric is not enabled. User can enroll the biometric
            binding.content.btnUnlock.text = getString(R.string.enroll_biometric)
        }
    }

    private fun showBiometricPrompt() {
        if (BiometricUtils.isEnrolledPending(this)) {
            //BiometricUtils.enrollBiometric(this)
            enrollBiometric()
        } else if (BiometricUtils.canAuthenticate(this)) {
            val promptInfo = BiometricUtils.createPromptInfo(this)
            biometricPrompt.authenticate(promptInfo)
        }
    }

    private fun enrollBiometric() {
        // Biometric is supported from Android 11 / Api level 30
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val intent: Intent = when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                    Intent(Settings.ACTION_BIOMETRIC_ENROLL).putExtra(
                        EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED,
                        authenticators
                    )
                }

                Build.VERSION.SDK_INT >= Build.VERSION_CODES.P -> {
                    Intent(Settings.ACTION_FINGERPRINT_ENROLL)
                }

                else -> {
                    Intent(Settings.ACTION_SECURITY_SETTINGS)
                }
            }

            enrollBiometricRequestLauncher.launch(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        //showBiometricPrompt()
    }

    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {

    }

    override fun onAuthenticationFailed() {

    }

    override fun onAuthenticationError(errCode: Int, errString: CharSequence) {

    }
}