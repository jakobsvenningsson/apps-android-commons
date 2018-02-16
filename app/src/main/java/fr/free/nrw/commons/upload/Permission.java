package fr.free.nrw.commons.upload;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.TextView;
import android.support.design.widget.Snackbar;

import fr.free.nrw.commons.R;

import static com.mapbox.mapboxsdk.Mapbox.getApplicationContext;

/**
 * Created by jakobsvenningsson on 2018-02-15.
 */


public class Permission {

    public Permission() {}

    private ShareActivityInterface shareActivityInterface;

    private boolean useNewPermissions = false;
    private boolean storagePermitted = false;
    private boolean locationPermitted = false;

    public static final int REQUEST_PERM_ON_CREATE_STORAGE = 1;
    public static final int REQUEST_PERM_ON_CREATE_LOCATION = 2;
    public static final int REQUEST_PERM_ON_CREATE_STORAGE_AND_LOCATION = 3;
    public static final int REQUEST_PERM_ON_SUBMIT_STORAGE = 4;

    public boolean getUseNewPermissions() {
        return useNewPermissions;
    }

    public boolean getStoragePermitted() {
        return storagePermitted;
    }

    public boolean getLocationPermitted() {
        return locationPermitted;
    }

    public void setShareActivityInterface(ShareActivityInterface shareActivityInterface) {
        this.shareActivityInterface = shareActivityInterface;
    }

    @RequiresApi(16)
    public boolean needsToRequestStoragePermission(Uri uri, Context context) {
        // We need to ask storage permission when
        // the file is not owned by this application, (e.g. shared from the Gallery)
        // and permission is not obtained.
        return !FileUtils.isSelfOwned(getApplicationContext(), uri)
                && (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED);
    }

    public void checkStoragePermission(Uri uri, Context context) {
        useNewPermissions = false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            useNewPermissions = true;

            if (!needsToRequestStoragePermission(uri, context)) {
                storagePermitted = true;
            }
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                locationPermitted = true;
            }
        }
        // Check storage permissions if marshmallow or newer
        if (!useNewPermissions && (!storagePermitted || !locationPermitted)) {
            if (!storagePermitted && !locationPermitted) {
                String permissionRationales =
                        context.getResources().getString(R.string.read_storage_permission_rationale) + "\n"
                                + context.getResources().getString(R.string.location_permission_rationale);
                Snackbar snackbar = shareActivityInterface.requestPermissionUsingSnackBar(
                        permissionRationales,
                        new String[]{
                                Manifest.permission.READ_EXTERNAL_STORAGE,
                                Manifest.permission.ACCESS_FINE_LOCATION},
                        REQUEST_PERM_ON_CREATE_STORAGE_AND_LOCATION);
                View snackbarView = snackbar.getView();
                TextView textView = (TextView) snackbarView.findViewById(android.support.design.R.id.snackbar_text);
                textView.setMaxLines(3);
                shareActivityInterface.setSnackbar(snackbar);
            } else if (!storagePermitted) {
                shareActivityInterface.requestPermissionUsingSnackBar(
                        context.getString(R.string.read_storage_permission_rationale),
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        REQUEST_PERM_ON_CREATE_STORAGE);
            } else if (!locationPermitted) {
                shareActivityInterface.requestPermissionUsingSnackBar(
                        context.getString(R.string.location_permission_rationale),
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        REQUEST_PERM_ON_CREATE_LOCATION);
            }
        }
    }

    public void updatePermissions(int requestCode, int[] grantResults) {
        switch (requestCode) {
            case REQUEST_PERM_ON_CREATE_STORAGE: {
                if (grantResults.length >= 1
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    storagePermitted = true;
                    shareActivityInterface.startPreprocessing(true);
                }
                return;
            }
            case REQUEST_PERM_ON_CREATE_LOCATION: {
                if (grantResults.length >= 1
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    locationPermitted = true;
                    shareActivityInterface.startPreprocessing(false);
                }
                return;
            }
            case REQUEST_PERM_ON_CREATE_STORAGE_AND_LOCATION: {
                if (grantResults.length >= 2
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    storagePermitted = true;
                    shareActivityInterface.startPreprocessing(true);
                }
                if (grantResults.length >= 2
                        && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                    locationPermitted = true;
                    shareActivityInterface.startPreprocessing(false);

                }
                return;
            }
            // Storage (from submit button) - this needs to be separate from (1) because only the
            // submit button should bring user to next screen
            case REQUEST_PERM_ON_SUBMIT_STORAGE: {
                if (grantResults.length >= 1
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    shareActivityInterface.startPreprocessingAndUpload();
                }
                return;
            }
        }
    }
}


     /*
               useNewPermissions = false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            useNewPermissions = true;

            if (!needsToRequestStoragePermission()) {
                storagePermitted = true;
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                locationPermitted = true;
            }
        }
        // Check storage permissions if marshmallow or newer
        if (useNewPermissions && (!storagePermitted || !locationPermitted)) {
            if (!storagePermitted && !locationPermitted) {
                String permissionRationales =
                        getResources().getString(R.string.read_storage_permission_rationale) + "\n"
                                + getResources().getString(R.string.location_permission_rationale);
                snackbar = requestPermissionUsingSnackBar(
                        permissionRationales,
                        new String[]{
                                Manifest.permission.READ_EXTERNAL_STORAGE,
                                Manifest.permission.ACCESS_FINE_LOCATION},
                        REQUEST_PERM_ON_CREATE_STORAGE_AND_LOCATION);
                View snackbarView = snackbar.getView();
                TextView textView = (TextView) snackbarView.findViewById(android.support.design.R.id.snackbar_text);
                textView.setMaxLines(3);
            } else if (!storagePermitted) {
                requestPermissionUsingSnackBar(
                        getString(R.string.read_storage_permission_rationale),
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        REQUEST_PERM_ON_CREATE_STORAGE);
            } else if (!locationPermitted) {
                requestPermissionUsingSnackBar(
                        getString(R.string.location_permission_rationale),
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        REQUEST_PERM_ON_CREATE_LOCATION);
            }
        }*/
