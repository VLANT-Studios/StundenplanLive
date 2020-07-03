package de.conradowatz.jkgvertretung.variables;

import com.raizlabs.android.dbflow.annotation.Column;
import com.raizlabs.android.dbflow.annotation.PrimaryKey;
import com.raizlabs.android.dbflow.annotation.Table;
import com.raizlabs.android.dbflow.structure.BaseModel;

import java.util.Date;

public abstract class Termin extends BaseModel{

    public abstract Date getDate();

    public abstract void setDate(Date date);
}
