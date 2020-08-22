package de.conradowatz.jkgvertretung.fragments;


import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.AppCompatSpinner;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import de.conradowatz.jkgvertretung.R;
import de.conradowatz.jkgvertretung.adapters.StundenplanPagerAdapter;
import de.conradowatz.jkgvertretung.events.DaysUpdatedEvent;
import de.conradowatz.jkgvertretung.events.KursChangedEvent;
import de.conradowatz.jkgvertretung.tools.ColorAPI;
import de.conradowatz.jkgvertretung.tools.PreferenceHelper;
import de.conradowatz.jkgvertretung.variables.Klasse;

public class StundenplanFragment extends Fragment {

    public static final int MODE_STUNDENPLAN = 1;
    public static final int MODE_VERTRETUNGSPLAN = 2;
    public static final int MODE_ALGVERTRETUNGSPLAN = 3;
    public static final int MODE_KLASSENPLAN = 4;
    boolean tabsNoSelect = false;
    private View contentView;
    private ViewPager viewPager;
    private TabLayout tabs;
    private AppCompatSpinner spinner;
    private int mode;
    private int viewpagerCount;
    private String klassenName;
    private EventBus eventBus = EventBus.getDefault();
    private ImageView backgroundView;

    public StundenplanFragment() {
        // Required empty public constructor
    }

    public static StundenplanFragment newInstance(int mode) {

        Bundle args = new Bundle();
        args.putInt("mode", mode);

        StundenplanFragment fragment = new StundenplanFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        if (savedInstanceState == null) {

            Bundle arguments = getArguments();
            mode = arguments.getInt("mode");
        } else {

            mode = savedInstanceState.getInt("mode");
            viewpagerCount = savedInstanceState.getInt("viewpagerCount");
            klassenName = savedInstanceState.getString("klassenName");
        }


        if (mode == MODE_KLASSENPLAN) {
            contentView = inflater.inflate(R.layout.fragment_klassenplan, container, false);
            contentView.findViewById(R.id.relativeLayout).setBackgroundColor(new ColorAPI(getActivity()).getActionBarColor());
        } else contentView = inflater.inflate(R.layout.fragment_stundenplan, container, false);
        viewPager = (ViewPager) contentView.findViewById(R.id.viewPager);
        tabs = (TabLayout) contentView.findViewById(R.id.materialTabs);
        tabs.setSelectedTabIndicatorColor(new ColorAPI(getActivity()).getTabAccentColor());
        backgroundView = (ImageView) contentView.findViewById(R.id.background_image);

        getAndSetBackground();

        eventBus.register(this);

        return contentView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        showData();
    }

    @Override
    public void onResume() {
        super.onResume();
        contentView.findViewById(R.id.layout).invalidate();
        getAndSetBackground();
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

    public static int getPowerOfTwoForSampleRatio(double ratio){
        int k = Integer.highestOneBit((int)Math.floor(ratio));
        if(k==0) return 1;
        else return k;
    }

    @SuppressLint("StaticFieldLeak")
    private void showData() {

        new AsyncTask<Boolean, Integer, Klasse>() {
            @Override
            protected Klasse doInBackground(Boolean... params) {
                return Klasse.getSelectedKlasse();
            }

            @Override
            protected void onPostExecute(Klasse klasse) {

                if ((mode==MODE_STUNDENPLAN || mode==MODE_VERTRETUNGSPLAN) && klasse==null) return;
                if (mode == MODE_KLASSENPLAN) {

                    spinner = (AppCompatSpinner) contentView.findViewById(R.id.spinner);
                    setUpSpinner();
                    if (klassenName != null) setUpViewPager(klassenName, null);
                } else {
                    setUpViewPager(null, null);
                }

            }
        }.execute();
    }

    @SuppressLint("StaticFieldLeak")
    private void setUpSpinner() {

        new AsyncTask<Activity, Integer, ArrayAdapter<String>>() {
            Activity activity;
            Klasse selectedKlasse;
            final List<Klasse> klassen = new ArrayList<>();
            @Override
            protected ArrayAdapter<String> doInBackground(Activity... params) {
                this.activity = params[0];
                ArrayList<String> klassenNamenListe = new ArrayList<>();
                klassen.addAll(Klasse.getAllKlassenSorted());
                for (Klasse klasse : klassen) klassenNamenListe.add(klasse.getName());

                selectedKlasse = Klasse.getSelectedKlasse();

                return new ArrayAdapter<>(activity, R.layout.item_klassenplan_spinner, klassenNamenListe);
            }

            @Override
            protected void onPostExecute(ArrayAdapter<String> spinnerArrayAdapter) {

                spinnerArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinner.setAdapter(spinnerArrayAdapter);

                spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

                        int viewPagerPosition = viewPager.getCurrentItem();
                        klassenName = klassen.get(position).getName();
                        setUpViewPager(klassenName, viewPagerPosition);

                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {

                    }
                });

                if (selectedKlasse!=null) {
                    int selectedIndex = 0;
                    for (int i=0; i<klassen.size(); i++) {
                        if (klassen.get(i).getId()==selectedKlasse.getId()) {
                            selectedIndex = i;
                            break;
                        }
                    }
                    spinner.setSelection(selectedIndex);
                }
            }
        }.execute(getActivity());

    }

    private void setUpViewPager(String klassenName, Integer viewPagerPosition) {

        final boolean firstStart = viewPager.getAdapter() == null;
        if (!isAdded()) return;
        final StundenplanPagerAdapter adapter = new StundenplanPagerAdapter(getChildFragmentManager(), mode, klassenName);
        viewPager.setAdapter(adapter);
        ColorAPI api = new ColorAPI(getActivity());

        if (firstStart) {
            tabs.setTabTextColors(ContextCompat.getColor(getContext(), R.color.tabs_unselected), ContextCompat.getColor(getContext(), R.color.white));
            tabs.setBackgroundColor(api.getActionBarColor());
            if (mode != MODE_STUNDENPLAN) {
                tabs.setTabMode(TabLayout.MODE_SCROLLABLE);
                tabs.setupWithViewPager(viewPager);
            } else {
                tabs.setTabMode(TabLayout.MODE_FIXED);
                tabs.addTab(tabs.newTab().setText("AKTUELL"));
                tabs.addTab(tabs.newTab().setText(adapter.getPageTitle(0)));
                tabs.addTab(tabs.newTab().setText(adapter.getPageTitle(1)));
                tabs.addTab(tabs.newTab().setText(adapter.getPageTitle(2)));
                tabs.getTabAt(1).select();
                tabsNoSelect = false;
                tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {

                    @Override
                    public void onTabSelected(TabLayout.Tab tab) {

                        if (tabsNoSelect) {
                            tabsNoSelect = false;
                            return;
                        }

                        if (tab.getPosition() == 0) {
                            aktuell(true);
                        } else if (tab.getPosition() == 1) {
                            if (viewPager.getCurrentItem() > 1) prevDay(true);
                            else if (viewPager.getCurrentItem() == 1) aktuell(true);
                        } else if (tab.getPosition() == 2) {
                            if (viewPager.getCurrentItem() == 0) nextDay(1, true);
                        } else {
                            if (viewPager.getCurrentItem() == 0) nextDay(2, true);
                            else nextDay(1, true);
                        }

                    }

                    @Override
                    public void onTabUnselected(TabLayout.Tab tab) {
                    }

                    @Override
                    public void onTabReselected(TabLayout.Tab tab) {
                        onTabSelected(tab);
                    }
                });
                viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {

                    private int prevPosition = viewPager.getCurrentItem();

                    @Override
                    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                    }

                    @Override
                    public void onPageSelected(int position) {

                        if (position < prevPosition) { //zurück
                            if (position > 0) prevDay(false);
                            else aktuell(false);
                        } else if (position > prevPosition) { //vor
                            nextDay(1, false);
                        }
                        prevPosition = position;

                    }

                    @Override
                    public void onPageScrollStateChanged(int state) {
                    }
                });
            }
        }

        if (viewPagerPosition != null) viewPager.setCurrentItem(viewPagerPosition);

        viewpagerCount = adapter.getCount();
    }

    private void aktuell(boolean scroll) {
        if (scroll) viewPager.setCurrentItem(0, true);
        tabsNoSelect = true;
        tabs.removeAllTabs();
        tabs.addTab(tabs.newTab().setText("AKTUELL"));
        tabs.addTab(tabs.newTab().setText(viewPager.getAdapter().getPageTitle(0)));
        tabs.addTab(tabs.newTab().setText(viewPager.getAdapter().getPageTitle(1)));
        tabs.addTab(tabs.newTab().setText(viewPager.getAdapter().getPageTitle(2)));
        tabsNoSelect = true;
        tabs.getTabAt(1).select();
    }

    private void nextDay(int pages, boolean scroll) {
        if (scroll) viewPager.setCurrentItem(viewPager.getCurrentItem() + pages, true);
        tabsNoSelect = true;
        tabs.removeAllTabs();
        tabs.addTab(tabs.newTab().setText("AKTUELL"));
        tabs.addTab(tabs.newTab().setText(viewPager.getAdapter().getPageTitle(viewPager.getCurrentItem() - 1)));
        tabs.addTab(tabs.newTab().setText(viewPager.getAdapter().getPageTitle(viewPager.getCurrentItem())));
        tabs.addTab(tabs.newTab().setText(viewPager.getAdapter().getPageTitle(viewPager.getCurrentItem() + 1)));
        tabsNoSelect = true;
        tabs.getTabAt(2).select();
    }

    private void prevDay(boolean scroll) {
        if (scroll) viewPager.setCurrentItem(viewPager.getCurrentItem() - 1, true);
        tabsNoSelect = true;
        tabs.removeAllTabs();
        tabs.addTab(tabs.newTab().setText("AKTUELL"));
        tabs.addTab(tabs.newTab().setText(viewPager.getAdapter().getPageTitle(viewPager.getCurrentItem() - 1)));
        tabs.addTab(tabs.newTab().setText(viewPager.getAdapter().getPageTitle(viewPager.getCurrentItem())));
        tabs.addTab(tabs.newTab().setText(viewPager.getAdapter().getPageTitle(viewPager.getCurrentItem() + 1)));
        tabsNoSelect = true;
        tabs.getTabAt(2).select();
    }

    /**
     * Läd den ViewPager neu, wenn Tage hinzugefügt wurden
     */
    @Subscribe
    public void onEvent(DaysUpdatedEvent event) {

        if (viewPager==null || viewPager.getAdapter()==null) return;

        if (mode!=MODE_STUNDENPLAN) ((StundenplanPagerAdapter)viewPager.getAdapter()).updateData();
    }

    @Subscribe
    public void onEvent(KursChangedEvent event) {

        if (viewPager!=null && viewPager.getAdapter()==null) showData();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {

        outState.putInt("mode", mode);
        outState.putInt("viewpagerCount", viewpagerCount);
        if (klassenName != null) outState.putString("klassenName", klassenName);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        eventBus.unregister(this);
    }
}
