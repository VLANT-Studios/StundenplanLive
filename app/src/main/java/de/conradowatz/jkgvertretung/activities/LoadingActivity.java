package de.conradowatz.jkgvertretung.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.pnikosis.materialishprogress.ProgressWheel;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import de.conradowatz.jkgvertretung.R;
import de.conradowatz.jkgvertretung.tools.PreferenceReader;
import de.conradowatz.jkgvertretung.tools.VertretungsAPI;
import de.conradowatz.jkgvertretung.tools.VertretungsData;

public class LoadingActivity extends AppCompatActivity implements Handler.Callback {

    private static final int CONTENT_LOADING = 1;
    private static final int CONTENT_NOCONNECTION = 2;
    private static final int CONTENT_NOACCES = 3;

    private final static int DATA_FINISHED = 1;
    private final static int PROGRESS = 2;
    private final static int NO_CONNECTION = 3;
    private final static int NO_ACCES = 4;
    private final static int ERROR = 5;

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

    private Handler taskHandler;
    private ExecutorService pool;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loading);

        taskHandler = new Handler(this);
        pool = Executors.newSingleThreadScheduledExecutor();

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

        downloadData();

    }

    /**
     * Läd alle Daten für maximal 3 Tage herunter
     */
    private void downloadData() {

        final String username = PreferenceReader.readStringFromPreferences(getApplicationContext(), "username", "");
        final String password = PreferenceReader.readStringFromPreferences(getApplicationContext(), "password", "");
        pool.submit(new Runnable() {
            @Override
            public void run() {

                VertretungsData.setInstance(null);
                new VertretungsAPI(username, password).downloadAllData(3, new VertretungsAPI.DownloadAllDataResponseListener() {

                    @Override
                    public void onSuccess() {
                        Message message = taskHandler.obtainMessage(DATA_FINISHED);
                        taskHandler.sendMessage(message);
                    }

                    @Override
                    public void onNoConnection() {
                        Message message = taskHandler.obtainMessage(NO_CONNECTION);
                        taskHandler.sendMessage(message);
                    }

                    @Override
                    public void onNoAccess() {
                        Message message = taskHandler.obtainMessage(NO_ACCES);
                        taskHandler.sendMessage(message);
                    }

                    @Override
                    public void onOtherError(Throwable throwable) {
                        Message message = taskHandler.obtainMessage(ERROR, throwable);
                        taskHandler.sendMessage(message);
                    }

                    @Override
                    public void onProgress(int progress) {
                        Message message = taskHandler.obtainMessage(PROGRESS, progress, 0);
                        taskHandler.sendMessage(message);
                    }
                });

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

    @Override
    public boolean handleMessage(Message message) {

        switch (message.what) {
            case DATA_FINISHED:
                onDataFinished();
                break;
            case PROGRESS:
                onProgress(message.arg1);
                break;
            case NO_CONNECTION:
                onNoConnection();
                break;
            case NO_ACCES:
                onNoAccess();
                break;
            case ERROR:
                onError((Throwable) message.obj);
                break;
            default:
                return false;
        }
        return true;
    }

    private void onDataFinished() {

        if (VertretungsData.getsInstance().getTagList().size() > 0) {

            Intent backToMain = new Intent();
            backToMain.putExtra("ExitCode", "LoadingDone");
            setResult(RESULT_OK, backToMain);
            finish();
        } else {
            onNoAccess();
        }

    }

    private void onProgress(int progress) {

        progressBar.setProgress(progress);
    }

    private void onNoConnection() {

        contentMode(CONTENT_NOCONNECTION);
        isnoConnection = true;

    }

    private void onNoAccess() {

        contentMode(CONTENT_NOACCES);

    }

    private void onError(Throwable throwable) {

        Log.e("JKGDEBUG", "Fehler beim Download oder der Verarbeitung der Daten!");
        Log.e("JKGDEBUG", "Message: "+throwable.getMessage());

    }
}
