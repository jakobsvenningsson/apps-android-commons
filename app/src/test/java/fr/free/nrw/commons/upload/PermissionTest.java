package fr.free.nrw.commons.upload;

import android.content.Context;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;


public class PermissionTest {

    private static final int REQUEST_PERM_ON_CREATE_STORAGE = 1;
    private static final int REQUEST_PERM_ON_CREATE_LOCATION = 2;
    private static final int REQUEST_PERM_ON_CREATE_STORAGE_AND_LOCATION = 3;
    private static final int REQUEST_PERM_ON_SUBMIT_STORAGE = 4;

    private Permission permission;
    @Mock
    private Permission mockPermission;
    @Mock
    private Context mockContext;
    @Mock
    private Uri mockUri;

    @Before
    public void setUp() {
        permission = new Permission();
        MockitoAnnotations.initMocks(this);
        when(mockPermission.checkStoragePermission(mockUri, mockContext)).thenCallRealMethod();
    }



    // 5 tests for updateStoragePermissions checking Case1/Case2 fulfilled(or not) and if = true/false
    @Test
    public void updateStoragePermissionsShouldReturnTrueWhenCase1IsFulfilledAndIfIsTrue() {
        int[] grantResults = {PackageManager.PERMISSION_GRANTED};
        assertTrue(permission.updateStoragePermissions(REQUEST_PERM_ON_CREATE_STORAGE, grantResults));
    }

    @Test
    public void updateStoragePermissionsShouldReturnFalseWhenCase1IsFulfilledAndIfIsFalse(){
        int [] grantResults = {PackageManager.PERMISSION_DENIED};
        assertFalse(permission.updateStoragePermissions(REQUEST_PERM_ON_CREATE_STORAGE, grantResults));
    }

    @Test
    public void updateStoragePermissionsShouldReturnTrueWhenCase2IsFulfilledAndIfIsTrue(){
        int[] grantResults = {PackageManager.PERMISSION_GRANTED, PackageManager.PERMISSION_GRANTED};
        assertTrue(permission.updateStoragePermissions(REQUEST_PERM_ON_CREATE_STORAGE_AND_LOCATION, grantResults));
    }

    @Test
    public void updateStoragePermissionsShouldReturnFalseWhenCase2IsFulfilledAndIfIsFalse(){
        int[] grantResults = {PackageManager.PERMISSION_DENIED, PackageManager.PERMISSION_GRANTED};
        assertFalse(permission.updateStoragePermissions(REQUEST_PERM_ON_CREATE_STORAGE_AND_LOCATION, grantResults));
    }

    @Test
    public void updateStoragePermissionShouldReturnFalseWhenNeitherCase1or2IsFulfilled(){
        int[] grantResults = {};
        assertFalse(permission.updateStoragePermissions(REQUEST_PERM_ON_CREATE_LOCATION, grantResults));
    }



    // 5 tests for updateLocationPermissions checking Case1/Case2 fulfilled(or not) and if = true/false
    @Test
    public void updateLocationPermissionsShouldReturnTrueWhenCase1IsFulfilledAndIfIsTrue() {
        int[] grantResults = {PackageManager.PERMISSION_GRANTED};
        assertTrue(permission.updateLocationPermissions(REQUEST_PERM_ON_CREATE_LOCATION, grantResults));
    }

    @Test
    public void updateLocationPermissionsShouldReturnFalseWhenCase1IsFulfilledAndIfIsFalse() {
        int[] grantResults = {PackageManager.PERMISSION_DENIED};
        assertFalse(permission.updateLocationPermissions(REQUEST_PERM_ON_CREATE_LOCATION, grantResults));
    }

    @Test
    public void updateLocationPermissionsShouldReturnTrueWhenCase2IsFulfilledAndIfIsTrue() {
        int[] grantResults = {PackageManager.PERMISSION_DENIED, PackageManager.PERMISSION_GRANTED};
        assertTrue(permission.updateLocationPermissions(REQUEST_PERM_ON_CREATE_STORAGE_AND_LOCATION, grantResults));
    }

    @Test
    public void updateLocationPermissionsShouldReturnFalseWhenCase2IsFulfilledAndIfIsFalse() {
        int[] grantResults = {PackageManager.PERMISSION_GRANTED, PackageManager.PERMISSION_DENIED};
        assertFalse(permission.updateLocationPermissions(REQUEST_PERM_ON_CREATE_STORAGE_AND_LOCATION, grantResults));
    }

    @Test
    public void updateLocationPermissionsShouldReturnFalseWhenNeitherCase1or2IsFulfilled() {
        int[] grantResults = {};
        assertFalse(permission.updateLocationPermissions(REQUEST_PERM_ON_CREATE_STORAGE, grantResults));
    }



    // 3 Tests for updatePermissionFromSubmitButton() checking Case1 fulfilled(or not) and if = true/false
    @Test
    public void updatePermissionFromSubmitButtonShouldReturnTrueWhenCase1IsFulfilledAndIfIsTrue(){
        int[] grantResults={PackageManager.PERMISSION_GRANTED};
        assertTrue(permission.updatePermissionFromSubmitButton(REQUEST_PERM_ON_SUBMIT_STORAGE, grantResults));
    }

    @Test
    public void updatePermissionFromSubmitButtonShouldReturnFalseWhenCase1IsFulfilledAndIfIsFalse(){
        int[] grantResults={PackageManager.PERMISSION_DENIED};
        assertFalse(permission.updatePermissionFromSubmitButton(REQUEST_PERM_ON_SUBMIT_STORAGE, grantResults));
    }

    @Test
    public void updatePermissionFromSubmitButtonShouldReturnFalseWhenCase1IsNotFulfilled(){
        int[] grantResults = {};
        assertFalse(permission.updatePermissionFromSubmitButton(REQUEST_PERM_ON_CREATE_STORAGE, grantResults));
    }


    @Test
    public void checkStoragePermissionShouldReturnTrueIfSDKCorrectAndIfIsTrue() throws Exception {
        setFinalSDK_INT(1);
        when(mockPermission.needsToRequestStoragePermission(mockUri, mockContext)).thenReturn(true);
        assertFalse(mockPermission.checkStoragePermission(mockUri, mockContext));
    }

    @Test
    public void checkStoragePermissionShouldReturnFalseIfSDKCorrectAndIfIsFalse() throws Exception {
        setFinalSDK_INT(1);
        when(mockPermission.needsToRequestStoragePermission(mockUri, mockContext)).thenReturn(false);
        assertTrue(mockPermission.checkStoragePermission(mockUri, mockContext));
    }

    @Test
    public void checkStoragePermissionShouldReturnFalseIfSDKLow() throws Exception {
        setFinalSDK_INT(-1);
        assertFalse(mockPermission.checkStoragePermission(mockUri, mockContext));
    }


    // Method to change the Build.VERSION.SDK_INT for testing purposes
    static void setFinalSDK_INT(int value) throws Exception {
        Field field = Build.VERSION.class.getField("SDK_INT");

        field.setAccessible(true);
        Field modifiersField = Field.class.getDeclaredField("modifiers");

        modifiersField.setAccessible(true);
        modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);

        field.set(null, Build.VERSION_CODES.M + value);
    }

}
