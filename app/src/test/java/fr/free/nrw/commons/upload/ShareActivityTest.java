package fr.free.nrw.commons.upload;

import android.net.Uri;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import javax.inject.Inject;

import fr.free.nrw.commons.BuildConfig;
import fr.free.nrw.commons.TestCommonsApplication;
import fr.free.nrw.commons.caching.CacheController;
import fr.free.nrw.commons.contributions.Contribution;
import fr.free.nrw.commons.nearby.NearbyActivity;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
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

    @Before
    public void setUp() {
        shareActivity = Robolectric.buildActivity(ShareActivity.class).get();
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void test1() {
        //when(fileImageManager.getFileMetadata(anyBoolean(), any(), any(), any(), any())).thenReturn("1 -1");
        //doNothing().when(uploadController).startUpload("testTitle", Uri.parse("testUri"), "anyDescription", anyString(), anyString(),
                                       // anyString(), any());
        Assert.assertNull(null);
    }
}
