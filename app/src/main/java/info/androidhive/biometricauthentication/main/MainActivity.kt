package info.androidhive.biometricauthentication.main

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.provider.Settings.EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricPrompt
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import info.androidhive.biometricauthentication.R
import info.androidhive.biometricauthentication.databinding.ActivityMainBinding
import info.androidhive.biometricauthentication.utils.BiometricUtils
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity(), BiometricUtils.BiometricAuthCallback {
    private val TAG = "MainActivity"
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var binding: ActivityMainBinding
    private lateinit var unlockDialog: AlertDialog
    private lateinit var sharedPref: SharedPreferences

    // App will ask for biometric auth after 10secs in background
    private val idleDuration = 30 //secs

    private val enrollBiometricRequestLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                showBiometricPrompt()
            } else {
                Log.e(
                    TAG,
                    "Failed to enroll in biometric authentication. Error code: ${it.resultCode}"
                )
                Toast.makeText(
                    this,
                    "Failed to enroll in biometric authentication. Error code: ${it.resultCode}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        sharedPref = getSharedPreferences(
            getString(R.string.preference_file_key), Context.MODE_PRIVATE
        )

        val navView: BottomNavigationView = binding.content.navView

        val navController = findNavController(R.id.nav_host_fragment_activity_main2)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_home, R.id.navigation_dashboard, R.id.navigation_notifications
            )
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)


        biometricPrompt = BiometricUtils.createBiometricPrompt(this, this)

        // Show enroll biometric dialog if none is enabled
        if (BiometricUtils.isEnrolledPending(applicationContext)) {
            // biometric is not enabled. User can enroll the biometric
            showEnrollBiometricDialog()
        }
    }

    private fun enrollBiometric() {
        // Biometric is supported from Android 11 / Api level 30
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            enrollBiometricRequestLauncher.launch(
                Intent(Settings.ACTION_BIOMETRIC_ENROLL).putExtra(
                    EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED, BiometricUtils.AUTHENTICATORS
                )
            )
        }
    }

    private fun showBiometricPrompt() {
        val promptInfo = BiometricUtils.createPromptInfo(this)
        biometricPrompt.authenticate(promptInfo)
    }

    override fun onPause() {
        super.onPause()

        // Save timestamp when app kept in background
        with(sharedPref.edit()) {
            putLong(getString(R.string.last_authenticate_time), System.currentTimeMillis())
            apply()
        }
    }

    override fun onResume() {
        super.onResume()

        // show biometric dialog when app is resumed from background
        showBiometricAuthIfNeeded()
    }

    /*
    * Show biometric auth if needed
    * Shows biometric auth if app kept in background more than 10secs
    * */
    private fun showBiometricAuthIfNeeded() {
        if (BiometricUtils.canAuthenticate(applicationContext)) {
            val lastMilliSec = sharedPref.getLong(getString(R.string.last_authenticate_time), -1)
            if (lastMilliSec.toInt() == -1) {
                showBiometricPrompt()
                return
            }

            // seconds difference between now and app background state time
            val secs = TimeUnit.MILLISECONDS.toSeconds(
                System.currentTimeMillis() - sharedPref.getLong(
                    getString(R.string.last_authenticate_time), 0
                )
            )
            Log.d(
                TAG, "Secs $secs, ${
                    sharedPref.getLong(
                        getString(R.string.last_authenticate_time), 0
                    )
                }"
            )

            // show biometric dialog if app idle time is more than 10secs
            if (secs > idleDuration) {
                showBiometricPrompt()
            }
        }
    }

    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
        dismissUnlockDialog()

        // store last authenticated timestamp
        with(sharedPref.edit()) {
            putLong(getString(R.string.last_authenticate_time), System.currentTimeMillis())
            apply()
        }
    }

    override fun onAuthenticationFailed() {
        Toast.makeText(this, R.string.auth_failed, Toast.LENGTH_SHORT).show()
    }

    override fun onAuthenticationError(errCode: Int, errString: CharSequence) {
        Log.e(TAG, "Authenticated error. Error code: $errCode, Message: $errString")

        // Show unlock dialog if user cancels auth dialog
        if (errCode == BiometricPrompt.ERROR_USER_CANCELED) {
            showLockedDialog()
        } else {
            // authentication error
            dismissUnlockDialog()
            Toast.makeText(
                this, getString(R.string.error_auth_error, errCode, errString), Toast.LENGTH_LONG
            ).show()
        }
    }

    // Enroll into biometric dialog
    private fun showEnrollBiometricDialog() {
        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle(R.string.enroll_biometric)
            .setMessage(R.string.enroll_biometric_message)
            .setNegativeButton(R.string.cancel) { dialog, _ ->
                dialog.dismiss()
            }
            .setPositiveButton(R.string.proceed) { _, _ ->
                enrollBiometric()
            }.setCancelable(false)
        unlockDialog = dialog.show()
    }

    // Show app locked dialog
    private fun showLockedDialog() {
        val dialog = MaterialAlertDialogBuilder(this).setTitle(R.string.locked)
            .setMessage(R.string.locked_message).setPositiveButton(R.string.btn_unlock) { _, _ ->
                showBiometricPrompt()
            }.setCancelable(false)
        unlockDialog = dialog.show()
    }

    private fun dismissUnlockDialog() {
        if (this::unlockDialog.isInitialized && unlockDialog.isShowing) unlockDialog.dismiss()
    }
}