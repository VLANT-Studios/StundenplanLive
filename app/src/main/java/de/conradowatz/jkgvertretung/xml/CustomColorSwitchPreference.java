package de.conradowatz.jkgvertretung.xml;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.BlendMode;
import android.graphics.BlendModeColorFilter;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.preference.SwitchPreference;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Switch;

import androidx.appcompat.widget.SwitchCompat;

import de.conradowatz.jkgvertretung.tools.ColorAPI;

@SuppressWarnings("unused")
@SuppressLint("UseSwitchCompatOrMaterialCode")
public class CustomColorSwitchPreference extends SwitchPreference {
    public CustomColorSwitchPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public CustomColorSwitchPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CustomColorSwitchPreference(Context context) {
        super(context);
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);
        Switch switch_ = findSwitchInChildviews((ViewGroup) view);
        if (switch_ != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && switch_.isChecked()) {
            ColorAPI api = new ColorAPI(getContext());
            Drawable thumb = switch_.getThumbDrawable();
            thumb.setTint(api.getAccentColor());
            switch_.setThumbDrawable(thumb);
            Drawable track = switch_.getTrackDrawable();
            track.setTint(api.getAccentColor());
            switch_.setTrackDrawable(track);
        } else if (switch_ != null && !switch_.isChecked() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Drawable thumb = switch_.getThumbDrawable();
            thumb.setTintList(null);
            switch_.setThumbDrawable(thumb);
            Drawable track = switch_.getTrackDrawable();
            track.setTintList(null);
            switch_.setTrackDrawable(track);
        }
    }

    private Switch findSwitchInChildviews(ViewGroup view) {
        for (int i=0;i<view.getChildCount();i++) {
            View thisChildview = view.getChildAt(i);
            if (thisChildview instanceof Switch) {
                return (Switch)thisChildview;
            }
            else if (thisChildview instanceof  ViewGroup) {
                Switch theSwitch = findSwitchInChildviews((ViewGroup) thisChildview);
                if (theSwitch!=null) return theSwitch;
            }
        }
        return null;
    }
}
