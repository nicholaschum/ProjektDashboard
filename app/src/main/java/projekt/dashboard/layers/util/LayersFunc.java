package projekt.dashboard.layers.util;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.util.Log;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;

import org.apache.commons.io.FileUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.StringTokenizer;

import projekt.dashboard.layers.R;


/**
 * Created by Aditya on 5/10/2016.
 */
public class LayersFunc {

    static Context context;
    final static String PREFS_NAME = "MyPrefsFile";
    static String link64 = "https://github.com/nicholaschum/ProjektDashboard/raw/resources/aapt-64";
    static String link = "https://github.com/nicholaschum/ProjektDashboard/raw/resources/aapt";
    public static String vendor = "/system/vendor/overlay";
    public static String mount = "/system";
    public static boolean downloaded = true;
    public static String themeframework = "Nill";
    public static String themesystemui = "Nill";

    public LayersFunc(Context contextxyz) {
        context = contextxyz;
    }

    public static void DownloadFirstResources(final Context context) {
        changeVendorAndMount();
        findFrameworkFile();
        findSystemUIFile();
        Log.e("Final Framework", themeframework);
        Log.e("Final SystemUI", themesystemui);
        File aa = new File("/system/bin/aapt");
        if (aa.exists()) {

        } else {
            if (isNetworkAvailable(context)) {
                Log.e("Switcher", "First time");
                Log.e("DownloadAAPT", "Calling Function");
                downloaded = true;
                downloadAAPT(context);
            } else {
                MaterialDialog md = new MaterialDialog.Builder(context)
                        .title("We Need to Download Some Resources")
                        .content("Please connect to the internet and meet us back here")
                        .positiveText("Open Settings")
                        .negativeText("Cancel")
                        .onPositive(new MaterialDialog.SingleButtonCallback() {
                            @Override
                            public void onClick(MaterialDialog dialog, DialogAction which) {
                                Intent intent_rrolayers = context.getPackageManager().getLaunchIntentForPackage("com.android.settings");
                                context.startActivity(intent_rrolayers);
                                downloaded = false;
                            }
                        })
                        .onNegative(new MaterialDialog.SingleButtonCallback() {
                            @Override
                            public void onClick(MaterialDialog dialog, DialogAction which) {
                                downloaded = false;
                            }
                        })
                        .dismissListener(new DialogInterface.OnDismissListener() {
                            @Override
                            public void onDismiss(DialogInterface dialog) {
                                downloaded = false;
                            }
                        })
                        .show();
            }

        }
    }

    private static void changeVendorAndMount() {
        if (checkbitphone()) {
            Log.e("Checking", "64 Bit Active");
            Log.e("64 bit Device ", Build.DEVICE + " Found,now changing the vendor and mount");
            vendor = "/vendor/overlay";
            mount = "/vendor";
            Log.e("64 bit Device ", Build.DEVICE + " changed the vendor and mount");
        } else {
            Log.e("Checking", "32 Bit Active");
            Log.e("32 bit Device ", Build.DEVICE + " Found,now changing the vendor and mount");
            vendor = "/system/vendor/overlay";
            mount = "/system";
            Log.e("32 bit Device ", Build.DEVICE + " changed the vendor and mount");
        }
    }

    private static boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) context.getSystemService(
                Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    public static boolean checkbitphone() {
        Log.e("Checkbitphone", "Function Called");
        Log.e("Checkbitphone", "Function Started");
        String[] bit = Build.SUPPORTED_32_BIT_ABIS;
        String[] bit64 = Build.SUPPORTED_64_BIT_ABIS;
        int flag = 0;
        try {
            if (bit64[0] != null) {
                Log.e("Checkbitphone", "64 Found");
                Log.e("Checkbitphone", "Checking if its one from FAB");
                if (Build.DEVICE.equals("flounder") || Build.DEVICE.equals("flounder_lte") || Build.DEVICE.equals("angler") || Build.DEVICE.equals("bullhead")) {
                    Log.e("64 bit Device ", Build.DEVICE + " Found,now returning");
                    Log.e("Checkbitphone", "Function Stopped");
                    return true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            if (flag == 0) {
                if (bit[0] != null) {
                    Log.e("Checkbitphone", "32 Bit Active");
                    Log.e("Checkbitphone", "Normal Phone Overlay Folder found");
                    Log.e("Checkbitphone", "Function Stopped");
                    return false;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static void downloadAAPT(Context context) {
        Log.e("DownloadAAPT", "Function Called");
        Log.e("DownloadAAPT", "Function Started");
        Log.e("Checkbitphone", "Calling Function");
        if (checkbitphone()) {
            Log.e("DownloadAAPT", "64 Bit Active");
            Log.e("64 bit Device ", Build.DEVICE + " Found,now changing the vendor and mount");
            Log.e("64 bit Device ", Build.DEVICE + " changed the vendor and mount");
            String[] downloadCommands = {link64,
                    "aapt"};
            Log.e("DownloadindResources", "Calling Function");
            new downloadResources().execute(downloadCommands);
            Log.e("DownloadAAPT", "Function Stopped");
        } else {
            Log.e("DownloadAAPT", "32 Bit Active");
            Log.e("32 bit Device ", Build.DEVICE + " Found,now changing the vendor and mount");
            Log.e("32 bit Device ", Build.DEVICE + " changed the vendor and mount");
            String[] downloadCommands = {link,
                    "aapt"};
            new downloadResources().execute(downloadCommands);
            Log.e("DownloadAAPT", "Function Stopped");
        }
    }

    public static String getvendor() {
        return vendor;
    }

    private static class downloadResources extends AsyncTask<String, Integer, String> {

        private ProgressDialog pd = new ProgressDialog(context);

        @Override
        protected void onPreExecute() {
            Log.e("Downloadind Resources", "Function Called");
            Log.e("Downloadind Resources", "Function Started");
            pd.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            pd.setMessage("Downloading Resources");
            pd.setIndeterminate(true);
            pd.setCancelable(false);
            pd.show();
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
            super.onProgressUpdate(progress);
        }

        @Override
        protected void onPostExecute(String result) {
            pd.setMessage("Download Complete,Getting Things Finalised");
            Log.e("File Downloaded Found", "Copying File");
            Log.e("copyAAPT", "Calling Function");
            copyAAPT();
            pd.dismiss();
            Log.e("Downloadind Resources", "Function Stoppped");
        }

        public void copyAAPT() {
            Log.e("copyAAPT", "Function Called");
            Log.e("copyAAPT", "Function Started");
            Log.e("copyAAPT", "Start");
            String mount = new String("mount -o remount,rw /");
            String mountsys = new String("mount -o remount,rw /system");
            String remount = new String("mount -o remount,ro /");
            String remountsys = new String("mount -o remount,ro /system");
            eu.chainfire.libsuperuser.Shell.SU.run(mount);
            Log.e("copyAAPT", "Mounted /");
            eu.chainfire.libsuperuser.Shell.SU.run(mountsys);
            Log.e("copyAAPT", "Mounted " + mount);

            eu.chainfire.libsuperuser.Shell.SU.run(
                    "cp " +
                            context.getFilesDir().getAbsolutePath() +
                            "/aapt" + " /system/bin/aapt");
            eu.chainfire.libsuperuser.Shell.SU.run("chmod 777 /system/bin/aapt");
            Log.e("copyAAPT", "Copied AAPT");
            eu.chainfire.libsuperuser.Shell.SU.run(remount);
            Log.e("copyAAPT", "ReMounted /");
            eu.chainfire.libsuperuser.Shell.SU.run(remountsys);
            Log.e("copyAAPT", "ReMounted " + mount);
            Log.e("copyAAPT", "End");
            Log.e("copyAAPT", "Function Stopped");
        }


        @Override
        protected String doInBackground(String... sUrl) {
            try {
                Log.e("File download", "Started from :" + sUrl[0]);
                URL url = new URL(sUrl[0]);
                //URLConnection connection = url.openConnection();
                File myDir = new File(context.getFilesDir().getAbsolutePath());
                HttpClient client = new DefaultHttpClient();
                HttpPost request = new HttpPost(sUrl[0]);
                request.setHeader("User-Agent", sUrl[0]);

                HttpResponse response = client.execute(request);
                // create the directory if it doesnt exist
                if (!myDir.exists()) myDir.mkdirs();

                File outputFile = new File(myDir, sUrl[1]);

                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                int fileLength = connection.getContentLength();
                // download the file
                InputStream input = new BufferedInputStream(url.openStream());
                OutputStream output = new FileOutputStream(outputFile);

                byte data[] = new byte[1024];
                long total = 0;
                int count;

                while ((count = input.read(data)) != -1) {
                    total += count;
                    // publishing the progress....
                    publishProgress((int) (total * 100 / fileLength));
                    output.write(data, 0, count);
                }
                output.flush();
                output.close();
                input.close();

                Log.e("File download", "complete");
            } catch (Exception e) {
                Log.e("File download", "error: " + e.getMessage());
            }
            return null;
        }
    }

    final public static boolean checkLayersInstalled(Context context) {

        if (isAppInstalled(context, "com.lovejoy777.rroandlayersmanager")) {
            return true;
        } else {
            return false;
        }
    }

    final public static boolean checkThemeMainSupported(Context context) {

        File f2 = new File(vendor, "Akzent_Framework.apk");
        if (f2.exists()) {
            return true;
        }
        return false;
    }

    final public static boolean checkThemeSysSupported(Context context) {

        File f2 = new File(vendor, "Akzent_SystemUI.apk");
        if (f2.exists()) {
            return true;
        }
        return false;
    }

    public static boolean isAppInstalled(Context context, String packageName) {
        try {
            context.getPackageManager().getApplicationInfo(packageName, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    public static void copyFileToApp(Context context, String theme_dir, String destination_dir) {
        Log.e("CopyFrameworkFile", "Function Called");
        Log.e("CopyFrameworkFile", "Function Started");
        String sourcePath = theme_dir;
        File source = new File(sourcePath);
        Log.e("Source", sourcePath);
        String destinationPath = context.getFilesDir().getAbsolutePath() + "/" + destination_dir;
        Log.e("Destination", destinationPath);
        Log.e("CopyFrameworkFile", "Function Started");
        File destination = new File(destinationPath);
        try {
            FileUtils.copyFile(source, destination);
            Log.e("CopyFrameworkFile",
                    "Successfully copied framework apk from overlays folder to work directory");
            Log.e("CopyFrameworkFile", "Function Stopped");
        } catch (IOException e) {
            Log.e("CopyFrameworkFile",
                    "Failed to copy framework apk from resource-cache to work directory");
            Log.e("CopyFrameworkFile", "Function Stopped");
            e.printStackTrace();
        }
    }

    public static void createXML(String string, Context context, String color_picked) {
        try {
            // Create the working directory
            File directory = new File(context.getFilesDir(), "/res/color/");
            if (!directory.exists()) {
                directory.mkdirs();
            }
            // Create the files
            File root = new File(context.getFilesDir(), "/res/color/" + string);
            if (!root.exists()) {
                root.createNewFile();
            }
            FileWriter fw = new FileWriter(root);
            BufferedWriter bw = new BufferedWriter(fw);
            PrintWriter pw = new PrintWriter(bw);
            String xmlTags = ("<?xml version=\"1.0\" encoding=\"utf-8\"?>" + "\n");
            String xmlRes1 = ("<selector" + "\n");
            String xmlRes2 = ("  xmlns:android=\"http://schemas.android.com/apk/res/android\">"
                    + "\n");
            String xmlRes3 = ("    <item android:state_enabled=\"false\" android:color=\"" + color_picked + "\" />"
                    + "\n");
            String xmlRes4 = ("    <item android:state_window_focused=\"false\" android:color=\"" + color_picked + "\" />"
                    + "\n");
            String xmlRes5 = ("    <item android:state_pressed=\"true\" android:color=\"" + color_picked + "\" />"
                    + "\n");
            String xmlRes6 = ("    <item android:state_selected=\"true\" android:color=\"" + color_picked + "\" />"
                    + "\n");
            String xmlRes7 = ("    <item android:color=\"" + color_picked + "\" />"
                    + "\n");
            String xmlRes8 = ("</selector>");
            pw.write(xmlTags);
            pw.write(xmlRes1);
            pw.write(xmlRes2);
            pw.write(xmlRes3);
            pw.write(xmlRes4);
            pw.write(xmlRes5);
            pw.write(xmlRes6);
            pw.write(xmlRes7);
            pw.write(xmlRes8);
            pw.close();
            bw.close();
            fw.close();
            Log.e("CreateXMLFile",
                    string + " Created");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void createManifest(Context context) throws Exception {
        File manifest = new File(context.getFilesDir(), "AndroidManifest.xml");
        if (!manifest.exists()) {
            manifest.createNewFile();
        }
        FileWriter fw = new FileWriter(manifest);
        BufferedWriter bw = new BufferedWriter(fw);
        PrintWriter pw = new PrintWriter(bw);
        //String xmlTags = ("<?xml version=\"1.0\" encoding=\"utf-8\" " +
        //          "standalone=\"no\"?>" + "\n");
        String xmlRes1 = ("<manifest xmlns:android=\"http://schemas.android.com/" +
                "apk/res/android\" package=\"common\" android:versionCode=\"1\"" +
                " android:versionName=\"1.0\">" + "\n");
        String xmlRes2 = ("<overlay android:targetPackage=\"android\" android:priority=\"100\"/>" + "\n");
        String xmlRes3 = ("</manifest>" + "\n");
        //  pw.write(xmlTags);
        pw.write(xmlRes1);
        pw.write(xmlRes2);
        pw.write(xmlRes3);
        pw.close();
        bw.close();
        fw.close();
    }

    public static void copyFinalizedAPK(Context context, String file, boolean files) {
        String mount = "mount -o remount,rw /";
        String mountsys = "mount -o remount,rw /system";
        String remount = "mount -o remount,ro /";
        String remountsys = "mount -o remount,ro /system";
        eu.chainfire.libsuperuser.Shell.SU.run(mount);
        eu.chainfire.libsuperuser.Shell.SU.run(mountsys);
        if (files) {
            eu.chainfire.libsuperuser.Shell.SU.run(
                    "cp " +
                            context.getFilesDir().getAbsolutePath() +
                            "/" + file + ".apk " + "/system/vendor/overlay/" + file + ".apk");
            Log.e("copyFinalizedAPK",
                    "Successfully copied the modified resource APK from " + context.getFilesDir().getAbsolutePath() + " into " +
                            "/system/vendor/overlay/ and modified the permissions!");
        } else {
            eu.chainfire.libsuperuser.Shell.SU.run(
                    "cp " +
                            context.getCacheDir().getAbsolutePath() +
                            "/" + file + ".apk " + "/system/vendor/overlay/" + file + ".apk");
            Log.e("copyFinalizedAPK",
                    "Successfully copied the modified resource APK from " + context.getCacheDir().getAbsolutePath() + " into " +
                            "/system/vendor/overlay/ and modified the permissions!");
        }
        eu.chainfire.libsuperuser.Shell.SU.run("chmod 644 " + "/system/vendor/overlay/" + file + ".apk");
        eu.chainfire.libsuperuser.Shell.SU.run(remount);
        eu.chainfire.libsuperuser.Shell.SU.run(remountsys);
        if (files) {
            eu.chainfire.libsuperuser.Shell.SU.run("rm -r /data/data/projekt.dashboard.layers/files");
        } else {
            eu.chainfire.libsuperuser.Shell.SU.run("rm -r /data/data/projekt.dashboard.layers/cache");
        }
        Log.e("copyFinalizedAPK",
                "Successfully Deleted Files ");

    }

    public static void copyFABFinalizedAPK(Context context, String file, boolean files) {
        String mount = "mount -o remount,rw /";
        String mountsys = "mount -o remount,rw /vendor";
        String remount = "mount -o remount,ro /";
        String remountsys = "mount -o remount,ro /vendor";
        eu.chainfire.libsuperuser.Shell.SU.run(mount);
        eu.chainfire.libsuperuser.Shell.SU.run(mountsys);
        if (files) {
            eu.chainfire.libsuperuser.Shell.SU.run(
                    "cp " +
                            context.getFilesDir().getAbsolutePath() +
                            "/" + file + ".apk " + "/vendor/overlay/" + file + ".apk");
            Log.e("copyFinalizedAPK",
                    "Successfully copied the modified resource APK from " + context.getFilesDir().getAbsolutePath() + " into " +
                            "/vendor/overlay/ and modified the permissions!");
        } else {
            eu.chainfire.libsuperuser.Shell.SU.run(
                    "cp " +
                            context.getCacheDir().getAbsolutePath() +
                            "/" + file + ".apk " + "/vendor/overlay/" + file + ".apk");
            Log.e("copyFinalizedAPK",
                    "Successfully copied the modified resource APK from " + context.getCacheDir().getAbsolutePath() + " into " +
                            "/vendor/overlay/ and modified the permissions!");
        }
        eu.chainfire.libsuperuser.Shell.SU.run("chmod 644 " + "/vendor/overlay/" + file + ".apk");
        eu.chainfire.libsuperuser.Shell.SU.run(remount);
        eu.chainfire.libsuperuser.Shell.SU.run(remountsys);
        Log.e("copyFinalizedAPK",
                "Successfully copied the modified resource APK into " +
                        "/vendor/overlay/ and modified the permissions!");
        if (files) {
            eu.chainfire.libsuperuser.Shell.SU.run("rm -r /data/data/projekt.dashboard.layers/files");
        } else {
            eu.chainfire.libsuperuser.Shell.SU.run("rm -r /data/data/projekt.dashboard.layers/cache");
        }
        Log.e("copyFinalizedAPK",
                "Successfully Deleted Files ");

    }

    public static void LayersColorSwitch(Context context, String Name, String file, String resource) throws Exception {
        eu.chainfire.libsuperuser.Shell.SU.run(
                "cp " + context.getFilesDir().getAbsolutePath() +
                        "/color-resources/res/" + resource + "/" + file + ".xml " +
                        "/res/" + resource + "/" + file + ".xml");
        Log.e("performAAPTonCommonsAPK",
                "Successfully copied all modified accent XMLs into the root folder.");

        Log.e("performAAPTonCommonsAPK",
                "Preparing for clean up on resources...");
        Process nativeApp3 = Runtime.getRuntime().exec(
                "aapt remove " +
                        context.getFilesDir().getAbsolutePath() +
                        "/" + Name + ".apk res/" + resource + "/" + file + ".xml");
        Log.e("performAAPTonCommonsAPK",
                "Deleted main " + file + " file!");
        nativeApp3.waitFor();
        eu.chainfire.libsuperuser.Shell.SU.run(
                "aapt add " +
                        context.getFilesDir().getAbsolutePath() +
                        "/" + Name + ".apk res/" + resource + "/" + file + ".xml");

        Log.e("performAAPTonCommonsAPK",
                "Added freshly created main " + file + " file...ALL DONE!");
    }

    public static void findFrameworkFile() {
        try {
            File f2 = new File(vendor + "/");
            File[] files2 = f2.listFiles();
            if (files2 != null) {
                for (File inFile2 : files2) {
                    if (inFile2.isFile()) {
                        Log.e("Bhadwa", "Mila " + inFile2);
                        String filenameParse[] = inFile2.getAbsolutePath().split("/");
                        String last = filenameParse[filenameParse.length - 1];
                        StringTokenizer stringTokenizer = new StringTokenizer(last, ".");
                        String finalname = stringTokenizer.nextToken();
                        if (finalname.contains("HeaderSwapperFrame")) {
                            Log.e("MILA", finalname);
                            themeframework = finalname;
                            break;
                        }
                    }
                }
            }
        } catch (Exception e) {

        }
    }

    public static void findSystemUIFile() {
        try {
            File f2 = new File(vendor + "/");
            File[] files2 = f2.listFiles();
            if (files2 != null) {
                for (File inFile2 : files2) {
                    if (inFile2.isFile()) {
                        Log.e("Bhadwa", "Mila " + inFile2);
                        String filenameParse[] = inFile2.getAbsolutePath().split("/");
                        String last = filenameParse[filenameParse.length - 1];
                        StringTokenizer stringTokenizer = new StringTokenizer(last, ".");
                        String finalname = stringTokenizer.nextToken();
                        if (finalname.contains("HeaderSwapperSys")) {
                            Log.e("MILA", finalname);
                            themesystemui = finalname;
                            break;
                        }
                    }
                }
            }
        } catch (Exception e) {

        }
    }
}