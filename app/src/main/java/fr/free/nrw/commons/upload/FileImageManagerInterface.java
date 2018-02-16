package fr.free.nrw.commons.upload;

import android.content.Context;
import android.net.Uri;

import fr.free.nrw.commons.caching.CacheController;

public interface FileImageManagerInterface {
    public void performPreuploadProcessingOfFile(Context context, Uri mediaUri, CacheController cacheController, GPSExtractor imageObj);

    public boolean useImageCoords(String decimalCoords, GPSExtractor imageObj, CacheController cacheController, Context context);
}
