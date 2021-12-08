package com.example.mymeetdemo

import android.app.Application
import android.content.Context
import android.os.Build
import org.webrtc.*

class RTCClient(
    context: Application,
    private var peerConnectionFactory: PeerConnectionFactory
) {

    companion object {
        private const val LOCAL_TRACK_ID = "ARDAMSv0"
        private const val LOCAL_AUDIO_TRACK_ID = "ARDAMSa0"
    }

    private val rootEglBase: EglBase = EglBase.create()
    private var localVideoTrack: VideoTrack? = null
    private var localAudioTrack: AudioTrack? = null
    private var audioConstraints: MediaConstraints? = null

    init {
        initPeerConnectionFactory(context)
        audioConstraints = MediaConstraints()
        /*audioConstraints?.mandatory?.add(MediaConstraints.KeyValuePair("googEchoCancellation","false"))
        audioConstraints?.mandatory?.add(MediaConstraints.KeyValuePair("googAutoGainControl","false"))
        audioConstraints?.mandatory?.add(MediaConstraints.KeyValuePair("googHighpassFilter","false"))
        audioConstraints?.mandatory?.add(MediaConstraints.KeyValuePair("googNoiseSuppression","false"))
        audioConstraints?.mandatory?.add(MediaConstraints.KeyValuePair("levelControl","true"))*/
    }

    //private val peerConnectionFactory by lazy { buildPeerConnectionFactory() }
    private val videoCapturer by lazy {
        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP){
            getVideoCapturer(context)
        }else{
            getAlternateVideoCapturer(Camera1Enumerator(true))
        }
    }

    private fun getAlternateVideoCapturer(enumerator: Camera1Enumerator): CameraVideoCapturer? {

        val deviceNames = enumerator.deviceNames
        for (deviceName in deviceNames) {
            if (enumerator.isFrontFacing(deviceName)) {
                val videoCapturer = enumerator.createCapturer(deviceName, null)
                if (videoCapturer != null) {
                    return videoCapturer
                }
            }
        }

        for (deviceName in deviceNames) {
            if (!enumerator.isFrontFacing(deviceName)) {
                val videoCapturer = enumerator.createCapturer(deviceName, null)
                if (videoCapturer != null) {
                    return videoCapturer
                }
            }
        }
        return null
    }

    private val localVideoSource by lazy { peerConnectionFactory.createVideoSource(false) }

    private fun initPeerConnectionFactory(context: Application) {
        val options = PeerConnectionFactory.InitializationOptions.builder(context)
            .setEnableInternalTracer(true)
            .setFieldTrials("WebRTC-H264HighProfile/Enabled/")
            .createInitializationOptions()
        PeerConnectionFactory.initialize(options)
    }

    private fun buildPeerConnectionFactory(): PeerConnectionFactory {
        return PeerConnectionFactory
            .builder()
            .setVideoDecoderFactory(DefaultVideoDecoderFactory(rootEglBase.eglBaseContext))
            .setVideoEncoderFactory(DefaultVideoEncoderFactory(rootEglBase.eglBaseContext, true, true))
            .setOptions(PeerConnectionFactory.Options().apply {
                disableEncryption = true
                disableNetworkMonitor = true
            })
            .createPeerConnectionFactory()
    }

    private fun getVideoCapturer(context: Context) =
        Camera1Enumerator(true).run {
            deviceNames.find {
                isFrontFacing(it)
            }?.let {
                createCapturer(it, null)
            } ?: throw IllegalStateException()
        }

    private fun initSurfaceView(view: SurfaceViewRenderer) = view.run {
        setMirror(true)
        setEnableHardwareScaler(true)
        init(rootEglBase.eglBaseContext, null)
    }

    fun startLocalVideoCapture(localVideoOutput: SurfaceViewRenderer) {
        initSurfaceView(localVideoOutput)
        val surfaceTextureHelper = SurfaceTextureHelper.create(Thread.currentThread().name, rootEglBase.eglBaseContext)
        (videoCapturer as VideoCapturer).initialize(surfaceTextureHelper, localVideoOutput.context, localVideoSource.capturerObserver)
        videoCapturer?.startCapture(1280, 720, 30)
//        localVideoTrack = peerConnectionFactory.createVideoTrack(LOCAL_TRACK_ID, localVideoSource)
        localVideoTrack?.addSink(localVideoOutput)
        localVideoTrack?.setEnabled(true)
    }

    fun stopVideoCapture(){
        videoCapturer?.stopCapture()
    }

    fun getVideoTrack():VideoTrack{
        if(localVideoTrack == null){
            localVideoTrack = peerConnectionFactory.createVideoTrack(LOCAL_TRACK_ID, localVideoSource)
        }
        return localVideoTrack!!
    }

    fun getAudioTrack():AudioTrack{
        if(localAudioTrack == null) {
            localAudioTrack = peerConnectionFactory.createAudioTrack(LOCAL_AUDIO_TRACK_ID, peerConnectionFactory.createAudioSource(audioConstraints))
            localAudioTrack?.setEnabled(true)
        }
        return localAudioTrack!!
    }

    fun switchCamera(){
        videoCapturer?.switchCamera(null)
    }
}