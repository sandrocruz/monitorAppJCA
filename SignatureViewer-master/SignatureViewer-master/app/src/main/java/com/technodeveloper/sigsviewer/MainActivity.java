package com.technodeveloper.sigsviewer;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.time.Instant;
import android.app.Activity;
import org.bouncycastle.Monitor;
import java.security.Provider;
import java.security.Security;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import java.text.DecimalFormat;
import android.Manifest;
import android.support.v4.app.ActivityCompat;
import android.os.Build;

public class MainActivity extends Activity {

    private ProgressBar appsLoadPb;
    private LinearLayout mainLay;
    private EditText searchEt;
    private ListView appsLv;

    private List<AppInfo> appInfos = new ArrayList<>();
    private AppsListAdapter adapter;

    private String[] tempos;
    private float sum;
    private long startMemory;
    private long endMemory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        loadViews();
        listAllApps();
        setListeners();

        if (checkPermissionForWriteExternalStorage() == false || checkPermissionForReadExternalStorage() == false) {
            try {
                requestPermissionForWriteExternalStorage();
                requestPermissionForReadExternalStorage();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        setupBouncyCastle();
        startMemory = Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        long statusMemory;
        sum = Monitor.writeTimes("", "SUM");
        endMemory = Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory();
        statusMemory = endMemory - startMemory;

        if (sum > 15000000) {
            System.out.println("[M] Tempo de execução: O sistema preparou a build...");
        }
        else {
        sum = sum/1000000;
        statusMemory = statusMemory/1000;
        DecimalFormat df = new DecimalFormat("#.####");
        DecimalFormat df2 = new DecimalFormat("#.####");
        String iString = df.format(sum);
        String iString2 = df2.format(statusMemory);
        System.out.println("[M] Tempo de execução: " + iString + " ms\n");
        System.out.println("[M] Memória: " + iString2 + " KB\n");
        }

        Monitor.writeTimes("", "CLEAN");
    }

    private boolean mSaveStateOnExit = true;

    private static final int WRITE_STORAGE_PERMISSION_REQUEST_CODE = 1;
    private static final int READ_STORAGE_PERMISSION_REQUEST_CODE = 2;


    public void requestPermissionForReadExternalStorage() throws Exception {
        try {
            ActivityCompat.requestPermissions((Activity) this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    READ_STORAGE_PERMISSION_REQUEST_CODE);
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    public void requestPermissionForWriteExternalStorage() throws Exception {
        try {
            ActivityCompat.requestPermissions((Activity) this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    WRITE_STORAGE_PERMISSION_REQUEST_CODE);
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    public boolean checkPermissionForReadExternalStorage() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int result = this.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE);
            return result == PackageManager.PERMISSION_GRANTED;
        }
        return false;
    }

    public boolean checkPermissionForWriteExternalStorage() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int result = this.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            return result == PackageManager.PERMISSION_GRANTED;
        }
        return false;
    }

    public static void setupBouncyCastle() {

        final Provider provider = Security.getProvider(BouncyCastleProvider.PROVIDER_NAME);
        if (provider == null) {
            // Web3j will set up the provider lazily when it's first used.
            return;
        }
        if (provider.getClass().equals(BouncyCastleProvider.class)) {
            // BC with same package name, shouldn't happen in real life.
            return;
        }
        // Android registers its own BC provider. As it might be outdated and might not include
        // all needed ciphers, we substitute it with a known BC bundled in the app.
        // Android's BC has its package rewritten to "com.android.org.bouncycastle" and because
        // of that it's possible to have another BC implementation loaded in VM.
        Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME);
        Security.insertProviderAt(new BouncyCastleProvider(), 1);
    }

    private void loadViews() {
        appsLoadPb = findViewById(R.id.apps_load_pb);
        mainLay = findViewById(R.id.main_lay);
        searchEt = findViewById(R.id.search_et);
        appsLv = findViewById(R.id.apps_lv);
    }

    private void listAllApps() {
        new LoadAppsTask().execute();
    }

    private void setListeners() {
        searchEt.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.getFilter().filter(s.toString().toLowerCase());
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        appsLv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                AppInfo appInfo = (AppInfo) adapter.getItem(position);
                showAlertDialog(appInfo.getName(), appInfo.toString(), appInfo.getIcon());
            }
        });
    }

    private void showAlertDialog(String title, String message, Drawable icon) {
        new AlertDialog.Builder(MainActivity.this)
                .setTitle(title)
                .setMessage(message)
                .setIcon(icon)
                .show();
    }

    private class AppInfo {

        private Drawable icon;
        private Signature signature;
        private String name;
        private String packageName;
        private String signatureString;

        AppInfo() {
        }

        Drawable getIcon() {
            return icon;
        }

        void setIcon(Drawable icon) {
            this.icon = icon;
        }

        String getName() {
            return name;
        }

        void setName(String name) {
            this.name = name;
        }

        String getSignatureString() {
            return signatureString;
        }

        void setSignatureString(String signatureString) {
            this.signatureString = signatureString;
        }

        String getPackageName() {
            return packageName;
        }

        void setPackageName(String packageName) {
            this.packageName = packageName;
        }

        Signature getSignature() {
            return signature;
        }

        void setSignature(Signature signature) {
            this.signature = signature;
        }

        @NonNull
        @Override
        public String toString() {
            SignatureInfo sigInfo = new SignatureInfo(getSignature());
            return "Package name: " +
                    getPackageName() +
                    System.lineSeparator() +
                    "Signature hash: " +
                    getSignatureString() +
                    System.lineSeparator() +
                    sigInfo.getSubject().replace(", ", System.lineSeparator()).replace("=", ": ") +
                    System.lineSeparator() +
                    "Serial number: " +
                    sigInfo.getSerialNumber();
        }
    }

    private class AppsListAdapter extends BaseAdapter implements Filterable {

        private List<AppInfo> appInfosLocal;
        private List<AppInfo> appInfosBackup;
        private Context context;

        AppsListAdapter(Context context, List<AppInfo> appsInfos) {
            this.context = context;
            this.appInfosLocal = appsInfos;
            this.appInfosBackup = appsInfos;
        }

        @Override
        public int getCount() {
            return appInfosLocal.size();
        }

        @Override
        public Object getItem(int position) {
            return appInfosLocal.get(position);
        }

        @Override
        public long getItemId(int position) {
            return getItem(position).hashCode();
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            View item = convertView;
            if (item == null) {
                LayoutInflater inflater = (LayoutInflater) getApplicationContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                item = inflater.inflate(R.layout.item_app, parent, false);
            }

            AppInfo appInfo = appInfosLocal.get(position);
            if (appInfo != null) {
                ImageView iconIv = item.findViewById(R.id.app_icon_iv);
                TextView nameTv = item.findViewById(R.id.app_name_tv);
                TextView signatureTv = item.findViewById(R.id.app_sig_string_tv);

                iconIv.setImageDrawable(appInfo.getIcon());
                nameTv.setText(appInfo.getName());
                signatureTv.setText(context.getString(R.string.sig_hash).concat(appInfo.getSignatureString()));
            }

            return item;
        }

        @Override
        public Filter getFilter() {
            return new Filter() {
                @Override
                protected FilterResults performFiltering(CharSequence constraint) {
                    appInfosLocal = appInfosBackup;
                    FilterResults results = new FilterResults();
                    if (constraint == null || constraint.length() == 0) {
                        results.values = appInfosLocal;
                        results.count = appInfosLocal.size();
                    } else {
                        List<AppInfo> filteredAppInfos = new ArrayList<>();
                        for (AppInfo appInfo : appInfosLocal) {
                            String name = appInfo.getName().toLowerCase();
                            String signatureHash = appInfo.getSignatureString();
                            String sigData = new SignatureInfo(appInfo.getSignature()).getSubject().toLowerCase();
                            String packageName = appInfo.getPackageName();
                            if (name.contains(constraint) || signatureHash.contains(constraint) || sigData.contains(constraint) || packageName.contains(constraint)) {
                                filteredAppInfos.add(appInfo);
                            }
                        }
                        results.values = filteredAppInfos;
                        results.count = filteredAppInfos.size();
                    }
                    return results;
                }

                @Override
                protected void publishResults(CharSequence constraint, FilterResults results) {
                    appInfosLocal = (List<AppInfo>) results.values;
                    notifyDataSetChanged();
                }
            };
        }
    }

    @SuppressLint("StaticFieldLeak")
    private class LoadAppsTask extends AsyncTask<Void, Integer, Void> {

        private int appsInstalled;

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected Void doInBackground(Void... voids) {
            PackageManager pm = getPackageManager();
            SignatureExtractor sigEx = new SignatureExtractor(getApplicationContext());
            List<ApplicationInfo> applications = pm.getInstalledApplications(PackageManager.GET_META_DATA);
            appsInstalled = applications.size();

            int appIndex = 0;
            for (ApplicationInfo appInfoRaw : applications) {
                if (appInfoRaw != null) {
                    AppInfo appInfo = new AppInfo();

                    appInfo.setIcon(appInfoRaw.loadIcon(pm));
                    appInfo.setName(appInfoRaw.loadLabel(pm).toString());
                    appInfo.setPackageName(appInfoRaw.packageName);
                    appInfo.setSignatureString(sigEx.getSignatureString(sigEx.getSignature(appInfoRaw.packageName)));
                    appInfo.setSignature(sigEx.getSignature(appInfoRaw.packageName));

                    appInfos.add(appInfo);
                }
                appIndex++;
                publishProgress(appIndex);
            }

            Collections.sort(appInfos, new Comparator<AppInfo>() {
                @Override
                public int compare(AppInfo appInfo1, AppInfo appInfo2) {
                    String sig1 = appInfo1.getSignatureString();
                    String sig2 = appInfo2.getSignatureString();
                    return sig1.compareTo(sig2);
                }
            });

            return null;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            int progress = (int) (((double) values[0] / appsInstalled) * 100.0);
            appsLoadPb.setProgress(progress);
        }

        @Override
        protected void onPostExecute(Void v) {
            adapter = new AppsListAdapter(MainActivity.this, appInfos);
            appsLv.setAdapter(adapter);
            appsLv.setTextFilterEnabled(true);
            appsLoadPb.setVisibility(View.GONE);
            mainLay.setVisibility(View.VISIBLE);
        }

    }
}
