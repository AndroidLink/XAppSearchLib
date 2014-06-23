
package org.x2ools.xappsearchlib;

import android.content.ContentValues;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Handler;

import org.x2ools.xappsearchlib.database.DBHelper;
import org.x2ools.xappsearchlib.model.SearchItem;
import org.x2ools.xappsearchlib.tools.ToPinYinUtils;

import java.util.ArrayList;
import java.util.List;

public class T9Search {

    private static final boolean DEBUG = false;

    private static final String TAG = "T9Search";

    protected static final int MSG_DATA_LOADED = 0;

    private Context mContext;

    private PackageManager mPackageManager;

    private DBHelper mDbHelper;

    private Handler mHandler;

    private List<SearchItem> results = new ArrayList<SearchItem>();

    public T9Search(Context context, Handler handler) {
        mContext = context;
        mHandler = handler;
        mPackageManager = context.getPackageManager();
        mDbHelper = new DBHelper(mContext);
        new GetAllAyncTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private class GetAllAyncTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            List<ApplicationInfo> mApplications = new ArrayList<ApplicationInfo>();

            mApplications.addAll(mPackageManager.getInstalledApplications(0));
            SQLiteDatabase db = mDbHelper.getWritableDatabase();
            for (ApplicationInfo appinfo : mApplications) {
                if (mPackageManager.getLaunchIntentForPackage(appinfo.packageName) == null)
                    continue;
                SearchItem appitem = new SearchItem();
                appitem.setName(appinfo.loadLabel(mPackageManager).toString());
                appitem.setPinyin(ToPinYinUtils.getPinyinNum(appitem.getName(), false));
                appitem.setFullpinyin(ToPinYinUtils.getPinyinNum(appitem.getName(), true));
                appitem.setPackageName(appinfo.packageName);
                appitem.setIcon(appinfo.icon);

                Cursor c = db.query(DBHelper.TABLE, new String[] {
                        DBHelper.COLUME_PACKAGENAME
                }, "packagename = ?",
                        new String[] {
                            appitem.getPackageName()
                        }, null, null, null);

                ContentValues values = new ContentValues();
                values.put(DBHelper.COLUME_NAME, appitem.getName());
                values.put(DBHelper.COLUME_PINYIN, appitem.getPinyin());
                values.put(DBHelper.COLUME_FULLPINYIN, appitem.getFullpinyin());
                values.put(DBHelper.COLUME_ICON, appitem.getIcon());

                if (c != null && c.moveToNext()) {
                    db.update(DBHelper.TABLE, values, "packagename = ?",
                            new String[] {
                                appitem.getPackageName()
                            });
                } else {
                    values.put(DBHelper.COLUME_PACKAGENAME, appitem.getPackageName());
                    db.insert(DBHelper.TABLE, null, values);
                }

                c.close();
            }

            db.close();

            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            mHandler.sendEmptyMessage(MSG_DATA_LOADED);
        }
    }

    public void reloadData() {
        new GetAllAyncTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public List<SearchItem> search(String number) {

        String selection = DBHelper.COLUME_PINYIN + " like ? OR " + DBHelper.COLUME_FULLPINYIN
                + " like ?";
        String[] args = new String[] {
                "%" + number + "%", "%" + number + "%"
        };
        return query(selection, args);
    }

    public List<SearchItem> getAll() {
        return query(null, null);
    }

    private List<SearchItem> query(String selection, String[] selectionArgs) {
        results.clear();
        SQLiteDatabase db = mDbHelper.getReadableDatabase();
        Cursor c = db.query(DBHelper.TABLE, null, selection, selectionArgs, null, null, null);
        if (c != null) {
            while (c.moveToNext()) {
                SearchItem item = new SearchItem();
                item.setIcon(c.getInt(c.getColumnIndex(DBHelper.COLUME_ICON)));
                item.setName(c.getString(c.getColumnIndex(DBHelper.COLUME_NAME)));
                item.setPackageName(c.getString(c.getColumnIndex(DBHelper.COLUME_PACKAGENAME)));
                results.add(item);
            }
        }

        return results;
    }

}
