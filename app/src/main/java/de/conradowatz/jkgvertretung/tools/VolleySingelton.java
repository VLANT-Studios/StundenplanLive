package de.conradowatz.jkgvertretung.tools;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;

import de.conradowatz.jkgvertretung.MyApplication;

public class VolleySingelton {

    private static VolleySingelton sInstance = null;
    private RequestQueue requestQueue;

    private VolleySingelton() {

        requestQueue = Volley.newRequestQueue(MyApplication.getAppContext());
    }

    public static VolleySingelton getsInstance() {

        if (sInstance == null) sInstance = new VolleySingelton();
        return sInstance;
    }

    public RequestQueue getRequestQueue() {

        return requestQueue;
    }
}
