package io.flutter.plugins.webviewflutter;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.util.Log;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebView;

import androidx.core.content.FileProvider;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;
import io.flutter.plugin.common.PluginRegistry;

import static android.app.Activity.RESULT_OK;

class FileChooserHelper {

    private final static int FILE_CHOOSER_RESULT_CODE = 1;

    private final ResultHandler resultHandler = new ResultHandler();
    private ValueCallback<Uri> mUploadMessage;
    private ValueCallback<Uri[]> mUploadMessageArray;
    private Uri fileUri;
    private Uri videoUri;
    private ActivityPluginBinding binding;
    private Activity activity;
    private Context context;
    private PluginRegistry.ActivityResultListener activityResultListener = new PluginRegistry.ActivityResultListener() {
        @Override
        public boolean onActivityResult(int requestCode, int resultCode, Intent data) {
            return resultHandler.handleResult(requestCode, resultCode, data);
        }
    };

    @TargetApi(7)
    class ResultHandler {
        public boolean handleResult(int requestCode, int resultCode, Intent intent) {
            boolean handled = false;
            if (Build.VERSION.SDK_INT >= 21) {
                if (requestCode == FILE_CHOOSER_RESULT_CODE) {
                    Uri[] results = null;
                    if (resultCode == RESULT_OK) {
                        if (fileUri != null && getFileSize(fileUri) > 0) {
                            results = new Uri[]{fileUri};
                        } else if (videoUri != null && getFileSize(videoUri) > 0) {
                            results = new Uri[]{videoUri};
                        } else if (intent != null) {
                            results = getSelectedFiles(intent);
                        }
                    }
                    if (mUploadMessageArray != null) {
                        mUploadMessageArray.onReceiveValue(results);
                        mUploadMessageArray = null;
                    }
                    handled = true;
                }
            } else {
                if (requestCode == FILE_CHOOSER_RESULT_CODE) {
                    Uri result = null;
                    if (resultCode == RESULT_OK && intent != null) {
                        result = intent.getData();
                    }
                    if (mUploadMessage != null) {
                        mUploadMessage.onReceiveValue(result);
                        mUploadMessage = null;
                    }
                    handled = true;
                }
            }
            return handled;
        }
    }

    private long getFileSize(Uri fileUri) {
        Cursor returnCursor = context.getContentResolver().query(fileUri, null, null, null, null);
        returnCursor.moveToFirst();
        int sizeIndex = returnCursor.getColumnIndex(OpenableColumns.SIZE);
        return returnCursor.getLong(sizeIndex);
    }

    private Uri[] getSelectedFiles(Intent data) {
        // we have one files selected
        if (data.getData() != null) {
            String dataString = data.getDataString();
            if (dataString != null) {
                return new Uri[]{Uri.parse(dataString)};
            }
        }
        // we have multiple files selected
        if (data.getClipData() != null) {
            final int numSelectedFiles = data.getClipData().getItemCount();
            Uri[] result = new Uri[numSelectedFiles];
            for (int i = 0; i < numSelectedFiles; i++) {
                result[i] = data.getClipData().getItemAt(i).getUri();
            }
            return result;
        }
        return null;
    }

    //The undocumented magic method override
    //Eclipse will swear at you if you try to put @Override here
    // For Android 3.0+
    public void openFileChooser(ValueCallback<Uri> uploadMsg) {
        mUploadMessage = uploadMsg;
        Intent i = new Intent(Intent.ACTION_GET_CONTENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("image/*");
        activity.startActivityForResult(Intent.createChooser(i, "File Chooser"), FILE_CHOOSER_RESULT_CODE);

    }

    // For Android 3.0+
    public void openFileChooser(ValueCallback uploadMsg, String acceptType) {
        mUploadMessage = uploadMsg;
        Intent i = new Intent(Intent.ACTION_GET_CONTENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("*/*");
        activity.startActivityForResult(
                Intent.createChooser(i, "File Browser"),
                FILE_CHOOSER_RESULT_CODE);
    }

    //For Android 4.1
    public void openFileChooser(ValueCallback<Uri> uploadMsg, String acceptType, String capture) {
        mUploadMessage = uploadMsg;
        Intent i = new Intent(Intent.ACTION_GET_CONTENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("image/*");
        activity.startActivityForResult(Intent.createChooser(i, "File Chooser"), FILE_CHOOSER_RESULT_CODE);

    }

    //For Android 5.0+
    public boolean onShowFileChooser(
            WebView webView, ValueCallback<Uri[]> filePathCallback,
            WebChromeClient.FileChooserParams fileChooserParams) {
        if (mUploadMessageArray != null) {
            mUploadMessageArray.onReceiveValue(null);
        }
        mUploadMessageArray = filePathCallback;

        final String[] acceptTypes = getSafeAcceptedTypes(fileChooserParams);
        List<Intent> intentList = new ArrayList<>();
        fileUri = null;
        videoUri = null;
        if (acceptsImages(acceptTypes)) {
            Intent takePhotoIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            fileUri = getOutputFilename(MediaStore.ACTION_IMAGE_CAPTURE);
            takePhotoIntent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri);
            intentList.add(takePhotoIntent);
        }
        if (acceptsVideo(acceptTypes)) {
            Intent takeVideoIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
            videoUri = getOutputFilename(MediaStore.ACTION_VIDEO_CAPTURE);
            takeVideoIntent.putExtra(MediaStore.EXTRA_OUTPUT, videoUri);
            intentList.add(takeVideoIntent);
        }
        Intent contentSelectionIntent;
        if (Build.VERSION.SDK_INT >= 21) {
            final boolean allowMultiple = fileChooserParams.getMode() == WebChromeClient.FileChooserParams.MODE_OPEN_MULTIPLE;
            contentSelectionIntent = fileChooserParams.createIntent();
            contentSelectionIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, allowMultiple);
        } else {
            contentSelectionIntent = new Intent(Intent.ACTION_GET_CONTENT);
            contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE);
            contentSelectionIntent.setType("*/*");
        }
        Intent[] intentArray = intentList.toArray(new Intent[intentList.size()]);

        Intent chooserIntent = new Intent(Intent.ACTION_CHOOSER);
        chooserIntent.putExtra(Intent.EXTRA_INTENT, contentSelectionIntent);
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentArray);
        activity.startActivityForResult(chooserIntent, FILE_CHOOSER_RESULT_CODE);
        return true;
    }

    private String[] getSafeAcceptedTypes(WebChromeClient.FileChooserParams params) {
        // the getAcceptTypes() is available only in api 21+
        // for lower level, we ignore it
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return params.getAcceptTypes();
        }

        return new String[]{};
    }
    private Uri getOutputFilename(String intentType) {
        String prefix = "";
        String suffix = "";

        if (MediaStore.ACTION_IMAGE_CAPTURE.equals(intentType)) {
            prefix = "image-";
            suffix = ".jpg";
        } else if (MediaStore.ACTION_VIDEO_CAPTURE.equals(intentType)) {
            prefix = "video-";
            suffix = ".mp4";
        }

        String packageName = context.getPackageName();
        File capturedFile = null;
        try {
            capturedFile = createCapturedFile(prefix, suffix);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return FileProvider.getUriForFile(context, packageName + ".fileprovider", capturedFile);
    }

    private File createCapturedFile(String prefix, String suffix) throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = prefix + "_" + timeStamp;
        File storageDir = context.getExternalFilesDir(null);
        return File.createTempFile(imageFileName, suffix, storageDir);
    }

    private Boolean acceptsImages(String[] types) {
        return isArrayEmpty(types) || arrayContainsString(types, "image");
    }

    private Boolean acceptsVideo(String[] types) {
        return isArrayEmpty(types) || arrayContainsString(types, "video");
    }

    private Boolean arrayContainsString(String[] array, String pattern) {
        for (String content : array) {
            if (content.contains(pattern)) {
                return true;
            }
        }
        return false;
    }

    private Boolean isArrayEmpty(String[] arr) {
        // when our array returned from getAcceptTypes() has no values set from the
        // webview
        // i.e. <input type="file" />, without any "accept" attr
        // will be an array with one empty string element, afaik
        return arr.length == 0 || (arr.length == 1 && arr[0].length() == 0);
    }

    public void setActivityPluginBinding(ActivityPluginBinding binding) {
        if (binding == null && this.binding != null) {
            this.binding.removeActivityResultListener(activityResultListener);
            this.binding = null;
            this.activity = null;
        } else if (binding != null) {
            binding.addActivityResultListener(activityResultListener);
            this.binding = binding;
            this.activity = binding.getActivity();
            this.context = binding.getActivity().getApplicationContext();
        }
    }
}
