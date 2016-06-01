package com.waylens.hachi.ui.clips;

import android.Manifest;
import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.MenuItem;
import android.view.View;

import com.waylens.hachi.R;
import com.waylens.hachi.ui.activities.BaseActivity;
import com.waylens.hachi.ui.fragments.MusicFragment;

/**
 * Created by Richard on 3/7/16.
 */
public class MusicDownloadActivity extends BaseActivity {

    public static final int PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 100;

    MusicFragment mMusicFragment;

    public static void launchForResult(Activity activity, int requestCode) {
        Intent intent = new Intent(activity, MusicDownloadActivity.class);
        activity.startActivityForResult(intent, requestCode);
    }

    public static void launchForResult(Fragment fragment, int requestCode) {
        Intent intent = new Intent(fragment.getActivity(), MusicDownloadActivity.class);
        fragment.startActivityForResult(intent, requestCode);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_single_fragment);
        setupToolbar();

        setHomeAsUpIndicator(R.drawable.navbar_close);


        mMusicFragment = new MusicFragment();
        getFragmentManager().beginTransaction()
                .replace(R.id.fragment_content, mMusicFragment).commit();
    }

    @Override
    public void setupToolbar() {
        super.setupToolbar();
        getToolbar().setTitle(R.string.musics);
        getToolbar().setNavigationIcon(R.drawable.navbar_close);
        getToolbar().setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }



    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE:
                int i = 0;
                for (String permission : permissions) {
                    if (Manifest.permission.WRITE_EXTERNAL_STORAGE.equals(permission)
                            && grantResults[i] == PackageManager.PERMISSION_GRANTED
                            && mMusicFragment != null) {
                        mMusicFragment.continueDownloadMusic();
                        break;
                    }
                    i++;
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }
}
