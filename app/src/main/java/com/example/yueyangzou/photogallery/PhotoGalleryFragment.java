package com.example.yueyangzou.photogallery;

import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.AbsListView;
import android.widget.ImageView;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by yueyangzou on 16/8/16.
 */
public class PhotoGalleryFragment extends Fragment{
    private static final String TAG = "PhotoGalleryFragment";
    private RecyclerView mPhotoRecyclerView;
    private List<GalleryItem> mItems = new ArrayList<>();
    private ThumbnailDownloader<PhotoHolder> mThumbnailDownloader;

    private int fetched_page = 1;

    public static PhotoGalleryFragment newInstance() {
        return new PhotoGalleryFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        new FetchItemsTask().execute(fetched_page);
        Handler responseHandler = new Handler();

        mThumbnailDownloader = new ThumbnailDownloader<>(responseHandler);
        mThumbnailDownloader.setThumbnailDownloadListener(
                new ThumbnailDownloader.ThumbnailDownloadListener<PhotoHolder>() {
                    @Override
                    public void onThumbnailDownloaded(PhotoHolder target, Bitmap thumbnail) {
                        Drawable drawable = new BitmapDrawable(getResources(), thumbnail);
                        target.bindDrawable(drawable);
                    }
                }
        );
        mThumbnailDownloader.start();
        mThumbnailDownloader.getLooper();
        Log.i(TAG, "Background thread started");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_photo_gallery, container, false);
        mPhotoRecyclerView = (RecyclerView)v.findViewById(R.id.fragment_photo_gallery_recyclerview);
        mPhotoRecyclerView.setLayoutManager(new GridLayoutManager(getActivity(), 3));
//        mPhotoRecyclerView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
//            @Override
//            public void onGlobalLayout() {
//                Point size = new Point();
//                getActivity().getWindowManager().getDefaultDisplay().getSize(size);
//                int newColumns = (int) Math.floor(size.x*3/1440);
//                if (newColumns != 3) {
//                    GridLayoutManager layoutManager = (GridLayoutManager) mPhotoRecyclerView.getLayoutManager();
//                    layoutManager.setSpanCount(newColumns);
//                }
//            }
//        });
        setupAdapter();
        mPhotoRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {

            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                PhotoAdapter adapter = (PhotoAdapter) recyclerView.getAdapter();
                int lastPosition = adapter.getLastBoundPosition();
                GridLayoutManager layoutManager = (GridLayoutManager) recyclerView.getLayoutManager();
                int loadBufferPosition = 1;
                if (lastPosition >= adapter.getItemCount() - layoutManager.getSpanCount() - loadBufferPosition) {
                    new FetchItemsTask().execute(fetched_page);
//                    Log.i(TAG, "last postion" + String.valueOf(fetched_page));
                }
            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
            }
        });
        return v;
    }
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mThumbnailDownloader.clearQueue();
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        mThumbnailDownloader.quit();
        Log.i(TAG, "Background thread destroyed");
    }


    private void setupAdapter() {
//        if (isAdded()) {
//            mPhotoRecyclerView.setAdapter(new PhotoAdapter(mItems));
//        }

        if (getActivity() == null || mPhotoRecyclerView == null) return;
        if (mItems != null) {
            mPhotoRecyclerView.setAdapter(new PhotoAdapter(mItems));
        }
        else {
            mPhotoRecyclerView.setAdapter(null);
        }
    }
    private class PhotoHolder extends RecyclerView.ViewHolder {
       private ImageView mItemImageView;

        public PhotoHolder(View itemView) {
            super(itemView);

            mItemImageView = (ImageView)itemView.findViewById(R.id.fragment_photo_gallery_image_view);

        }
//        public void bindGalleryItem(GalleryItem item) {
//            mTitleTextView.setText(item.toString());
//        }
        public void bindDrawable(Drawable drawable) {
            mItemImageView.setImageDrawable(drawable);
        }
    }


    private class PhotoAdapter extends RecyclerView.Adapter<PhotoHolder> {
        private List<GalleryItem> mGalleryItems;
        private int lastBoundPosition;

        public int getLastBoundPosition() {
            return lastBoundPosition;
        }

        public PhotoAdapter(List<GalleryItem> galleryItems) {
            mGalleryItems = galleryItems;
        }

        @Override
        public PhotoHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(getActivity());
            View view = inflater.inflate(R.layout.gallery_item, viewGroup, false);
            return new PhotoHolder(view);
        }

        @Override
        public void onBindViewHolder(PhotoHolder photoHolder, int position) {
            GalleryItem galleryItem = mGalleryItems.get(position);
            Drawable placeholder = getResources().getDrawable(R.drawable.ic_launcher);
            photoHolder.bindDrawable(placeholder);
            mThumbnailDownloader.queueThumbnail(photoHolder, galleryItem.getUrl());

            lastBoundPosition = position;
//            Log.i(TAG, "Last bound position is " + Integer.toString(lastBoundPosition));
        }
        @Override
        public int getItemCount() {
            return mGalleryItems.size();
        }
    }

    private class FetchItemsTask extends AsyncTask<Integer, Void, List<GalleryItem>> {
        @Override
        protected List<GalleryItem> doInBackground(Integer... params) {
//            try {
//                String result = new FlickrFetchr().getUrlString("http://www.google.com");
//                Log.i(TAG, "Fetched contents of URL: " + result);
//            }
//            catch (IOException ioe) {
//                Log.e(TAG, "Failed to fetch URL: ", ioe);
//            }
            return new FlickrFetchr().fetchItems(params[0]);
//            return null;
        }

        @Override
        protected void onPostExecute(List<GalleryItem> items) {
            if (fetched_page > 1) {
                mItems.addAll(items);
                mPhotoRecyclerView.getAdapter().notifyDataSetChanged();
            }
            else {
                mItems = items;
                setupAdapter();
            }
            fetched_page++;

        }
    }
}
