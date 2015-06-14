package de.conradowatz.jkgvertretung.activities;

import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.pnikosis.materialishprogress.ProgressWheel;

import de.conradowatz.jkgvertretung.R;
import de.conradowatz.jkgvertretung.tools.PreferenceReader;
import de.conradowatz.jkgvertretung.tools.VertretungsAPI;
import de.greenrobot.event.EventBus;

public class LoadingActivity extends AppCompatActivity implements View.OnClickListener {

    private boolean isnoConnection = false;
    private TextView ladeDatenText;
    private TextView errorText;
    private ProgressWheel progressWheel;
    private ProgressBar progressBar;
    private ImageView cloudImage;
    private ImageView logoImage;
    private RelativeLayout loadingLayout;

    private VertretungsAPI vertretungsAPI;
    private EventBus eventBus = EventBus.getDefault();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loading);

        ladeDatenText = (TextView) findViewById(R.id.ladeDatenText);
        errorText = (TextView) findViewById(R.id.errorText);
        progressWheel = (ProgressWheel) findViewById(R.id.progressWheel);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        cloudImage = (ImageView) findViewById(R.id.cloudImage);
        logoImage = (ImageView) findViewById(R.id.logoImage);
        loadingLayout = (RelativeLayout) findViewById(R.id.loadingLayout);
        loadingLayout.setOnClickListener(this);

        String username = PreferenceReader.readStringFromPreferences(this, "username", "");
        String password = PreferenceReader.readStringFromPreferences(this, "password", "");
        vertretungsAPI = new VertretungsAPI(username, password);

        downloadData();

    }

    private void downloadData() {

        vertretungsAPI.getAllInfo(new VertretungsAPI.AsyncVertretungsResponseHandler() {
            @Override
            public void onSuccess() {
                eventBus.post(vertretungsAPI);
                finish();
            }

            @Override
            public void onProgress(double progress) {
                progressBar.setProgress((int) progress);
            }

            @Override
            public void onNoConnection() {
                noConnection();
            }

            @Override
            public void onNoAccess() {
                Log.d("SHIT", "VERDAMMTE SCHEISSE");
            }

            @Override
            public void onOtherError(Throwable e) {
                e.printStackTrace();
            }
        });

    }

    private void noConnection() {

        ladeDatenText.setVisibility(View.INVISIBLE);
        progressBar.setVisibility(View.INVISIBLE);
        progressWheel.setVisibility(View.INVISIBLE);
        logoImage.setVisibility(View.INVISIBLE);
        cloudImage.setVisibility(View.VISIBLE);
        errorText.setVisibility(View.VISIBLE);

        isnoConnection = true;

    }

    @Override
    public void onBackPressed() {

        //Anwendung schließen
        Intent backToMain = new Intent();
        backToMain.putExtra("ExitCode", "Exit");
        setResult(RESULT_OK, backToMain);
        finish();
    }

    @Override
    public void onClick(View v) {

        if (!isnoConnection) {
            return;
        }

        ladeDatenText.setVisibility(View.VISIBLE);
        progressBar.setVisibility(View.VISIBLE);
        progressWheel.setVisibility(View.VISIBLE);
        logoImage.setVisibility(View.VISIBLE);
        cloudImage.setVisibility(View.INVISIBLE);
        errorText.setVisibility(View.INVISIBLE);

        isnoConnection = false;
        downloadData();

    }
}
