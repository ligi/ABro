package org.ligi.abro;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import org.json.JSONException;
import org.json.JSONObject;
import org.ligi.android.common.net.NetHelper;
import org.ligi.tracedroid.Log;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

public class ResourceBrowser extends Activity {


    JSONObject res_obj = new JSONObject();
    private ListView mListView;
    private LayoutInflater mLayoutInflater;
    private APIListAdapter mResAdapter;
    private ArrayList<Method> methods = new ArrayList<Method>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mListView = new ListView(this);
        mLayoutInflater = getLayoutInflater();

        mResAdapter = new APIListAdapter();

        mListView.setAdapter(mResAdapter);

        setContentView(mListView);

        refreshList(getIntent().getData().toString());

    }

    public void refreshList(String url_str) {

        new RefreshAsyncTask(this, url_str).execute();
    }

    private class APIListAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return methods.size();
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View mView = mLayoutInflater.inflate(R.layout.item, null);

            TextView title_tv = (TextView) mView.findViewById(R.id.title);
            title_tv.setText(methods.get(position).name);

            TextView description_tv = (TextView) mView.findViewById(R.id.description);
            description_tv.setText(methods.get(position).description);

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
                res_obj = jsonObject.getJSONObject("resources");

                for (int i = 0; i < res_obj.length(); i++) {


                    try {
                        JSONObject res = res_obj.getJSONObject(res_obj.names().getString(i)).getJSONObject("methods");
                        for (int method_i = 0; method_i < res.length(); method_i++)
                            try {
                                JSONObject method_json=res.getJSONObject(res.names().getString(method_i));
                                methods.add(new Method(method_json.getString("id"), method_json.getString("description")));
                            } catch (JSONException e) {
                                methods.add(new Method(res_obj.names().getString(i), "problem"));

                            }
                    } catch (JSONException e) {
                        methods.add(new Method(res_obj.names().getString(i), "no method"));

                    }


                }

                mResAdapter.notifyDataSetChanged();
            } catch (JSONException e) {
                new AlertDialog.Builder(ctx).setMessage("malformed discovery file" + e).show();
            }
            super.onPostExecute(s);    //To change body of overridden methods use File | Settings | File Templates.
        }
    }

    private class Method {
        public String name;
        public String description;

        public Method(String name, String description) {
            this.name = name;
            this.description = description;
        }
    }

}
