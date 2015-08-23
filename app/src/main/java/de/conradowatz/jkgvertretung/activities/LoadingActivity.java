package de.conradowatz.jkgvertretung.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.pnikosis.materialishprogress.ProgressWheel;

import de.conradowatz.jkgvertretung.R;
import de.conradowatz.jkgvertretung.tools.PreferenceReader;
import de.conradowatz.jkgvertretung.tools.VertretungsAPI;
import de.conradowatz.jkgvertretung.tools.VertretungsData;

public class LoadingActivity extends AppCompatActivity {

    private static final int CONTENT_LOADING = 1;
    private static final int CONTENT_NOCONNECTION = 2;
    private static final int CONTENT_NOACCES = 3;
    private boolean isnoConnection = false;

    private TextView ladeDatenText;
    private TextView errorText;
    private ProgressWheel progressWheel;
    private ProgressBar progressBar;
    private ImageView cloudImage;
    private ImageView logoImage;
    private RelativeLayout loadingLayout;
    private CardView noaccesLayout;
    private Button retryButton;
    private Button changepwButton;

    private String username;
    private String password;

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
        noaccesLayout = (CardView) findViewById(R.id.noacces_layout);
        retryButton = (Button) findViewById(R.id.retryButton);
        changepwButton = (Button) findViewById(R.id.changepwButton);
        loadingLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            if (!isnoConnection) {
                return;
            }

            contentMode(CONTENT_LOADING);

            isnoConnection = false;
            downloadData();

        }});
        retryButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

            contentMode(CONTENT_LOADING);
            downloadData();
        }});
        changepwButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent backToMain = new Intent();
                backToMain.putExtra("ExitCode", "ReLog");
                setResult(RESULT_OK, backToMain);
                finish();
            }
        });

        username = PreferenceReader.readStringFromPreferences(this, "username", "");
        password = PreferenceReader.readStringFromPreferences(this, "password", "");

        downloadData();

    }

    /**
     * Läd alle Daten für maximal 3 Tage herunter
     */
    private void downloadData() {

        VertretungsAPI vertretungsAPI = new VertretungsAPI(username, password);

        vertretungsAPI.getAllInfo(3, new VertretungsAPI.AllInfoResponseListener() {
            @Override
            public void onSuccess() {

                if (VertretungsData.getsInstance().getTagList().size() > 0) {

                    Intent backToMain = new Intent();
                    backToMain.putExtra("ExitCode", "LoadingDone");
                    setResult(RESULT_OK, backToMain);
                    finish();
                } else {
                    onNoAccess();
                }
            }

            @Override
            public void onProgress(double progress) {
                progressBar.setProgress((int) progress);
            }

            @Override
            public void onNoConnection() {

                contentMode(CONTENT_NOCONNECTION);
                isnoConnection = true;

            }

            @Override
            public void onNoAccess() {

                contentMode(CONTENT_NOACCES);

            }

            @Override
            public void onOtherError(Throwable e) {
                e.printStackTrace();
            }
        });

    }

    /**
     * Wechselt die anzuzeigenden Elemente
     *
     * @param contentMode Ansicht
     */
    private void contentMode(int contentMode) {

        if (contentMode == CONTENT_LOADING) {

            ladeDatenText.setVisibility(View.VISIBLE);
            progressBar.setVisibility(View.VISIBLE);
            progressWheel.setVisibility(View.VISIBLE);
            logoImage.setVisibility(View.VISIBLE);
            cloudImage.setVisibility(View.INVISIBLE);
            errorText.setVisibility(View.INVISIBLE);
            noaccesLayout.setVisibility(View.GONE);

        } else if (contentMode == CONTENT_NOCONNECTION) {

            ladeDatenText.setVisibility(View.INVISIBLE);
            progressBar.setVisibility(View.INVISIBLE);
            progressWheel.setVisibility(View.INVISIBLE);
            logoImage.setVisibility(View.INVISIBLE);
            cloudImage.setVisibility(View.VISIBLE);
            errorText.setVisibility(View.VISIBLE);
            noaccesLayout.setVisibility(View.GONE);

        } else if (contentMode == CONTENT_NOACCES) {

            ladeDatenText.setVisibility(View.INVISIBLE);
            progressBar.setVisibility(View.INVISIBLE);
            progressWheel.setVisibility(View.INVISIBLE);
            logoImage.setVisibility(View.INVISIBLE);
            cloudImage.setVisibility(View.INVISIBLE);
            errorText.setVisibility(View.INVISIBLE);
            noaccesLayout.setVisibility(View.VISIBLE);

        }
    }

    @Override
    public void onBackPressed() {

        //Anwendung schließen
        Intent backToMain = new Intent();
        backToMain.putExtra("ExitCode", "Exit");
        setResult(RESULT_OK, backToMain);
        finish();
    }
}
