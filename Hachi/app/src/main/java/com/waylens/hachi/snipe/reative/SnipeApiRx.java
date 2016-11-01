package com.waylens.hachi.snipe.reative;

import com.waylens.hachi.snipe.vdb.Clip;
import com.waylens.hachi.snipe.vdb.ClipSet;
import com.waylens.hachi.snipe.vdb.SpaceInfo;
import com.waylens.hachi.snipe.vdb.rawdata.RawDataBlock;
import com.waylens.hachi.snipe.vdb.urls.PlaybackUrl;
import com.waylens.hachi.snipe.vdb.urls.PlaylistPlaybackUrl;

import java.util.List;
import java.util.concurrent.ExecutionException;

import rx.Observable;
import rx.functions.Func0;
import rx.functions.Func1;


/**
 * Created by Xiaofei on 2016/10/11.
 */

public class SnipeApiRx {

    public static Observable<ClipSet> getClipSetRx(final int type, final int flag) {
        return getClipSetRx(type, flag, 0);
    }

    public static Observable<ClipSet> getClipSetRx(final int type, final int flag, final int attr) {
        return Observable.defer(new Func0<Observable<ClipSet>>() {
            @Override
            public Observable<ClipSet> call() {
                try {
                    return Observable.just(SnipeApi.getClipSet(type, flag, attr));
                } catch (ExecutionException | InterruptedException e) {
                    return Observable.error(e);
                }
            }
        });
    }

    public static Observable<Integer> deleteClipRx(final Clip.ID cid) {
        return Observable.defer(new Func0<Observable<Integer>>() {
            @Override
            public Observable<Integer> call() {
                try {
                    return Observable.just(SnipeApi.deleteClip(cid));
                } catch (ExecutionException | InterruptedException e) {
                    return Observable.error(e);
                }
            }
        });
    }

    public static Observable<Integer> deleteClipListRx(List<Clip> clipList) {
        return Observable.from(clipList)
            .concatMap(new Func1<Clip, Observable<Integer>>() {
                @Override
                public Observable<Integer> call(Clip clip) {
                    return SnipeApiRx.deleteClipRx(clip.cid);
                }
            });
    }

    public static Observable<SpaceInfo> getSpaceInfoRx() {
        return Observable.defer(new Func0<Observable<SpaceInfo>>() {
            @Override
            public Observable<SpaceInfo> call() {
                try {
                    return Observable.just(SnipeApi.getSpaceInfo());
                } catch (ExecutionException | InterruptedException e) {
                    return Observable.error(e);
                }
            }
        });
    }

    public static Observable<RawDataBlock> getRawDataBlockRx(final Clip clip, final int dataType) {
        return getRawDataBlockRx(clip, dataType, clip.getStartTimeMs(), clip.getDurationMs());
    }

    public static Observable<RawDataBlock> getRawDataBlockRx(final Clip clip, final int dataType, final long startTime, final int duration) {
        return Observable.defer(new Func0<Observable<RawDataBlock>>() {
            @Override
            public Observable<RawDataBlock> call() {
                return Observable.just(SnipeApi.getRawDataBlock(clip, dataType, startTime, duration));
            }
        });
    }
    public static Observable<byte[]> getRawDataBufRx(final Clip clip, final int dataType, final long startTime, final int duration) {
        return Observable.defer(new Func0<Observable<byte[]>>() {
            @Override
            public Observable<byte[]> call() {
                return Observable.just(SnipeApi.getRawDataBuf(clip, dataType, startTime, duration));
            }
        });
    }

    public static Observable<PlaylistPlaybackUrl> getPlaylistPlaybackUrl(final int playlistId, final int startTime) {
        return Observable.defer(new Func0<Observable<PlaylistPlaybackUrl>>() {
            @Override
            public Observable<PlaylistPlaybackUrl> call() {
                try {
                    return Observable.just(SnipeApi.getPlaylistPlaybackUrl(playlistId, startTime));
                } catch (ExecutionException | InterruptedException e) {
                    return Observable.error(e);
                }
            }
        });
    }


    public static Observable<PlaybackUrl> getClipPlaybackUrl(final Clip.ID clipId, final long startTime, final long clipTimeMs, final int maxLength) {
        return Observable.defer(new Func0<Observable<PlaybackUrl>>() {
            @Override
            public Observable<PlaybackUrl> call() {
                try {
                    return Observable.just(SnipeApi.getClipPlaybackUrl(clipId, startTime, clipTimeMs, maxLength));
                } catch (ExecutionException | InterruptedException e) {
                    return Observable.error(e);
                }
            }
        });
    }

    public static Observable<Integer> addHighlightRx(final Clip.ID clipId, final long startTimeMs, final long endTimeMs) {
        return Observable.defer(new Func0<Observable<Integer>>() {
            @Override
            public Observable<Integer> call() {
                try {
                    return Observable.just(SnipeApi.addHighlight(clipId, startTimeMs, endTimeMs));
                } catch (ExecutionException | InterruptedException e) {
                    return Observable.error(e);
                }
            }
        });
    }


}
