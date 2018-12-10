package com.sungjkim.react;

import com.sungjkim.react.ReactVideoView.Events;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.common.MapBuilder;
import com.facebook.react.uimanager.annotations.ReactProp;
import com.facebook.react.uimanager.SimpleViewManager;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.events.RCTEventEmitter;
import com.yqritc.scalablevideoview.ScalableType;

import javax.annotation.Nullable;
import java.util.Map;

public class ReactVideoViewManager extends SimpleViewManager<ReactVideoView> {

    public static final String REACT_CLASS = "RCTVideo";

    public static final String PROP_SRC = "src";
    public static final String PROP_SRC_URI = "uri";
    // public static final String PROP_RESIZE_MODE = "resizeMode";
    public static final String PROP_PAUSED = "paused";
     public static final String PROP_SUBTITLE = "subtitle";
    // public static final String PROP_MUTED = "muted";
     public static final String PROP_SEEK = "seek";
    // public static final String PROP_VOLUME = "volume";

    @Override
    public String getName() {
        return REACT_CLASS;
    }

    @Override
    protected ReactVideoView createViewInstance(ThemedReactContext themedReactContext) {
        return new ReactVideoView(themedReactContext);
    }

    @Override
    public void onDropViewInstance(ReactVideoView view) {
        super.onDropViewInstance(view);
    }

    @Override
    @Nullable
    public Map getExportedCustomDirectEventTypeConstants() {
        MapBuilder.Builder builder = MapBuilder.builder();
         for (Events event : Events.values()) {
         builder.put(event.toString(), MapBuilder.of("registrationName",
         event.toString()));
         }
        return builder.build();
    }

    @Override
    @Nullable
    public Map getExportedViewConstants() {
        return MapBuilder.of("ScaleNone", Integer.toString(ScalableType.LEFT_TOP.ordinal()), "ScaleToFill",
                Integer.toString(ScalableType.FIT_XY.ordinal()), "ScaleAspectFit",
                Integer.toString(ScalableType.FIT_CENTER.ordinal()), "ScaleAspectFill",
                Integer.toString(ScalableType.CENTER_CROP.ordinal()));
    }

    @ReactProp(name = PROP_SRC)
    public void setSrc(final ReactVideoView videoView, @Nullable ReadableMap src) {
        String sourceUri = src.getString(PROP_SRC_URI);
        videoView.setSrc(sourceUri);
    }

    @ReactProp(name = PROP_PAUSED, defaultBoolean = false)
    public void setPaused(final ReactVideoView videoView, final boolean paused) {
        videoView.setPaused(paused);
    }

     @ReactProp(name = PROP_SUBTITLE, defaultBoolean = false)
     public void setSubtitle(final ReactVideoView videoView, final boolean subtitle) {
         videoView.setSubtitle(subtitle);
     }

    // @ReactProp(name = PROP_VOLUME, defaultFloat = 1.0f)
    // public void setVolume(final ReactVideoView videoView, final float volume) {
    //     videoView.setVolume(volume);
    // }

     @ReactProp(name = PROP_SEEK)
     public void setSeek(final ReactVideoView videoView, final float seek) {
         videoView.seekTo(Math.round(seek * 1000.0f));
     }

}
