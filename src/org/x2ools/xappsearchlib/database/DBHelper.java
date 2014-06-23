
package org.x2ools.xappsearchlib.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;

public class DBHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "search.db";
    private static final int VERSION = 1;

    public static final String TABLE = "search";
    public static final String COLUME_NAME = "name";
    public static final String COLUME_TYPE = "type";
    public static final String COLUME_PINYIN = "pinyin";
    public static final String COLUME_FULLPINYIN = "fullpinyin";
    public static final String COLUME_PACKAGENAME = "packagename";
    public static final String COLUME_ICON = "icon";
    public static final String COLUME_PHOTO = "photo";
    public static final String COLUME_PHONE = "phone";

    public DBHelper(Context context) {
        super(context, DATABASE_NAME, null, VERSION);
    }

    public DBHelper(Context context, String name, CursorFactory factory, int version) {
        super(context, name, factory, version);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE + "("
                + "id integer primary key autoincrement,"
                + COLUME_NAME + " varchar,"
                + COLUME_TYPE + " integer,"
                + COLUME_PINYIN + " varchar,"
                + COLUME_FULLPINYIN + " varchar,"
                + COLUME_PACKAGENAME + " varchar,"
                + COLUME_ICON + " integer,"
                + COLUME_PHOTO + " varchar,"
                + COLUME_PHONE + " varchar)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE);
        onCreate(db);
    }

}
