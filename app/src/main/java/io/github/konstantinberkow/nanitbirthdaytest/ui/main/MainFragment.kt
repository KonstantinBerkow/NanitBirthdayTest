package io.github.konstantinberkow.nanitbirthdaytest.ui.main

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import io.github.konstantinberkow.nanitbirthdaytest.R

private const val TAG = "MainFragment"

class MainFragment : Fragment() {

    companion object {
        fun newInstance() = MainFragment()
    }

    private val viewModel: MainViewModel by viewModels { MainViewModel.Factory }

    private lateinit var connectButton: Button

    private lateinit var editAddressInput: EditText

    private lateinit var progressBar: ProgressBar

    private var lastState: MainViewModel.State? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel.stateLiveData.observe(this) { newState ->
            render(newState)
        }
    }

    private fun render(newState: MainViewModel.State) {
        val oldState = lastState
        lastState = newState

        val enableInput = !newState.connecting && !newState.connected
        val hidInput = newState.connected
        val inputVisibility = if (hidInput) {
            View.GONE
        } else {
            View.VISIBLE
        }

        connectButton.isEnabled = enableInput
        connectButton.visibility = inputVisibility

        editAddressInput.isEnabled = enableInput
        editAddressInput.visibility = inputVisibility

        val info = newState.info

        val showLoading = newState.connecting || (newState.connected && info == null)
        progressBar.visibility = if (newState.connecting) {
            View.VISIBLE
        } else {
            View.GONE
        }

        if (info != oldState?.info && info != null) {
            Log.d(TAG, "render birthday info: $info")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_main, container, false).also {
            connectButton = it.findViewById(R.id.connect_button)
            editAddressInput = it.findViewById(R.id.edit_address_text)
            progressBar = it.findViewById(R.id.progress_bar)

            connectButton.setOnClickListener {
                val currentState = lastState ?: return@setOnClickListener

                val addressTextRaw = editAddressInput.text.toString()
                if (currentState.rawAddress != addressTextRaw) {
                    // address changed
                    viewModel.setWebSocketAddress(addressTextRaw)
                }
            }
        }
    }

}