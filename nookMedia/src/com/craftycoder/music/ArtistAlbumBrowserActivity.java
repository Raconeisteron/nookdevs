/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.craftycoder.music;

import com.craftycoder.music.R;
import com.craftycoder.music.QueryBrowserActivity.QueryListAdapter.QueryHandler;
import com.nookdevs.common.GreyBarTouchInterface;
import com.nookdevs.common.GreyBarTouchListener;
import com.nookdevs.common.nookExpandableListActivity;

import android.app.ExpandableListActivity;
import android.app.SearchManager;
import android.content.AsyncQueryHandler;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.CursorWrapper;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.MediaFile;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.provider.MediaStore;
import android.util.Log;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.CursorAdapter;
import android.widget.CursorTreeAdapter;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.SectionIndexer;
import android.widget.SimpleCursorTreeAdapter;
import android.widget.TextView;
import android.widget.ExpandableListView.ExpandableListContextMenuInfo;

import java.text.Collator;


public class ArtistAlbumBrowserActivity extends nookExpandableListActivity
        implements View.OnCreateContextMenuListener, MusicUtils.Defs , GreyBarTouchInterface
{
    private String mCurrentArtistId;
    private String mCurrentArtistName;
    private String mCurrentAlbumId;
    private String mCurrentAlbumName;
    private String mCurrentArtistNameForAlbum;
    private ArtistAlbumListAdapter mAdapter;
    private boolean mAdapterSent;
    private final static int SEARCH = CHILD_MENU_BASE;

    public ArtistAlbumBrowserActivity()
    {
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent evnt) {
        boolean handled = false;
    	int pos = this.mArtistCursor.getPosition();
    	if(pos == this.mArtistCursor.getCount()) {
    		pos = -1;
    	}
    	KeyEvent event;
    	Log.d(LOGTAG,"Current Position " + pos);
        switch (keyCode) {
	        case NOOK_PAGE_UP_KEY_LEFT:
	        	event = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_UP);
	        	getExpandableListView().onKeyDown(KeyEvent.KEYCODE_DPAD_UP, event);
	            event = new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_UP);
	            getExpandableListView().onKeyUp(KeyEvent.KEYCODE_DPAD_UP, event);
	            getExpandableListView().dispatchKeyEvent(event); 
	            showSoftKeyboard();
	        	break;
	        case NOOK_PAGE_UP_KEY_RIGHT:
	        	Log.d(LOGTAG,"BACK");
	        	keyCode = KeyEvent.KEYCODE_BACK;
	        	super.onKeyDown(keyCode,new KeyEvent(evnt));
	        	break;
            case SOFT_KEYBOARD_CLEAR:
            	Log.d(LOGTAG,"CLEAR");
            	getExpandableListView().clearTextFilter();
            	break;  	        	
	        case SOFT_KEYBOARD_SUBMIT:
            	Log.d(LOGTAG,"SUBMIT");
            	this.openContextMenu(getExpandableListView());
            	break;
	        case NOOK_PAGE_UP_SWIPE:
	        	Log.d(LOGTAG,"SWIPE");
	        	break;
	        case NOOK_PAGE_DOWN_KEY_LEFT:
	            event = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_DOWN);
	            getExpandableListView().onKeyDown(KeyEvent.KEYCODE_DPAD_DOWN, event);
	            event = new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_DOWN);
	            getExpandableListView().onKeyUp(KeyEvent.KEYCODE_DPAD_DOWN, event);
	            getExpandableListView().dispatchKeyEvent(event);
	            showSoftKeyboard();
	        	break;            	
	        case NOOK_PAGE_DOWN_KEY_RIGHT:
	            event = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_CENTER);
	            getExpandableListView().onKeyDown(KeyEvent.KEYCODE_DPAD_CENTER, event);
	            event = new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_CENTER);
	            getExpandableListView().onKeyUp(KeyEvent.KEYCODE_DPAD_CENTER, event); 
	            getExpandableListView().dispatchKeyEvent(event);
	        	Log.d(LOGTAG,"FORWARD");
	        	break;            	
	        case NOOK_PAGE_DOWN_SWIPE:
	        	Log.d(LOGTAG,"SWIPE");
	        	break;
	        default:
	            break;
        }
        if (handled) {

        } 
        return handled;
    }       
    
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        if (icicle != null) {
            mCurrentAlbumId = icicle.getString("selectedalbum");
            mCurrentAlbumName = icicle.getString("selectedalbumname");
            mCurrentArtistId = icicle.getString("selectedartist");
            mCurrentArtistName = icicle.getString("selectedartistname");
        }
        MusicUtils.bindToService(this);

        IntentFilter f = new IntentFilter();
        f.addAction(Intent.ACTION_MEDIA_SCANNER_STARTED);
        f.addAction(Intent.ACTION_MEDIA_SCANNER_FINISHED);
        f.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
        f.addDataScheme("file");
        registerReceiver(mScanListener, f);

        setContentView(R.layout.media_picker_activity_expanding);
        ExpandableListView lv = getExpandableListView();
        lv.setFastScrollEnabled(true);
        lv.setOnCreateContextMenuListener(this);
        lv.setTextFilterEnabled(true);

        mAdapter = (ArtistAlbumListAdapter) getLastNonConfigurationInstance();
        if (mAdapter == null) {
            //Log.i("@@@", "starting query");
            mAdapter = new ArtistAlbumListAdapter(
                    getApplication(),
                    this,
                    null, // cursor
                    R.layout.track_list_item_group,
                    new String[] {},
                    new int[] {},
                    R.layout.track_list_item_child,
                    new String[] {},
                    new int[] {});
            setListAdapter(mAdapter);
            setTitle(R.string.working_artists);
            getArtistCursor(mAdapter.getQueryHandler(), null);
        } else {
            mAdapter.setActivity(this);
            setListAdapter(mAdapter);
            mArtistCursor = mAdapter.getCursor();
            if (mArtistCursor != null) {
                init(mArtistCursor);
            } else {
                getArtistCursor(mAdapter.getQueryHandler(), null);
            }
        }
        View b = (View) findViewById(R.id.media_picker_touchsurface);	
        b.setOnTouchListener(new GreyBarTouchListener(this));
    }

    @Override
    public Object onRetainNonConfigurationInstance() {
        mAdapterSent = true;
        return mAdapter;
    }
    
    @Override
    public void onSaveInstanceState(Bundle outcicle) {
        // need to store the selected item so we don't lose it in case
        // of an orientation switch. Otherwise we could lose it while
        // in the middle of specifying a playlist to add the item to.
        outcicle.putString("selectedalbum", mCurrentAlbumId);
        outcicle.putString("selectedalbumname", mCurrentAlbumName);
        outcicle.putString("selectedartist", mCurrentArtistId);
        outcicle.putString("selectedartistname", mCurrentArtistName);
        super.onSaveInstanceState(outcicle);
    }

    @Override
    public void onDestroy() {
        MusicUtils.unbindFromService(this);
        if (!mAdapterSent) {
            Cursor c = mAdapter.getCursor();
            if (c != null) {
                c.close();
            }
        }
        unregisterReceiver(mScanListener);
        hideSoftKeyboard();
        super.onDestroy();
    }
    
    @Override
    public void onResume() {
        super.onResume();
        IntentFilter f = new IntentFilter();
        f.addAction(MediaPlaybackService.META_CHANGED);
        f.addAction(MediaPlaybackService.QUEUE_CHANGED);
        registerReceiver(mTrackListListener, f);
        mTrackListListener.onReceive(null, null);

        MusicUtils.setSpinnerState(this);
    }

    private BroadcastReceiver mTrackListListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            getExpandableListView().invalidateViews();
        }
    };
    private BroadcastReceiver mScanListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            MusicUtils.setSpinnerState(ArtistAlbumBrowserActivity.this);
            mReScanHandler.sendEmptyMessage(0);
            if (intent.getAction().equals(Intent.ACTION_MEDIA_UNMOUNTED)) {
                MusicUtils.clearAlbumArtCache();
            }
        }
    };
    
    private Handler mReScanHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            getArtistCursor(mAdapter.getQueryHandler(), null);
        }
    };

    @Override
    public void onPause() {
        unregisterReceiver(mTrackListListener);
        mReScanHandler.removeCallbacksAndMessages(null);
        hideSoftKeyboard();
        super.onPause();
    }
    
    public void init(Cursor c) {

        mAdapter.changeCursor(c); // also sets mArtistCursor

        if (mArtistCursor == null) {
            MusicUtils.displayDatabaseError(this);
            closeContextMenu();
            mReScanHandler.sendEmptyMessageDelayed(0, 1000);
            return;
        }

        MusicUtils.hideDatabaseError(this);
        setTitle();
    }

    private void setTitle() {
        setTitle(R.string.artists_title);
    }
    
    @Override
    public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {

        mCurrentAlbumId = Long.valueOf(id).toString();
        
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setDataAndType(Uri.EMPTY, "vnd.android.cursor.dir/track");
        intent.putExtra("album", mCurrentAlbumId);
        Cursor c = (Cursor) getExpandableListAdapter().getChild(groupPosition, childPosition);
        String album = c.getString(c.getColumnIndex(MediaStore.Audio.Albums.ALBUM));
        if (album == null || album.equals(MediaFile.UNKNOWN_STRING)) {
            // unknown album, so we should include the artist ID to limit the songs to songs only by that artist 
            mArtistCursor.moveToPosition(groupPosition);
            mCurrentArtistId = mArtistCursor.getString(mArtistCursor.getColumnIndex(MediaStore.Audio.Artists._ID));
            intent.putExtra("artist", mCurrentArtistId);
        }
        startActivity(intent);
        return true;
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.add(0, GOTO_START, 0, R.string.goto_start).setIcon(R.drawable.ic_menu_music_library);
        menu.add(0, GOTO_PLAYBACK, 0, R.string.goto_playback).setIcon(R.drawable.ic_menu_playback);
        menu.add(0, SHUFFLE_ALL, 0, R.string.shuffle_all).setIcon(R.drawable.ic_menu_shuffle);
        return true;
    }
    
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.findItem(GOTO_PLAYBACK).setVisible(MusicUtils.isMusicLoaded());
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent;
        Cursor cursor;
        switch (item.getItemId()) {
            case GOTO_START:
                intent = new Intent();
                intent.setClass(this, MusicBrowserActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                return true;

            case GOTO_PLAYBACK:
                intent = new Intent("com.craftycoder.music.PLAYBACK_VIEWER");
                startActivity(intent);
                return true;
                
            case SHUFFLE_ALL:
                cursor = MusicUtils.query(this, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        new String [] { MediaStore.Audio.Media._ID}, 
                        MediaStore.Audio.Media.IS_MUSIC + "=1", null,
                        MediaStore.Audio.Media.DEFAULT_SORT_ORDER);
                if (cursor != null) {
                    MusicUtils.shuffleAll(this, cursor);
                    cursor.close();
                }
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo menuInfoIn) {
        menu.add(0, PLAY_SELECTION, 0, R.string.play_selection);
        SubMenu sub = menu.addSubMenu(0, ADD_TO_PLAYLIST, 0, R.string.add_to_playlist);
        MusicUtils.makePlaylistMenu(this, sub);
        menu.add(0, DELETE_ITEM, 0, R.string.delete_item);
        menu.add(0, CLOSE, 0, R.string.close_playlist_menu);
//        menu.add(0, SEARCH, 0, R.string.search_title);
        
        ExpandableListContextMenuInfo mi = (ExpandableListContextMenuInfo) menuInfoIn;
        setCurrentSelectionVariables(menu,mi);
    }
    private void setCurrentSelectionVariables(ContextMenu menu, ExpandableListContextMenuInfo mi) {
        int itemtype;
        int gpos;
        int cpos;
        if(mi != null) {
        	itemtype = ExpandableListView.getPackedPositionType(mi.packedPosition);
        	gpos = ExpandableListView.getPackedPositionGroup(mi.packedPosition);
        	cpos = ExpandableListView.getPackedPositionChild(mi.packedPosition);
        } else {
        	itemtype = ExpandableListView.getPackedPositionType(this.getExpandableListView().getExpandableListPosition(getExpandableListView().getSelectedItemPosition()));
        	gpos = ExpandableListView.getPackedPositionGroup(this.getExpandableListView().getExpandableListPosition(getExpandableListView().getSelectedItemPosition()));
        	cpos = ExpandableListView.getPackedPositionChild(this.getExpandableListView().getExpandableListPosition(getExpandableListView().getSelectedItemPosition()));
        }
        if (itemtype == ExpandableListView.PACKED_POSITION_TYPE_GROUP) {
            if (gpos == -1) {
                // this shouldn't happen
                Log.d("Artist/Album", "no group");
                return;
            }
            gpos = gpos - getExpandableListView().getHeaderViewsCount();
            mArtistCursor.moveToPosition(gpos);
            mCurrentArtistId = mArtistCursor.getString(mArtistCursor.getColumnIndexOrThrow(MediaStore.Audio.Artists._ID));
            mCurrentArtistName = mArtistCursor.getString(mArtistCursor.getColumnIndexOrThrow(MediaStore.Audio.Artists.ARTIST));
            mCurrentAlbumId = null;
            if(menu != null)
            	menu.setHeaderTitle(mCurrentArtistName);
            return;
        } else if (itemtype == ExpandableListView.PACKED_POSITION_TYPE_CHILD) {
            if (cpos == -1) {
                // this shouldn't happen
                Log.d("Artist/Album", "no child");
                return;
            }
            Cursor c = (Cursor) getExpandableListAdapter().getChild(gpos, cpos);
            c.moveToPosition(cpos);
            mCurrentArtistId = null;
            mCurrentAlbumId = c.getString(c.getColumnIndexOrThrow(MediaStore.Audio.Albums._ID));
            mCurrentAlbumName = c.getString(c.getColumnIndexOrThrow(MediaStore.Audio.Albums.ALBUM));
            gpos = gpos - getExpandableListView().getHeaderViewsCount();
            mArtistCursor.moveToPosition(gpos);
            mCurrentArtistNameForAlbum = mArtistCursor.getString(
                    mArtistCursor.getColumnIndexOrThrow(MediaStore.Audio.Artists.ARTIST));
            if(menu != null)
            	menu.setHeaderTitle(mCurrentAlbumName);
        }
    }
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case PLAY_SELECTION: {
                // play everything by the selected artist
                int [] list =
                    mCurrentArtistId != null ?
                    MusicUtils.getSongListForArtist(this, Integer.parseInt(mCurrentArtistId))
                    : MusicUtils.getSongListForAlbum(this, Integer.parseInt(mCurrentAlbumId));
                        
                MusicUtils.playAll(this, list, 0);
                return true;
            }

            case QUEUE: {
                int [] list =
                    mCurrentArtistId != null ?
                    MusicUtils.getSongListForArtist(this, Integer.parseInt(mCurrentArtistId))
                    : MusicUtils.getSongListForAlbum(this, Integer.parseInt(mCurrentAlbumId));
                MusicUtils.addToCurrentPlaylist(this, list);
                return true;
            }

            case NEW_PLAYLIST: {
                Intent intent = new Intent();
                intent.setClass(this, CreatePlaylist.class);
                startActivityForResult(intent, NEW_PLAYLIST);
                return true;
            }

            case PLAYLIST_SELECTED: {
                int [] list =
                    mCurrentArtistId != null ?
                    MusicUtils.getSongListForArtist(this, Integer.parseInt(mCurrentArtistId))
                    : MusicUtils.getSongListForAlbum(this, Integer.parseInt(mCurrentAlbumId));
                int playlist = item.getIntent().getIntExtra("playlist", 0);
                MusicUtils.addToPlaylist(this, list, playlist);
                return true;
            }
            
            case DELETE_ITEM: {
                int [] list;
                String desc;
                if (mCurrentArtistId != null) {
                    list = MusicUtils.getSongListForArtist(this, Integer.parseInt(mCurrentArtistId));
                    String f = getString(R.string.delete_artist_desc);
                    desc = String.format(f, mCurrentArtistName);
                } else {
                    list = MusicUtils.getSongListForAlbum(this, Integer.parseInt(mCurrentAlbumId));
                    String f = getString(R.string.delete_album_desc); 
                    desc = String.format(f, mCurrentAlbumName);
                }
                Bundle b = new Bundle();
                b.putString("description", desc);
                b.putIntArray("items", list);
                Intent intent = new Intent();
                intent.setClass(this, DeleteItems.class);
                intent.putExtras(b);
                startActivityForResult(intent, -1);
                return true;
            }
            case CLOSE: {
            	return true;
            }
            case SEARCH:
//                doSearch();
                return true;
        }
        return super.onContextItemSelected(item);
    }

    void doSearch() {
        CharSequence title = null;
        String query = null;
        
        Intent i = new Intent();
        i.setAction(MediaStore.INTENT_ACTION_MEDIA_SEARCH);
        
        if (mCurrentArtistId != null) {
            title = mCurrentArtistName;
            query = mCurrentArtistName;
            i.putExtra(MediaStore.EXTRA_MEDIA_ARTIST, mCurrentArtistName);
            i.putExtra(MediaStore.EXTRA_MEDIA_FOCUS, MediaStore.Audio.Artists.ENTRY_CONTENT_TYPE);
        } else {
            title = mCurrentAlbumName;
            query = mCurrentArtistNameForAlbum + " " + mCurrentAlbumName;
            i.putExtra(MediaStore.EXTRA_MEDIA_ARTIST, mCurrentArtistNameForAlbum);
            i.putExtra(MediaStore.EXTRA_MEDIA_ALBUM, mCurrentAlbumName);
            i.putExtra(MediaStore.EXTRA_MEDIA_FOCUS, MediaStore.Audio.Albums.ENTRY_CONTENT_TYPE);
        }
        title = getString(R.string.mediasearch, title);
        i.putExtra(SearchManager.QUERY, query);

        startActivity(Intent.createChooser(i, title));
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        switch (requestCode) {
            case SCAN_DONE:
                if (resultCode == RESULT_CANCELED) {
                    finish();
                } else {
                    getArtistCursor(mAdapter.getQueryHandler(), null);
                }
                break;

            case NEW_PLAYLIST:
                if (resultCode == RESULT_OK) {
                    Uri uri = intent.getData();
                    if (uri != null) {
                        int [] list = null;
                        if (mCurrentArtistId != null) {
                            list = MusicUtils.getSongListForArtist(this, Integer.parseInt(mCurrentArtistId));
                        } else if (mCurrentAlbumId != null) {
                            list = MusicUtils.getSongListForAlbum(this, Integer.parseInt(mCurrentAlbumId));
                        }
                        MusicUtils.addToPlaylist(this, list, Integer.parseInt(uri.getLastPathSegment()));
                    }
                }
                break;
        }
    }

    private Cursor getArtistCursor(AsyncQueryHandler async, String filter) {

        StringBuilder where = new StringBuilder();
        where.append(MediaStore.Audio.Artists.ARTIST + " != ''");
        
        // Add in the filtering constraints
        String [] keywords = null;
        if (filter != null) {
            String [] searchWords = filter.split(" ");
            keywords = new String[searchWords.length];
            Collator col = Collator.getInstance();
            col.setStrength(Collator.PRIMARY);
            for (int i = 0; i < searchWords.length; i++) {
                keywords[i] = '%' + MediaStore.Audio.keyFor(searchWords[i]) + '%';
            }
            for (int i = 0; i < searchWords.length; i++) {
                where.append(" AND ");
                where.append(MediaStore.Audio.Media.ARTIST_KEY + " LIKE ?");
            }
        }

        String whereclause = where.toString();  
        String[] cols = new String[] {
                MediaStore.Audio.Artists._ID,
                MediaStore.Audio.Artists.ARTIST,
                MediaStore.Audio.Artists.NUMBER_OF_ALBUMS,
                MediaStore.Audio.Artists.NUMBER_OF_TRACKS
        };
        Cursor ret = null;
        if (async != null) {
            async.startQuery(0, null, MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI,
                    cols, whereclause , keywords, MediaStore.Audio.Artists.ARTIST_KEY);
        } else {
            ret = MusicUtils.query(this, MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI,
                    cols, whereclause , keywords, MediaStore.Audio.Artists.ARTIST_KEY);
        }
        return ret;
    }
    
    static class ArtistAlbumListAdapter extends SimpleCursorTreeAdapter implements SectionIndexer {
        
        private final Drawable mNowPlayingOverlay;
        private final BitmapDrawable mDefaultAlbumIcon;
        private int mGroupArtistIdIdx;
        private int mGroupArtistIdx;
        private int mGroupAlbumIdx;
        private int mGroupSongIdx;
        private final Context mContext;
        private final Resources mResources;
        private final String mAlbumSongSeparator;
        private final String mUnknownAlbum;
        private final String mUnknownArtist;
        private final StringBuilder mBuffer = new StringBuilder();
        private final Object[] mFormatArgs = new Object[1];
        private final Object[] mFormatArgs3 = new Object[3];
        private MusicAlphabetIndexer mIndexer;
        private ArtistAlbumBrowserActivity mActivity;
        private AsyncQueryHandler mQueryHandler;
        private String mConstraint = null;
        private boolean mConstraintIsValid = false;
        
        class ViewHolder {
            TextView line1;
            TextView line2;
            ImageView play_indicator;
            ImageView icon;
        }

        class QueryHandler extends AsyncQueryHandler {
            QueryHandler(ContentResolver res) {
                super(res);
            }
            
            @Override
            protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
                //Log.i("@@@", "query complete");
                mActivity.init(cursor);
            }
        }

        ArtistAlbumListAdapter(Context context, ArtistAlbumBrowserActivity currentactivity,
                Cursor cursor, int glayout, String[] gfrom, int[] gto, 
                int clayout, String[] cfrom, int[] cto) {
            super(context, cursor, glayout, gfrom, gto, clayout, cfrom, cto);
            mActivity = currentactivity;
            mQueryHandler = new QueryHandler(context.getContentResolver());

            Resources r = context.getResources();
            mNowPlayingOverlay = r.getDrawable(R.drawable.indicator_ic_mp_playing_list);
            mDefaultAlbumIcon = (BitmapDrawable) r.getDrawable(R.drawable.albumart_mp_unknown_list);
            // no filter or dither, it's a lot faster and we can't tell the difference
            mDefaultAlbumIcon.setFilterBitmap(false);
            mDefaultAlbumIcon.setDither(false);
            
            mContext = context;
            getColumnIndices(cursor);
            mResources = context.getResources();
            mAlbumSongSeparator = context.getString(R.string.albumsongseparator);
            mUnknownAlbum = context.getString(R.string.unknown_album_name);
            mUnknownArtist = context.getString(R.string.unknown_artist_name);
        }
        
        private void getColumnIndices(Cursor cursor) {
            if (cursor != null) {
                mGroupArtistIdIdx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Artists._ID);
                mGroupArtistIdx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Artists.ARTIST);
                mGroupAlbumIdx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Artists.NUMBER_OF_ALBUMS);
                mGroupSongIdx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Artists.NUMBER_OF_TRACKS);
                if (mIndexer != null) {
                    mIndexer.setCursor(cursor);
                } else {
                    mIndexer = new MusicAlphabetIndexer(cursor, mGroupArtistIdx, 
                            mResources.getString(R.string.fast_scroll_alphabet));
                }
            }
        }
        
        public void setActivity(ArtistAlbumBrowserActivity newactivity) {
            mActivity = newactivity;
        }
        
        public AsyncQueryHandler getQueryHandler() {
            return mQueryHandler;
        }

        @Override
        public View newGroupView(Context context, Cursor cursor, boolean isExpanded, ViewGroup parent) {
            View v = super.newGroupView(context, cursor, isExpanded, parent);
            ImageView iv = (ImageView) v.findViewById(R.id.icon);
            ViewGroup.LayoutParams p = iv.getLayoutParams();
            p.width = ViewGroup.LayoutParams.WRAP_CONTENT;
            p.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            ViewHolder vh = new ViewHolder();
            vh.line1 = (TextView) v.findViewById(R.id.line1);
            vh.line2 = (TextView) v.findViewById(R.id.line2);
            vh.play_indicator = (ImageView) v.findViewById(R.id.play_indicator);
            vh.icon = (ImageView) v.findViewById(R.id.icon);
            vh.icon.setPadding(0, 0, 1, 0);
            v.setTag(vh);
            return v;
        }

        @Override
        public View newChildView(Context context, Cursor cursor, boolean isLastChild,
                ViewGroup parent) {
            View v = super.newChildView(context, cursor, isLastChild, parent);
            ViewHolder vh = new ViewHolder();
            vh.line1 = (TextView) v.findViewById(R.id.line1);
            vh.line2 = (TextView) v.findViewById(R.id.line2);
            vh.play_indicator = (ImageView) v.findViewById(R.id.play_indicator);
            vh.icon = (ImageView) v.findViewById(R.id.icon);
            vh.icon.setBackgroundDrawable(mDefaultAlbumIcon);
            vh.icon.setPadding(0, 0, 1, 0);
            v.setTag(vh);
            return v;
        }
        
        @Override
        public void bindGroupView(View view, Context context, Cursor cursor, boolean isexpanded) {

            ViewHolder vh = (ViewHolder) view.getTag();

            String artist = cursor.getString(mGroupArtistIdx);
            String displayartist = artist;
            boolean unknown = artist == null || artist.equals(MediaFile.UNKNOWN_STRING);
            if (unknown) {
                displayartist = mUnknownArtist;
            }
            vh.line1.setText(displayartist);

            int numalbums = cursor.getInt(mGroupAlbumIdx);
            int numsongs = cursor.getInt(mGroupSongIdx);
            
            String songs_albums = MusicUtils.makeAlbumsLabel(context,
                    numalbums, numsongs, unknown);
            
            vh.line2.setText(songs_albums);
            
            int currentartistid = MusicUtils.getCurrentArtistId();
            int artistid = cursor.getInt(mGroupArtistIdIdx);
            if (currentartistid == artistid && !isexpanded) {
                vh.play_indicator.setImageDrawable(mNowPlayingOverlay);
            } else {
                vh.play_indicator.setImageDrawable(null);
            }
        }

        @Override
        public void bindChildView(View view, Context context, Cursor cursor, boolean islast) {

            ViewHolder vh = (ViewHolder) view.getTag();

            String name = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.ALBUM));
            String displayname = name;
            boolean unknown = name == null || name.equals(MediaFile.UNKNOWN_STRING); 
            if (unknown) {
                displayname = mUnknownAlbum;
            }
            vh.line1.setText(displayname);

            int numsongs = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.NUMBER_OF_SONGS));
            int numartistsongs = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.NUMBER_OF_SONGS_FOR_ARTIST));
            int first = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.FIRST_YEAR));
            int last = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.LAST_YEAR));

            if (first == 0) {
                first = last;
            }

            final StringBuilder builder = mBuffer;
            builder.delete(0, builder.length());
            if (unknown) {
                numsongs = numartistsongs;
            }
              
            if (numsongs == 1) {
                builder.append(context.getString(R.string.onesong));
            } else {
                if (numsongs == numartistsongs) {
                    final Object[] args = mFormatArgs;
                    args[0] = numsongs;
                    builder.append(mResources.getQuantityString(R.plurals.Nsongs, numsongs, args));
                } else {
                    final Object[] args = mFormatArgs3;
                    args[0] = numsongs;
                    args[1] = numartistsongs;
                    args[2] = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Artists.ARTIST));
                    builder.append(mResources.getQuantityString(R.plurals.Nsongscomp, numsongs, args));
                }
            }
            vh.line2.setText(builder.toString());
            
            ImageView iv = vh.icon;
            // We don't actually need the path to the thumbnail file,
            // we just use it to see if there is album art or not
            String art = cursor.getString(cursor.getColumnIndexOrThrow(
                    MediaStore.Audio.Albums.ALBUM_ART));
            if (unknown || art == null || art.length() == 0) {
                iv.setBackgroundDrawable(mDefaultAlbumIcon);
                iv.setImageDrawable(null);
            } else {
                int artIndex = cursor.getInt(0);
                Drawable d = MusicUtils.getCachedArtwork(context, artIndex, mDefaultAlbumIcon);
                iv.setImageDrawable(d);
            }

            int currentalbumid = MusicUtils.getCurrentAlbumId();
            int aid = cursor.getInt(0);
            iv = vh.play_indicator;
            if (currentalbumid == aid) {
                iv.setImageDrawable(mNowPlayingOverlay);
            } else {
                iv.setImageDrawable(null);
            }
        }

        
        @Override
        protected Cursor getChildrenCursor(Cursor groupCursor) {
            
            int id = groupCursor.getInt(groupCursor.getColumnIndexOrThrow(MediaStore.Audio.Artists._ID));
            
            String[] cols = new String[] {
                    MediaStore.Audio.Albums._ID,
                    MediaStore.Audio.Albums.ALBUM,
                    MediaStore.Audio.Albums.NUMBER_OF_SONGS,
                    MediaStore.Audio.Albums.NUMBER_OF_SONGS_FOR_ARTIST,
                    MediaStore.Audio.Albums.FIRST_YEAR,
                    MediaStore.Audio.Albums.LAST_YEAR,
                    MediaStore.Audio.Albums.ALBUM_ART
            };
            Cursor c = MusicUtils.query(mActivity,
                    MediaStore.Audio.Artists.Albums.getContentUri("external", id),
                    cols, null, null, MediaStore.Audio.Albums.DEFAULT_SORT_ORDER);
            
            class MyCursorWrapper extends CursorWrapper {
                String mArtistName;
                int mMagicColumnIdx;
                MyCursorWrapper(Cursor c, String artist) {
                    super(c);
                    mArtistName = artist;
                    if (mArtistName == null || mArtistName.equals(MediaFile.UNKNOWN_STRING)) {
                        mArtistName = mUnknownArtist;
                    }
                    mMagicColumnIdx = c.getColumnCount();
                }
                
                @Override
                public String getString(int columnIndex) {
                    if (columnIndex != mMagicColumnIdx) {
                        return super.getString(columnIndex);
                    }
                    return mArtistName;
                }
                
                @Override
                public int getColumnIndexOrThrow(String name) {
                    if (MediaStore.Audio.Albums.ARTIST.equals(name)) {
                        return mMagicColumnIdx;
                    }
                    return super.getColumnIndexOrThrow(name); 
                }
                
                @Override
                public String getColumnName(int idx) {
                    if (idx != mMagicColumnIdx) {
                        return super.getColumnName(idx);
                    }
                    return MediaStore.Audio.Albums.ARTIST;
                }
                
                @Override
                public int getColumnCount() {
                    return super.getColumnCount() + 1;
                }
            }
            return new MyCursorWrapper(c, groupCursor.getString(mGroupArtistIdx));
        }

        @Override
        public void changeCursor(Cursor cursor) {
            if (cursor != mActivity.mArtistCursor) {
                mActivity.mArtistCursor = cursor;
                getColumnIndices(cursor);
                super.changeCursor(cursor);
            }
        }
        
        @Override
        public Cursor runQueryOnBackgroundThread(CharSequence constraint) {
            String s = constraint.toString();
            if (mConstraintIsValid && (
                    (s == null && mConstraint == null) ||
                    (s != null && s.equals(mConstraint)))) {
                return getCursor();
            }
            Cursor c = mActivity.getArtistCursor(null, s);
            mConstraint = s;
            mConstraintIsValid = true;
            return c;
        }

        public Object[] getSections() {
            return mIndexer.getSections();
        }
        
        public int getPositionForSection(int sectionIndex) {
            return mIndexer.getPositionForSection(sectionIndex);
        }
        
        public int getSectionForPosition(int position) {
            return 0;
        }
    }
    
    private Cursor mArtistCursor;
    
    @Override
	public boolean button0(View v, MotionEvent event) {
    	View thisbutton = findViewById(R.id.play_all_holder);
		if(event.getAction() == MotionEvent.ACTION_DOWN) {
			thisbutton.setPressed(true);
    		return true;
    	} else if(event.getAction() == MotionEvent.ACTION_UP) {
    		thisbutton.setPressed(false);
    		setCurrentSelectionVariables(null,null);
			try {
				int [] list = null;
				if (mCurrentArtistId != null) {
                    list = MusicUtils.getSongListForArtist(this, Integer.parseInt(mCurrentArtistId));
                } else if (mCurrentAlbumId != null) {
                    list = MusicUtils.getSongListForAlbum(this, Integer.parseInt(mCurrentAlbumId));
                }
				if (list != null) {
					MusicUtils.playAll(this, list, 0);
				}
			} catch (Throwable e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return false;
    	}
    	return true;
	}
	@Override
	public boolean button1(View v, MotionEvent event) {
    	View thisbutton = findViewById(R.id.library_holder);
		if(event.getAction() == MotionEvent.ACTION_DOWN) {
			thisbutton.setPressed(true);
    		return true;
    	} else if(event.getAction() == MotionEvent.ACTION_UP) {
    		thisbutton.setPressed(false);
    		Intent intent = new Intent();
            intent.setClass(this, MusicBrowserActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
			return false;
    	}
    	return true;
	}
	@Override
	public boolean button2(View v, MotionEvent event) {
    	View thisbutton = findViewById(R.id.playback_holder);
		if(event.getAction() == MotionEvent.ACTION_DOWN) {
			thisbutton.setPressed(true);
    		return true;
    	} else if(event.getAction() == MotionEvent.ACTION_UP) {
    		thisbutton.setPressed(false);
    		Intent intent = new Intent("com.craftycoder.music.PLAYBACK_VIEWER");
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
			return false;
    	}
    	return true;
	}
	@Override
	public boolean button3(View v, MotionEvent event) {
    	View thisbutton = findViewById(R.id.shuffle_all_holder);
		if(event.getAction() == MotionEvent.ACTION_DOWN) {
			thisbutton.setPressed(true);
    		return true;
    	} else if(event.getAction() == MotionEvent.ACTION_UP) {
    		thisbutton.setPressed(false);
    		setCurrentSelectionVariables(null,null);
			try {
				int [] list = null;
				if (mCurrentArtistId != null) {
                    list = MusicUtils.getSongListForArtist(this, Integer.parseInt(mCurrentArtistId));
                } else if (mCurrentAlbumId != null) {
                    list = MusicUtils.getSongListForAlbum(this, Integer.parseInt(mCurrentAlbumId));
                }
				if (list != null) {
					MusicUtils.playAll(this, list, 0);
					doNormalShuffle();
				}
			} catch (Throwable e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return false;
    	}
    	return true;
	}
    private void doNormalShuffle() {
        bindService((new Intent()).setClass(this, MediaPlaybackService.class), normalshuffle, 0);
    }

    private ServiceConnection normalshuffle = new ServiceConnection() {
        public void onServiceConnected(ComponentName classname, IBinder obj) {
            // we need to be able to bind again, so unbind
            try {
                unbindService(this);
            } catch (IllegalArgumentException e) {
            }
            IMediaPlaybackService serv = IMediaPlaybackService.Stub.asInterface(obj);
            if (serv != null) {
                try {
                    serv.setShuffleMode(MediaPlaybackService.SHUFFLE_NORMAL);
                } catch (RemoteException ex) {
                }
            }
        }

        public void onServiceDisconnected(ComponentName classname) {
        }
    };
}


