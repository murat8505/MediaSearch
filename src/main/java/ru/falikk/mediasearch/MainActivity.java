package ru.falikk.mediasearch;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.util.List;
import java.util.concurrent.Executors;

import ru.falikk.mediasearch.dao.Entry;
import ru.falikk.mediasearch.xml.HttpGetter;
import ru.falikk.mediasearch.xml.VkParser;
import ru.falikk.mediasearch.xml.YoutubeParser;

public class MainActivity extends Activity {

    private static final String TAG = "MainActivity";
    private Context context = MainActivity.this;
    private ProgressBar progressBar, progressBarHor;
    private EditText etSearch;
    private ListView listView;
    private ParserAdapter parserAdapter;
    private Button buttonSearch;
    private GrabYoutubeTask youtubeTask;
    private GrabVkTask vkTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        UICreate();
    }

    private View.OnClickListener l = new View.OnClickListener() {
        @TargetApi(Build.VERSION_CODES.HONEYCOMB)
        @Override
        public void onClick(View v) {

            if (buttonSearch.getText().toString().equals("Search")) {
                updateList();
                String searchString = etSearch.getText().toString().trim().replace(' ', '+');

                youtubeTask = new GrabYoutubeTask();
                vkTask = new GrabVkTask();
                youtubeTask.execute(searchString);
                vkTask.executeOnExecutor(Executors.newFixedThreadPool(1),searchString);


                buttonSearch.setText("Stop");
            } else {
                vkTask.cancel(true);
                youtubeTask.cancel(true);
                buttonSearch.setText("Search");
            }

        }


    };


    private AdapterView.OnItemClickListener itemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
            String url = parserAdapter.getItem(i).getUrl();
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
        }
    };

    // Initialize User Interface
    private void UICreate() {

        buttonSearch = (Button) findViewById(R.id.buttonSearch);
        buttonSearch.setOnClickListener(l);
        etSearch = (EditText) findViewById(R.id.editSearch);


        parserAdapter = new ParserAdapter(context);
        youtubeTask = new GrabYoutubeTask();
        vkTask = new GrabVkTask();

        listView = (ListView) findViewById(R.id.listView);
        listView.setAdapter(parserAdapter);
        listView.setOnItemClickListener(itemClickListener);


        progressBar = (ProgressBar) findViewById(R.id.progressBar2);
        progressBar.setVisibility(View.GONE);

        progressBarHor = (ProgressBar) findViewById(R.id.progressBar);
        progressBarHor.setVisibility(View.GONE);
        progressBarHor.setMax(60);
        progressBarHor.setProgress(0);


    }

    // Update of ListView
    private void updateList() {
        if (!parserAdapter.isEmpty()) {
            parserAdapter.clear();
            parserAdapter.notifyDataSetChanged();
        }
    }

    // Saving search results data to DB before exit via Back button
    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(context)
                .setTitle("Close program?")
                .setMessage("You really want close?")
                .setNegativeButton("No", null)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Log.d(TAG, "before onDestroy");
                        DbSavingTask dbSavingTask = new DbSavingTask();
                        dbSavingTask.execute();

                    }
                }).create().show();
    }

    // Grabbing data from Youtube
    private class GrabYoutubeTask extends AsyncTask<String, List<Entry>, Void> {

        @Override
        protected void onPreExecute() {
            progressBar.setVisibility(View.VISIBLE);
            progressBarHor.setVisibility(View.VISIBLE);
            progressBarHor.setProgress(0);


        }

        @Override
        protected Void doInBackground(String... searchString) {

            YoutubeParser xmlYoutubeParser = new YoutubeParser();
            for (int i = 1; i <= 300; i += 10) {
                String strUrl = "http://gdata.youtube.com/feeds/api/videos?q=" +
                        searchString[0] +
                        "&start-index=" + i + "&max-results=10&v=2";
                List<Entry> list = xmlYoutubeParser.parse(HttpGetter.GetHttpResponseReader(strUrl));
                publishProgress(list);
                if (isCancelled()) return null;
            }


            return null;
        }

        @Override
        protected void onProgressUpdate(List<Entry>... values) {

            for (Entry e : values[0]) parserAdapter.add(e);
            progressBarHor.incrementProgressBy(1);
            parserAdapter.notifyDataSetChanged();
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            Toast.makeText(context, "Parsed", Toast.LENGTH_SHORT).show();
            progressBar.setVisibility(View.GONE);
            progressBarHor.setVisibility(View.GONE);

        }

        @Override
        protected void onCancelled() {
            Toast.makeText(context, "Canceled", Toast.LENGTH_SHORT).show();
            progressBar.setVisibility(View.GONE);
            progressBarHor.setVisibility(View.GONE);
        }
    }

    // Grabbing data from VK
    private class GrabVkTask extends AsyncTask<String, List<Entry>, Void> {


        @Override
        protected Void doInBackground(String... strings) {

            VkParser vkParser = new VkParser();
            for (int i = 0; i < 300; i += 10) {
                String strUrl = "https://api.vkontakte.ru/method/video.search.xml?" +
                        "q=" +
                        strings[0] +
                        "&count=10&offset=" +
                        i +
                        "&access_token=2f71b716e7fcb1a91d54719e54e5f50cf7685d2ca1a2c34f85dbfd8de65de5998bea586430844eec0940d";
                List<Entry> list = vkParser.parse(HttpGetter.GetHttpResponseReader(strUrl));
                publishProgress(list);
                if (isCancelled()) return null;
            }


            return null;
        }

        @Override
        protected void onProgressUpdate(List<Entry>... values) {
            for (Entry e : values[0]) parserAdapter.add(e);
            progressBarHor.incrementProgressBy(1);
            parserAdapter.notifyDataSetChanged();
        }


    }

    // Saving parsed data to DB before exit
    private class DbSavingTask extends AsyncTask<Void, Void, Void> {
        ProgressDialog progressDialog;

        @Override
        protected void onPreExecute() {
            progressDialog = ProgressDialog.show(context, "Data Saving", "Saving to Database...");
            Log.d(TAG, "in onPreExecute");

        }

        @Override
        protected Void doInBackground(Void... voids) {
            Log.d(TAG, "Begin doInBackground");
            parserAdapter.addItemsToDB();
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            progressDialog.dismiss();
            parserAdapter.closeAll();
            Log.d(TAG, "in onPostExecute");
            finish();

        }


    }

}
