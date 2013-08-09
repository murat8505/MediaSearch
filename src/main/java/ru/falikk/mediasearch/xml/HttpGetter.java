package ru.falikk.mediasearch.xml;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by Dell on 06.08.13.
 * <p/>
 * This class helps to obtain HTTP Response for specific HTTP Request
 */
public class HttpGetter {

    private static final String TAG = "HttpGetter";

    public static BufferedReader GetHttpResponseReader(String url) {
        try {
            HttpURLConnection urlConnection = (HttpURLConnection) new URL(url).openConnection();
            return new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
