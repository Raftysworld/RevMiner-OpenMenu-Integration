package com.net.rmopenmenu;

import java.util.ArrayList;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TabHost;
import android.widget.TabWidget;
import android.widget.Toast;

/**
 * Demonstrates combining a TabHost with a ViewPager to implement a tab UI
 * that switches between tabs and also allows the user to perform horizontal
 * flicks to move between the tabs.
 */
public class SearchActivity extends ActionBarActivity {
    TabHost mTabHost;
    ViewPager  mViewPager;
    TabsAdapter mTabsAdapter;
    boolean menu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.fragment_tabs_pager);
        
        Intent intent = getIntent();
        String query = intent.getStringExtra(SearchManager.QUERY);
		Bundle appData = intent.getBundleExtra(SearchManager.APP_DATA);
		
		if (appData != null) {
			menu = appData.getBoolean("menu");
		}

		if (!Intent.ACTION_SEARCH.equals(intent.getAction())) {
			query = intent.getStringExtra("query");
			menu = intent.getBooleanExtra("menu", true);
		}

		SQLiteDatabase db = new Database(getBaseContext()).getReadableDatabase();
		ArrayList<Integer> item_ids = new ArrayList<Integer>();
		ArrayList<String> restaurant_names = new ArrayList<String>();
		ArrayList<String> restaurant_addresses = new ArrayList<String>();
		ArrayList<String> item_names = new ArrayList<String>();
		ArrayList<String> item_prices = new ArrayList<String>();
		ArrayList<String> item_descriptions = new ArrayList<String>();
		if (menu) {
			Cursor cursor = db.query("items", null, "name LIKE '%" + query + "%'", null, null, null, null);
			cursor.moveToFirst();
				
			while (!cursor.isAfterLast()) {
				item_ids.add(cursor.getInt(cursor.getColumnIndex("iid")));
				item_names.add(cursor.getString(cursor.getColumnIndex("name")));
				item_descriptions.add(cursor.getString(cursor.getColumnIndex("description")));
				String price = cursor.getString(cursor.getColumnIndex("price"));
				if (price.equals("0.00")) {
					price = "Unknown Price";
				}
				item_prices.add(price);
				cursor.moveToNext();
			}
			
			for (int i = 0; i < item_ids.size(); i++) {
				cursor = db.query("restaurants_items", null, "iid = " + item_ids.get(i), null, null, null, null);
				cursor.moveToFirst();
				
				int rid = cursor.getInt(cursor.getColumnIndex("rid"));
				
				cursor = db.query("restaurants", null, "rid = " + rid, null, null, null, null);
				cursor.moveToFirst();
				
				restaurant_names.add(cursor.getString(cursor.getColumnIndex("name")));
				restaurant_addresses.add(cursor.getString(cursor.getColumnIndex("address")));
			}
		} else {
			Cursor cursor = db.query("restaurants", null, "name LIKE '%" + query + "%'", null, null, null, null);
			cursor.moveToFirst();
	        
	        int rid = cursor.getInt(cursor.getColumnIndex("rid"));
	        String restaurant_name = cursor.getString(cursor.getColumnIndex("name"));
	        String restaurant_address = cursor.getString(cursor.getColumnIndex("address"));

	        cursor = db.query("restaurants_items", null, "rid = " + rid, null, null, null, null);
			cursor.moveToFirst();
			
			while (!cursor.isAfterLast()) {
				item_ids.add(cursor.getInt(cursor.getColumnIndex("iid")));
				cursor.moveToNext();
			}

			for (int i = 0; i < item_ids.size(); i++) {
				cursor = db.query("items", null, "iid = " + item_ids.get(i), null, null, null, null);
				cursor.moveToFirst();
				
				restaurant_names.add(restaurant_name);
				restaurant_addresses.add(restaurant_address);
				
				item_names.add(cursor.getString(cursor.getColumnIndex("name")));
				String price = cursor.getString(cursor.getColumnIndex("price"));
				if (price.equals("0.00")) {
					price = "Unknown Price";
				}
				item_prices.add(price);
				item_descriptions.add(cursor.getString(cursor.getColumnIndex("description")));
			}
		}
		
		db.close();

        mTabHost = (TabHost)findViewById(android.R.id.tabhost);
        mTabHost.setup();

        mViewPager = (ViewPager)findViewById(R.id.pager);

        mTabsAdapter = new TabsAdapter(this, mTabHost, mViewPager);
        Bundle b = new Bundle();
        b.putString("query", query);
        b.putBoolean("menu", menu);
        b.putIntegerArrayList("item_ids", item_ids);
        b.putStringArrayList("restaurant_names", restaurant_names);
        b.putStringArrayList("restaurant_addresses", restaurant_addresses);
        b.putStringArrayList("item_names", item_names);
        b.putStringArrayList("item_prices", item_prices);
        b.putStringArrayList("item_descriptions", item_descriptions);

        mTabsAdapter.addTab(mTabHost.newTabSpec("list").setIndicator("List"),
                SearchList.class, b);
        mTabsAdapter.addTab(mTabHost.newTabSpec("map").setIndicator("Map"),
                MapFragment.class, b);

        if (savedInstanceState != null) {
            mTabHost.setCurrentTabByTag(savedInstanceState.getString("tab"));
        }
    }
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("tab", mTabHost.getCurrentTabTag());
    }

    /**
     * This is a helper class that implements the management of tabs and all
     * details of connecting a ViewPager with associated TabHost.  It relies on a
     * trick.  Normally a tab host has a simple API for supplying a View or
     * Intent that each tab will show.  This is not sufficient for switching
     * between pages.  So instead we make the content part of the tab host
     * 0dp high (it is not shown) and the TabsAdapter supplies its own dummy
     * view to show as the tab content.  It listens to changes in tabs, and takes
     * care of switch to the correct paged in the ViewPager whenever the selected
     * tab changes.
     */
    public static class TabsAdapter extends FragmentPagerAdapter
            implements TabHost.OnTabChangeListener, ViewPager.OnPageChangeListener {
        private final Context mContext;
        private final TabHost mTabHost;
        private final ViewPager mViewPager;
        private final ArrayList<TabInfo> mTabs = new ArrayList<TabInfo>();

        static final class TabInfo {
            private final String tag;
            private final Class<?> clss;
            private final Bundle args;

            TabInfo(String _tag, Class<?> _class, Bundle _args) {
                tag = _tag;
                clss = _class;
                args = _args;
            }
        }

        static class DummyTabFactory implements TabHost.TabContentFactory {
            private final Context mContext;

            public DummyTabFactory(Context context) {
                mContext = context;
            }

            @Override
            public View createTabContent(String tag) {
                View v = new View(mContext);
                v.setMinimumWidth(0);
                v.setMinimumHeight(0);
                return v;
            }
        }

        public TabsAdapter(FragmentActivity activity, TabHost tabHost, ViewPager pager) {
            super(activity.getSupportFragmentManager());
            mContext = activity;
            mTabHost = tabHost;
            mViewPager = pager;
            mTabHost.setOnTabChangedListener(this);
            mViewPager.setAdapter(this);
            mViewPager.setOnPageChangeListener(this);
        }

        public void addTab(TabHost.TabSpec tabSpec, Class<?> clss, Bundle args) {
            tabSpec.setContent(new DummyTabFactory(mContext));
            String tag = tabSpec.getTag();

            TabInfo info = new TabInfo(tag, clss, args);
            mTabs.add(info);
            mTabHost.addTab(tabSpec);
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return mTabs.size();
        }

        @Override
        public Fragment getItem(int position) {
            TabInfo info = mTabs.get(position);
            return Fragment.instantiate(mContext, info.clss.getName(), info.args);
        }

        @Override
        public void onTabChanged(String tabId) {
            int position = mTabHost.getCurrentTab();
            mViewPager.setCurrentItem(position);
        }

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        }

        @Override
        public void onPageSelected(int position) {
            // Unfortunately when TabHost changes the current tab, it kindly
            // also takes care of putting focus on it when not in touch mode.
            // The jerk.
            // This hack tries to prevent this from pulling focus out of our
            // ViewPager.
            TabWidget widget = mTabHost.getTabWidget();
            int oldFocusability = widget.getDescendantFocusability();
            widget.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
            mTabHost.setCurrentTab(position);
            widget.setDescendantFocusability(oldFocusability);
        }

        @Override
        public void onPageScrollStateChanged(int state) {
        }
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.main, menu);

        // Calling super after populating the menu is necessary here to ensure that the
        // action bar helpers have a chance to handle this event.
        return super.onCreateOptionsMenu(menu);
    }
    
    @Override
    public boolean onSearchRequested() {
         Bundle appData = new Bundle();
         
         appData.putBoolean("menu", menu);
         startSearch(null, false, appData, false);
         return true;
     }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
            	finish();
            	break;

            case R.id.menu_refresh:
                getActionBarHelper().setRefreshActionItemState(true);
                getWindow().getDecorView().postDelayed(
                        new Runnable() {
                            @Override
                            public void run() {
                                getActionBarHelper().setRefreshActionItemState(false);
                            }
                        }, 1000);
                break;

            case R.id.menu_search:
                onSearchRequested();
                break;

            case R.id.menu_share:
                Toast.makeText(this, "Tapped share", Toast.LENGTH_SHORT).show();
                break;
        }
        return super.onOptionsItemSelected(item);
    }
}
/*
public class SearchActivity extends ActionBarActivity {
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.search);
		
		Intent intent = getIntent();
		String query = intent.getStringExtra("query");

		if (Intent.ACTION_SEARCH.equals(intent.getAction()) || !query.equals("")) {
			if (query.equals("")) {
				query = intent.getStringExtra(SearchManager.QUERY);
			}
		}
        
	}
	
	@Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("tab", getActionBar().getSelectedNavigationIndex());
    }

    public static class TabListener<T extends Fragment> implements ActionBar.TabListener {
        private final Activity mActivity;
        private final String mTag;
        private final Class<T> mClass;
        private final Bundle mArgs;
        private Fragment mFragment;

        public TabListener(Activity activity, String tag, Class<T> clz) {
            this(activity, tag, clz, null);
        }

        public TabListener(Activity activity, String tag, Class<T> clz, Bundle args) {
            mActivity = activity;
            mTag = tag;
            mClass = clz;
            mArgs = args;

            // Check to see if we already have a fragment for this tab, probably
            // from a previously saved state.  If so, deactivate it, because our
            // initial state is that a tab isn't shown.
            mFragment = mActivity.getFragmentManager().findFragmentByTag(mTag);
            if (mFragment != null && !mFragment.isDetached()) {
                FragmentTransaction ft = mActivity.getFragmentManager().beginTransaction();
                ft.detach(mFragment);
                ft.commit();
            }
        }

        public void onTabSelected(Tab tab, FragmentTransaction ft) {
            if (mFragment == null) {
                mFragment = Fragment.instantiate(mActivity, mClass.getName(), mArgs);
                ft.add(android.R.id.content, mFragment, mTag);
            } else {
                ft.attach(mFragment);
            }
        }

        public void onTabUnselected(Tab tab, FragmentTransaction ft) {
            if (mFragment != null) {
                ft.detach(mFragment);
            }
        }

        public void onTabReselected(Tab tab, FragmentTransaction ft) {
        }
    }

}*/
