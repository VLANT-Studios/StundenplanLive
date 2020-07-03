package de.conradowatz.jkgvertretung.variables;

import com.raizlabs.android.dbflow.annotation.Database;
import com.raizlabs.android.dbflow.annotation.Migration;
import com.raizlabs.android.dbflow.sql.SQLiteType;
import com.raizlabs.android.dbflow.sql.migration.AlterTableMigration;

@Database(name = AppDatabase.NAME, version = AppDatabase.VERSION, foreignKeyConstraintsEnforced = true, backupEnabled = true)
public class AppDatabase {

    public static final String NAME = "AppDatabase";
    public static final int VERSION = 2;

    @Migration(version = 2, database = AppDatabase.class)
    public static class AddEmailToUserMigration extends AlterTableMigration<OnlineTag> {

        public AddEmailToUserMigration(Class<OnlineTag> table) {
            super(table);
        }

        @Override
        public void onPreMigrate() {
            addColumn(SQLiteType.TEXT, "infotext");
        }
    }
}
