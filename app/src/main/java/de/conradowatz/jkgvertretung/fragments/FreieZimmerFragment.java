package de.conradowatz.jkgvertretung.fragments;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

import de.conradowatz.jkgvertretung.R;
import de.conradowatz.jkgvertretung.adapters.FreieZimmerPagerAdapter;
import de.conradowatz.jkgvertretung.events.DaysUpdatedEvent;
import de.conradowatz.jkgvertretung.tools.ColorAPI;
import de.conradowatz.jkgvertretung.tools.PreferenceHelper;

import static de.conradowatz.jkgvertretung.fragments.StundenplanFragment.getPowerOfTwoForSampleRatio;

public class FreieZimmerFragment extends Fragment {

    private View contentView;

    private ViewPager viewPager;
    private TabLayout tabs;
    private ImageView backgroundView;

    private EventBus eventBus = EventBus.getDefault();

    public FreieZimmerFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        contentView = inflater.inflate(R.layout.fragment_stundenplan, container, false);
        viewPager = (ViewPager) contentView.findViewById(R.id.viewPager);
        tabs = (TabLayout) contentView.findViewById(R.id.materialTabs);
        tabs.setSelectedTabIndicatorColor((new ColorAPI(getActivity())).getTabAccentColor());
        backgroundView = (ImageView) contentView.findViewById(R.id.background_image);

        eventBus.register(this);

        getAndSetBackground();

        setUpViewPager();

        return contentView;
    }

    private void setUpViewPager() {

        boolean firstStart = viewPager.getAdapter() == null;
        FreieZimmerPagerAdapter adapter = new FreieZimmerPagerAdapter(getChildFragmentManager());
        viewPager.setAdapter(adapter);

        if (firstStart) {
            tabs.setTabTextColors(ContextCompat.getColor(getContext(), R.color.tabs_unselected), ContextCompat.getColor(getContext(), R.color.white));
            tabs.setBackgroundColor(new ColorAPI(getActivity()).getActionBarColor());
            tabs.setTabMode(TabLayout.MODE_SCROLLABLE);
            tabs.setupWithViewPager(viewPager);
        }
    }

    private void getAndSetBackground() {
        int[] backgrounds = {R.drawable.background1, R.drawable.background2, R.drawable.background3};

        int background = Integer.parseInt(PreferenceHelper.readStringFromPreferences(getContext(), "background", "-1"));
        if (background > 0 && background < 4) {
            backgroundView.setImageResource(backgrounds[background-1]);
        } else {
            backgroundView.setImageBitmap(Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888));
            if (background == 4) {
                int startColor = PreferenceHelper.readIntFromPreferences(getContext(), "color1", Color.rgb(51, 193, 238));
                int endColor = PreferenceHelper.readIntFromPreferences(getContext(), "color2", Color.WHITE);
                GradientDrawable gradientDrawable = new GradientDrawable(
                        GradientDrawable.Orientation.TOP_BOTTOM,
                        new int[]{startColor, endColor});
                backgroundView.setBackground(gradientDrawable);
            } else if (background == 0) {
                backgroundView.setBackground(new ColorDrawable(Color.BLACK));
            } else {
                backgroundView.setBackground(null);
            }
            if (background == 5) {
                String uriStr = PreferenceHelper.readStringFromPreferences(getActivity(), "backgroundPictureURI", null);
                if (uriStr == null)
                    return;
                Uri uri;
                try {
                    uri = Uri.parse(new URI(uriStr).toString());
                    backgroundView.setImageBitmap(getThumbnail(uri));
                    int fitMode = Integer.parseInt(PreferenceHelper.readStringFromPreferences(getActivity(), "pictureFitMode", "1"));
                    switch (fitMode) {
                        case 1:
                            backgroundView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                            break;
                        case 2:
                            backgroundView.setScaleType(ImageView.ScaleType.FIT_XY);
                            break;
                        case 3:
                            backgroundView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                            break;
                        default:
                            backgroundView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                            break;
                    }
                } catch (Throwable t) {
                    Toast.makeText(getActivity(), "Fehler beim Laden des Hintergrundbildes...", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    public Bitmap getThumbnail(Uri uri) throws IOException {
        double THUMBNAIL_SIZE = 1000.0;
        InputStream input = getActivity().getContentResolver().openInputStream(uri);

        BitmapFactory.Options onlyBoundsOptions = new BitmapFactory.Options();
        onlyBoundsOptions.inJustDecodeBounds = true;
        onlyBoundsOptions.inDither=true;//optional
        onlyBoundsOptions.inPreferredConfig=Bitmap.Config.ARGB_8888;//optional
        BitmapFactory.decodeStream(input, null, onlyBoundsOptions);
        input.close();

        if ((onlyBoundsOptions.outWidth == -1) || (onlyBoundsOptions.outHeight == -1)) {
            return null;
        }

        int originalSize = Math.max(onlyBoundsOptions.outHeight, onlyBoundsOptions.outWidth);

        double ratio = (originalSize > THUMBNAIL_SIZE) ? (originalSize / THUMBNAIL_SIZE) : 1.0;

        BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();
        bitmapOptions.inSampleSize = getPowerOfTwoForSampleRatio(ratio);
        bitmapOptions.inDither = true;
        bitmapOptions.inPreferredConfig=Bitmap.Config.ARGB_8888;
        input = getActivity().getContentResolver().openInputStream(uri);
        Bitmap bitmap = BitmapFactory.decodeStream(input, null, bitmapOptions);
        input.close();
        return bitmap;
    }

    /**
     * Läd den ViewPager neu, wenn Tage hinzugefügt wurden
     */
    @Subscribe
    public void onEvent(DaysUpdatedEvent event) {

        if (viewPager == null) return;

        ((FreieZimmerPagerAdapter) viewPager.getAdapter()).updateData();
        tabs.setupWithViewPager(viewPager);


    }

    @Override
    public void onStop() {
        super.onStop();

        eventBus.unregister(this);
    }
}

