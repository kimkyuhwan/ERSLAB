/*
 * Copyright (C) 2016 The Android Open Source Project
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
package com.google.android.exoplayer2.demo;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.JsonReader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ParserException;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DataSourceInputStream;
import com.google.android.exoplayer2.upstream.DataSpec;
import com.google.android.exoplayer2.upstream.DefaultDataSource;
import com.google.android.exoplayer2.util.Assertions;
import com.google.android.exoplayer2.util.Util;

import org.json.JSONException;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import gyuhwan.Constants;
import gyuhwan.HttpManager;
import gyuhwan.PlayerClock;
import gyuhwan.SettingDialog;
import gyuhwan.VideoManager;

/**
 * An activity for selecting from a list of samples.
 */
public class SampleChooserActivity extends Activity {

  private static final String TAG = "SampleChooserActivity";
  private SettingDialog SettingDialog;

  private String serverFileName[]={"motion","car","oops","dance","moving","truck","hongkong","walking","football"};
  private float videoFrameRate[]={29.970f, 23.976f,23.976f, 23.976f,29.970f,24f,23.976f,23.976f,30f,25f,29.970f};

  private static final int GROUP_NUMBER_CUSTOM=0;
  private static final int GROUP_NUMBER_EXAMPLE_ERSLAB=1;
  private static final int GROUP_NUMBER_EXAMPLBE_BASIC=2;

  private static boolean visited_SettingDialog=false;
  private static List<SampleGroup> groups_copy;
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.sample_chooser_activity);
    Intent intent = getIntent();
    String dataUri = intent.getDataString();
    String[] uris;
    BatteryManager mBatteryManager =
            (BatteryManager) getSystemService(Context.BATTERY_SERVICE);
    if (dataUri != null) {
      uris = new String[] {dataUri};
    } else {
      ArrayList<String> uriList = new ArrayList<>();
      AssetManager assetManager = getAssets();
      try {
        for (String asset : assetManager.list("")) {
          if (asset.endsWith(".exolist.json")) {
            uriList.add("asset:///" + asset);
          }
        }
      } catch (IOException e) {
        Toast.makeText(getApplicationContext(), R.string.sample_list_load_error, Toast.LENGTH_LONG)
            .show();
      }
      uris = new String[uriList.size()];
      uriList.toArray(uris);
      Arrays.sort(uris);
    }
    SampleListLoader loaderTask = new SampleListLoader();
    loaderTask.execute(uris);
    if(!visited_SettingDialog) {
      callSettingDialog();
      visited_SettingDialog=true;
    }
  }

  void callSettingDialog(){
    SettingDialog = new SettingDialog(this, new SettingDialog.ISettingDialogEventListener() {
      @Override
      public void settingDialogEvent(int minbuffervalue, int maxbuffervalue, int playbackbuffervalue, int playbackrebuffervalue) {
        PlayerActivity.MIN_BUFFER_MS = minbuffervalue;
        PlayerActivity.MAX_BUFFER_MS = maxbuffervalue;
        PlayerActivity.PLAYBACK_BUFFER_MS = playbackbuffervalue;
        PlayerActivity.PLAYBACK_REBUFFER_MS = playbackrebuffervalue;
      }
    });
    SettingDialog.setCancelable(false);
    SettingDialog.show();
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    MenuInflater inflater= getMenuInflater();
    inflater.inflate(R.menu.sample_chooser_menu_item,menu);
    return super.onCreateOptionsMenu(menu);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()){
      case R.id.action_setting:
        callSettingDialog();
        break;
    }
    return super.onOptionsItemSelected(item);
  }

  private void onSampleGroups(final List<SampleGroup> groups, boolean sawError) {
    if (sawError) {
      Toast.makeText(getApplicationContext(), R.string.sample_list_load_error, Toast.LENGTH_LONG)
          .show();
    }
    groups_copy=groups;
    ExpandableListView sampleList = (ExpandableListView) findViewById(R.id.sample_list);
    sampleList.setAdapter(new SampleAdapter(this, groups));
    sampleList.setOnChildClickListener(new OnChildClickListener() {
      @Override
      public boolean onChildClick(ExpandableListView parent, View view, int groupPosition,
                                  int childPosition, long id) {
        onSampleSelected(groups.get(groupPosition).samples.get(childPosition));
        PlayerClock.getInstance().setMacro_video_index(childPosition);
        Log.d("DEBUGYU", "Group Pos : " + groupPosition);

        if (groupPosition == GROUP_NUMBER_CUSTOM) {
          PlayerClock.getInstance().setVideo_idx(childPosition);
          Log.d("DEBUGYU", "Child Pos : " + childPosition);
          if (PlayerClock.getInstance().getPlayMode() == Constants.PLAYMODE_SSIM) {
        //    PlayerClock.getInstance().setSSIMSelectFileName(serverFileName[childPosition]);
          }
          else {
            PlayerClock.getInstance().setKnapsackSelectFileName(serverFileName[childPosition]);
          }
          PlayerClock.getInstance().setFrameRate(videoFrameRate[childPosition]);
          PlayerClock.getInstance().setErsMode(true);
        } else if (groupPosition == GROUP_NUMBER_EXAMPLE_ERSLAB) {
          if (PlayerClock.getInstance().getPlayMode() == Constants.PLAYMODE_SSIM) {
            PlayerClock.getInstance().setSSIMSelectFileName(serverFileName[childPosition]);
            PlayerClock.getInstance().setVideo_idx(childPosition);
          }
          else if (PlayerClock.getInstance().getPlayMode() == Constants.PLAYMODE_KNAPSACK) {
            PlayerClock.getInstance().setKnapsackSelectFileName(serverFileName[childPosition]);
            PlayerClock.getInstance().setVideo_idx(childPosition);
          }
          PlayerClock.getInstance().setFrameRate(videoFrameRate[childPosition]);
          PlayerClock.getInstance().setErsMode(true);
        } else {
          PlayerClock.getInstance().setErsMode(false);
        }

        return true;
      }
    });
  }

  @Override
  protected void onResume() {
    super.onResume();
    VideoManager.getInstance();
    if(Constants.PLAY_MODE_STATE==Constants.MODE_AUTO) {
      mRunnable = new Runnable() {
        @Override
        public void run() {
          if(PlayerClock.getInstance().getPlayMode()== Constants.PLAYMODE_KNAPSACK){
            Log.d("player-clock","ABCDEF!");
            int rate=PlayerClock.getInstance().getSelectKnapsackEnergyRate();
            int current_video_idx=PlayerClock.getInstance().getMacro_video_index();
            if(rate==100){
              rate=90;
              if(current_video_idx==8){
                current_video_idx=0;
                PlayerClock.getInstance().addAutoCount();
                PlayerClock.getInstance().setMacro_video_index(current_video_idx);
              }
              else{
                PlayerClock.getInstance().setMacro_video_index(++current_video_idx);
              }
            }
            else{rate+=5;}
            PlayerClock.getInstance().setSelectKnapsackEnergyRate(rate);
            PlayerClock.getInstance().setKnapsackSelectFileName(serverFileName[current_video_idx]);
            onSampleSelected(groups_copy.get(0).samples.get(current_video_idx));
          }
          else {
            int macro_idx=PlayerClock.getInstance().getMacro_mode_index();

            if (macro_idx >= 4) {
              PlayerClock.getInstance().setMacro_mode_index(-1);
              int videoIdx=PlayerClock.getInstance().getMacro_video_index();
              PlayerClock.getInstance().setMacro_video_index(++videoIdx);
            }
            if (PlayerClock.getInstance().getMacro_video_index() != -1) {
              int childPosition = PlayerClock.getInstance().getMacro_video_index();
              onSampleSelected(groups_copy.get(0).samples.get(childPosition));
              PlayerClock.getInstance().setSSIMSelectFileName(serverFileName[childPosition]);
              PlayerClock.getInstance().setFrameRate(videoFrameRate[childPosition]);
            }
          }
        }
      };
      Handler handler = new Handler();
      handler.postDelayed(mRunnable, 1000);
    }
  }
  private Runnable mRunnable ;
  private void onSampleSelected(Sample sample) {
    try {
      HttpManager.getInstance().Start();
    } catch (JSONException e) {
      e.printStackTrace();
    }
    startActivity(sample.buildIntent(this));
  }

  private final class SampleListLoader extends AsyncTask<String, Void, List<SampleGroup>> {

    private boolean sawError;

    @Override
    protected List<SampleGroup> doInBackground(String... uris) {
      List<SampleGroup> result = new ArrayList<>();
      Context context = getApplicationContext();
      String userAgent = Util.getUserAgent(context, "ExoPlayerDemo");
      DataSource dataSource = new DefaultDataSource(context, null, userAgent, false);
      for (String uri : uris) {
        DataSpec dataSpec = new DataSpec(Uri.parse(uri));
        InputStream inputStream = new DataSourceInputStream(dataSource, dataSpec);
        try {
          readSampleGroups(new JsonReader(new InputStreamReader(inputStream, "UTF-8")), result);
        } catch (Exception e) {
          Log.e(TAG, "Error loading sample list: " + uri, e);
          sawError = true;
        } finally {
          Util.closeQuietly(dataSource);
        }
      }
      return result;
    }

    @Override
    protected void onPostExecute(List<SampleGroup> result) {
      onSampleGroups(result, sawError);
    }

    private void readSampleGroups(JsonReader reader, List<SampleGroup> groups) throws IOException {
      reader.beginArray();
      while (reader.hasNext()) {
        readSampleGroup(reader, groups);
      }
      reader.endArray();
    }



    private void readSampleGroup(JsonReader reader, List<SampleGroup> groups) throws IOException {
      String groupName = "";
      ArrayList<Sample> samples = new ArrayList<>();

      reader.beginObject();
      while (reader.hasNext()) {
        String name = reader.nextName();
        switch (name) {
          case "name":
            groupName = reader.nextString();
            break;
          case "samples":
            reader.beginArray();
            while (reader.hasNext()) {
              samples.add(readEntry(reader, false));
            }
            reader.endArray();
            break;
          case "_comment":
            reader.nextString(); // Ignore.
            break;
          default:
            throw new ParserException("Unsupported name: " + name);
        }
      }
      reader.endObject();

      SampleGroup group = getGroup(groupName, groups);
      group.samples.addAll(samples);
    }

    private Sample readEntry(JsonReader reader, boolean insidePlaylist) throws IOException {
      String sampleName = null;
      String uri = null;
      String extension = null;
      UUID drmUuid = null;
      String drmLicenseUrl = null;
      String[] drmKeyRequestProperties = null;
      boolean preferExtensionDecoders = false;
      ArrayList<UriSample> playlistSamples = null;

      reader.beginObject();
      while (reader.hasNext()) {
        String name = reader.nextName();
        switch (name) {
          case "name":
            sampleName = reader.nextString();
            break;
          case "uri":
            uri = reader.nextString();
            break;
          case "extension":
            extension = reader.nextString();
            break;
          case "drm_scheme":
            Assertions.checkState(!insidePlaylist, "Invalid attribute on nested item: drm_scheme");
            drmUuid = getDrmUuid(reader.nextString());
            break;
          case "drm_license_url":
            Assertions.checkState(!insidePlaylist,
                "Invalid attribute on nested item: drm_license_url");
            drmLicenseUrl = reader.nextString();
            break;
          case "drm_key_request_properties":
            Assertions.checkState(!insidePlaylist,
                "Invalid attribute on nested item: drm_key_request_properties");
            ArrayList<String> drmKeyRequestPropertiesList = new ArrayList<>();
            reader.beginObject();
            while (reader.hasNext()) {
              drmKeyRequestPropertiesList.add(reader.nextName());
              drmKeyRequestPropertiesList.add(reader.nextString());
            }
            reader.endObject();
            drmKeyRequestProperties = drmKeyRequestPropertiesList.toArray(new String[0]);
            break;
          case "prefer_extension_decoders":
            Assertions.checkState(!insidePlaylist,
                "Invalid attribute on nested item: prefer_extension_decoders");
            preferExtensionDecoders = reader.nextBoolean();
            break;
          case "playlist":
            Assertions.checkState(!insidePlaylist, "Invalid nesting of playlists");
            playlistSamples = new ArrayList<>();
            reader.beginArray();
            while (reader.hasNext()) {
              playlistSamples.add((UriSample) readEntry(reader, true));
            }
            reader.endArray();
            break;
          default:
            throw new ParserException("Unsupported attribute name: " + name);
        }
      }
      reader.endObject();

      if (playlistSamples != null) {
        UriSample[] playlistSamplesArray = playlistSamples.toArray(
            new UriSample[playlistSamples.size()]);
        return new PlaylistSample(sampleName, drmUuid, drmLicenseUrl, drmKeyRequestProperties,
            preferExtensionDecoders, playlistSamplesArray);
      } else {
        return new UriSample(sampleName, drmUuid, drmLicenseUrl, drmKeyRequestProperties,
            preferExtensionDecoders, uri, extension);
      }
    }

    private SampleGroup getGroup(String groupName, List<SampleGroup> groups) {
      for (int i = 0; i < groups.size(); i++) {
        if (Util.areEqual(groupName, groups.get(i).title)) {
          return groups.get(i);
        }
      }
      SampleGroup group = new SampleGroup(groupName);
      groups.add(group);
      return group;
    }

    private UUID getDrmUuid(String typeString) throws ParserException {
      switch (Util.toLowerInvariant(typeString)) {
        case "widevine":
          return C.WIDEVINE_UUID;
        case "playready":
          return C.PLAYREADY_UUID;
        case "cenc":
          return C.CLEARKEY_UUID;
        default:
          try {
            return UUID.fromString(typeString);
          } catch (RuntimeException e) {
            throw new ParserException("Unsupported drm type: " + typeString);
          }
      }
    }

  }

  private static final class SampleAdapter extends BaseExpandableListAdapter {

    private final Context context;
    private final List<SampleGroup> sampleGroups;

    public SampleAdapter(Context context, List<SampleGroup> sampleGroups) {
      this.context = context;
      this.sampleGroups = sampleGroups;
    }

    @Override
    public Sample getChild(int groupPosition, int childPosition) {
      return getGroup(groupPosition).samples.get(childPosition);
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
      return childPosition;
    }

    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild,
        View convertView, ViewGroup parent) {
      View view = convertView;
      if (view == null) {
        view = LayoutInflater.from(context).inflate(android.R.layout.simple_list_item_1, parent,
            false);
      }
      ((TextView) view).setText(getChild(groupPosition, childPosition).name);
      if(groupPosition==GROUP_NUMBER_CUSTOM || groupPosition==GROUP_NUMBER_EXAMPLE_ERSLAB) {
        ((TextView) view).setTextColor(Color.parseColor("#2b90d9"));
      }
      else{
        ((TextView) view).setTextColor(Color.parseColor("#9baec8"));
      }
      ((TextView) view).setTypeface(null, Typeface.BOLD);
      return view;
    }

    @Override
    public int getChildrenCount(int groupPosition) {
      return getGroup(groupPosition).samples.size();
    }

    @Override
    public SampleGroup getGroup(int groupPosition) {
      return sampleGroups.get(groupPosition);
    }

    @Override
    public long getGroupId(int groupPosition) {
      return groupPosition;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded, View convertView,
        ViewGroup parent) {
      View view = convertView;
      if (view == null) {
        view = LayoutInflater.from(context).inflate(android.R.layout.simple_expandable_list_item_1,
            parent, false);
      }
      ((TextView) view).setText(getGroup(groupPosition).title);
      return view;
    }

    @Override
    public int getGroupCount() {
      return sampleGroups.size();
    }

    @Override
    public boolean hasStableIds() {
      return false;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
      return true;
    }

  }

  private static final class SampleGroup {

    public final String title;
    public final List<Sample> samples;

    public SampleGroup(String title) {
      this.title = title;
      this.samples = new ArrayList<>();
    }

  }

  private abstract static class Sample {

    public final String name;
    public final boolean preferExtensionDecoders;
    public final UUID drmSchemeUuid;
    public final String drmLicenseUrl;
    public final String[] drmKeyRequestProperties;

    public Sample(String name, UUID drmSchemeUuid, String drmLicenseUrl,
        String[] drmKeyRequestProperties, boolean preferExtensionDecoders) {
      this.name = name;
      this.drmSchemeUuid = drmSchemeUuid;
      this.drmLicenseUrl = drmLicenseUrl;
      this.drmKeyRequestProperties = drmKeyRequestProperties;
      this.preferExtensionDecoders = preferExtensionDecoders;
    }

    public Intent buildIntent(Context context) {
      Intent intent = new Intent(context, PlayerActivity.class);
      intent.putExtra(PlayerActivity.PREFER_EXTENSION_DECODERS, preferExtensionDecoders);
      if (drmSchemeUuid != null) {
        intent.putExtra(PlayerActivity.DRM_SCHEME_UUID_EXTRA, drmSchemeUuid.toString());
        intent.putExtra(PlayerActivity.DRM_LICENSE_URL, drmLicenseUrl);
        intent.putExtra(PlayerActivity.DRM_KEY_REQUEST_PROPERTIES, drmKeyRequestProperties);

      }
      return intent;
    }

  }

  private static final class UriSample extends Sample {

    public final String uri;
    public final String extension;

    public UriSample(String name, UUID drmSchemeUuid, String drmLicenseUrl,
        String[] drmKeyRequestProperties, boolean preferExtensionDecoders, String uri,
        String extension) {
      super(name, drmSchemeUuid, drmLicenseUrl, drmKeyRequestProperties, preferExtensionDecoders);
      this.uri = uri;
      this.extension = extension;
    }

    @Override
    public Intent buildIntent(Context context) {
      return super.buildIntent(context)
          .setData(Uri.parse(uri))
          .putExtra(PlayerActivity.EXTENSION_EXTRA, extension)
          .setAction(PlayerActivity.ACTION_VIEW);
    }

  }

  private static final class PlaylistSample extends Sample {

    public final UriSample[] children;

    public PlaylistSample(String name, UUID drmSchemeUuid, String drmLicenseUrl,
        String[] drmKeyRequestProperties, boolean preferExtensionDecoders,
        UriSample... children) {
      super(name, drmSchemeUuid, drmLicenseUrl, drmKeyRequestProperties, preferExtensionDecoders);
      this.children = children;
    }

    @Override
    public Intent buildIntent(Context context) {
      String[] uris = new String[children.length];
      String[] extensions = new String[children.length];
      for (int i = 0; i < children.length; i++) {
        uris[i] = children[i].uri;
        extensions[i] = children[i].extension;
      }
      return super.buildIntent(context)
          .putExtra(PlayerActivity.URI_LIST_EXTRA, uris)
          .putExtra(PlayerActivity.EXTENSION_LIST_EXTRA, extensions)
          .setAction(PlayerActivity.ACTION_VIEW_LIST);
    }

  }

}
