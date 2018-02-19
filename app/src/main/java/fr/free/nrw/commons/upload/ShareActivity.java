package fr.free.nrw.commons.upload;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.graphics.drawable.VectorDrawableCompat;
import android.support.v4.app.ActivityCompat;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.drawee.generic.GenericDraweeHierarchyBuilder;
import com.facebook.drawee.view.SimpleDraweeView;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import butterknife.ButterKnife;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.auth.AuthenticatedActivity;
import fr.free.nrw.commons.auth.SessionManager;
import fr.free.nrw.commons.caching.CacheController;
import fr.free.nrw.commons.category.CategorizationFragment;
import fr.free.nrw.commons.category.OnCategoriesSaveHandler;
import fr.free.nrw.commons.contributions.Contribution;
import fr.free.nrw.commons.modifications.CategoryModifier;
import fr.free.nrw.commons.modifications.ModificationsContentProvider;
import fr.free.nrw.commons.modifications.ModifierSequence;
import fr.free.nrw.commons.modifications.ModifierSequenceDao;
import fr.free.nrw.commons.modifications.TemplateRemoveModifier;
import fr.free.nrw.commons.mwapi.MediaWikiApi;
import timber.log.Timber;

import static fr.free.nrw.commons.upload.Permission.REQUEST_PERM_ON_CREATE_STORAGE;

/**
 * Activity for the title/desc screen after image is selected. Also starts processing image
 * GPS coordinates or user location (if enabled in Settings) for category suggestions.
 */
public  class      ShareActivity
        extends    AuthenticatedActivity
        implements SingleUploadFragment.OnUploadActionInitiated,
        OnCategoriesSaveHandler {

    private CategorizationFragment categorizationFragment;

    @Inject MediaWikiApi mwApi;
    @Inject CacheController cacheController;
    @Inject SessionManager sessionManager;
    @Inject UploadController uploadController;
    @Inject ModifierSequenceDao modifierSequenceDao;
    @Inject @Named("default_preferences") SharedPreferences prefs;
    @Inject Permission permission;
    @Inject FileImageManager imageManager;


    private String source;
    private String mimeType;

    private Uri mediaUri;
    private Contribution contribution;
    private SimpleDraweeView backgroundImageView;

    private boolean cacheFound;

    private GPSExtractor imageObj;

    private String title;
    private String description;
    private Snackbar snackbar;

    /**
     * Called when user taps the submit button.
     */
    @Override
    public void uploadActionInitiated(String title, String description) {

        this.title = title;
        this.description = description;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Check for Storage permission that is required for upload.
            // Do not allow user to proceed without permission, otherwise will crash
            if (permission.needsToRequestStoragePermission(mediaUri, this)) {
                permission.requestPermission(this);
                return;
            }
        }
        uploadBegins();
    }

    private void uploadBegins() {
        String decimalCoords = imageManager.getFileMetadata(permission.getLocationPermitted(), this, mediaUri, imageObj, cacheController);

        Toast startingToast = Toast.makeText(this, R.string.uploading_started, Toast.LENGTH_LONG);
        startingToast.show();

        if (!cacheFound) {
            //Has to be called after apiCall.request()
            cacheController.cacheCategory();
            Timber.d("Cache the categories found");
        }

        uploadController.startUpload(title, mediaUri, description, mimeType, source, decimalCoords, c -> {
            ShareActivity.this.contribution = c;
            showPostUpload();
        });
    }

    private void showPostUpload() {
        if (categorizationFragment == null) {
            categorizationFragment = new CategorizationFragment();
        }
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.single_upload_fragment_container, categorizationFragment, "categorization")
                .commit();
    }

    @Override
    public void onCategoriesSave(List<String> categories) {
        if (categories.size() > 0) {
            ModifierSequence categoriesSequence = new ModifierSequence(contribution.getContentUri());
            categoriesSequence.queueModifier(new CategoryModifier(categories.toArray(new String[]{})));
            categoriesSequence.queueModifier(new TemplateRemoveModifier("Uncategorized"));
            modifierSequenceDao.save(categoriesSequence);
        }

        // FIXME: Make sure that the content provider is up
        // This is the wrong place for it, but bleh - better than not having it turned on by default for people who don't go throughl ogin
        ContentResolver.setSyncAutomatically(sessionManager.getCurrentAccount(), ModificationsContentProvider.MODIFICATIONS_AUTHORITY, true); // Enable sync by default!
        finish();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (contribution != null) {
            outState.putParcelable("contribution", contribution);
        }
    }

    @Override
    protected void onAuthCookieAcquired(String authCookie) {
        mwApi.setAuthCookie(authCookie);
    }

    @Override
    protected void onAuthFailure() {
        Toast failureToast = Toast.makeText(this, R.string.authentication_failed, Toast.LENGTH_LONG);
        failureToast.show();
        finish();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_share);
        ButterKnife.bind(this);
        initBack();
        backgroundImageView = (SimpleDraweeView) findViewById(R.id.backgroundImage);
        backgroundImageView.setHierarchy(GenericDraweeHierarchyBuilder
                .newInstance(getResources())
                .setPlaceholderImage(VectorDrawableCompat.create(getResources(),
                        R.drawable.ic_image_black_24dp, getTheme()))
                .setFailureImage(VectorDrawableCompat.create(getResources(),
                        R.drawable.ic_error_outline_black_24dp, getTheme()))
                .build());

        //Receive intent from ContributionController.java when user selects picture to upload
        Intent intent = getIntent();

        if (intent.getAction().equals(Intent.ACTION_SEND)) {
            mediaUri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
            if (intent.hasExtra(UploadService.EXTRA_SOURCE)) {
                source = intent.getStringExtra(UploadService.EXTRA_SOURCE);
            } else {
                source = Contribution.SOURCE_EXTERNAL;
            }
            mimeType = intent.getType();
        }

        if (mediaUri != null) {
            backgroundImageView.setImageURI(mediaUri);
        }

        if (savedInstanceState != null) {
            contribution = savedInstanceState.getParcelable("contribution");
        }

        requestAuthToken();

        Timber.d("Uri: %s", mediaUri.toString());
        Timber.d("Ext storage dir: %s", Environment.getExternalStorageDirectory());

        permission.checkStoragePermission(mediaUri, this);
        permission.checkLocationPermission(this);

        askForPermissionsIfNeeded();

        performPreuploadProcessingOfFile();

        SingleUploadFragment shareView = (SingleUploadFragment) getSupportFragmentManager().findFragmentByTag("shareView");
        categorizationFragment = (CategorizationFragment) getSupportFragmentManager().findFragmentByTag("categorization");
        if (shareView == null && categorizationFragment == null) {
            shareView = new SingleUploadFragment();
            getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.single_upload_fragment_container, shareView, "shareView")
                    .commitAllowingStateLoss();
        }
        uploadController.prepareService();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions, @NonNull int[] grantResults) {

        // If the permission has been requested from pushing the submit button then
        // we have to handle this case seperatly.
        boolean permissionFromSubmit = permission.updatePermissionFromSubmitButton(requestCode, grantResults);
        permission.updateLocationPermissions(requestCode, grantResults);
        permission.updateStoragePermissions(requestCode, grantResults);

        if(permissionFromSubmit) {
            performPreuploadProcessingOfFile();
            uploadBegins();
            snackbar.dismiss();
        } else if(permission.getLocationPermitted() && permission.getStoragePermitted()) {
            backgroundImageView.setImageURI(mediaUri);
            performPreuploadProcessingOfFile();
        } else if(permission.getStoragePermitted()) {
            backgroundImageView.setImageURI(mediaUri);
            performPreuploadProcessingOfFile();
        } else if(permission.getLocationPermitted()) {
            performPreuploadProcessingOfFile();
        }
    }

    public Snackbar requestPermissionUsingSnackBar(String rationale,
                                                    final String[] perms,
                                                    final int code) {
        Snackbar snackbar = Snackbar.make(findViewById(android.R.id.content), rationale,
                Snackbar.LENGTH_INDEFINITE).setAction(R.string.ok,
                view -> ActivityCompat.requestPermissions(ShareActivity.this, perms, code));
        snackbar.show();
        return snackbar;
    }

    private void performPreuploadProcessingOfFile() {
        if(imageManager.isImageDuplicate(this, mediaUri)){
            String decimalCoords = imageManager.getFileMetadata(permission.getLocationPermitted(), this, mediaUri, imageObj, cacheController);
            if(decimalCoords != null) {
                cacheFound = imageManager.useImageCoords(decimalCoords, imageObj, cacheController, new MwVolleyApi(this));
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        try {
            imageObj.unregisterLocationManager();
            Timber.d("Unregistered locationManager");
        } catch (NullPointerException e) {
            Timber.d("locationManager does not exist, not unregistered");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        uploadController.cleanup();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                if (categorizationFragment != null && categorizationFragment.isVisible()) {
                    categorizationFragment.showBackButtonDialog();
                } else {
                    onBackPressed();
                }
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void askForPermissionsIfNeeded() {

        if (permission.getUseNewPermission() && (!permission.getStoragePermitted() || !permission.getLocationPermitted())) {
            if (!permission.getStoragePermitted() && !permission.getLocationPermitted()) {
                String permissionRationales =
                        getResources().getString(R.string.read_storage_permission_rationale) + "\n"
                                + getResources().getString(R.string.location_permission_rationale);
                snackbar = requestPermissionUsingSnackBar(
                        permissionRationales,
                        new String[]{
                                Manifest.permission.READ_EXTERNAL_STORAGE,
                                Manifest.permission.ACCESS_FINE_LOCATION},
                        permission.REQUEST_PERM_ON_CREATE_STORAGE_AND_LOCATION);
                View snackbarView = snackbar.getView();
                TextView textView = (TextView) snackbarView.findViewById(android.support.design.R.id.snackbar_text);
                textView.setMaxLines(3);
            } else if (!permission.getStoragePermitted()) {
                requestPermissionUsingSnackBar(
                        getString(R.string.read_storage_permission_rationale),
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        REQUEST_PERM_ON_CREATE_STORAGE);
            } else if (!permission.getLocationPermitted()) {
                requestPermissionUsingSnackBar(
                        getString(R.string.location_permission_rationale),
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        permission.REQUEST_PERM_ON_CREATE_LOCATION);
            }
        }
    }
}
