package de.conradowatz.jkgvertretung.activities;

import android.content.pm.PackageManager;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.transition.Slide;
import android.view.Gravity;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import org.greenrobot.eventbus.EventBus;

import de.conradowatz.jkgvertretung.events.PermissionGrantedEvent;
import de.conradowatz.jkgvertretung.fragments.SettingsFragment;
import de.conradowatz.jkgvertretung.tools.ColorAPI;

public class SettingsActivity extends AppCompatActivity {

    private EventBus eventBus = EventBus.getDefault();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ColorAPI api = new ColorAPI(this);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setBackgroundDrawable(new ColorDrawable(new ColorAPI(this).getActionBarColor()));

        ColorAPI api2 = new ColorAPI(null);
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new SettingsFragment())
                .commit();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setEnterTransition(new Slide(Gravity.END));
            getWindow().setAllowEnterTransitionOverlap(true);
        }
    }

    @Override
    public void onBackPressed() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) finishAfterTransition();
        else finish();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (item.getItemId() == android.R.id.home) {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) finishAfterTransition();
            else finish();
            return true;
        }
        return false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

            eventBus.post(new PermissionGrantedEvent(requestCode));

        }
    }
}
