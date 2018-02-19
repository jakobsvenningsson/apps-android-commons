package fr.free.nrw.commons.upload;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.support.annotation.Nullable;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import fr.free.nrw.commons.caching.CacheController;
import fr.free.nrw.commons.mwapi.MediaWikiApi;
import timber.log.Timber;

import static fr.free.nrw.commons.upload.ExistingFileAsync.Result.DUPLICATE_PROCEED;
import static fr.free.nrw.commons.upload.ExistingFileAsync.Result.NO_DUPLICATE;

/**
 * Class responsible for image processing and handling.
 * Used by {@link ShareActivity} when user uploads images.
 */
public class FileImageManager {

    private Permission permission;
    private MediaWikiApi mwApi;
    private SharedPreferences prefs;

    private ExistingFileAsync fileAsyncTask;

    private boolean duplicateCheckPassed = false;
    public boolean getDuplicateCheckPassed() { return duplicateCheckPassed; }

    public FileImageManager(MediaWikiApi mwApi, Permission permission, SharedPreferences prefs) {
        this.permission = permission;
        this.mwApi = mwApi;
        this.prefs = prefs;
    }

    public boolean isImageDuplicate(Context context, Uri mediaUri) {
        if (!permission.getUseNewPermission() || permission.getStoragePermitted()) {
            if (!duplicateCheckPassed) {
                //Test SHA1 of image to see if it matches SHA1 of a file on Commons
                try {
                    InputStream inputStream = context.getContentResolver().openInputStream(mediaUri);
                    Timber.d("Input stream created from %s", mediaUri.toString());
                    String fileSHA1 = getSHA1(inputStream);
                    Timber.d("File SHA1 is: %s", fileSHA1);

                    fileAsyncTask =
                            new ExistingFileAsync(fileSHA1, context, result -> {
                                Timber.d("%s duplicate check: %s", mediaUri.toString(), result);
                                duplicateCheckPassed = (result == DUPLICATE_PROCEED
                                        || result == NO_DUPLICATE);
                            }, mwApi);

                    fileAsyncTask.execute();
                } catch (IOException e) {
                    Timber.d(e, "IO Exception: ");
                }
            }
            return true;
        } else {
            Timber.w("not ready for preprocessing: useNewPermissions=%s storage=%s location=%s",
                    permission.getUseNewPermission(), permission.getStoragePermitted(), permission.getLocationPermitted());
        }
        return false;
    }

    /**
     * Gets coordinates for category suggestions, either from EXIF data or user location
     *
     * @param gpsEnabled if true use GPS
     */
    @Nullable
    protected String getFileMetadata(boolean gpsEnabled, Context context, Uri mediaUri, GPSExtractor imageObj, CacheController cacheController) {
        Timber.d("Calling GPSExtractor");
        String decimalCoords;
        try {
            if (imageObj == null) {
                ParcelFileDescriptor descriptor
                        = context.getContentResolver().openFileDescriptor(mediaUri, "r");
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    if (descriptor != null) {
                        imageObj = new GPSExtractor(descriptor.getFileDescriptor(), context, prefs);
                    }
                } else {
                    String filePath = getPathOfMediaOrCopy(context, mediaUri);
                    if (filePath != null) {
                        imageObj = new GPSExtractor(filePath, context, prefs);
                    }
                }
            }

            if (imageObj != null) {
                // Gets image coords from exif data or user location
                decimalCoords = imageObj.getCoords(gpsEnabled);
                return decimalCoords;
            }
        } catch (FileNotFoundException e) {
            Timber.w("File not found: " + mediaUri, e);
        }
        return null;
    }

    /**
     * Initiates retrieval of image coordinates or user coordinates, and caching of coordinates.
     * Then initiates the calls to MediaWiki API through an instance of MwVolleyApi.
     */
    public boolean useImageCoords(String decimalCoords, GPSExtractor imageObj, CacheController cacheController, MwVolleyApi apiCall) {
        boolean cacheFound = false;
        if (decimalCoords != null) {
            Timber.d("Decimal coords of image: %s", decimalCoords);

            // Only set cache for this point if image has coords
            if (imageObj.imageCoordsExists) {
                double decLongitude = imageObj.getDecLongitude();
                double decLatitude = imageObj.getDecLatitude();
                cacheController.setQtPoint(decLongitude, decLatitude);
            }

            List<String> displayCatList = cacheController.findCategory();
            boolean catListEmpty = displayCatList.isEmpty();

            // If no categories found in cache, call MediaWiki API to match image coords with nearby Commons categories
            if (catListEmpty) {
                cacheFound = false;
                apiCall.request(decimalCoords);
                Timber.d("displayCatList size 0, calling MWAPI %s", displayCatList);
            } else {
                cacheFound = true;
                Timber.d("Cache found, setting categoryList in MwVolleyApi to %s", displayCatList);
                MwVolleyApi.setGpsCat(displayCatList);
            }
        }
        return cacheFound;
    }

    @Nullable
    private String getPathOfMediaOrCopy(Context context, Uri mediaUri) {
        String filePath = FileUtils.getPath(context.getApplicationContext(), mediaUri);
        Timber.d("Filepath: " + filePath);
        if (filePath == null) {
            // in older devices getPath() may fail depending on the source URI
            // creating and using a copy of the file seems to work instead.
            // TODO: there might be a more proper solution than this
            String copyPath = null;
            try {
                ParcelFileDescriptor descriptor
                        = context.getContentResolver().openFileDescriptor(mediaUri, "r");
                if (descriptor != null) {
                    boolean useExtStorage = prefs.getBoolean("useExternalStorage", true);
                    if (useExtStorage) {
                        copyPath = Environment.getExternalStorageDirectory().toString()
                                + "/CommonsApp/" + new Date().getTime() + ".jpg";
                        File newFile = new File(Environment.getExternalStorageDirectory().toString() + "/CommonsApp");
                        newFile.mkdir();
                        FileUtils.copy(
                                descriptor.getFileDescriptor(),
                                copyPath);
                        Timber.d("Filepath (copied): %s", copyPath);
                        return copyPath;
                    }
                    copyPath = context.getApplicationContext().getCacheDir().getAbsolutePath()
                            + "/" + new Date().getTime() + ".jpg";
                    FileUtils.copy(
                            descriptor.getFileDescriptor(),
                            copyPath);
                    Timber.d("Filepath (copied): %s", copyPath);
                    return copyPath;
                }
            } catch (IOException e) {
                Timber.w(e, "Error in file " + copyPath);
                return null;
            }
        }
        return filePath;
    }

    // Get SHA1 of file from input stream
    private String getSHA1(InputStream is) {

        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA1");
        } catch (NoSuchAlgorithmException e) {
            Timber.e(e, "Exception while getting Digest");
            return "";
        }

        byte[] buffer = new byte[8192];
        int read;
        try {
            while ((read = is.read(buffer)) > 0) {
                digest.update(buffer, 0, read);
            }
            byte[] md5sum = digest.digest();
            BigInteger bigInt = new BigInteger(1, md5sum);
            String output = bigInt.toString(16);
            // Fill to 40 chars
            output = String.format("%40s", output).replace(' ', '0');
            Timber.i("File SHA1: %s", output);

            return output;
        } catch (IOException e) {
            Timber.e(e, "IO Exception");
            return "";
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                Timber.e(e, "Exception on closing MD5 input stream");
            }
        }
    }
}
