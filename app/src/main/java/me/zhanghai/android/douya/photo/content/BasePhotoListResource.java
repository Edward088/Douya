/*
 * Copyright (c) 2017 Zhang Hai <Dreaming.in.Code.ZH@Gmail.com>
 * All Rights Reserved.
 */

package me.zhanghai.android.douya.photo.content;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.Collections;
import java.util.List;

import me.zhanghai.android.douya.content.MoreRawListResourceFragment;
import me.zhanghai.android.douya.eventbus.PhotoDeletedEvent;
import me.zhanghai.android.douya.network.api.ApiError;
import me.zhanghai.android.douya.network.api.ApiRequest;
import me.zhanghai.android.douya.network.api.info.frodo.Photo;
import me.zhanghai.android.douya.network.api.info.frodo.PhotoList;

public abstract class BasePhotoListResource extends MoreRawListResourceFragment<Photo, PhotoList> {

    @Override
    protected ApiRequest<PhotoList> onCreateRequest(boolean more, int count) {
        Integer start = more && has() ? get().size() : null;
        return onCreateRequest(start, count);
    }

    protected abstract ApiRequest<PhotoList> onCreateRequest(Integer start, Integer count);

    @Override
    protected void onLoadStarted() {
        getListener().onLoadPhotoListStarted(getRequestCode());
    }

    @Override
    protected void onLoadFinished(boolean more, int count, boolean successful, PhotoList response,
                                  ApiError error) {
        onLoadFinished(more, count, successful, response != null ? response.photos : null, error);
    }

    private void onLoadFinished(boolean more, int count, boolean successful, List<Photo> response,
                                ApiError error) {
        getListener().onLoadPhotoListFinished(getRequestCode());
        if (successful) {
            if (more) {
                append(response);
                getListener().onPhotoListAppended(getRequestCode(),
                        Collections.unmodifiableList(response));
            } else {
                set(response);
                getListener().onPhotoListChanged(getRequestCode(),
                        Collections.unmodifiableList(get()));
            }
        } else {
            getListener().onLoadPhotoListError(getRequestCode(), error);
        }
    }

    protected void appendAndNotifyListener(List<Photo> photoList) {
        append(photoList);
        getListener().onPhotoListAppended(getRequestCode(), photoList);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onPhotoDeleted(PhotoDeletedEvent event) {

        if (event.isFromMyself(this) || isEmpty()) {
            return;
        }

        List<Photo> photoList = get();
        for (int i = 0, size = photoList.size(); i < size; ) {
            Photo photo = photoList.get(i);
            if (photo.id == event.photoId) {
                photoList.remove(i);
                getListener().onPhotoRemoved(getRequestCode(), i);
                --size;
            } else {
                ++i;
            }
        }
    }

    private Listener getListener() {
        return (Listener) getTarget();
    }

    public interface Listener {
        void onLoadPhotoListStarted(int requestCode);
        void onLoadPhotoListFinished(int requestCode);
        void onLoadPhotoListError(int requestCode, ApiError error);
        /**
         * @param newPhotoList Unmodifiable.
         */
        void onPhotoListChanged(int requestCode, List<Photo> newPhotoList);
        /**
         * @param appendedPhotoList Unmodifiable.
         */
        void onPhotoListAppended(int requestCode, List<Photo> appendedPhotoList);
        void onPhotoRemoved(int requestCode, int position);
    }
}
