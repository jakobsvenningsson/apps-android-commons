package fr.free.nrw.commons.upload;

import android.support.design.widget.Snackbar;

import dagger.Provides;

/**
 * Created by jakobsvenningsson on 2018-02-15.
 */

public interface ShareActivityInterface {
    public void startPreprocessing(boolean setImageBackground);
    public void startPreprocessingAndUpload();
    public void setSnackbar(Snackbar snackbar);
    public Snackbar requestPermissionUsingSnackBar(String rationale,
                                                   final String[] perms,
                                                   final int code);
}
