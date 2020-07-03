package de.conradowatz.jkgvertretung.variables;

import com.raizlabs.android.dbflow.annotation.Column;
import com.raizlabs.android.dbflow.annotation.QueryModel;

@QueryModel(database = AppDatabase.class)
public class AverageZensur {

    @Column
    public double avgZensur;
}
