package com.sungjkim.react;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.PixelFormat;
import android.util.Log;
import android.view.SurfaceHolder;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.events.RCTEventEmitter;
import com.visualon.OSMPPlayer.VOCommonPlayer;
import com.visualon.OSMPPlayer.VOCommonPlayerListener;
import com.visualon.OSMPPlayer.VOOSMPInitParam;
import com.visualon.OSMPPlayer.VOOSMPOpenParam;
import com.visualon.OSMPPlayer.VOOSMPType;
import com.visualon.OSMPPlayer.VOOSMPType.*;
import com.visualon.OSMPPlayerImpl.VOCommonPlayerImpl;
import com.visualon.OSMPUtils.voSurfaceView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

@SuppressLint("ViewConstructor")
public class ReactVideoView extends voSurfaceView implements SurfaceHolder.Callback, LifecycleEventListener {

    @Override
    public void onHostResume() {

    }

    @Override
    public void onHostPause() {

    }

    @Override
    public void onHostDestroy() {  }

    public enum Events {
        EVENT_LOAD_START("onVideoLoadStart"),
        EVENT_LOAD("onVideoLoad"),
        EVENT_ERROR("onVideoError"),
        EVENT_PROGRESS("onVideoProgress"),
        EVENT_SEEK("onVideoSeek"),
        EVENT_END("onVideoEnd"),
        EVENT_STALLED("onPlaybackStalled"),
        EVENT_RESUME("onPlaybackResume"),
        EVENT_READY_FOR_DISPLAY("onReadyForDisplay");

        private final String mName;

        Events(final String name) {
            mName = name;
        }

        @Override
        public String toString() {
            return mName;
        }
    }

    private static final String TAG = "SkitterPlayer"; // Tag for VOLog messages

    private SurfaceHolder m_surfaceHolder = null;
    private voSurfaceView m_svMain; // Drawing surface (must be passed to SDK)
    private VOCommonPlayer m_sdkPlayer = null; // SDK player
    private String m_strVideoPath = ""; // URL or file path to the media source

    private int m_nVideoWidth = 0; // Video width
    private int m_nVideoHeight = 0; // video height

    private Runnable mProgressUpdateRunnable = null;
    private ThemedReactContext mThemedReactContext;
    private RCTEventEmitter mEventEmitter;

    private boolean mPaused = false;
    private boolean mSubtitle = false;

    public ReactVideoView(ThemedReactContext context) {

        super(context);

        mThemedReactContext = context;
        mEventEmitter = context.getJSModule(RCTEventEmitter.class);

        m_svMain = this;
        m_svMain.getHolder().addCallback(this);
        m_svMain.getHolder().setFormat(PixelFormat.RGBA_8888);

        copyfile(this.getContext(), "cap.xml", "cap.xml");

        // mProgressUpdateRunnable = new Runnable() {
        //     @Override
        //     public void run() {

        //         if (mMediaPlayerValid && !isCompleted &&!mPaused) {
        //             WritableMap event = Arguments.createMap();
        //             event.putDouble(EVENT_PROP_CURRENT_TIME, mMediaPlayer.getCurrentPosition() / 1000.0);
        //             event.putDouble(EVENT_PROP_PLAYABLE_DURATION, mVideoBufferedDuration / 1000.0); //TODO:mBufferUpdateRunnable
        //             mEventEmitter.receiveEvent(getId(), Events.EVENT_PROGRESS.toString(), event);

        //             // Check for update after an interval
        //             mProgressUpdateHandler.postDelayed(mProgressUpdateRunnable, Math.round(mProgressUpdateInterval));
        //         }
        //     }
        // };
    }

    // Notify SDK on Surface Change
    public void surfaceChanged(SurfaceHolder surfaceholder, int format, int w, int h) {
        Log.i(TAG, "Surface Changed");
        if (m_sdkPlayer != null)
            m_sdkPlayer.setSurfaceChangeFinished();
    }

    // Notify SDK on Surface Creation
    // Used to initialize the SDK and start playback
    public void surfaceCreated(SurfaceHolder surfaceholder) {
        m_surfaceHolder = surfaceholder;
        initializePlayerIfNeeded();
    }

    // Notify SDK on Surface Destroyed
    public void surfaceDestroyed(SurfaceHolder surfaceholder) {
        Log.i(TAG, "Surface Destroyed");

        if (m_sdkPlayer != null) {
            m_sdkPlayer.stop();
            m_sdkPlayer.close();
            m_sdkPlayer.destroy();
            m_sdkPlayer = null;
            Log.v(TAG, "MediaPlayer is released.");
        }
    }

    // Display error messages and stop player
    public boolean onError(VOCommonPlayer mp, int what, int extra) {
        Log.v(TAG, "Error message, what is " + what + " extra is " + extra);
        return true;
    }

    private VOCommonPlayerListener m_listenerEvent = new VOCommonPlayerListener() {
        /* SDK event handling */
        @SuppressWarnings("incomplete-switch")
        public VO_OSMP_RETURN_CODE onVOEvent(VO_OSMP_CB_EVENT_ID nID, int nParam1, int nParam2, Object obj) {
            switch (nID) {
            case VO_OSMP_CB_ERROR:
            case VO_OSMP_SRC_CB_CONNECTION_FAIL:
            case VO_OSMP_SRC_CB_DOWNLOAD_FAIL:
            case VO_OSMP_SRC_CB_DRM_FAIL:
            case VO_OSMP_SRC_CB_PLAYLIST_PARSE_ERR:
            case VO_OSMP_SRC_CB_CONNECTION_REJECTED:
            case VO_OSMP_SRC_CB_DRM_NOT_SECURE:
            case VO_OSMP_SRC_CB_DRM_AV_OUT_FAIL:
            case VO_OSMP_CB_LICENSE_FAIL: { // Error
                // Display error dialog and stop player
                onError(m_sdkPlayer, nID.getValue(), 0);
                break;
            }
            case VO_OSMP_SRC_CB_OPEN_FINISHED: {

                if (nParam1 == VOOSMPType.VO_OSMP_RETURN_CODE.VO_OSMP_ERR_NONE.getValue()) {

                    VOOSMPType.VO_OSMP_RETURN_CODE nRet;

                    // Start (play) media pipeline
                    nRet = m_sdkPlayer.start();

                    if (nRet == VOOSMPType.VO_OSMP_RETURN_CODE.VO_OSMP_ERR_NONE) {
                    } else {
                        onError(m_sdkPlayer, nRet.getValue(), 0);
                    }

                } else {
                    onError(m_sdkPlayer, nParam1, 0);
                }

                break;
            }
            case VO_OSMP_CB_PLAY_COMPLETE: {
                if (m_sdkPlayer != null) {
                    m_sdkPlayer.stop();
                    m_sdkPlayer.close();
                    m_sdkPlayer.destroy();
                    m_sdkPlayer = null;
                }
                break;
            }
            case VO_OSMP_CB_VIDEO_SIZE_CHANGED: { // Video size changed
                m_nVideoWidth = nParam1;
                m_nVideoHeight = nParam2;
                break;
            }
            }
            return VOOSMPType.VO_OSMP_RETURN_CODE.VO_OSMP_ERR_NONE;
        }

        // @Override
        public VO_OSMP_RETURN_CODE onVOSyncEvent(VO_OSMP_CB_SYNC_EVENT_ID arg0, int arg1, int arg2,
                Object arg3) {
            // TODO Auto-generated method stub
            return VO_OSMP_RETURN_CODE.VO_OSMP_ERR_NONE;
        }
    };

    private void initializePlayerIfNeeded() {
        if (m_sdkPlayer == null && m_surfaceHolder != null) {

            if ((m_strVideoPath == null) || (m_strVideoPath.trim().length() <= 0)) {
                return;
            }

            if (m_sdkPlayer != null)
                return;

            // Initialize the SDK
            VO_OSMP_RETURN_CODE nRet;

            m_sdkPlayer = new VOCommonPlayerImpl();

            // Retrieve location of libraries
            String apkPath = getUserNativeLibPath(this.getContext());
            String cfgPath = getUserPath(this.getContext()) + "/";

            // SDK player engine type
            VO_OSMP_PLAYER_ENGINE eEngineType = VO_OSMP_PLAYER_ENGINE.VO_OSMP_VOME2_PLAYER;

            VOOSMPInitParam init = new VOOSMPInitParam();
            init.setContext(this.getContext());
            init.setLibraryPath(apkPath);

            // Initialize SDK player
            nRet = m_sdkPlayer.init(eEngineType, init);

            // Set view
            m_sdkPlayer.setView(m_svMain);

            // Register SDK event listener
            m_sdkPlayer.setOnEventListener(m_listenerEvent);

            if (nRet == VO_OSMP_RETURN_CODE.VO_OSMP_ERR_NONE) {
                Log.v(TAG, "MediaPlayer is created.");
            } else {
                onError(m_sdkPlayer, nRet.getValue(), 0);
                return;
            }

            // Set device capability file location
            String capFile = cfgPath + "cap.xml";
            m_sdkPlayer.setDeviceCapabilityByFile(capFile);

            // Read license file location
            m_sdkPlayer.setLicenseContent(readAsset(this.getContext(), "voVidDec.dat"));

            // Start playing the video
            // First open the media source
            // Auto-detect source format
            VO_OSMP_SRC_FORMAT format = VO_OSMP_SRC_FORMAT.VO_OSMP_SRC_AUTO_DETECT;

            // Set source flag to Asynchronous Open - Using Async Open
            VO_OSMP_SRC_FLAG eSourceFlag;
            eSourceFlag = VO_OSMP_SRC_FLAG.VO_OSMP_FLAG_SRC_OPEN_ASYNC;

            VOOSMPOpenParam openParam = new VOOSMPOpenParam();
            openParam.setDecoderType(VO_OSMP_DECODER_TYPE.VO_OSMP_DEC_VIDEO_SW.getValue()
                    | VO_OSMP_DECODER_TYPE.VO_OSMP_DEC_AUDIO_SW.getValue());

            // Open media source
            nRet = m_sdkPlayer.open(m_strVideoPath, eSourceFlag, format, openParam);

            m_sdkPlayer.enableSubtitle(mSubtitle);


            if (nRet == VO_OSMP_RETURN_CODE.VO_OSMP_ERR_NONE) {
                Log.v(TAG, "MediaPlayer is Opened (Async Open).");
            } else {
                onError(m_sdkPlayer, nRet.getValue(), 0);
                return;
            }
        }
    }

    public void setSrc(final String uriString) {
        if(m_sdkPlayer != null) {
            m_sdkPlayer.stop();
            m_sdkPlayer.close();

            VO_OSMP_SRC_FORMAT format = VO_OSMP_SRC_FORMAT.VO_OSMP_SRC_AUTO_DETECT;
            VO_OSMP_SRC_FLAG eSourceFlag;
            eSourceFlag = VO_OSMP_SRC_FLAG.VO_OSMP_FLAG_SRC_OPEN_ASYNC;

            VOOSMPOpenParam openParam = new VOOSMPOpenParam();
            openParam.setDecoderType(VO_OSMP_DECODER_TYPE.VO_OSMP_DEC_VIDEO_SW.getValue()
                    | VO_OSMP_DECODER_TYPE.VO_OSMP_DEC_AUDIO_SW.getValue());

            // Open media source
            VO_OSMP_RETURN_CODE nRet = m_sdkPlayer.open(uriString, eSourceFlag, format, openParam);

            if (nRet == VO_OSMP_RETURN_CODE.VO_OSMP_ERR_NONE) {
                Log.v(TAG, "MediaPlayer is Opened (Async Open).");
            } else {
                onError(m_sdkPlayer, nRet.getValue(), 0);
                return;
            }
        } else {
            m_strVideoPath = uriString;
            initializePlayerIfNeeded();
        }
    }

    public void setPaused(final boolean paused) {
        if(m_sdkPlayer != null) {
            if(!mPaused) {
                m_sdkPlayer.pause();
                mPaused = true;
            } else {
                m_sdkPlayer.start();
                mPaused = false;
            }
        }
    }

    public void setSubtitle(final boolean subtitle) {
        mSubtitle = subtitle;
        if(m_sdkPlayer != null) {
            m_sdkPlayer.enableSubtitle(subtitle);
        }
    }

    public void seekTo(final float seek) {
        if(m_sdkPlayer != null) {
            int m_Duration = (int) m_sdkPlayer.getDuration();
            int m_Position = (int) m_sdkPlayer.getPosition();
            long newPosition = 0;
            if(m_Duration > 0) {
                newPosition = m_Position + (int) seek;
                if(newPosition < 0) {
                    newPosition = 0;
                }
            }
            m_sdkPlayer.setPosition(newPosition);
        }
    }

    // Get user path
    public static String getUserPath(Context context) {
        String path = context.getPackageName();
        String userPath = "/data/data/" + path;

        try {
            PackageInfo p = context.getPackageManager().getPackageInfo(path, 0);
            userPath = p.applicationInfo.dataDir;
        } catch (PackageManager.NameNotFoundException e) {
        }

        return userPath;
    }

    // Get user lib path
    private static String getUserNativeLibPath(Context context) {

        String path = context.getPackageName();
        String userPath = "/data/data/" + path + "/lib/";

        try {
            PackageInfo p = context.getPackageManager().getPackageInfo(path, 0);
            userPath = p.applicationInfo.dataDir + "/lib/";
        } catch (PackageManager.NameNotFoundException e) {
        }

        File libFile = new File(userPath);
        if (!libFile.exists() || null == libFile.listFiles()) {
            try {
                PackageInfo p = context.getPackageManager().getPackageInfo(path, 0);
                userPath = p.applicationInfo.nativeLibraryDir + "/";
            } catch (PackageManager.NameNotFoundException e) {
            }
        }

        return userPath;
    }

    // Copy file from asset directory to destination
    private static void copyfile(Context context, String filename, String desName) {
        try {
            InputStream InputStreamis = context.getAssets().open(filename);
            File desFile = new File(getUserPath(context) + "/" + desName);
            desFile.createNewFile();
            FileOutputStream fos = new FileOutputStream(desFile);
            int bytesRead;
            byte[] buf = new byte[4 * 1024]; // 4K buffer
            while ((bytesRead = InputStreamis.read(buf)) != -1) {
                fos.write(buf, 0, bytesRead);
            }
            fos.flush();
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Read asset to memory
    private static byte[] readAsset(Context context, final String assetName) {
        try {
            InputStream is = context.getAssets().open(assetName);
            try {
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                final int CAPACITY = 1024 * 16;
                byte[] buffer = new byte[CAPACITY];
                int len = -1;
                while ((len = is.read(buffer, 0, CAPACITY)) != -1) {
                    byteArrayOutputStream.write(buffer, 0, len);
                }
                byte[] content = byteArrayOutputStream.toByteArray();
                byteArrayOutputStream.flush();
                byteArrayOutputStream.close();
                return content;
            } finally {
                is.close();
            }
        } catch (Exception e) {
            throw new RuntimeException("Could not load asset: " + assetName);
        }
    }

}
