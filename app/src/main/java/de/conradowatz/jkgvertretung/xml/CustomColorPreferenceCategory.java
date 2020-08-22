package de.conradowatz.jkgvertretung.xml;

import android.content.Context;
import android.preference.PreferenceCategory;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

import de.conradowatz.jkgvertretung.tools.ColorAPI;

public class CustomColorPreferenceCategory extends PreferenceCategory {
    public CustomColorPreferenceCategory(Context context) {
        super(context);
    }

    public CustomColorPreferenceCategory(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CustomColorPreferenceCategory(Context context, AttributeSet attrs,
                                         int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);
        TextView titleView = (TextView) view.findViewById(android.R.id.title);
        titleView.setTextColor(new ColorAPI(getContext()).getAccentColor());
    }
}
