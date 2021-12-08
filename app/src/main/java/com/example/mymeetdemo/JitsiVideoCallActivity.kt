package com.example.mymeetdemo

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.BottomSheetCallback
import org.apache.commons.lang3.RandomStringUtils
/*import org.jitsi.meet.sdk.JitsiMeet
import org.jitsi.meet.sdk.JitsiMeetActivity
import org.jitsi.meet.sdk.JitsiMeetConferenceOptions*/
import java.lang.Exception
import java.net.MalformedURLException
import java.net.URL


class JitsiVideoCallActivity : AppCompatActivity() {

    private var behavior: BottomSheetBehavior<*>? = null
    private var behaviorJoin: BottomSheetBehavior<*>? = null
    private var coordinatorLayout: CoordinatorLayout? = null

    private var mNewMeet: TextView? = null
    private var mJoinMeet: TextView? = null

    private var mShareButton: TextView? = null
    private var mStartInstantMeet: TextView? = null
    private var mCloseNewMeetSheet: TextView? = null

    private var mBackMeet: TextView? = null
    private var mEditMeetCode: EditText? = null
    private var mStartExistingMeet: TextView? = null

    private var randomString: String? = null

    private val sharedPrefFile = "kotlin"
    private var sharedPreferences: SharedPreferences? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize default options for Jitsi Meet conferences.
        val serverURL: URL
        serverURL = try {
            // When using JaaS, replace "https://meet.jit.si" with the proper serverURL
            URL("https://meet.jit.si")
        } catch (e: MalformedURLException) {
            e.printStackTrace()
            throw RuntimeException("Invalid server URL!")
        }
       /* val defaultOptions = JitsiMeetConferenceOptions.Builder()
            .setServerURL(serverURL)
            // When using JaaS, set the obtained JWT here
            //.setToken("MyJWT")
            .setWelcomePageEnabled(false)
            .build()
        JitsiMeet.setDefaultConferenceOptions(defaultOptions)*/

        sharedPreferences = this.getSharedPreferences(sharedPrefFile, Context.MODE_PRIVATE)
        mDeclarations()
    }

    private fun mDeclarations() {
        getItemIds()
        setSelections()
        getDataFromExternalLink()
    }

    private fun getDataFromExternalLink() {
        if (intent.data != null) {
            val uri: Uri? = intent.data
            val path: String = uri?.path!!

            if (path != "" || path.isNotEmpty()) {
                randomString = path
                randomString?.replace("/" , "")
                // Build options object for joining the conference. The SDK will merge the default
                // one we set earlier and this one when joining.
                /*val options = JitsiMeetConferenceOptions.Builder()
                    .setRoom(randomString)
                    .setAudioMuted(true)
                    .setVideoMuted(true)
                    .setAudioOnly(false)
                    .build()
                // Launch the new activity with the given options. The launch() method takes care
                // of creating the required Intent and passing the options.
                JitsiMeetActivity.launch(this, options)*/
                randomString = ""

                try {
                    if (!sharedPreferences?.getString("room_code","null").equals("")) {
                        val editor:SharedPreferences.Editor =  sharedPreferences!!.edit()
                        editor.putString("room_code","")
                        editor.apply()
                    }
                } catch (ex: Exception) {
                    ex.printStackTrace()
                }

            }
        }
    }

    private fun setSelections() {
        newMeet()
        joinMeet()
        shareMeet()
        mEditMeetCode?.addTextChangedListener(textWatcher)
    }

    private fun newMeet() {
        mNewMeet?.setOnClickListener {
            behavior!!.setState(BottomSheetBehavior.STATE_EXPANDED)
        }

        mCloseNewMeetSheet?.setOnClickListener {
            behavior!!.setState(BottomSheetBehavior.STATE_COLLAPSED)
        }

        mStartInstantMeet?.setOnClickListener {
            behavior!!.state = BottomSheetBehavior.STATE_COLLAPSED

            randomString = RandomStringUtils.randomAlphanumeric(3)
            randomString = randomString +  "-" + RandomStringUtils.randomAlphanumeric(4)
            randomString = randomString +  "-" +  RandomStringUtils.randomAlphanumeric(2)
            randomString = randomString?.replace("\\s".toRegex(), "")

            if (!sharedPreferences?.getString("room_code","null").equals("")) {
                // Build options object for joining the conference. The SDK will merge the default
                // one we set earlier and this one when joining.
                /*val options = JitsiMeetConferenceOptions.Builder()
                    .setRoom(sharedPreferences?.getString("room_code","null"))
                    .setAudioMuted(false)
                    .setVideoMuted(true)
                    .setAudioOnly(false)
                    .build()
                // Launch the new activity with the given options. The launch() method takes care
                // of creating the required Intent and passing the options.
                JitsiMeetActivity.launch(this, options)*/
                randomString = ""
                val editor:SharedPreferences.Editor =  sharedPreferences!!.edit()
                editor.putString("room_code","")
                editor.apply()
            } else {
                // Build options object for joining the conference. The SDK will merge the default
                // one we set earlier and this one when joining.
               /* val options = JitsiMeetConferenceOptions.Builder()
                    .setRoom(randomString)
                    .setAudioMuted(false)
                    .setVideoMuted(true)
                    .setAudioOnly(false)
                    .build()
                // Launch the new activity with the given options. The launch() method takes care
                // of creating the required Intent and passing the options.
                JitsiMeetActivity.launch(this, options)*/
                randomString = ""
            }


        }
    }

    private fun joinMeet() {
        mJoinMeet?.setOnClickListener {
            behaviorJoin!!.setState(BottomSheetBehavior.STATE_EXPANDED)
        }

        mBackMeet?.setOnClickListener {
            behaviorJoin!!.state = BottomSheetBehavior.STATE_COLLAPSED
            mEditMeetCode?.setText("")
        }

        mStartExistingMeet?.setOnClickListener {
            randomString = mEditMeetCode?.text.toString().replace("\\s".toRegex(), "")
            when {
                mEditMeetCode?.text.toString().isEmpty() -> {
                    mEditMeetCode?.error = "Please Enter Code Here"
                }
                randomString?.length == 11 -> {
                    behaviorJoin!!.state = BottomSheetBehavior.STATE_COLLAPSED

                    // Build options object for joining the conference. The SDK will merge the default
                    // one we set earlier and this one when joining.
                    /*val options = JitsiMeetConferenceOptions.Builder()
                        .setRoom(randomString)
                        .setAudioMuted(false)
                        .setVideoMuted(true)
                        .setAudioOnly(false)
                        .build()
                    // Launch the new activity with the given options. The launch() method takes care
                    // of creating the required Intent and passing the options.
                    JitsiMeetActivity.launch(this, options)*/
                    randomString = ""
                    mEditMeetCode?.setText("")
                }
                else -> {
                    mEditMeetCode?.error = "Please Enter Valid Code"
                }
            }

        }
    }

    private fun shareMeet() {
        mShareButton?.setOnClickListener {

            randomString = RandomStringUtils.randomAlphanumeric(3)
            randomString = randomString +  "-" + RandomStringUtils.randomAlphanumeric(4)
            randomString = randomString +  "-" +  RandomStringUtils.randomAlphanumeric(2)
            randomString = randomString?.replace("\\s".toRegex(), "")

            val sendIntent = Intent()
            sendIntent.action = Intent.ACTION_SEND
            sendIntent.putExtra(
                Intent.EXTRA_TEXT, "To join the video meeting, click this link: " +
                        "https://com.example.mymeetdemo/" + randomString +
                        " " +
                        " " +
                        " " +
                        " " +
                        " Or use this code to join: " + randomString
            )
            sendIntent.type = "text/plain"
            if (sendIntent.resolveActivity(packageManager) != null) {
                startActivity(Intent.createChooser(sendIntent, "Share link!"))
            }

            val editor:SharedPreferences.Editor =  sharedPreferences!!.edit()
            editor.putString("room_code",randomString)
            editor.apply()
            editor.commit()
        }
    }

    private val textWatcher = object : TextWatcher {
        override fun afterTextChanged(s: Editable?) {
        }
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
        }
        @SuppressLint("SetTextI18n")
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            var editCode: String?
            if (mEditMeetCode?.text.toString().length == 3) {
                editCode = mEditMeetCode?.text.toString().trim()
                mEditMeetCode?.setText("$editCode-")
                mEditMeetCode?.setSelection(4)
            }
            if (mEditMeetCode?.text.toString().length == 8) {
                editCode = mEditMeetCode?.text.toString().trim()
                mEditMeetCode?.setText("$editCode-")
                mEditMeetCode?.setSelection(9)
            }
        }
    }

    private fun getItemIds() {
        mNewMeet = findViewById(R.id.new_meet_button)
        mJoinMeet = findViewById(R.id.join_meet_button)

        mShareButton = findViewById(R.id.share_meet)
        mStartInstantMeet = findViewById(R.id.start_meet)
        mCloseNewMeetSheet = findViewById(R.id.close_sheet)

        mBackMeet = findViewById(R.id.back_meet)
        mEditMeetCode = findViewById(R.id.edt_meet_code)
        mStartExistingMeet = findViewById(R.id.start_new_meet)

        coordinatorLayout = findViewById(R.id.coordinatorLayout)

        val bottomSheetNewMeet: View = findViewById(R.id.bottom_sheet_new_meet)
        behavior = BottomSheetBehavior.from<View>(bottomSheetNewMeet)

        behavior!!.addBottomSheetCallback(object : BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                // React to state change
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                // React to dragging events
            }
        })

        val bottomSheetJoinMeet: View = findViewById(R.id.bottom_sheet_join_meet)
        behaviorJoin = BottomSheetBehavior.from<View>(bottomSheetJoinMeet)

        behaviorJoin!!.addBottomSheetCallback(object : BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                // React to state change
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                // React to dragging events
            }
        })
    }
}