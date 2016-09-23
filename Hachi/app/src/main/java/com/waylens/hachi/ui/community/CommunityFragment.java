package com.waylens.hachi.ui.community;

import android.app.Activity;
import android.app.Fragment;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.speech.RecognizerIntent;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.TextView;

import com.github.clans.fab.FloatingActionMenu;
import com.lapism.searchview.SearchAdapter;
import com.lapism.searchview.SearchHistoryTable;
import com.lapism.searchview.SearchItem;
import com.lapism.searchview.SearchView;
import com.orhanobut.logger.Logger;
import com.waylens.hachi.R;
import com.waylens.hachi.app.GlobalVariables;
import com.waylens.hachi.session.SessionManager;
import com.waylens.hachi.ui.activities.NotificationActivity;
import com.waylens.hachi.ui.adapters.SimpleFragmentPagerAdapter;
import com.waylens.hachi.ui.clips.ClipChooserActivity;
import com.waylens.hachi.ui.community.event.ScrollEvent;
import com.waylens.hachi.ui.community.feed.FeedFragment;
import com.waylens.hachi.ui.community.feed.MomentListFragment;
import com.waylens.hachi.ui.fragments.BaseFragment;
import com.waylens.hachi.ui.fragments.FragmentNavigator;
import com.xfdingustc.rxutils.library.RxBus;
import com.xfdingustc.rxutils.library.SimpleSubscribe;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.OnClick;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;


/**
 * Created by Xiaofei on 2016/1/22.
 */
public class CommunityFragment extends BaseFragment implements FragmentNavigator {
    private static final String TAG = CommunityFragment.class.getSimpleName();

    private SimpleFragmentPagerAdapter mFeedPageAdapter;

    private SearchHistoryTable mHistoryDatabase;

    private final static int TAKE_PHOTO = 1;
    private final static int FROM_LOCAL = 2;

    private Uri mPictureUri;

    private static final int REQUEST_CODE_CHOOSE_CLIP = 1000;

    private Subscription mListScrollSubscription;

    @BindView(R.id.viewpager)
    ViewPager mViewPager;

    @BindView(R.id.searchView)
    SearchView mSearchView;

    @BindView(R.id.fab_menu)
    FloatingActionMenu mFabMenu;

    @OnClick(R.id.fab_from_waylens)
    public void onFabFromWaylensClicked() {
        ClipChooserActivity.launch(getActivity(), false);
        mFabMenu.close(false);
    }

    @OnClick(R.id.fab_from_camera)
    public void onFabFromCameraClicked() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        ContentValues values = new ContentValues(2);
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        //mAvatarUri = Uri.fromFile(new File(GlobalVariables.getPictureName()));
        mPictureUri = GlobalVariables.getPictureUri();
        Logger.t(TAG).d(mPictureUri.getPath());
        intent.putExtra(MediaStore.EXTRA_OUTPUT, mPictureUri);
        startActivityForResult(intent, TAKE_PHOTO);
        mFabMenu.close(false);
    }

    @OnClick(R.id.fab_from_gallery)
    public void onFabFromGalleryClicked() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, FROM_LOCAL);
        mFabMenu.close(false);
    }


    @Override
    protected String getRequestTag() {
        return TAG;
    }


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = createFragmentView(inflater, container, R.layout.fragment_moment, savedInstanceState);
        setupSearchView();
        setupViewPager();
//        mFabMenu.setMenuButtonShowAnimation(AnimationUtils.loadAnimation(getActivity(), R.anim.fab_scale_up));

        return view;
    }


    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mFabMenu.setClosedOnTouchOutside(true);

        mFabMenu.hideMenuButton(false);
//        mFabMenu.showMenuButton(true);
    }

    @Override
    public void onStart() {
        super.onStart();
        mListScrollSubscription = RxBus.getDefault().toObserverable(ScrollEvent.class)
            .subscribeOn(AndroidSchedulers.mainThread())
            .subscribe(new SimpleSubscribe<ScrollEvent>() {
                @Override
                public void onNext(ScrollEvent scrollEvent) {
                    if (scrollEvent.shouldHide) {
                        mFabMenu.hideMenuButton(true);
                    } else {
                        mFabMenu.showMenuButton(true);
                    }
                }
            });
    }

    @Override
    public void onStop() {
        super.onStop();
        if (!mListScrollSubscription.isUnsubscribed()) {
            mListScrollSubscription.unsubscribe();
        }
    }

    private void setupSearchView() {
        mHistoryDatabase = new SearchHistoryTable(getActivity());
        mHistoryDatabase.setHistorySize(5);
        mSearchView.setVersion(SearchView.VERSION_MENU_ITEM);
        mSearchView.setVersionMargins(SearchView.VERSION_MARGINS_MENU_ITEM);
        mSearchView.setHint(R.string.search_hint);
        mSearchView.setTextSize(16);
        mSearchView.setDivider(false);
        mSearchView.setVoice(true);
        mSearchView.setAnimationDuration(SearchView.ANIMATION_DURATION);
        mSearchView.setShadowColor(ContextCompat.getColor(getActivity(), R.color.search_shadow_layout));
        mSearchView.setTheme(SearchView.THEME_DARK, true);
        mSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                mHistoryDatabase.addItem(new SearchItem(query));
                MomentSearchActivity.launch(getActivity(), query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });


        List<SearchItem> suggestionsList = new ArrayList<>();


        SearchAdapter searchAdapter = new SearchAdapter(getActivity(), suggestionsList);
        searchAdapter.setOnItemClickListener(new SearchAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {

                TextView textView = (TextView) view.findViewById(R.id.textView_item_text);
                String query = textView.getText().toString();
//                    getData(query, position);
                MomentSearchActivity.launch(getActivity(), query);
            }
        });
        mSearchView.setAdapter(searchAdapter);
    }

    @Override
    public void setupToolbar() {
        getToolbar().setTitle(R.string.moments);
        super.setupToolbar();
    }


    @Override
    public void onResume() {
        super.onResume();
        getToolbar().getMenu().clear();
        getToolbar().inflateMenu(R.menu.menu_community);
        if (!SessionManager.getInstance().isLoggedIn()) {
            getToolbar().getMenu().removeItem(R.id.my_notification);
        }
        getToolbar().setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.search:
//                        MomentSearchActivity.launch(getActivity());
                        mSearchView.open(true);
                        return false;
                    case R.id.my_notification:
                        NotificationActivity.launch(getActivity());
                        return false;
                    default:
                        return false;
                }

            }
        });
    }

    @Override
    public boolean onInterceptBackPressed() {
        Fragment fragment = mFeedPageAdapter.getItem(mViewPager.getCurrentItem());
        if (fragment instanceof FragmentNavigator) {
            return ((FragmentNavigator) fragment).onInterceptBackPressed();
        }
        return false;
    }


    private void setupViewPager() {
        mFeedPageAdapter = new FeedPageAdapter(getChildFragmentManager());
        mFeedPageAdapter.addFragment(FeedFragment.newInstance(), getString(R.string.my_feed));
        mFeedPageAdapter.addFragment(MomentListFragment.newInstance(MomentListFragment.FEED_TAG_LATEST), getString(R.string.latest));
        mFeedPageAdapter.addFragment(MomentListFragment.newInstance(MomentListFragment.FEED_TAG_STAFF_PICKS), getString(R.string
            .staff_picks));
        //mFeedPageAdapter.addFragment(PerformanceTestFragment.newInstance(0), getString(R.string.leaderboard));
        mViewPager.setAdapter(mFeedPageAdapter);


        //mTabLayout.setTabMode(TabLayout.MODE_SCROLLABLE);
        getTablayout().setupWithViewPager(mViewPager);

        if (SessionManager.getInstance().isLoggedIn()) {
            mViewPager.setCurrentItem(0);
        } else {
            mViewPager.setCurrentItem(4);
        }

    }

    public void notifyDateChanged() {
        if (mFeedPageAdapter != null) {
            mFeedPageAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_CANCELED) {
            return;
        }

        if (requestCode == FROM_LOCAL) {
            Uri imageUri = data.getData();
            Logger.t(TAG).d("image selected path " + imageUri.getPath());

            String[] projection = {MediaStore.Images.Media.DATA};
            Cursor cursor = getActivity().getContentResolver().query(imageUri, projection, null, null, null);
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            String imageUrl = cursor.getString(column_index);
            PublishActivity.launch(getActivity(), imageUrl);
        } else if (requestCode == TAKE_PHOTO) {
            PublishActivity.launch(getActivity(), mPictureUri.getPath());
        } else if (requestCode == SearchView.SPEECH_REQUEST_CODE) {
            List<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (results != null && results.size() > 0) {
                String searchWrd = results.get(0);
                if (!TextUtils.isEmpty(searchWrd)) {
                    mSearchView.setQuery(searchWrd);
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }


}
