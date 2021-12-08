package com.example.mymeetdemo

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.example.mymeetdemo.databinding.ActivitySamplePeerConnectionBinding
import io.socket.client.IO
import io.socket.client.Socket
import io.socket.client.Socket.EVENT_CONNECT
import io.socket.client.Socket.EVENT_DISCONNECT
import org.json.JSONException
import org.json.JSONObject
import org.webrtc.*
import org.webrtc.PeerConnection.*
import org.webrtc.audio.AudioDeviceModule
import org.webrtc.audio.JavaAudioDeviceModule
import org.webrtc.voiceengine.WebRtcAudioUtils
import java.net.URISyntaxException
import java.util.*


class CompleteActivity : AppCompatActivity(), RendererCommon.RendererEvents {

    private var socket: Socket? = null
    private var isInitiator = false
    private var isChannelReady = false
    private var isStarted = false
    private var binding: ActivitySamplePeerConnectionBinding? = null
    private var peerConnection: PeerConnection? = null
    private var rootEglBase: EglBase? = null
    private var factory: PeerConnectionFactory? = null
    private var videoTrackFromCamera: VideoTrack? = null
    private var audioManager: AudioManager? = null
    private var videoCapturer = createVideoCapturer()

    val HIGH_VIDEO_RESOLUTION_WIDTH = 1280
    val HIGH_VIDEO_RESOLUTION_HEIGHT = 720
    val HIGH_FPS = 60

    val LOW_VIDEO_RESOLUTION_WIDTH = 480
    val LOW_VIDEO_RESOLUTION_HEIGHT = 360
    val LOW_FPS = 30

    var rtcClient: RTCClient? = null

    private var imgBtnMute: ImageView? = null
    private var imgBtnEndCall: ImageView? = null
    private var imgBtnPause: ImageView? = null
    private var imgBtnSwitchCamera: ImageView? = null
    private var isAudioMute: Boolean = false
    private var isVideoMute: Boolean = false
    private var mRoomCode: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_sample_peer_connection)

        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager?
        audioManager?.isMicrophoneMute = false
        if (!audioManager?.isSpeakerphoneOn!!) {
            Log.d("SpeakerStatus", "Speaker already OFF")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val playbackAttributes: AudioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
                val focusRequest: AudioFocusRequest =
                    AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                        .setAudioAttributes(playbackAttributes)
                        .setAcceptsDelayedFocusGain(true)
                        .setOnAudioFocusChangeListener { }
                        .build()
                audioManager!!.requestAudioFocus(focusRequest)
            } else {
                audioManager!!.requestAudioFocus(
                    null, AudioManager.STREAM_VOICE_CALL,
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT
                )
            }
            audioManager?.mode = AudioManager.MODE_IN_COMMUNICATION
            audioManager?.isSpeakerphoneOn = true
        } else {
            Log.d("SpeakerStatus", "Speaker already ON")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val playbackAttributes: AudioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
                val focusRequest: AudioFocusRequest =
                    AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                        .setAudioAttributes(playbackAttributes)
                        .setAcceptsDelayedFocusGain(true)
                        .setOnAudioFocusChangeListener { }
                        .build()
                audioManager!!.requestAudioFocus(focusRequest)
            } else {
                audioManager!!.requestAudioFocus(
                    null, AudioManager.STREAM_VOICE_CALL,
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT
                )
            }
            audioManager?.mode = AudioManager.MODE_IN_COMMUNICATION
        }

        start()
    }

    override fun onDestroy() {
        if (socket != null) {
            sendMessage("bye")
            socket!!.disconnect()
            peerConnection?.close()
            finish()
        }
        super.onDestroy()
    }

    private fun start() {
        initView()
        clickListeners()
        startWebRTCFlow()
    }

    private fun startWebRTCFlow() {
        runOnUiThread {
            connectToSignallingServer()
            initializeSurfaceViews()
            initializePeerConnectionFactory()
            createVideoTrackFromCameraAndShowIt()
            initializePeerConnections()
            startStreamingVideo()
        }
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private fun clickListeners() {
        imgBtnMute?.setOnClickListener {
            if (isAudioMute) {
                isAudioMute = false
                rtcClient?.getAudioTrack()?.setEnabled(true)
                imgBtnMute?.setBackgroundResource(R.drawable.button_circle)
                imgBtnMute?.setImageDrawable(
                    resources.getDrawable(
                        R.drawable.ic_mic_on,
                        applicationContext.theme
                    )
                )
            } else {
                isAudioMute = true
                rtcClient?.getAudioTrack()?.setEnabled(false)
                imgBtnMute?.setBackgroundResource(R.drawable.button_circle)
                imgBtnMute?.setImageDrawable(
                    resources.getDrawable(
                        R.drawable.ic_mic_off,
                        applicationContext.theme
                    )
                )
            }
        }

        imgBtnPause?.setOnClickListener {
            if (isVideoMute) {
                isVideoMute = false
                videoTrackFromCamera?.setEnabled(true)
                imgBtnPause?.setBackgroundResource(R.drawable.button_circle)
                imgBtnPause?.setImageDrawable(
                    resources.getDrawable(
                        R.drawable.ic_video,
                        applicationContext.theme
                    )
                )
            } else {
                isVideoMute = true
                videoTrackFromCamera?.setEnabled(false)
                imgBtnPause?.setBackgroundResource(R.drawable.button_circle)
                imgBtnPause?.setImageDrawable(
                    resources.getDrawable(
                        R.drawable.ic_video_off,
                        applicationContext.theme
                    )
                )
            }
        }

        imgBtnSwitchCamera?.setOnClickListener {
            (videoCapturer as CameraVideoCapturer).switchCamera(null)
        }

        imgBtnEndCall?.setOnClickListener {
            sendMessage("bye")
            socket!!.disconnect()
            peerConnection?.close()
            finish()
        }
    }

    private fun initView() {
        mRoomCode = intent?.getStringExtra("room_code")

        imgBtnMute = findViewById(R.id.button_mic)
        imgBtnEndCall = findViewById(R.id.button_call)
        imgBtnPause = findViewById(R.id.button_video)
        imgBtnSwitchCamera = findViewById(R.id.button_switch_camera)
    }

    private fun connectToSignallingServer() = try {
        runOnUiThread {
            //AWS URL
            socket = IO.socket("https://signal.doctoronline.io/")
            /*//URL for step-05
            socket = IO.socket("https://cryptic-atoll-46983.herokuapp.com/")*/
            /*//URL for step-06
            socket = IO.socket("https://guarded-springs-49203.herokuapp.com/")*/
            socket?.on(EVENT_CONNECT) { args: Array<Any?>? ->
                Log.d(TAG, "connectToSignallingServer: connect")
                socket?.emit("create or join", mRoomCode)
            }?.on("ipaddr") { args: Array<Any?>? ->
                Log.d(TAG, "connectToSignallingServer: ipaddr")
            }?.on("created") { args: Array<Any?>? ->
                Log.d(TAG, "connectToSignallingServer: created")
                isInitiator = true
            }?.on("full") { args: Array<Any?>? ->
                Log.d(TAG, "connectToSignallingServer: full")
            }?.on("join") { args: Array<Any?>? ->
                Log.d(TAG, "connectToSignallingServer: join")
                Log.d(TAG, "connectToSignallingServer: Another peer made a request to join room")
                Log.d(TAG, "connectToSignallingServer: This peer is the initiator of room")
                isChannelReady = true
            }?.on("joined") { args: Array<Any?>? ->
                Log.d(TAG, "connectToSignallingServer: joined")
                isChannelReady = true
            }?.on("log") { args: Array<Any> ->
                for (arg in args) {
                    Log.d(TAG, "connectToSignallingServer: $arg")
                }
            }?.on("message") { args: Array<Any?>? ->
                Log.d(TAG, "connectToSignallingServer: got a message")
            }?.on("message") { args: Array<Any> ->
                try {
                    if (args[0] is String) {
                        val message = args[0] as String
                        if (message == "got user media") {
                            maybeStart()
                        } else if (message == "bye" && isStarted) {
                            Log.d(TAG, "connectToSignallingServer: got a message")
                            socket!!.disconnect()
                            peerConnection?.close()
                            finish()
                        }
                    } else {
                        val message = args[0] as JSONObject
                        Log.d(TAG, "connectToSignallingServer: got message $message")
                        if (message.getString("type") == "offer") {
                            Log.d(
                                TAG,
                                "connectToSignallingServer: received an offer $isInitiator $isStarted"
                            )
                            if (!isInitiator && !isStarted) {
                                maybeStart()
                            }
                            peerConnection!!.setRemoteDescription(
                                SimpleSdpObserver(), SessionDescription(
                                    SessionDescription.Type.OFFER, message.getString(
                                        "sdp"
                                    )
                                )
                            )
                            doAnswer()
                        } else if (message.getString("type") == "answer" && isStarted) {
                            peerConnection!!.setRemoteDescription(
                                SimpleSdpObserver(), SessionDescription(
                                    SessionDescription.Type.ANSWER, message.getString(
                                        "sdp"
                                    )
                                )
                            )
                        } else if (message.getString("type") == "candidate" && isStarted) {
                            Log.d(TAG, "connectToSignallingServer: receiving candidates")
                            val candidate = IceCandidate(
                                message.getString("id"), message.getInt(
                                    "label"
                                ), message.getString("candidate")
                            )
                            peerConnection!!.addIceCandidate(candidate)
                        }
                    }
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            }?.on(EVENT_DISCONNECT) { args: Array<Any?>? ->
                Log.d(TAG, "connectToSignallingServer: disconnect")
                socket!!.disconnect()
                peerConnection?.close()
                finish()
            }
            socket?.connect()
        }
    } catch (e: URISyntaxException) {
        e.printStackTrace()
    }

    private fun doAnswer() {
        peerConnection!!.createAnswer(object : SimpleSdpObserver() {
            override fun onCreateSuccess(sessionDescription: SessionDescription) {
                peerConnection!!.setLocalDescription(SimpleSdpObserver(), sessionDescription)
                val message = JSONObject()
                try {
                    message.put("type", "answer")
                    message.put("sdp", sessionDescription.description)
                    sendMessage(message)
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            }
        }, MediaConstraints())
    }

    private fun maybeStart() {
        Log.d(TAG, "maybeStart: $isStarted $isChannelReady")
        if (!isStarted && isChannelReady) {
            isStarted = true
            if (isInitiator) {
                doCall()
            }
        }
    }

    private fun doCall() {
        val sdpMediaConstraints = MediaConstraints()
        peerConnection!!.createOffer(object : SimpleSdpObserver() {
            override fun onCreateSuccess(sessionDescription: SessionDescription) {
                Log.d(TAG, "onCreateSuccess: ")
                peerConnection!!.setLocalDescription(SimpleSdpObserver(), sessionDescription)
                val message = JSONObject()
                try {
                    message.put("type", "offer")
                    message.put("sdp", sessionDescription.description)
                    sendMessage(message)
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            }
        }, sdpMediaConstraints)
    }

    private fun sendMessage(message: Any) {
        socket!!.emit("message", message)
    }

    private fun initializeSurfaceViews() {
        rootEglBase = EglBase.create()
        binding?.surfaceView?.init(rootEglBase?.eglBaseContext, null)
        binding?.surfaceView?.setEnableHardwareScaler(true)
        binding?.surfaceView?.setMirror(false)
        binding?.surfaceView?.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT)
        binding?.surfaceView?.setZOrderOnTop(true)

        binding?.surfaceView2?.init(rootEglBase?.eglBaseContext, null)
        binding?.surfaceView2?.setEnableHardwareScaler(true)
        binding?.surfaceView2?.setMirror(false)
    }

    private fun initializePeerConnectionFactory() {
        val initializationOptions =
            PeerConnectionFactory.InitializationOptions.builder(this@CompleteActivity)
                .createInitializationOptions()
        PeerConnectionFactory.initialize(initializationOptions)
        val options = PeerConnectionFactory.Options()
        val defaultVideoEncoderFactory = DefaultVideoEncoderFactory(
            rootEglBase?.eglBaseContext, true, true
        )
        val defaultVideoDecoderFactory = DefaultVideoDecoderFactory(rootEglBase?.eglBaseContext)
        val audioDeviceModule: AudioDeviceModule = JavaAudioDeviceModule.builder(this)
            .setUseHardwareAcousticEchoCanceler(false)
            .setUseHardwareNoiseSuppressor(false)
            .createAudioDeviceModule()
        factory = PeerConnectionFactory.builder()
            .setOptions(options)
            .setAudioDeviceModule(audioDeviceModule)
            .setVideoEncoderFactory(defaultVideoEncoderFactory)
            .setVideoDecoderFactory(defaultVideoDecoderFactory)
            .createPeerConnectionFactory()

        WebRtcAudioUtils.setWebRtcBasedNoiseSuppressor(true)
        WebRtcAudioUtils.setWebRtcBasedAcousticEchoCanceler(true)
        WebRtcAudioUtils.setWebRtcBasedAutomaticGainControl(true)
    }

    private fun createVideoTrackFromCameraAndShowIt() {
        videoCapturer = createVideoCapturer()
        //Create a VideoSource instance
        var videoSource: VideoSource? = null
        if (videoCapturer != null) {
            val surfaceTextureHelper =
                SurfaceTextureHelper.create("CaptureThread", rootEglBase!!.eglBaseContext)
            videoSource = factory!!.createVideoSource(videoCapturer?.isScreencast!!)
            videoCapturer?.initialize(surfaceTextureHelper, this, videoSource.capturerObserver)
        }
        videoTrackFromCamera = factory!!.createVideoTrack("ARDAMSv0", videoSource)

        videoCapturer?.startCapture(
            LOW_VIDEO_RESOLUTION_WIDTH,
            LOW_VIDEO_RESOLUTION_HEIGHT,
            LOW_FPS
        )
        videoTrackFromCamera?.addSink(binding?.surfaceView)
        videoTrackFromCamera?.setEnabled(true)
    }

    private fun initializePeerConnections() {
        peerConnection = createPeerConnection(factory)
    }

    private fun startStreamingVideo() {
        peerConnection = createPeerConnection(factory!!)
        rtcClient = RTCClient(this.application!!, factory!!)
        var mediaStream = Collections.singletonList("ARDAMS")
        Log.d("getRemoteStream", "local: " + rtcClient?.getVideoTrack()?.id())
        peerConnection?.addTrack(videoTrackFromCamera, mediaStream)
        peerConnection?.addTrack(rtcClient?.getAudioTrack(), mediaStream)
        sendMessage("got user media")
    }

    private fun createPeerConnection(factory: PeerConnectionFactory?): PeerConnection? {
        val iceServers = ArrayList<IceServer>()
        val stun = IceServer.builder("stun:65.0.104.5:3478").createIceServer()
        val turn =
            IceServer.builder("turn:65.0.104.5:3478").setUsername("aimdek").setPassword("aimdek123")
                .createIceServer()
        iceServers.add(stun)
        iceServers.add(turn)
        val rtcConfig = RTCConfiguration(iceServers)
        val pcConstraints = MediaConstraints()
        val pcObserver: PeerConnection.Observer = object : PeerConnection.Observer {
            override fun onSignalingChange(signalingState: SignalingState) {
                Log.d(TAG, "onSignalingChange: ")
            }

            override fun onIceConnectionChange(iceConnectionState: IceConnectionState) {
                Log.d(TAG, "onIceConnectionChange: ")
            }

            override fun onIceConnectionReceivingChange(b: Boolean) {
                Log.d(TAG, "onIceConnectionReceivingChange: ")
            }

            override fun onIceGatheringChange(iceGatheringState: IceGatheringState) {
                Log.d(TAG, "onIceGatheringChange: ")
            }

            override fun onIceCandidate(iceCandidate: IceCandidate) {
                Log.d(TAG, "onIceCandidate: ")
                val message = JSONObject()
                try {
                    message.put("type", "candidate")
                    message.put("label", iceCandidate.sdpMLineIndex)
                    message.put("id", iceCandidate.sdpMid)
                    message.put("candidate", iceCandidate.sdp)
                    Log.d(TAG, "onIceCandidate: sending candidate $message")
                    sendMessage(message)
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            }

            override fun onIceCandidatesRemoved(iceCandidates: Array<IceCandidate>) {
                Log.d(TAG, "onIceCandidatesRemoved: ")
            }

            override fun onAddStream(mediaStream: MediaStream) {
                Log.d(TAG, "onAddStream: " + mediaStream.videoTracks.size)
                val remoteVideoTrack = mediaStream.videoTracks.first()
                val remoteAudioTrack = mediaStream.audioTracks.first()
                remoteVideoTrack.setEnabled(true)
                remoteVideoTrack.addSink(binding?.surfaceView2)
            }

            override fun onRemoveStream(mediaStream: MediaStream) {
                Log.d(TAG, "onRemoveStream: ")
            }

            override fun onDataChannel(dataChannel: DataChannel) {
                Log.d(TAG, "onDataChannel: ")
            }

            override fun onRenegotiationNeeded() {
                Log.d(TAG, "onRenegotiationNeeded: ")
            }

            override fun onAddTrack(p0: RtpReceiver?, p1: Array<out MediaStream>?) {

            }
        }
        return factory!!.createPeerConnection(rtcConfig, pcObserver)
    }

    private fun createVideoCapturer(): VideoCapturer? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getVideoCapturer(this)
        } else {
            createCameraCapturer(Camera1Enumerator(true))
        }
    }

    private fun getVideoCapturer(context: Context) =
        Camera1Enumerator(true).run {
            deviceNames.find {
                isFrontFacing(it)
            }?.let {
                createCapturer(it, null)
            } ?: throw IllegalStateException()
        }

    private fun createCameraCapturer(enumerator: CameraEnumerator): VideoCapturer? {
        val deviceNames = enumerator.deviceNames
        for (deviceName in deviceNames) {
            if (enumerator.isFrontFacing(deviceName)) {
                val videoCapturer: VideoCapturer? = enumerator.createCapturer(deviceName, null)
                if (videoCapturer != null) {
                    return videoCapturer
                }
            }
        }
        for (deviceName in deviceNames) {
            if (!enumerator.isFrontFacing(deviceName)) {
                val videoCapturer: VideoCapturer? = enumerator.createCapturer(deviceName, null)
                if (videoCapturer != null) {
                    return videoCapturer
                }
            }
        }
        return null
    }

    companion object {
        private const val TAG = "CompleteActivity"
    }

    override fun onFirstFrameRendered() {
        Log.d("SVRCallBack", "onFirstFrameRendered")
    }

    override fun onFrameResolutionChanged(p0: Int, p1: Int, p2: Int) {
        Log.d("SVRCallBack", "onFrameResolutionChanged")
    }
}
