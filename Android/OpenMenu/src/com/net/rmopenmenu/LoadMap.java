package com.net.rmopenmenu;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Geocoder;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TabHost;
import android.widget.TabWidget;
import android.widget.TextView;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.MyLocationOverlay;
import com.google.android.maps.Overlay;
import com.google.android.maps.OverlayItem;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonStreamParser;
import com.net.rmopenmenu.SearchActivity.TabsAdapter;
import com.net.rmopenmenu.SearchActivity.TabsAdapter.DummyTabFactory;
import com.net.rmopenmenu.SearchActivity.TabsAdapter.TabInfo;

public class LoadMap extends AsyncTask<String, Integer, Bundle> {
	
	private Context context;
	private Bundle b;
	private Activity activity;
	private ArrayList<OverlayItem> overlayList;
	
	public LoadMap(Context context, Bundle b, Activity activity) {
		this.context = context;
		this.activity = activity;
		this.b = b;
		
		overlayList = new ArrayList<OverlayItem>();
	}

	@Override
	protected Bundle doInBackground(String... params) {
		return load(params[0]);
	}
	
	protected void onProgressUpdate(Integer... progress) {
    }
	
	@Override
	protected void onPreExecute() {
		MapFragment.overlays.clear();
	}

    protected void onPostExecute(Bundle b) {
		MapFragment.overlays.add(MapFragment.locOverlay);
		MapFragment.overlays.add(MapFragment.itemizedOverlay);

    	MapFragment.mapView.invalidate();
    	
    	((ActionBarActivity) activity).getActionBarHelper().setRefreshActionItemState(false);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) activity.setProgressBarIndeterminateVisibility(false);
	}
    
  
	
	public Bundle load(String query) {
		ArrayList<Integer> item_ids = b.getIntegerArrayList("item_ids");
		ArrayList<String> restaurant_names = b.getStringArrayList("restaurant_names");
		ArrayList<String> restaurant_addresses = b.getStringArrayList("restaurant_addresses");
		ArrayList<String> item_names = b.getStringArrayList("item_names");
		ArrayList<String> item_prices = b.getStringArrayList("item_prices");
		ArrayList<String> item_descriptions = b.getStringArrayList("item_descriptions");
		
		Geocoder gc = new Geocoder(context); //create new geocoder instance
		
		for (int i = 0; i < restaurant_names.size(); i++) {
			
			List<Address> list = null;
			String addr = restaurant_addresses.get(i);
			
			try {
				list = gc.getFromLocationName(addr, 1);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			if (list != null && list.size() > 0) {
				Address address = list.get(0);
				
				int thisLat = (int)(address.getLatitude() * 1000000);
				int thisLon = (int)(address.getLongitude() * 1000000);
								
				GeoPoint point = new GeoPoint(thisLat, thisLon);
				OverlayItem overlayitem = new OverlayItem(point, "", "");
				
				MapFragment.itemizedOverlay.addOverlay(overlayitem);
			}
		}
		
		return null;
	}
		
}
