package de.conradowatz.jkgvertretung.fragments;


import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.text.SimpleDateFormat;
import java.util.ArrayList;

import de.conradowatz.jkgvertretung.R;
import de.conradowatz.jkgvertretung.activities.MainActivity;
import de.conradowatz.jkgvertretung.tools.PreferenceReader;
import de.conradowatz.jkgvertretung.variables.StuPlaKlasse;
import de.conradowatz.jkgvertretung.variables.Stunde;
import de.conradowatz.jkgvertretung.variables.Tag;

public class StundenplanFragment extends Fragment {

    private View contentView;

    private ViewPager viewPager;
    private TabLayout tabs;
    private int dayCount = -1;

    public StundenplanFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        contentView = inflater.inflate(R.layout.fragment_stundenplan, container, false);
        viewPager = (ViewPager) contentView.findViewById(R.id.viewPager);
        tabs = (TabLayout) contentView.findViewById(R.id.materialTabs);

        MainActivity mainActivity = (MainActivity) getActivity();
        if (mainActivity.vertretungsAPI==null) return contentView;

        int klassenIndex = PreferenceReader.readIntFromPreferences(getActivity(), "meineKlasseInt", 0);
        ArrayList<String> nichtKurse = PreferenceReader.readStringListFromPreferences(getActivity(), "meineNichtKurse");
        StundenplanPagerAdapter stundenplanPagerAdapter = new StundenplanPagerAdapter(mainActivity.vertretungsAPI.getTagList(), klassenIndex, nichtKurse, getActivity().getLayoutInflater());
        viewPager.setAdapter(stundenplanPagerAdapter);
        tabs.setTabTextColors(getResources().getColor(R.color.white), getResources().getColor(R.color.white));
        tabs.setTabMode(TabLayout.MODE_SCROLLABLE);
        tabs.setupWithViewPager(viewPager);

        return contentView;
    }

    public void onDayAdded() {

        if (viewPager!=null) {
            viewPager.getAdapter().notifyDataSetChanged();
            tabs.setupWithViewPager(viewPager);
        }
    }

    private class StundenplanPagerAdapter extends PagerAdapter {

        private ArrayList<Tag> tagList;
        private int klasseIndex;
        private LayoutInflater layoutInflater;
        private ArrayList<String> nichtKurse;

        @Override
        public CharSequence getPageTitle(int position) {

            return tagList.get(position).getDatumString().split(",")[0];
        }

        public StundenplanPagerAdapter(ArrayList<Tag> tagList, int klasseIndex, ArrayList<String> nichtKurse, LayoutInflater layoutInflater) {

            this.tagList = tagList;
            this.klasseIndex = klasseIndex;
            this.nichtKurse = nichtKurse;
            this.layoutInflater = layoutInflater;
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {

            View page = layoutInflater.inflate(R.layout.stundenplan_page, container, false);
            LinearLayout linearLayout = (LinearLayout) page.findViewById(R.id.linearLayout);
            TextView datumText = (TextView) page.findViewById(R.id.datumText);
            TextView zeitstempelText = (TextView) page.findViewById(R.id.zeitstempelText);

            datumText.setText(tagList.get(position).getDatumString());
            zeitstempelText.setText("aktualisiert am "+tagList.get(position).getZeitStempel());

            StuPlaKlasse stuPlaKlasse = tagList.get(position).getStuplaKlasseList().get(klasseIndex);
            ArrayList<Stunde> stundenList = stuPlaKlasse.getStundenList();
            for (int i=0; i<stundenList.size(); i++) {

                Stunde stunde = stundenList.get(i);
                if (nichtKurse.contains(stunde.getKurs()))
                    continue;

                View stundenItem = layoutInflater.inflate(R.layout.stundenplan_stunde_item, linearLayout, false);
                TextView stundeText = (TextView) stundenItem.findViewById(R.id.stundeText);
                TextView fachText = (TextView) stundenItem.findViewById(R.id.fachText);
                TextView raumText = (TextView) stundenItem.findViewById(R.id.raumText);
                TextView infoText = (TextView) stundenItem.findViewById(R.id.infoText);

                stundeText.setText(stunde.getStunde());
                fachText.setText(stunde.getFach());
                if (stunde.isFachg())
                    fachText.setTextColor(getResources().getColor(R.color.accent));
                raumText.setText(stunde.getRaum());
                if (stunde.isRaumg())
                    raumText.setTextColor(getResources().getColor(R.color.accent));
                if (stunde.getInfo().isEmpty())
                    infoText.setHeight(0);
                infoText.setText(stunde.getInfo());

                linearLayout.addView(stundenItem);
            }


            container.addView(page);
            return page;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView((View) object);
        }

        @Override
        public int getCount() {
            return tagList.size();
        }
    }


}
