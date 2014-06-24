
package org.x2ools.xappsearchlib;

import android.app.ActivityManager;
import android.app.ActivityManager.RecentTaskInfo;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

import org.x2ools.xappsearchlib.model.SearchItem;
import org.x2ools.xappsearchlib.tools.IconCache;

import java.util.ArrayList;
import java.util.List;

public class AppsGridView extends GridView {
    private AppsAdapter mAppsAdapter;

    private static final String TAG = "AppsGridView";

    private static final boolean DEBUG = false;

    private Context mContext;

    private static T9Search sT9Search;

    private ArrayList<SearchItem> apps;

    private PackageManager mPackageManager;

    private ActivityManager mActivityManager;

    private LayoutInflater mLayoutInflater;

    private String mFilterStr = null;

    private HideViewCallback mCallback;

    private IconCache mIconCache;

    private boolean mAllMode = false;

    private Handler mHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case T9Search.MSG_DATA_LOADED:
                    if (!TextUtils.isEmpty(mFilterStr)) {
                        filter(mFilterStr);
                    } else if (mAllMode) {
                        setAllApplicationsData();
                    }
                    break;

                default:
                    break;
            }
        }
    };

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (sT9Search != null) {
                sT9Search.reloadData();
            }
        }
    };

    public AppsGridView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        mPackageManager = context.getPackageManager();
        mActivityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        mLayoutInflater = LayoutInflater.from(context);
        mIconCache = new IconCache(context);
        sT9Search = new T9Search(context, mIconCache, mHandler);
        setApplicationsData();
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.PACKAGE_ADDED");
        filter.addAction("android.intent.action.PACKAGE_CHANGED");
        filter.addAction("android.intent.action.PACKAGE_REMOVED");
        filter.addDataScheme("package");
        context.registerReceiver(mReceiver, filter);
    }

    @Override
    protected void onVisibilityChanged(View changedView, int visibility) {
        Log.d(TAG, "visibility changed to " + visibility);
        super.onVisibilityChanged(changedView, visibility);
    }

    public void setApplicationsData() {
        new GetRecentTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public void setAllApplicationsData() {
        List<SearchItem> allApps = sT9Search.getAll();
        mAppsAdapter = new AppsAdapter(allApps);
        setAdapter(mAppsAdapter);
        mAppsAdapter.notifyDataSetChanged();
        mAllMode = true;
    }

    public boolean isAllMode() {
        return mAllMode;
    }

    public boolean startAcivityByIndex(int index) {
        if (DEBUG) {
            dumpApplications();
        }
        if (index < apps.size()) {
            SearchItem item = apps.get(index);
            if (item.getType() == 0) {
                Intent i = mPackageManager.getLaunchIntentForPackage(item.getPackageName());
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                mContext.startActivity(i);
                Log.d(TAG, "start " + item.getPackageName());
                return true;
            } else {
                Intent intent = new Intent("android.intent.action.CALL", Uri.parse("tel:"
                        + item.getPhoneNumber()));
                mContext.startActivity(intent);
                return true;
            }
        }
        return false;
    }

    public void dumpApplications() {
        for (SearchItem item : apps) {
            Log.d(TAG, "info.packageName " + item.getPackageName());
        }
    }

    public void filter(String string) {
        mFilterStr = string;
        if (sT9Search == null)
            return;
        if (TextUtils.isEmpty(string)) {
            new GetRecentTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            return;
        }
        new SearchTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, string);
    }

    private class SearchTask extends AsyncTask<String, Void, List<SearchItem>> {

        @Override
        protected List<SearchItem> doInBackground(String... params) {
            return sT9Search.search(params[0]);
        }

        @Override
        protected void onPostExecute(List<SearchItem> result) {
            if (result != null) {
                mAppsAdapter = new AppsAdapter(result);
                setAdapter(mAppsAdapter);
                mAppsAdapter.notifyDataSetChanged();
            } else {
                setAdapter(null);
                mAppsAdapter.notifyDataSetChanged();
            }
            mAllMode = false;
            super.onPostExecute(result);
        }

    }

    private class GetRecentTask extends AsyncTask<Void, Void, List<SearchItem>> {

        @Override
        protected List<SearchItem> doInBackground(Void... params) {
            return getRecentApps();
        }

        @Override
        protected void onPostExecute(List<SearchItem> result) {
            mAppsAdapter = new AppsAdapter(result);
            setAdapter(mAppsAdapter);
            mAppsAdapter.notifyDataSetChanged();
            mAllMode = false;
            super.onPostExecute(result);
        }

    }

    private ArrayList<SearchItem> getRecentApps() {
        List<RecentTaskInfo> recentTasks = mActivityManager.getRecentTasks(9,
                ActivityManager.RECENT_IGNORE_UNAVAILABLE | ActivityManager.RECENT_WITH_EXCLUDED);
        ArrayList<SearchItem> recents = new ArrayList<SearchItem>();
        if (DEBUG) {
            Log.d(TAG, "recentTasks:  " + recentTasks);
        }
        if (recentTasks != null) {
            for (RecentTaskInfo recentInfo : recentTasks) {
                try {
                    if (DEBUG) {
                        Log.d(TAG, "recentInfo.baseIntent:  "
                                + recentInfo.baseIntent.getComponent().getPackageName());

                    }
                    ApplicationInfo info = mPackageManager.getApplicationInfo(recentInfo.baseIntent
                            .getComponent().getPackageName(), 0);
                    if (mPackageManager.getLaunchIntentForPackage(info.packageName) == null)
                        continue;
                    boolean added = false;
                    for (SearchItem tmp : recents) {
                        if (tmp.getPackageName().equals(info.packageName))
                            added = true;
                    }
                    if (!added) {

                        if ((recentInfo.baseIntent != null)
                                && ((recentInfo.baseIntent.getFlags() & Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS) != 0)) {
                            Log.d(TAG, "This task has flag = FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS");
                            continue;
                        }

                        SearchItem item = new SearchItem();
                        item.setName(info.loadLabel(mPackageManager).toString());
                        item.setPackageName(info.packageName);
                        item.setId(recentInfo.id);
                        item.setBaseIntent(recentInfo.baseIntent);
                        mIconCache.getIcon(item, info);
                        recents.add(item);
                    }
                } catch (NameNotFoundException e) {
                    // Log.e(TAG, "cannot find package", e);
                }
            }
        }

        return recents;
    }

    private boolean isTaskInRecentList(SearchItem item) {
        final int taskId = item.getId();
        final Intent intent = item.getBaseIntent();
        if ((intent != null)
                && ((intent.getFlags() & Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS) != 0)) {
            // / M: Don't care exclude-from-recent app.
            Log.d(TAG, "This task has flag = FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS");
            return true;
        }
        final ActivityManager am = (ActivityManager) mContext
                .getSystemService(Context.ACTIVITY_SERVICE);
        final List<ActivityManager.RecentTaskInfo> recentTasks = am.getRecentTasks(20,
                ActivityManager.RECENT_IGNORE_UNAVAILABLE);

        for (int i = 0; i < recentTasks.size(); ++i) {
            final ActivityManager.RecentTaskInfo info = recentTasks.get(i);
            if (info.id == taskId) {
                return true;
            }
        }

        Log.d(TAG, "This task is not in recent list for " + taskId);

        return false;
    }

    public class AppsAdapter extends BaseAdapter {

        private List<SearchItem> mItems;

        public AppsAdapter(List<SearchItem> items) {
            mItems = items;
        }

        @Override
        public int getCount() {
            return mItems.size();
        }

        @Override
        public Object getItem(int position) {
            return mItems.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder viewHolder = null;
            final SearchItem item = (SearchItem) getItem(position);
            if (convertView == null) {
                convertView = mLayoutInflater.inflate(R.layout.package_item, null);
                viewHolder = new ViewHolder();

                viewHolder.textTitle = (TextView) convertView.findViewById(R.id.textTitle);
                viewHolder.icon = (ImageView) convertView.findViewById(R.id.icon);
                convertView.setTag(viewHolder);

            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }

            convertView.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View arg0) {
                    if (item.getType() == 1) {
                        Intent intent = new Intent("android.intent.action.CALL", Uri.parse("tel:"
                                + item.getPhoneNumber()));
                        mContext.startActivity(intent);
                    } else {
                        if (item.getId() >= 0 && isTaskInRecentList(item)) {
                            mActivityManager.moveTaskToFront(item.getId(),
                                    ActivityManager.MOVE_TASK_WITH_HOME);
                            Log.v(TAG, "Move Task To Front for " + item.getId());
                        } else if (item.getBaseIntent() != null) {
                            Intent intent = item.getBaseIntent();
                            intent.addFlags(Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY
                                    | Intent.FLAG_ACTIVITY_TASK_ON_HOME
                                    | Intent.FLAG_ACTIVITY_NEW_TASK);
                            Log.v(TAG, "Starting activity " + intent);
                            try {
                                mContext.startActivity(intent);
                            } catch (SecurityException e) {
                                Log.e(TAG, "Recents does not have the permission to launch "
                                        + intent,
                                        e);
                                mContext.startActivity(mPackageManager.getLaunchIntentForPackage(
                                        item.getPackageName()).addFlags(
                                        Intent.FLAG_ACTIVITY_NEW_TASK));
                            } catch (ActivityNotFoundException e) {
                                Log.e(TAG, "Error launching activity " + intent, e);
                                mContext.startActivity(mPackageManager.getLaunchIntentForPackage(
                                        item.getPackageName()).addFlags(
                                        Intent.FLAG_ACTIVITY_NEW_TASK));
                            }

                        } else {
                            mContext.startActivity(mPackageManager.getLaunchIntentForPackage(
                                    item.getPackageName()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                        }
                        mCallback.hideView();
                    }

                }

            });

            convertView.setOnLongClickListener(new OnLongClickListener() {

                @Override
                public boolean onLongClick(View arg0) {
                    Log.d(TAG, "onLongClick ");
                    if (item.getType() == 1) {
                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        Uri uri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI,
                                item.getId() + "");
                        intent.setData(uri);
                        mContext.startActivity(intent);
                    } else {
                        Intent i = new Intent();
                        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        i.setAction("android.settings.APPLICATION_DETAILS_SETTINGS");
                        i.setData(Uri.parse("package:" + item.getPackageName()));
                        mContext.startActivity(i);
                        mCallback.hideView();
                    }

                    return true;
                }

            });
            viewHolder.textTitle.setText(item.getName());
            viewHolder.icon.setImageDrawable(item.getPhoto());
            return convertView;
        }
    }

    public interface HideViewCallback {
        public void hideView();
    }

    public void setCallback(HideViewCallback callback) {
        mCallback = callback;
    }

    static class ViewHolder {
        TextView textTitle;

        ImageView icon;
    }
}
