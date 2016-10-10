package de.conradowatz.jkgvertretung.fragments;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import de.conradowatz.jkgvertretung.tools.LocalData;
import de.conradowatz.jkgvertretung.tools.PreferenceHelper;
import de.conradowatz.jkgvertretung.tools.VertretungsAPI;
import de.conradowatz.jkgvertretung.tools.VertretungsData;

public class TaskFragment extends Fragment implements Handler.Callback {

    public static final String TAG_TASK_FRAGMENT = "task_fragment";

    private final static int VERTRETUNGSDATA_CREATED = 1;
    private final static int VERTRETUNGSDATA_CREATE_ERROR = 2;
    private final static int DAY_ADDED = 3;
    private final static int KLASSENLIST_UPDATED = 4;
    private final static int DOWNLOAD_ERROR = 5;
    private final static int DOWNLOAD_FINISHED = 6;
    private final static int NO_CONNECTION = 7;
    private final static int NO_ACCES = 8;
    private final static int LOCALDATA_CREATED = 9;
    private final static int LOCALDATA_CREATE_ERROR = 10;
    private final static int EVENTS_ADDED = 11;
    private Handler taskHandler;
    private ExecutorService pool = Executors.newSingleThreadScheduledExecutor();
    private Context context;
    private Context appContext;
    private boolean createVertretungsDataFromFileWhenAttached = false;
    private boolean createLocalDataFromFileWhenAttached = false;
    private boolean downloadAllDataWhenAttached = false;
    private int downloadAllDataDays;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        this.context = context;
        appContext = context.getApplicationContext();
        if (createVertretungsDataFromFileWhenAttached) {
            createVertretungsDataFromFile();
            createVertretungsDataFromFileWhenAttached = false;
        }
        if (createLocalDataFromFileWhenAttached) {
            createLocalDataFromFile();
            createLocalDataFromFileWhenAttached = false;
        }
        if (downloadAllDataWhenAttached) {
            downloadAllData(downloadAllDataDays);
            downloadAllDataWhenAttached = false;
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
        TaskFragmentCallbacks taskCallbacks = (TaskFragmentCallbacks) context;
        switch (message.what) {
            case VERTRETUNGSDATA_CREATED:
                if (taskCallbacks instanceof SplashScreenCallbacks)
                    ((SplashScreenCallbacks) taskCallbacks).onVertretungsDataCreated();
                else if (taskCallbacks instanceof LoginCallbacks)
                    ((LoginCallbacks) taskCallbacks).onVertretungsDataCreated();
                break;
            case VERTRETUNGSDATA_CREATE_ERROR:
                if (taskCallbacks instanceof SplashScreenCallbacks)
                    ((SplashScreenCallbacks) taskCallbacks).onVertretungsDataCreateError((Throwable) message.obj);
                else if (taskCallbacks instanceof LoginCallbacks)
                    ((LoginCallbacks) taskCallbacks).onVertretungsDataCreateError((Throwable) message.obj);
                break;
            case DAY_ADDED:
                if (taskCallbacks instanceof MainCallbacks)
                    ((MainCallbacks) taskCallbacks).onDayAdded(message.arg1);
                break;
            case KLASSENLIST_UPDATED:
                if (taskCallbacks instanceof MainCallbacks)
                    ((MainCallbacks) taskCallbacks).onKlassenListUpdated();
                break;
            case DOWNLOAD_ERROR:
                taskCallbacks.onDownloadError((Throwable) message.obj);
                break;
            case DOWNLOAD_FINISHED:
                taskCallbacks.onDownloadFinished();
                break;
            case NO_CONNECTION:
                taskCallbacks.onNoConnection();
                break;
            case NO_ACCES:
                taskCallbacks.onNoAccess();
                break;
            case LOCALDATA_CREATED:
                if (taskCallbacks instanceof SplashScreenCallbacks)
                    ((SplashScreenCallbacks) taskCallbacks).onLocalDataCreated();
                break;
            case LOCALDATA_CREATE_ERROR:
                if (taskCallbacks instanceof SplashScreenCallbacks)
                    ((SplashScreenCallbacks) taskCallbacks).onLocalDataCreateError((Throwable) message.obj);
                break;
            case EVENTS_ADDED:
                if (taskCallbacks instanceof MainCallbacks)
                    ((MainCallbacks) taskCallbacks).onEventsAdded();
                break;
            default:
                return false;
        }

        return true;
    }

    /**
     * Erstellt die VertretungsData instance aus dem Speicher
     * Callbacks: onVertretungsDataCreated, onVertretungsDataCreateError
     */
    public void createVertretungsDataFromFile() {

        if (getActivity() == null) {
            createVertretungsDataFromFileWhenAttached = true;
            return;
        }

        pool.submit(new Runnable() {
            @Override
            public void run() {

                VertretungsData.createDataFromFile(appContext, new VertretungsData.CreateDataFromFileListener() {
                    @Override
                    public void onCreated() {
                        Message message = taskHandler.obtainMessage(VERTRETUNGSDATA_CREATED);
                        taskHandler.sendMessage(message);
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        Message message = taskHandler.obtainMessage(VERTRETUNGSDATA_CREATE_ERROR, throwable);
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
    public void saveVertretungsDataToFile() {

        pool.submit(new Runnable() {
            @Override
            public void run() {

                VertretungsData.saveDataToFile(appContext);

            }
        });

    }

    /**
     * Erstellt die VertretungsData instance aus dem Speicher
     * Callbacks: onVertretungsDataCreated, onVertretungsDataCreateError
     */
    public void createLocalDataFromFile() {

        if (getActivity() == null) {
            createLocalDataFromFileWhenAttached = true;
            return;
        }

        pool.submit(new Runnable() {
            @Override
            public void run() {

                LocalData.createFromFile(appContext, new LocalData.CreateDataFromFileListener() {
                    @Override
                    public void onDataCreated() {
                        Message message = taskHandler.obtainMessage(LOCALDATA_CREATED);
                        taskHandler.sendMessage(message);
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        Message message = taskHandler.obtainMessage(LOCALDATA_CREATE_ERROR, throwable);
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
    public void saveLocalDataToFile() {

        pool.submit(new Runnable() {
            @Override
            public void run() {

                LocalData.saveToFile(appContext);

            }
        });

    }

    /**
     * Läd die komplette VertretungsData neu herunter
     * Callbacks: onDownloadFinished, onNoConnection, onNoAccess, onDownloadError, onDayAdded, onKlassenListUpdated
     *
     * @param dayCount wie viele Schultage geladen werden sollen, 0 wenn alle
     */
    public void downloadAllData(final int dayCount) {

        if (getActivity() == null) {
            downloadAllDataWhenAttached = true;
            downloadAllDataDays = dayCount;
            return;
        }

        final boolean downloadEvents = dayCount != 0 && PreferenceHelper.readBooleanFromPreferences(appContext, "onlineEvents", true);

        pool.submit(new Runnable() {
            @Override
            public void run() {

                final String username = PreferenceHelper.readStringFromPreferences(appContext, "username", "");
                final String password = PreferenceHelper.readStringFromPreferences(appContext, "password", "");
                new VertretungsAPI(username, password, appContext).downloadAllData(dayCount, downloadEvents, new VertretungsAPI.DownloadAllDataResponseListener() {
                    @Override
                    public void onSuccess() {

                        Message message = taskHandler.obtainMessage(DOWNLOAD_FINISHED);
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
                    public void onProgress(int progress) {

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

                    @Override
                    public void onEventsAdded() {
                        Message message = taskHandler.obtainMessage(EVENTS_ADDED);
                        taskHandler.sendMessage(message);
                    }
                });

            }
        });

    }

    private interface TaskFragmentCallbacks {

        void onDownloadError(Throwable throwable);

        void onNoAccess();

        void onNoConnection();

        void onDownloadFinished();

    }

    public interface MainCallbacks extends TaskFragmentCallbacks {

        void onDayAdded(int position);
        void onKlassenListUpdated();

        void onEventsAdded();

    }

    public interface SplashScreenCallbacks extends TaskFragmentCallbacks {

        void onLocalDataCreated();

        void onLocalDataCreateError(Throwable throwable);

        void onVertretungsDataCreated();

        void onVertretungsDataCreateError(Throwable throwable);

    }

    public interface LoginCallbacks extends TaskFragmentCallbacks {

        void onVertretungsDataCreated();

        void onVertretungsDataCreateError(Throwable throwable);
    }
}
