package fr.free.nrw.commons.upload;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v4.content.ContextCompat;

import static com.mapbox.mapboxsdk.Mapbox.getApplicationContext;


public class Permission {


    public Permission() {}

    private boolean useNewPermission = false;
    private boolean storagePermitted = false;
    private boolean locationPermitted = false;

    public static final int REQUEST_PERM_ON_CREATE_STORAGE = 1;
    public static final int REQUEST_PERM_ON_CREATE_LOCATION = 2;
    public static final int REQUEST_PERM_ON_CREATE_STORAGE_AND_LOCATION = 3;
    public static final int REQUEST_PERM_ON_SUBMIT_STORAGE = 4;

    public boolean getUseNewPermission() {
        return useNewPermission;
    }

    public boolean getStoragePermitted() {
        return storagePermitted;
    }

    public boolean getLocationPermitted() {
        return locationPermitted;
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


    public boolean checkLocationPermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            useNewPermission = true;
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                locationPermitted = true;
                return true;
            }
        }
        return false;
    }

    public boolean checkStoragePermission(Uri uri, Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            useNewPermission = true;
            if (!needsToRequestStoragePermission(uri, context)) {
                storagePermitted = true;
                return true;
            }
        }
        return false;
    }

    public boolean updateStoragePermissions(int requestCode, int[] grantResults) {
        switch (requestCode) {
            case REQUEST_PERM_ON_CREATE_STORAGE: {
                if (grantResults.length >= 1
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    return true;
                }
            }
            case REQUEST_PERM_ON_CREATE_STORAGE_AND_LOCATION: {
                if (grantResults.length >= 2
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    return true;
                }
            }
            default:
                return false;
        }
    }


    public boolean updateLocationPermissions(int requestCode, int[] grantResults) {
        switch (requestCode) {
            case REQUEST_PERM_ON_CREATE_LOCATION: {
                if (grantResults.length >= 1
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    return true;
                }
            }
            case REQUEST_PERM_ON_CREATE_STORAGE_AND_LOCATION: {
                if (grantResults.length >= 2
                        && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                    return true;
                }
            }
            default:
                return false;
        }
    }


    public boolean updatePermissionFromSubmitButton(int requestCode, int[] grantResults) {
        switch (requestCode) {
            // Storage (from submit button) - this needs to be separate from (1) because only the
            // submit button should bring user to next screen
            case REQUEST_PERM_ON_SUBMIT_STORAGE: {
                if (grantResults.length >= 1
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    locationPermitted = storagePermitted;
                    return true;
                }
            }
            default:
                return false;
        }
    }

    @RequiresApi(23)
    public void requestPermission(Activity activity) {
        activity.requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                REQUEST_PERM_ON_SUBMIT_STORAGE);
    }
}
