package de.conradowatz.jkgvertretung.fragments;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import androidx.fragment.app.Fragment;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import de.conradowatz.jkgvertretung.tools.PreferenceHelper;
import de.conradowatz.jkgvertretung.tools.VertretungsAPI;

public class TaskFragment extends Fragment implements Handler.Callback {

    public static final String TAG_TASK_FRAGMENT = "task_fragment";

    private final static int DAYS_UPDATED = 0;
    private final static int KLASSENLIST_UPDATED = 1;
    private final static int FREIETAGE_ADDED = 2;
    private final static int DOWNLOAD_ERROR = 3;
    private final static int DOWNLOAD_FINISHED = 4;
    private final static int NO_CONNECTION = 5;
    private final static int NO_ACCESS = 6;
    private Handler taskHandler;
    private ExecutorService pool = Executors.newSingleThreadScheduledExecutor();
    private Context context;
    private Context appContext;
    private boolean downloadAllDataWhenAttached = false;
    private int downloadAllDataDays;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        this.context = context;
        appContext = context.getApplicationContext();
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
            case DAYS_UPDATED:
                if (taskCallbacks instanceof MainCallbacks)
                    ((MainCallbacks) taskCallbacks).onDaysUpdated();
                break;
            case KLASSENLIST_UPDATED:
                if (taskCallbacks instanceof MainCallbacks)
                    ((MainCallbacks) taskCallbacks).onKlassenListUpdated();
                break;
            case FREIETAGE_ADDED:
                if (taskCallbacks instanceof MainCallbacks)
                    ((MainCallbacks) taskCallbacks).onFreieTageAdded();
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
            case NO_ACCESS:
                taskCallbacks.onNoAccess();
                break;
            default:
                return false;
        }

        return true;
    }

    public void downloadAllData(int dayCount) {

        if (getActivity() == null) {
            downloadAllDataWhenAttached = true;
            downloadAllDataDays = dayCount;
            return;
        }

        boolean hasAuth = PreferenceHelper.readBooleanFromPreferences(appContext, "hasAuth", false);

        if (hasAuth) {
            String username = PreferenceHelper.readStringFromPreferences(appContext, "username", "");
            String password = PreferenceHelper.readStringFromPreferences(appContext, "password", "");
            downloadAllData(dayCount, username, password);
        } else {
            downloadAllData(dayCount, null, null);
        }
    }

    /**
     * Läd die komplette VertretungsData neu herunter
     * Callbacks: onDownloadFinished, onNoConnection, onNoAccess, onDownloadError, onDayAdded, onKlassenListUpdated
     *
     * @param dayCount wie viele Schultage geladen werden sollen, 0 wenn alle
     */
    public void downloadAllData(final int dayCount, final String username, final String password) {

        pool.submit(new Runnable() {
            @Override
            public void run() {

                Thread.currentThread().setPriority(Thread.MIN_PRIORITY);
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                VertretungsAPI api = username==null||username.isEmpty()?new VertretungsAPI():new VertretungsAPI(username, password);
                api.downloadAllData(dayCount, new VertretungsAPI.DownloadAllDataResponseListener() {
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
                        Message message = taskHandler.obtainMessage(NO_ACCESS);
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
                    public void onDaysUpdated() {
                        Message message = taskHandler.obtainMessage(DAYS_UPDATED);
                        taskHandler.sendMessage(message);
                    }

                    @Override
                    public void onKlassenListFinished() {
                        Message message = taskHandler.obtainMessage(KLASSENLIST_UPDATED);
                        taskHandler.sendMessage(message);
                    }

                    @Override
                    public void onFreieTageAdded() {
                        Message message = taskHandler.obtainMessage(FREIETAGE_ADDED);
                        taskHandler.sendMessage(message);
                    }
                });

            }
        });

    }

    public interface TaskFragmentCallbacks {

        void onDownloadError(Throwable throwable);
        void onNoAccess();
        void onNoConnection();
        void onDownloadFinished();
    }

    public interface MainCallbacks extends TaskFragmentCallbacks {

        void onDaysUpdated();
        void onKlassenListUpdated();
        void onFreieTageAdded();

    }
}
