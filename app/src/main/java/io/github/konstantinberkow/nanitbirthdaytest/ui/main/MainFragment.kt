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
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import io.github.konstantinberkow.nanitbirthdaytest.R
import io.github.konstantinberkow.nanitbirthdaytest.network.BirthdayMessage
import java.time.temporal.ChronoUnit
import java.util.Calendar
import java.util.GregorianCalendar

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

    private lateinit var nanitLogo: View

    private var lastState: MainViewModel.State? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel.stateLiveData.observe(this) { newState ->
            render(newState)
        }
    }

    private fun render(newState: MainViewModel.State) {
        Log.d(TAG, "render $newState")
        val oldState = lastState
        lastState = newState

        val enableInput = !newState.connecting && !newState.connected
        Log.d(TAG, "enableInput = $enableInput")
        val hideInput = newState.connected
        Log.d(TAG, "hideInput = $hideInput")
        val inputVisibility = if (hideInput) {
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
        Log.d(TAG, "showLoading = $showLoading")
        progressBar.visibility = if (showLoading) {
            View.VISIBLE
        } else {
            View.GONE
        }

        val hideDecorations = info == null
        Log.d(TAG, "hideDecorations = $hideDecorations")
        val decorationsVisibility = if (hideDecorations) {
            View.GONE
        } else {
            View.VISIBLE
        }
        decorationImageView.visibility = decorationsVisibility
        babyImageView.visibility = decorationsVisibility
        nanitLogo.visibility = decorationsVisibility
        if (info != oldState?.info && info != null) {
            Log.d(TAG, "render birthday info: $info")
            showBirthdayMessage(info)
        }
    }

    private fun showBirthdayMessage(info: BirthdayMessage) {
        val (screenDecorationId, centerDecorationId, bgColorId) = ThemeResources[info.theme.ordinal]
        val bgColor = ResourcesCompat.getColor(resources, bgColorId, requireActivity().theme)

        rootView.setBackgroundColor(bgColor)
        decorationImageView.setImageResource(screenDecorationId)
        babyImageView.setImageResource(centerDecorationId)

        val birthDate = (Calendar.getInstance().apply {
            timeInMillis = info.dateOfBirth
        } as GregorianCalendar).toZonedDateTime()

        val now = (Calendar.getInstance() as GregorianCalendar).toZonedDateTime()
        Log.d(TAG, "Birth date: $birthDate")
        Log.d(TAG, "now   date: $now")

        val differenceInMonths = ChronoUnit.MONTHS.between(birthDate, now)
        if (differenceInMonths > 11) {
            val differenceInYears = ChronoUnit.YEARS.between(birthDate, now)
            showAgeInYears(differenceInYears)
        } else {
            showAgeInMonths(differenceInMonths)
        }
    }

    private fun showAgeInMonths(differenceInMonths: Long) {
        Log.d(TAG, "showAgeInMonths: $differenceInMonths")

    }

    private fun showAgeInYears(differenceInYears: Long) {
        Log.d(TAG, "showAgeInYears: $differenceInYears")
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
            nanitLogo = it.findViewById(R.id.nanit_logo)

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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            Log.d(TAG, "system bars insets: $systemBars")

            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}