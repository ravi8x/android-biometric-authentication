package info.androidhive.biometricauthentication

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import info.androidhive.biometricauthentication.databinding.FragmentLockScreenBinding

class LockScreenFragment : DialogFragment() {
    private lateinit var binding: FragmentLockScreenBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentLockScreenBinding.inflate(layoutInflater)
        return binding.root
    }

    companion object {
        @JvmStatic
        fun newInstance() =
            LockScreenFragment().apply {

            }
    }
}