package com.aokp.backup.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Fragment;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import com.aokp.backup.AOKPBackup;
import com.aokp.backup.BackupService;
import com.aokp.backup.BackupService.BackupFileSystemChange;
import com.aokp.backup.R;
import com.aokp.backup.R.drawable;
import com.aokp.backup.backup.Backup;
import com.aokp.backup.backup.BackupFactory;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by roman on 6/3/13.
 */
public class BackupDetailFragment extends Fragment {

    Backup mBackup;
    ListView mListView;

    public static BackupDetailFragment newDetailsFragment(Backup backup) {
        Bundle b = new Bundle();

        BackupDetailFragment f = new BackupDetailFragment();
        b.putString("path", backup.getZipFile().getAbsolutePath());
        f.setArguments(b);

        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments().containsKey("path")) {
            String path = getArguments().getString("path");
            try {
                mBackup = BackupFactory.fromZipOrDirectory(getActivity(), new File(path));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        setHasOptionsMenu(true);


    }

    private List getListItems() {
        List items = new ArrayList();
        items.add(new SectionHeader("Backup details"));
        items.add(new SectionRow<Pair>(new Pair("Backup type", mBackup.isOldStyleBackup() ? "Old" : "New")));

        items.add(new SectionRow<Pair>(new Pair("Date", DateFormat.getDateFormat(getActivity()).format(mBackup.getBackupDate()))));

        if (mBackup.getBackupSdkLevel() > 0)
            items.add(new SectionRow<Pair>(new Pair("SDK version", mBackup.getBackupSdkLevel())));

        if (mBackup.getDevice() != null)
            items.add(new SectionRow<Pair>(new Pair("Device", mBackup.getDevice())));

        if (mBackup.getRomVersion() != null)
            items.add(new SectionRow<Pair>(new Pair("ROM Version", mBackup.getRomVersion())));

        if (mBackup.hasCategoryFilter()) {

            String[] categories = getResources().getStringArray(BackupFactory.getCategoryArrayResourceId());

            String cats = new String();
            for (Integer categoryToBackup : mBackup.getCategoryFilter()) {
                cats += categories[categoryToBackup] + "\n";
            }
            items.add(new SectionRow<Pair>(new Pair("Categories", cats)));
        }

        return items;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.backup_details_fragment, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.delete_backup:
                confirmDelete();
                return true;
            case R.id.restore_backup:
                confirmRestore();
                return true;

        }


        return super.onOptionsItemSelected(item);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.backup_detail_fragment, container, false);

        mListView = (ListView) root.findViewById(R.id.listView);
        mListView.setAdapter(new SummaryListAdapter(getActivity(), getListItems()));

        TextView title = (TextView) root.findViewById(R.id.title);
        title.setText(mBackup.getName());

        return root;
    }

    private void confirmRestore() {
        new Builder(getActivity())
                .setTitle("Restore backup?")
                .setMessage("Want to restore '" + mBackup.getName() + "'?")
                .setPositiveButton("Yes", new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent restore = new Intent(getActivity(), BackupService.class);
                        restore.setAction(BackupService.ACTION_RESTORE_BACKUP);
                        restore.putExtra("path", mBackup.getZipFile().getAbsolutePath());

                        getActivity().startService(restore);
                    }
                })
                .setNegativeButton("No", null)
                .create()
                .show();
    }

    private void confirmDelete() {
        new Builder(getActivity())
                .setTitle("Delete backup?")
                .setMessage("Want to delete '" + mBackup.getName() + "'?")
                .setPositiveButton("Yes", new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        new AsyncTask<String, Void, Boolean>() {
                            @Override
                            protected Boolean doInBackground(String... params) {
//                                try {
//                                    deleteMe = BackupFactory.fromZipOrDirectory(getActivity(), mBackup.getZipFile());
//                                } catch (IOException e) {
//                                    e.printStackTrace();
//                                    return false;
//                                }

                                return mBackup.deleteFromDisk();
                            }

                            @Override
                            protected void onPostExecute(Boolean success) {
                                super.onPostExecute(success);
                                Toast.makeText(getActivity(), "Backup deleted", Toast.LENGTH_SHORT).show();
                                getFragmentManager().popBackStack();
                            }
                        }.execute();
                    }
                })
                .setNegativeButton("No", null)
                .create()
                .show();
    }

    public class SummaryListAdapter extends BaseAdapter {

        private List mItems;

        public static final int VIEW_TYPE_SECTION_HEADER = 0;
        public static final int VIEW_TYPE_SECTION_ROW_PAIR = 1;
        //        public static final int VIEW_TYPE_SECTION_ROW = 2;

        @Override
        public int getViewTypeCount() {
            return 2;
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

        Activity mContext;

        public SummaryListAdapter(Activity mContext, List mItems) {
            this.mContext = mContext;
            this.mItems = mItems;
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
        public boolean areAllItemsEnabled() {
            return false;
        }

        @Override
        public boolean isEnabled(int position) {
            return getItemViewType(position) != VIEW_TYPE_SECTION_HEADER;
        }

        @Override
        public int getItemViewType(int position) {
            Object item = getItem(position);
            if (item instanceof SectionHeader) {
                return VIEW_TYPE_SECTION_HEADER;
            } else { // if (item instanceof SectionRowPair) {
                return VIEW_TYPE_SECTION_ROW_PAIR;
            }
            // return -1;
        }

        @Override
        public long getItemId(int position) {
            Object item = getItem(position);
            return item.hashCode();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            switch (getItemViewType(position)) {

                case VIEW_TYPE_SECTION_HEADER: {
                    if (convertView == null) {
                        convertView = getActivity().getLayoutInflater().inflate(
                                R.layout.list_row_scan_summary_header, parent, false);
                    }
                    TextView label = (TextView) convertView.findViewById(R.id.label);

                    label.setText(((SectionHeader) getItem(position)).title);
                    return convertView;
                }

                case VIEW_TYPE_SECTION_ROW_PAIR: {
                    if (convertView == null) {
                        convertView = getActivity().getLayoutInflater().inflate(
                                R.layout.list_row_scan_summary_pair, parent, false);
                    }
                    TextView key = (TextView) convertView.findViewById(R.id.key);
                    TextView value = ((TextView) convertView.findViewById(R.id.value));

                    SectionRow<Pair> row = (SectionRow<Pair>) getItem(position);

                    key.setText((String) row.row.first);
                    value.setText((row.row.second + ""));
                    return convertView;
                }
            }

            return null;
        }
    }

    public static class SummaryRowHolder {
        public TextView key;
        public TextView value;
    }

    public class SectionHeader {
        public String title;

        public SectionHeader(String title) {
            this.title = title;
        }
    }

    public class SectionRow<T> {
        public T row;

        public SectionRow(T row) {
            this.row = row;
        }
    }
}