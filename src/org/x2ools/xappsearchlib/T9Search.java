
package org.x2ools.xappsearchlib;

import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Handler;
import android.provider.ContactsContract;
import android.text.TextUtils;

import org.x2ools.xappsearchlib.model.SearchItem;
import org.x2ools.xappsearchlib.tools.IconCache;
import org.x2ools.xappsearchlib.tools.ToPinYinUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

public class T9Search {

    private static final boolean DEBUG = false;

    private static final String TAG = "T9Search";

    protected static final int MSG_DATA_LOADED = 0;

    private Context mContext;

    private PackageManager mPackageManager;

    private ContentResolver mResolver;

    private Handler mHandler;

    private List<SearchItem> mAllItems = new ArrayList<SearchItem>();
    private HashMap<String, SearchItem> mAddedContact = new HashMap<String, SearchItem>();

    private IconCache mIconCache;

    private String mPrevInput;
    private ArrayList<SearchItem> mSearchResult = new ArrayList<SearchItem>();
    private ArrayList<SearchItem> mPrevResult = new ArrayList<SearchItem>();

    public T9Search(Context context, IconCache iconCache, Handler handler) {
        mContext = context;
        mHandler = handler;
        mPackageManager = context.getPackageManager();
        mResolver = context.getContentResolver();
        mIconCache = iconCache;
        new GetAllAyncTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private class GetAllAyncTask extends AsyncTask<Void, Void, List<SearchItem>> {

        @Override
        protected List<SearchItem> doInBackground(Void... params) {
            List<SearchItem> all = new ArrayList<SearchItem>();

            List<ApplicationInfo> mApplications = new ArrayList<ApplicationInfo>();
            mApplications.addAll(mPackageManager.getInstalledApplications(0));
            for (ApplicationInfo appinfo : mApplications) {
                if (mPackageManager.getLaunchIntentForPackage(appinfo.packageName) == null)
                    continue;
                SearchItem appitem = new SearchItem();
                appitem.setType(0);
                appitem.setName(appinfo.loadLabel(mPackageManager).toString());
                appitem.setPinyin(ToPinYinUtils.getPinyinNum(appitem.getName(), false));
                appitem.setFullpinyin(ToPinYinUtils.getPinyinNum(appitem.getName(), true));
                appitem.setPackageName(appinfo.packageName);
                mIconCache.getIcon(appitem, appinfo);
                all.add(appitem);
            }

            Cursor cursor = mResolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    null,
                    null, null, null);
            while (cursor.moveToNext()) {
                int id = cursor.getInt(cursor
                        .getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID));
                String name = cursor.getString(cursor
                        .getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
                String phoneNumber = cursor.getString(cursor
                        .getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                String photoUri = cursor.getString(cursor
                        .getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_URI));

                SearchItem item = new SearchItem();
                item.setType(1);
                item.setId(id);
                item.setName(name);
                item.setPinyin(ToPinYinUtils.getPinyinNum(item.getName(), false));
                item.setFullpinyin(ToPinYinUtils.getPinyinNum(item.getName(), true));
                item.setPhoneNumber(phoneNumber);
                mIconCache.getIcon(item, mResolver, photoUri);

                if (mAddedContact.containsKey(name)) {
                    SearchItem added = mAddedContact.get(name);
                    if (added.getPhoneNumber().equals(phoneNumber)) {
                        continue;
                    } else {
                        item.setName(name + "(" + phoneNumber + ")");
                    }
                }

                all.add(item);
                mAddedContact.put(name, item);
            }
            cursor.close();

            return all;
        }

        @Override
        protected void onPostExecute(List<SearchItem> result) {
            mAllItems = result;
            mHandler.sendEmptyMessage(MSG_DATA_LOADED);
            super.onPostExecute(result);
        }

    }

    public void reloadData() {
        new GetAllAyncTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public List<SearchItem> getAll() {
        Collections.sort(mAllItems, new NameComparator());
        return mAllItems;
    }

    public class NameComparator implements Comparator<SearchItem> {
        @Override
        public int compare(SearchItem lhs, SearchItem rhs) {
            int type = lhs.getType() - rhs.getType();
            int name = lhs.getName().compareTo(rhs.getName());
            if (type != 0) {
                return type;
            } else {
                return name;
            }
        }

    }

    public List<SearchItem> search(String number) {
        mSearchResult.clear();
        int pos = 0;
        boolean newQuery = mPrevInput == null || number.length() <= mPrevInput.length();
        for (SearchItem item : (newQuery ? mAllItems : mPrevResult)) {
            pos = item.getPinyin().indexOf(number);
            if (pos != -1) {
                mSearchResult.add(item);
                continue;
            }

            pos = item.getFullpinyin().indexOf(number);
            if (pos != -1) {
                mSearchResult.add(item);
                continue;
            }

            if (!TextUtils.isEmpty(item.getPhoneNumber())) {
                pos = item.getPhoneNumber().indexOf(number);
                if (pos != -1) {
                    mSearchResult.add(item);
                    continue;
                }
            }
        }
        mPrevResult.clear();
        mPrevInput = number;
        if (mSearchResult.size() > 0) {
            mPrevResult.addAll(mSearchResult);
            return mSearchResult;
        }
        return null;
    }

}
