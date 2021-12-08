package com.example.mymeetdemo

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class RoomCodeActivity : AppCompatActivity() {

    private var mRoomCode: EditText? = null
    private var mStartMeet: TextView? = null

    private val CAMERA_PERMISSION = Manifest.permission.CAMERA
    private val RECORD_AUDIO_PERMISSION = Manifest.permission.RECORD_AUDIO
    private val CAMERA_PERMISSION_REQUEST_CODE = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_enter_room_code)

        mStart()
    }

    private fun mStart() {
        mInit()
    }

    private fun mInit() {
        mRoomCode = findViewById(R.id.edt_room_code)
        mStartMeet = findViewById(R.id.start_new_video_call)

        mStartMeet?.setOnClickListener {
            if (mRoomCode?.text.toString() == "") {
                mRoomCode?.error = "Enter Room Code To Continue"
            } else {
                if (ContextCompat.checkSelfPermission(this, CAMERA_PERMISSION) != PackageManager.PERMISSION_GRANTED) {
                     requestCameraPermission()
                } else {
                    val intent = Intent(this@RoomCodeActivity, CompleteActivity::class.java)
                    intent.putExtra("room_code",mRoomCode?.text.toString().trim())
                    startActivity(intent)
                }
            }
        }
    }

    private fun requestCameraPermission(dialogShown: Boolean = false) {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, CAMERA_PERMISSION) && !dialogShown) {
            showPermissionRationaleDialog()
        } else {
            requestPermissions(arrayOf(CAMERA_PERMISSION, RECORD_AUDIO_PERMISSION), CAMERA_PERMISSION_REQUEST_CODE)
        }
    }

    private fun showPermissionRationaleDialog() {
        AlertDialog.Builder(this)
            .setTitle("Camera Permission Required")
            .setMessage("This app need the camera to function")
            .setPositiveButton("Grant") { dialog, _ ->
                dialog.dismiss()
                requestCameraPermission(true)
            }
            .setNegativeButton("Deny") { dialog, _ ->
                dialog.dismiss()
                onCameraPermissionDenied()
            }
            .show()
    }

    private fun onCameraPermissionDenied() {
        Toast.makeText(this, "Camera Permission Denied", Toast.LENGTH_LONG).show()
    }

}