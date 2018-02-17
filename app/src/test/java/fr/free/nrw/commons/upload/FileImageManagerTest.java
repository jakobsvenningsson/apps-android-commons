package fr.free.nrw.commons.upload;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.InputStream;

import fr.free.nrw.commons.caching.CacheController;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Created by jakobsvenningsson on 2018-02-17.
 */

public class FileImageManagerTest {
    //TODO Write unit tests for the FileImageManagerClass

    @Mock
    private Permission permission;
    @InjectMocks
    private FileImageManager fileImageManager;

    @Before
    public void setUp() {
        fileImageManager = new FileImageManager();
    }

    @Test
    public void performPreuploadProcessingOfFileShouldNotInvokeAnyMethodsOnContextIfNoPermission() throws Exception {
        Context mockContext = mock(Context.class);
        ContentResolver mockResolver = mock(ContentResolver.class);
        InputStream mockInputStream = mock(InputStream.class);
        Uri uri = mock(Uri.class);
        CacheController cacheController = new CacheController();
        GPSExtractor imageObj = mock(GPSExtractor.class);
        MockitoAnnotations.initMocks(this);
        when(permission.getStoragePermitted()).thenReturn(false);
        when(permission.getUseNewPermissions()).thenReturn(false);
        when(permission.getLocationPermitted()).thenReturn(false);
        when(mockContext.getContentResolver()).thenReturn(mockResolver);
        when(mockResolver.openInputStream(any())).thenReturn(mockInputStream);
        when(mockInputStream.read()).thenReturn(-1);
        //fileImageManager.performPreuploadProcessingOfFile(mockContext, uri, cacheController, imageObj);
        verify(mockContext, times(0)).getContentResolver();
    }
}
