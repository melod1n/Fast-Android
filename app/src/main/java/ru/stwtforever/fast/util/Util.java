package ru.stwtforever.fast.util;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.os.Environment;
import android.util.DisplayMetrics;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Locale;

import ru.stwtforever.fast.common.AppGlobal;
import ru.stwtforever.fast.io.BytesOutputStream;

public class Util {

    public static SimpleDateFormat dateFormatter;

    static {
        dateFormatter = new SimpleDateFormat("HH:mm", Locale.getDefault()); // 15:57
        //SimpleDateFormat dateMonthFormatter = new SimpleDateFormat("d MMM", Locale.getDefault());
        //SimpleDateFormat dateYearFormatter = new SimpleDateFormat("d MMM, yyyy", Locale.getDefault());
        //SimpleDateFormat dateFullFormatter = new SimpleDateFormat("dd.MM.yyyy, HH:mm", Locale.getDefault());
    }

    public static void copyText(String text) {
        ClipboardManager cm = (ClipboardManager) AppGlobal.context.getSystemService(Context.CLIPBOARD_SERVICE);
        cm.setPrimaryClip(ClipData.newPlainText(null, text));
    }

    public static String parseSize(long sizeInBytes) {
        long unit = 1024;
        if (sizeInBytes < unit) return sizeInBytes + " B";
        int exp = (int) (Math.log(sizeInBytes) / Math.log(unit));
        String pre = ("KMGTPE").charAt(exp - 1) + ("i");
        return String.format(Locale.US, "%.1f %sB", sizeInBytes / Math.pow(unit, exp), pre);
    }

    public static byte[] serialize(Object source) {
        try {
            BytesOutputStream bos = new BytesOutputStream();
            ObjectOutputStream out = new ObjectOutputStream(bos);

            out.writeObject(source);
            out.close();
            return bos.getByteArray();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Object deserialize(byte[] source) {
        if (ArrayUtil.isEmpty(source)) {
            return null;
        }

        try {
            ByteArrayInputStream bis = new ByteArrayInputStream(source);
            ObjectInputStream in = new ObjectInputStream(bis);

            Object o = in.readObject();

            in.close();
            return o;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static int px(int dp) {
        Resources resources = AppGlobal.context.getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        return dp * (metrics.densityDpi / DisplayMetrics.DENSITY_DEFAULT);
    }

    public static int dp(int px) {
        Resources resources = AppGlobal.context.getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        return px * (metrics.densityDpi / DisplayMetrics.DENSITY_DEFAULT);
    }

    public static boolean hasConnection() {
        ConnectivityManager cm = (ConnectivityManager) AppGlobal.context.getSystemService(Context.CONNECTIVITY_SERVICE);
        return (cm.getActiveNetworkInfo() != null &&
                cm.getActiveNetworkInfo().isAvailable() &&
                cm.getActiveNetworkInfo().isConnected());
    }

    public static void saveFileByUrl(String link) throws Exception {
        URL url = new URL(link);
        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        urlConnection.setRequestMethod("GET");
        urlConnection.setDoOutput(false);
        urlConnection.connect();

        File directory = new File(Environment.getExternalStorageDirectory() + "/Download");

        if (!directory.exists()) directory.mkdir();

        String name = link.substring(link.lastIndexOf("/") + 1);

        File file = new File(directory, name);

        int i = 1;
        while (file.exists()) {
            String fileName = link.substring(link.lastIndexOf("/") + 1);

            int dotIndex = fileName.lastIndexOf(".");

            String name_ = fileName.substring(0, dotIndex);
            String ext = fileName.substring(dotIndex);
            name_ += "-" + i;

            fileName = name_ + ext;

            file = new File(directory, fileName);
            i++;
        }

        FileOutputStream fileOutput = new FileOutputStream(file);
        InputStream inputStream = urlConnection.getInputStream();

        byte[] buffer = new byte[1024];
        int bufferLength;

        while ((bufferLength = inputStream.read(buffer)) > 0) {
            fileOutput.write(buffer, 0, bufferLength);
        }
        fileOutput.close();
    }
}