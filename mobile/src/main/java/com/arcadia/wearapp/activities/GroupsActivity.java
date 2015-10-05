package com.arcadia.wearapp.activities;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.widget.AbsListView;
import android.widget.ListView;

import com.arcadia.wearapp.adapters.GroupsAdapter;
import com.arcadia.wearapp.MobileApp;
import com.arcadia.wearapp.realm_objects.ParseGroup;
import com.arcadia.wearapp.R;

import io.realm.Realm;
import io.realm.RealmResults;

public class GroupsActivity extends AppCompatActivity implements SwipeRefreshLayout.OnRefreshListener {
    public static final String Description = "description";
    public static final String Title = "title";
    private MobileApp app;
    private ListView listView;
    private SwipeRefreshLayout mSwipeRefreshLayout;

    private GroupsAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_groups);

        mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.refresh);
        mSwipeRefreshLayout.setOnRefreshListener(this);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        app = (MobileApp) getApplication();

        listView = (ListView) findViewById(R.id.groups_list);
//        listView.setOnItemClickListener(onClickListener);
        listView.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                int topRowVerticalPosition = (listView == null || listView.getChildCount() == 0) ?
                        0 : listView.getChildAt(0).getTop();
                mSwipeRefreshLayout.setEnabled(firstVisibleItem == 0 && topRowVerticalPosition >= 0);
            }
        });

        if (app!=null)
            app.getFromParse();
    }

//    private void prepareToUpdate() {
//        if (!isOnline()) {
//            AlertDialog.Builder build = new AlertDialog.Builder(GroupsActivity.this);
//            build.setCancelable(true)
//                    .setNegativeButton("OK", new DialogInterface.OnClickListener() {
//                        @Override
//                        public void onClick(DialogInterface dialog, int which) {
//                            dialog.cancel();
//                        }
//                    })
//                    .setTitle("No Internet connection")
//                    .setMessage("Please connect to internet and try again!");
//            AlertDialog warning = build.create();
//            warning.show();
////        } else {
////            ConnectDataBase connect = new ConnectDataBase(this);
////            connect.startFetch();
////            Realm realm = Realm.getInstance(this);
////            resultList = realm.allObjects(ParseGroup.class);
////            updateList = new ArrayList<>();
////            for (ParseGroup table : resultList)
////                if (!table.getOldFileVersion().equals(table.getNewFileVersion()))
////                    if (new File(getFilesDir(), table.getPdf_FileName()).exists())
////                        updateList.add(table);
////            dataChange();
////            if (!updateList.isEmpty())
////                if (isAutoUpdate)
////                    startUpdateAll();
////                else {
////                    AlertDialog.Builder builder = new AlertDialog.Builder(GroupsActivity.this);
////                    builder.setCancelable(true)
////                            .setMessage(String.format("Some files (%d) have a new version. Update all?", updateList.size()))
////                            .setIcon(R.drawable.refresh)
////                            .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
////                                @Override
////                                public void onClick(DialogInterface dialog, int which) {
////                                    dialog.cancel();
////                                }
////                            })
////                            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
////                                @Override
////                                public void onClick(DialogInterface dialog, int which) {
////                                    startUpdateAll();
////                                }
////                            })
////                            .setTitle("Update Files");
////                    AlertDialog alert = builder.create();
////                    alert.show();
////                }
////            realm.close();
//        }
//    }

    public boolean isOnline() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo nInfo = cm.getActiveNetworkInfo();
        return nInfo != null && nInfo.isConnected();
    }

    public void dataChange() {

        Realm realm = Realm.getInstance(this);
        RealmResults<ParseGroup> resultList = realm.allObjectsSorted(ParseGroup.class, "title", true);
        realm.close();
        adapter = new GroupsAdapter(this, resultList);
        listView.setAdapter(adapter);
    }

    @Override
    public void onRefresh() {
        mSwipeRefreshLayout.setRefreshing(true);
        mSwipeRefreshLayout.postDelayed(new Runnable() {
            @Override
            public void run() {
//                prepareToUpdate();
                if (app != null)
                    app.getFromParse();
                dataChange();
                mSwipeRefreshLayout.setRefreshing(false);
            }
        }, 1200);
    }

    protected void onPostResume() {
        super.onPostResume();
        dataChange();
    }
}
