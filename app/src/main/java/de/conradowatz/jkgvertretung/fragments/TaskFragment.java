package de.conradowatz.jkgvertretung.fragments;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import de.conradowatz.jkgvertretung.tools.PreferenceReader;
import de.conradowatz.jkgvertretung.tools.VertretungsAPI;

public class TaskFragment extends Fragment implements Handler.Callback {

    private final static int DATA_CREATED = 1;
    private final static int DATA_CREATE_ERROR = 2;
    private final static int UPDATE_DAYS_FINISHED = 3;
    private final static int DAY_ADDED = 4;
    private final static int KLASSENLIST_UPDATED = 5;
    private final static int DOWNLOAD_ERROR = 6;
    private final static int REFRESH_FINISHED = 7;
    private final static int REFRESH_NOCONNECTION = 8;
    private final static int NO_ACCES = 9;
    private Handler taskHandler;
    private ExecutorService pool = Executors.newSingleThreadScheduledExecutor();
    private Context context;
    private Context appContext;
    private boolean createDataFromFileWhenAttached = false;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        this.context = context;
        appContext = context.getApplicationContext();
        if (createDataFromFileWhenAttached) {
            createDataFromFile();
            createDataFromFileWhenAttached = false;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setRetainInstance(true);
        taskHandler = new Handler(this);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        context = null;
    }

    @Override
    public boolean handleMessage(Message message) {

        if (context == null) return false;
        TaskCallbacks taskCallbacks = (TaskCallbacks) context;
        switch (message.what) {
            case DATA_CREATED:
                taskCallbacks.onDataCreated();
                break;
            case DATA_CREATE_ERROR:
                taskCallbacks.onDataCreateError((Throwable) message.obj);
                break;
            case UPDATE_DAYS_FINISHED:
                taskCallbacks.onUpdateDaysFinished(message.arg1);
                break;
            case DAY_ADDED:
                taskCallbacks.onDayAdded(message.arg1);
                break;
            case KLASSENLIST_UPDATED:
                taskCallbacks.onKlassenListUpdated();
                break;
            case DOWNLOAD_ERROR:
                taskCallbacks.onDownloadError((Throwable) message.obj);
                break;
            case REFRESH_FINISHED:
                taskCallbacks.onRefreshFinished();
                break;
            case REFRESH_NOCONNECTION:
                taskCallbacks.onRefreshNoConnection();
                break;
            case NO_ACCES:
                taskCallbacks.onNoAccess();
                break;
            default:
                return false;
        }

        return true;
    }

    /**
     * Erstellt die VertretungsData instance aus dem Speicher
     * Callbacks: onDataCreated, onDataCreateError
     */
    public void createDataFromFile() {

        if (!isAdded()) {
            createDataFromFileWhenAttached = true;
            return;
        }

        pool.submit(new Runnable() {
            @Override
            public void run() {

                VertretungsAPI.createDataFromFile(appContext, new VertretungsAPI.CreateDataFromFileListener() {
                    @Override
                    public void onCreated() {
                        Message message = taskHandler.obtainMessage(DATA_CREATED);
                        taskHandler.sendMessage(message);
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        Message message = taskHandler.obtainMessage(DATA_CREATE_ERROR, throwable);
                        taskHandler.sendMessage(message);
                    }
                });

            }
        });

    }

    /**
     * Speichert die VertretungsData in den Speicher
     * Callbacks: keine
     */
    public void saveDataToFile() {

        pool.submit(new Runnable() {
            @Override
            public void run() {

                VertretungsAPI.saveDataToFile(appContext);

            }
        });

    }

    /**
     * Läd eine bestimmt Anzahl Tage neu herunter
     * Callbacks: onUpdateDaysFinished, onDownloadError, onDayAdded
     *
     * @param days     wie viele Schultage geladen werden sollen
     * @param skipDays wie viele Schultage übersprungen werden sollen
     */
    public void updateDays(final int days, final int skipDays) {

        pool.submit(new Runnable() {
            @Override
            public void run() {

                final String username = PreferenceReader.readStringFromPreferences(appContext, "username", "");
                final String password = PreferenceReader.readStringFromPreferences(appContext, "password", "");
                new VertretungsAPI(username, password).downloadDays(days, skipDays, new VertretungsAPI.DownloadDaysListener() {
                    @Override
                    public void onFinished() {
                        Message message = taskHandler.obtainMessage(UPDATE_DAYS_FINISHED, skipDays, 0);
                        taskHandler.sendMessage(message);
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        Message message = taskHandler.obtainMessage(DOWNLOAD_ERROR, throwable);
                        taskHandler.sendMessage(message);
                    }

                    @Override
                    public void onDayAdded(int position) {
                        Message message = taskHandler.obtainMessage(DAY_ADDED, position, 0);
                        taskHandler.sendMessage(message);
                    }
                });

            }
        });

    }

    /**
     * Läd die komplette VertretungsData neu herunter
     * Callbacks: onRefreshFinished, onRefreshNoConnection, onNoAccess, onDownloadError, onDayAdded, onKlassenListUpdated
     *
     * @param dayCount wie viele Schultage geladen werden sollen
     */
    public void downloadAllData(final int dayCount) {

        pool.submit(new Runnable() {
            @Override
            public void run() {

                final String username = PreferenceReader.readStringFromPreferences(appContext, "username", "");
                final String password = PreferenceReader.readStringFromPreferences(appContext, "password", "");
                new VertretungsAPI(username, password).downloadAllData(dayCount, new VertretungsAPI.DownloadAllDataResponseListener() {
                    @Override
                    public void onSuccess() {

                        Message message = taskHandler.obtainMessage(REFRESH_FINISHED);
                        taskHandler.sendMessage(message);
                    }

                    @Override
                    public void onNoConnection() {
                        Message message = taskHandler.obtainMessage(REFRESH_NOCONNECTION);
                        taskHandler.sendMessage(message);
                    }

                    @Override
                    public void onNoAccess() {
                        Message message = taskHandler.obtainMessage(NO_ACCES);
                        taskHandler.sendMessage(message);
                    }

                    @Override
                    public void onOtherError(Throwable throwable) {
                        Message message = taskHandler.obtainMessage(DOWNLOAD_ERROR, throwable);
                        taskHandler.sendMessage(message);
                    }

                    @Override
                    public void onDayAdded(int position) {
                        Message message = taskHandler.obtainMessage(DAY_ADDED, position, 0);
                        taskHandler.sendMessage(message);
                    }

                    @Override
                    public void onKlassenListFinished() {
                        Message message = taskHandler.obtainMessage(KLASSENLIST_UPDATED);
                        taskHandler.sendMessage(message);
                    }
                });

            }
        });

    }

    public interface TaskCallbacks {
        void onDataCreated();

        void onDataCreateError(Throwable throwable);

        void onUpdateDaysFinished(int skipDays);

        void onDayAdded(int position);

        void onKlassenListUpdated();

        void onDownloadError(Throwable throwable);

        void onRefreshFinished();

        void onRefreshNoConnection();

        void onNoAccess();
    }
}
