package fr.free.nrw.commons.upload;

import android.content.Context;
import android.net.Uri;
import android.os.Build;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import fr.free.nrw.commons.BuildConfig;
import fr.free.nrw.commons.TestCommonsApplication;
import fr.free.nrw.commons.caching.CacheController;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 21, application = TestCommonsApplication.class)
public class ShareActivityTest {

    @Mock
    private Permission permission;
    @Mock
    private FileImageManager fileImageManager;
    @Mock
    private UploadController uploadController;
    @Mock
    private CacheController cacheController;
    @InjectMocks
    private ShareActivity shareActivity;

    String title = "testTitle";
    String description = "testDescription";

    @Before
    public void setUp() {
        shareActivity = Robolectric.buildActivity(ShareActivity.class).get();
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void startUploadShouldCallGetFileMetaDataAndStartUploadWhenPermissionIsGranted() throws Exception {
        ShareActivityTest.setFinalSDK_INT(0);
        shareActivity.uploadActionInitiated(title, description);
        when(permission.needsToRequestStoragePermission(any(Uri.class), any(Context.class))).thenReturn(false);
        when(fileImageManager.getFileMetadata(anyBoolean(), any(), any(), any(), any())).thenReturn("1 -1");
        doNothing().when(uploadController).startUpload(eq(title), any(Uri.class), eq(description), anyString(), anyString(),anyString(), any());
        verify(uploadController, times(1)).startUpload(eq(title), any(Uri.class), eq(description), anyString(), anyString(),anyString(), any());
        verify(fileImageManager, times(1)).getFileMetadata(anyBoolean(), any(), any(), any(), any());
    }

    @Test
    public void startUploadShouldAskForPermissionIfBuildVersionIsGreaterThanASpecificVersionAndDoesNotOwnFile() throws Exception {
        ShareActivityTest.setFinalSDK_INT(1);
        when(shareActivity.permission.needsToRequestStoragePermission(any(Uri.class), any(Context.class))).thenReturn(true);
        doNothing().when(permission).requestPermission(any());
        shareActivity.uploadActionInitiated(title, description);
        verify(permission, times(1)).requestPermission(any());
        verify(uploadController, times(0)).startUpload(eq(title), any(Uri.class), eq(description), anyString(), anyString(),anyString(), any());
        verify(fileImageManager, times(0)).getFileMetadata(anyBoolean(), any(), any(), any(), any());
    }

    @Test
    public void startUploadShouldNotAskForPermissionIfBuildVersionIsGreaterThanASpecificVersionButOwnTheFile() throws Exception {
        ShareActivityTest.setFinalSDK_INT(1);
        when(permission.needsToRequestStoragePermission(any(Uri.class), any(Context.class))).thenReturn(false);
        shareActivity.uploadActionInitiated(title, description);
        verify(uploadController, times(1)).startUpload(eq(title), any(Uri.class), eq(description), anyString(), anyString(),anyString(), any());
        verify(fileImageManager, times(1)).getFileMetadata(anyBoolean(), any(), any(), any(), any());
    }

    @Test
    public void startUploadShouldNotAskForPermissionIfBuildVersionIsLowerThanASpecificVersion() throws Exception {
        ShareActivityTest.setFinalSDK_INT(-1);
        shareActivity.uploadActionInitiated(title, description);
        verify(uploadController, times(1)).startUpload(eq(title), any(Uri.class), eq(description), anyString(), anyString(),anyString(), any());
        verify(fileImageManager, times(1)).getFileMetadata(anyBoolean(), any(), any(), any(), any());
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
