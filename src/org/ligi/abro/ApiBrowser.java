package org.ligi.abro;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.nostra13.universalimageloader.cache.disc.impl.TotalSizeLimitedDiscCache;
import com.nostra13.universalimageloader.cache.memory.impl.UsingFreqLimitedMemoryCache;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.ligi.android.common.net.NetHelper;
import org.ligi.tracedroid.Log;

import java.net.MalformedURLException;
import java.net.URL;

public class ApiBrowser extends Activity {

    JSONArray arr = new JSONArray();
    private BaseAdapter mListAdapter;
    private APIListAdapter mAPIAdapter;
    private LayoutInflater mLayoutInflater;
    private ImageLoader imageLoader;
    private ArrayAdapter<String> mSpinnerAdapter;
    private Spinner service_spinner;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Get singletone instance of ImageLoader
        imageLoader = ImageLoader.getInstance();

        // Initialize ImageLoader with configuration. Do it once.
        imageLoader.init(

                new ImageLoaderConfiguration.Builder(getApplicationContext())
                        .memoryCacheExtraOptions(480, 800) // max width, max height
                        .threadPoolSize(3)
                        .threadPriority(Thread.NORM_PRIORITY - 1)
                        .denyCacheImageMultipleSizesInMemory()
                        .offOutOfMemoryHandling()
                        .memoryCache(new UsingFreqLimitedMemoryCache(2 * 1024 * 1024)) // You can pass your own memory cache implementation
                        .discCache(new TotalSizeLimitedDiscCache(this.getFilesDir().getAbsoluteFile(), 1024 * 1024)) // 1MB

                        .enableLogging()
                        .build());

        setContentView(R.layout.main);

        setTitle("API Browser");

        service_spinner = (Spinner) findViewById(R.id.service_spinner);

        service_spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                refreshList("https://" + mSpinnerAdapter.getItem(service_spinner.getSelectedItemPosition()) + "/discovery/v1/apis");
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        mSpinnerAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, new String[]{"www.googleapis.com", "democloudpoint.appspot.com/_ah/api"});
        service_spinner.setAdapter(mSpinnerAdapter);
        mSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_dropdown_item_1line);

        ListView api_list = (ListView) findViewById(R.id.api_list);

        mAPIAdapter = new APIListAdapter();
        refreshList("https://" + mSpinnerAdapter.getItem(service_spinner.getSelectedItemPosition()) + "/discovery/v1/apis");
        api_list.setAdapter(mAPIAdapter);

        mLayoutInflater = this.getLayoutInflater();


        api_list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                try {
                    Intent intent=new Intent(ApiBrowser.this,ResourceBrowser.class);
                    intent.setData(Uri.parse(arr.getJSONObject(position).getString("discoveryRestUrl")));
                    ApiBrowser.this.startActivity(intent);
                } catch (JSONException e) {
                    new AlertDialog.Builder(ApiBrowser.this).setMessage("cannot extract discoveryRestUrl");
                }

            }
        });

    }

    public void refreshList(String url_str) {

        new RefreshAsyncTask(this, url_str).execute();
    }

    private class APIListAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return arr.length();
        }

        @Override
        public Object getItem(int position) {
            try {
                return arr.getJSONObject(position);
            } catch (JSONException e) {
                return null;
            }
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            View mView = mLayoutInflater.inflate(R.layout.item, null);

            TextView title_tv = (TextView) mView.findViewById(R.id.title);
            String title = null;

            try {
                title = arr.getJSONObject(position).getString("title");
            } catch (JSONException e) {
            }

            if (title == null)
                try {
                    title = arr.getJSONObject(position).getString("name");
                } catch (JSONException e) {
                }

            title_tv.setText(title);


            try {
                TextView descr_tv = (TextView) mView.findViewById(R.id.description);
                descr_tv.setText(arr.getJSONObject(position).getString("description"));
            } catch (JSONException e) {
            }

            try {

                ImageView imageView = ((ImageView) mView.findViewById(R.id.imageView));

                imageLoader.displayImage(arr.getJSONObject(position).getJSONObject("icons").getString("x32"), imageView);
            } catch (JSONException e) {

            }
            return mView;  //To change body of implemented methods use File | Settings | File Templates.

        }
    }

    public class RefreshAsyncTask extends AsyncTask<Void, Void, String> {

        Context ctx;
        String url_str;

        public RefreshAsyncTask(Context ctx, String url_str) {
            this.ctx = ctx;
            this.url_str = url_str;
        }

        @Override
        protected String doInBackground(Void... params) {
            try {
                Log.i("downloading: " + url_str);
                return NetHelper.downloadURL2String(new URL(url_str));


            } catch (MalformedURLException e) {
                new AlertDialog.Builder(ctx).setMessage("cannot fetch discovery file").show();
            }

            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }

        @Override
        protected void onPostExecute(String s) {
            if (s == null) {
                new AlertDialog.Builder(ctx).setMessage("cannot fetch discovery file").show();
                return;
            }
            try {
                JSONObject jsonObject = new JSONObject(s);
                arr = jsonObject.getJSONArray("items");
                mAPIAdapter.notifyDataSetChanged();
            } catch (JSONException e) {
                new AlertDialog.Builder(ctx).setMessage("malformed discovery file").show();
            }
            super.onPostExecute(s);    //To change body of overridden methods use File | Settings | File Templates.
        }
    }
}
