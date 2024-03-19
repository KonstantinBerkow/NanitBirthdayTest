package io.github.konstantinberkow.nanitbirthdaytest.ui.main

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import io.github.konstantinberkow.nanitbirthdaytest.R
import io.github.konstantinberkow.nanitbirthdaytest.network.BirthdayMessage

private const val TAG = "MainFragment"

private val ThemeResources = arrayOf(
    intArrayOf(R.drawable.fox_bg, R.drawable.fox_fg, R.color.fox_background_color),
    intArrayOf(R.drawable.elephant_bg, R.drawable.elephant_fg, R.color.elephant_background_color),
    intArrayOf(R.drawable.pelican_bg, R.drawable.pelican_fg, R.color.pelican_background_color),
)

class MainFragment : Fragment() {

    companion object {
        fun newInstance() = MainFragment()
    }

    private val viewModel: MainViewModel by viewModels { MainViewModel.Factory }

    private lateinit var rootView: View

    private lateinit var connectButton: Button

    private lateinit var editAddressInput: EditText

    private lateinit var progressBar: ProgressBar

    private lateinit var babyImageView: ImageView

    private lateinit var decorationImageView: ImageView

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
        progressBar.visibility = if (showLoading) {
            View.VISIBLE
        } else {
            View.GONE
        }

        val decorationsVisibility = if (info == null) {
            View.GONE
        } else {
            View.VISIBLE
        }
        rootView.visibility = decorationsVisibility
        decorationImageView.visibility = decorationsVisibility
        babyImageView.visibility = decorationsVisibility
        if (info != oldState?.info && info != null) {
            Log.d(TAG, "render birthday info: $info")
            showBirthdayMessage(info)
        }
    }

    private fun showBirthdayMessage(info: BirthdayMessage) {
        val (screenDecoration, centerDecoration, bgColor) = ThemeResources[info.theme.ordinal]

        rootView.setBackgroundColor(bgColor)
        decorationImageView.setImageResource(screenDecoration)
        babyImageView.setImageResource(centerDecoration)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_main, container, false).also {
            rootView = it

            connectButton = it.findViewById(R.id.connect_button)
            editAddressInput = it.findViewById(R.id.edit_address_text)
            progressBar = it.findViewById(R.id.progress_bar)

            babyImageView = it.findViewById(R.id.baby_image)
            decorationImageView = it.findViewById(R.id.theme_decoration_image)

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