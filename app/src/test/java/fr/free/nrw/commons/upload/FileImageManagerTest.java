package fr.free.nrw.commons.upload;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;

import fr.free.nrw.commons.BuildConfig;
import fr.free.nrw.commons.TestCommonsApplication;
import fr.free.nrw.commons.caching.CacheController;
import fr.free.nrw.commons.mwapi.MediaWikiApi;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 21, application = TestCommonsApplication.class)
public class FileImageManagerTest {
    //TODO Write unit tests for the FileImageManagerClass

    @Mock
    private Permission permission;
    @Mock
    private MediaWikiApi mwApi;
    @Mock
    private SharedPreferences prefs;
    @Mock
    private CacheController cache;
    @Mock
    private Uri uri;
    @Mock
    private Context mockContext;
    @Mock
    private InputStream mockInputStream;
    @Mock
    private ContentResolver mockResolver;
    @InjectMocks
    private FileImageManager fileImageManager;

    @Before
    public void setUp() {
        fileImageManager = new FileImageManager(mwApi, permission, prefs);
    }

    @Test
    public void isImageDuplicateShouldReturnFalseAndDuplicateCheckShouldFailIfUsingNewPermissionAndStorageNotPermitted() throws Exception {
        fileImageManager = new FileImageManager(mwApi, permission, prefs);
        MockitoAnnotations.initMocks(this);
        when(permission.getStoragePermitted()).thenReturn(false);
        when(permission.getUseNewPermission()).thenReturn(true);
        assertFalse(fileImageManager.isImageDuplicate(mockContext, uri));
        assertFalse(fileImageManager.getDuplicateCheckPassed());
    }

    @Test
    public void isImageDuplicateShouldReturnTrueAndDuplicateCheckShouldPassIfStoragePermittedAndGetUseNewPermission() throws IOException {
        fileImageManager = new FileImageManager(mwApi, permission, prefs);
        MockitoAnnotations.initMocks(this);
        when(permission.getStoragePermitted()).thenReturn(true);
        when(permission.getUseNewPermission()).thenReturn(true);
        when(mockContext.getContentResolver()).thenReturn(mockResolver);
        when(mockResolver.openInputStream(any())).thenReturn(mockInputStream);
        when(mwApi.existingFile(any())).thenReturn(false);
        assertTrue(fileImageManager.isImageDuplicate(mockContext, uri));
        assertTrue(fileImageManager.getDuplicateCheckPassed());
    }

    @Test
    public void isImageDuplicateShouldReturnTrueAndDuplicateCheckShouldPassIfNotStoragePermittedAndNotGetUseNewPermission() throws IOException {
        fileImageManager = new FileImageManager(mwApi, permission, prefs);
        MockitoAnnotations.initMocks(this);
        when(permission.getStoragePermitted()).thenReturn(true);
        when(permission.getUseNewPermission()).thenReturn(false);
        when(mockContext.getContentResolver()).thenReturn(mockResolver);
        when(mockResolver.openInputStream(any())).thenReturn(mockInputStream);
        when(mwApi.existingFile(any())).thenReturn(false);
        assertTrue(fileImageManager.isImageDuplicate(mockContext, uri));
        assertTrue(fileImageManager.getDuplicateCheckPassed());
    }

    @Test
    public void isImageDuplicateShouldReturnTrueAndDuplicateCheckShouldFailIfExceptionOccursWhenOpeningIOStream() throws IOException {
        fileImageManager = new FileImageManager(mwApi, permission, prefs);
        MockitoAnnotations.initMocks(this);
        when(permission.getStoragePermitted()).thenReturn(true);
        when(permission.getUseNewPermission()).thenReturn(false);
        when(mockContext.getContentResolver()).thenReturn(mockResolver);
        when(mockResolver.openInputStream(any())).thenThrow(new FileNotFoundException());
        assertTrue(fileImageManager.isImageDuplicate(mockContext, uri));
        assertFalse(fileImageManager.getDuplicateCheckPassed());
    }

    @Test
    public void getFileMetaDataShouldReturnNullIfFileNotFound() throws FileNotFoundException {
        GPSExtractor imageObj = null;
        MockitoAnnotations.initMocks(this);
        when(mockContext.getContentResolver()).thenReturn(mockResolver);
        when(mockResolver.openFileDescriptor(any(), any())).thenThrow(new FileNotFoundException());
        assertNull(fileImageManager.getFileMetadata(true, mockContext, uri, imageObj, cache));
        assertNull(imageObj);
    }

    @Test
    public void getFileMetaDataShouldReturnCoordsContainedInGPSExtractorObject() throws FileNotFoundException {
        String decimalCoords = "123 123";
        FileImageManager fileImageManager2 = mock(FileImageManager.class);
        GPSExtractor imageObj = mock(GPSExtractor.class);
        MockitoAnnotations.initMocks(this);
        when(imageObj.getCoords(anyBoolean())).thenReturn(decimalCoords);
        when(fileImageManager2.getFileMetadata(anyBoolean(), any(), any(), any(), any())).thenCallRealMethod();
        when(fileImageManager2.useImageCoords(any(), any(), any(), any())).thenReturn(true);
        assertEquals(decimalCoords, fileImageManager2.getFileMetadata(true, mockContext, uri, imageObj, cache));
    }

    @Test
    public void useImageCoordsShouldreturnFalseWhenFileIsNotInCache() throws Exception {
        String decimalCoords = "123 123";
        CacheController cacheController = mock(CacheController.class);
        GPSExtractor imageObj = mock(GPSExtractor.class);
        MwVolleyApi volleyApi = mock(MwVolleyApi.class);
        when(cacheController.findCategory()).thenReturn(Collections.emptyList());
        boolean res = fileImageManager.useImageCoords(decimalCoords, imageObj, cacheController, volleyApi);
        assertFalse(res);
        verify(volleyApi, times(1)).request(decimalCoords);
    }

    @Test
    public void useImageCoordsShouldreturnTrueWhenFileIsInCache() throws Exception {
        String decimalCoords = "123 123";
        CacheController cacheController = mock(CacheController.class);
        GPSExtractor imageObj = mock(GPSExtractor.class);
        MwVolleyApi volleyApi = mock(MwVolleyApi.class);
        when(cacheController.findCategory()).thenReturn(Collections.singletonList("str"));
        boolean res = fileImageManager.useImageCoords(decimalCoords, imageObj, cacheController, volleyApi);
        assertTrue(res);
    }

    @Test
    public void useImageCoordsShouldCacheCoordsIfTheyExists() throws Exception {
        String decimalCoords = "123 123";
        CacheController cacheController = mock(CacheController.class);
        GPSExtractor imageObj = mock(GPSExtractor.class);
        MwVolleyApi volleyApi = mock(MwVolleyApi.class);
        when(imageObj.getDecLatitude()).thenReturn(-1.0);
        when(imageObj.getDecLongitude()).thenReturn(1.0);
        imageObj.imageCoordsExists = true;
        when(cacheController.findCategory()).thenReturn(Collections.singletonList("str"));
        fileImageManager.useImageCoords(decimalCoords, imageObj, cacheController, volleyApi);
        verify(cacheController, times(1)).setQtPoint(1.0, -1.0);
    }
}
