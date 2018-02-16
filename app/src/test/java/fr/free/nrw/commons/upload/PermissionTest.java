package fr.free.nrw.commons.upload;

import android.content.pm.PackageManager;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Created by jakobsvenningsson on 2018-02-16.
 */

public class PermissionTest {

    public static final int REQUEST_PERM_ON_CREATE_STORAGE = 1;
    public static final int REQUEST_PERM_ON_CREATE_LOCATION = 2;
    public static final int REQUEST_PERM_ON_CREATE_STORAGE_AND_LOCATION = 3;
    public static final int REQUEST_PERM_ON_SUBMIT_STORAGE = 4;

    private Permission permission;
    private ShareActivityInterface mockDelegate;

    @Before
    public void setUp() {
        mockDelegate = mock(ShareActivityInterface.class);
        permission = new Permission();
        permission.setShareActivityInterface(mockDelegate);
    }

    @Test
    public void updatePermissionShoudSetStoragePermittedToTrueWhenCalledWithREQUEST_PERM_ON_CREATE_STORAGE() {
        // Contract: If updatePermission is called with REQUEST_PERM_ON_CREATE_STORAGE
        // and PERMISSION_GRANTED then the storagePermitted flag shall be set to true and
        // startPreprocessing shall be called
        int[] grantResults = {PackageManager.PERMISSION_GRANTED };
        permission.updatePermissions(REQUEST_PERM_ON_CREATE_STORAGE, grantResults);
        assertTrue(permission.getStoragePermitted());
        verify(mockDelegate, times(1)).startPreprocessing(true);
    }

    @Test
    public void updatePermissionShoudNotChangeStateWhenCalledWithEmptyGrandReult() {
        // Contract: If updatePermission with is called with REQUEST_PERM_ON_CREATE_STORAGE
        // and  but without PERMISSION_GRANTED then no methods on the ShareActivityInterface
        // shall be invoked.
        int[] grantResults = {};
        permission.updatePermissions(REQUEST_PERM_ON_CREATE_STORAGE, grantResults);
        verify(mockDelegate, times(0)).startPreprocessing(anyBoolean());
        verify(mockDelegate, times(0)).setSnackbar(any());
        verify(mockDelegate, times(0)).startPreprocessingAndUpload();
    }

    @Test
    public void updatePermissionShoudSetLocationPermittedToTrueWhenCalledWithREQUEST_PERM_ON_CREATE_LOCATION() {
        // Contract: If updatePermission with is called with REQUEST_PERM_ON_CREATE_LOCATION
        // and  with PERMISSION_GRANTED then locationPermitted shall be set to true
        // and startPreprocessing shall be invoked
        int[] grantResults = {PackageManager.PERMISSION_GRANTED };
        permission.updatePermissions(REQUEST_PERM_ON_CREATE_LOCATION, grantResults);
        assertTrue(permission.getLocationPermitted());
        verify(mockDelegate, times(1)).startPreprocessing(false);
    }

    @Test
    public void updatePermissionShoudSetStoragePermittedToTrueWhenCalledWithREQUEST_PERM_ON_CREATE_STORAGE_AND_LOCATION() {
        // Contract: If updatePermission with is called with REQUEST_PERM_ON_CREATE_STORAGE_AND_LOCATION
        // and  with PERMISSION_GRANTED and len(grantResults) > 1 then storagePermitted shall be set to true
        // and startPreprocessing shall be invoked with a true argument
        int[] grantResults = {PackageManager.PERMISSION_GRANTED, 2 };
        permission.updatePermissions(REQUEST_PERM_ON_CREATE_STORAGE_AND_LOCATION, grantResults);
        assertTrue(permission.getStoragePermitted());
        verify(mockDelegate, times(1)).startPreprocessing(true);
    }

    @Test
    public void updatePermissionShoudSetLocationPermittedToTrueWhenCalledWithREQUEST_PERM_ON_CREATE_STORAGE_AND_LOCATION() {
        // Contract: If updatePermission with is called with REQUEST_PERM_ON_CREATE_STORAGE_AND_LOCATION
        // and  with PERMISSION_GRANTED and len(grantResults) == 1 then locationPermitted shall be set to true
        // and startPreprocessing shall be invoked with a false argument
        int[] grantResults = { 2,PackageManager.PERMISSION_GRANTED };
        permission.updatePermissions(REQUEST_PERM_ON_CREATE_STORAGE_AND_LOCATION, grantResults);
        assertTrue(permission.getLocationPermitted());
        verify(mockDelegate, times(1)).startPreprocessing(false);
    }

    @Test
    public void updatePermissionShoudCallPreprocessingAndUploadWhenCalledWithREQUEST_PERM_ON_CREATE_STORAGE_AND_LOCATION() {
        // Contract: If updatePermission with is called with REQUEST_PERM_ON_CREATE_STORAGE_AND_LOCATION
        // and  with PERMISSION_GRANTED then startPreprocessingAndUpload shall be invoked.
        int[] grantResults = {PackageManager.PERMISSION_GRANTED };
        permission.updatePermissions(REQUEST_PERM_ON_SUBMIT_STORAGE, grantResults);
        verify(mockDelegate, times(1)).startPreprocessingAndUpload();
    }

    //TODO Test checkStoragePermission when we will find a way to properly inject android context
}
