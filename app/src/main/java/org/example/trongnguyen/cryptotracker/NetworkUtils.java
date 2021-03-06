package org.example.trongnguyen.cryptotracker;

import android.net.Uri;
import android.util.JsonReader;
import android.util.Log;

import com.github.mikephil.charting.data.CandleEntry;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Scanner;

import static android.content.ContentValues.TAG;

public class NetworkUtils {
    final static String CRYPTOCOMPARE_BASE_URL = "https://min-api.cryptocompare.com/data/pricemultifull";
    final static String PARAM_CURRENCY = "fsyms";
    final static String PARAM_FIAT = "tsyms";
    final static String PARAM_CURRENCY_DETAIL = "fsym";
    final static String PARAM_FIAT_DETAIL = "tsym";
    final static String CRYPTOCOMPARE_COINLIST = "https://www.cryptocompare.com/api/data/coinlist/";
    final static String CRYPTOCOMPARE_BASE_PICTURE_URL = "https://www.cryptocompare.com";
    //final static String CRYPTOCOMPARE_DAY_DETAILS= "https://min-api.cryptocompare.com/data/histohour?fsym=BTC&tsym=USD&limit=60&aggregate=1&e=CCCAGG";
    final static String CRYPTOCOMPARE_DAY_DETAILS = "https://min-api.cryptocompare.com/data/histohour";
    final static String PARAM_DETAILS_CURRENCY = "fsym";
    final static String PARAM_DETAILS_FIAT = "tsym";
    final static String PARAM_DETAILS_LIMIT = "limit";
    final static String PARAM_DETAILS_AGGREGATE = "aggregate";
    final static String PARAM_DETAILS_EXCHANGE = "e";


    // buildUrl method used to get the url
    // Primarily used to obtain a list of coins
    public static String buildUri(String[] currency) {
        StringBuilder builder = new StringBuilder();
        for (String s : currency) {
            builder.append(s);
            builder.append(",");
        }
        builder.deleteCharAt(builder.length() - 1);
        String currencyString = builder.toString();
        Uri buildUri = Uri.parse(CRYPTOCOMPARE_BASE_URL).buildUpon()
                .appendQueryParameter(PARAM_CURRENCY, currencyString)
                .appendQueryParameter(PARAM_FIAT, "USD")
                .build();


        Log.d(TAG, "buildUrl: CURRENT URL IS " + buildUri.toString());
        return buildUri.toString();
    }

    // buildUrl method used to get the url
    // Primarily used to obtain a single list of coins. Used to get details
    public static String buildUri(String currency) {

        Uri buildUri = Uri.parse(CRYPTOCOMPARE_DAY_DETAILS).buildUpon()
                .appendQueryParameter(PARAM_DETAILS_CURRENCY, currency)
                .appendQueryParameter(PARAM_DETAILS_FIAT, "USD")
                .appendQueryParameter(PARAM_DETAILS_LIMIT, "24")
                .appendQueryParameter(PARAM_DETAILS_AGGREGATE, "1")
                .appendQueryParameter(PARAM_DETAILS_EXCHANGE, "CCCAGG")
                .build();


        Log.d(TAG, "buildUrl: CURRENT URL IS " + buildUri.toString());
        return buildUri.toString();
    }

    // buildUrl method with no params used to get the URL of the coin list
    // Primarily used by Search
    public static String buildUri() {
        Uri buildUri = Uri.parse(CRYPTOCOMPARE_COINLIST).buildUpon()
                .build();

        return buildUri.toString();
    }

    public static URL buildURL(String builtString) {
        URL url = null;
        try {
            url = new URL(builtString);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return url;
    }

    // getResponseFromHttpUrl used to get the results from the HTTP response.
    public static String getResponseFromHttpUrl(URL url) throws IOException {
        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        try {
            InputStream is = urlConnection.getInputStream();

            Scanner scanner = new Scanner(is);
            scanner.useDelimiter("\\A");

            boolean hasInput = scanner.hasNext();
            if (hasInput) {
                return scanner.next();
            } else {
                return null;
            }
        } finally {
            urlConnection.disconnect();
        }
    }

    // Parsing JSON method
    public static ArrayList<Ticker> extractFeatureFromJson(String jsonResponse, String[] currency, String fiat, int jsonType) {
        try {
            JSONObject baseJsonResponse = new JSONObject(jsonResponse);
            ArrayList<Ticker> baseArray = new ArrayList<Ticker>();
            if (baseJsonResponse.length() == 0) {
                return null;
            } else {
                if (jsonType == 1) {
                    JSONObject currencyData = baseJsonResponse.getJSONObject("Data");
                    if (currencyData.length() > 0) {
                        // Used to iterate through the list of coins without knowing the actual coin being looked up
                        // While we know the term that the user places, we need to get into the object to check
                        // for the full name and ticker symbol to determine if there is a match.
                        String searchTerm;
                        String coinFullName;

                        Iterator<String> iterator = currencyData.keys();
                        while (iterator.hasNext()) {
                            String coinTicker = iterator.next();
                            JSONObject tickerSymbol = currencyData.getJSONObject(coinTicker);
                            coinFullName = tickerSymbol.getString("FullName").toLowerCase();
                            searchTerm = currency[0].toLowerCase();
                            if (coinFullName.contains(searchTerm)) {
                                Log.d(TAG, "extractFeatureFromJson: WE HAVE A MATCH " + coinFullName);
                                String foundName = tickerSymbol.getString("CoinName");
                                String foundTicker = tickerSymbol.getString("Name");
                                String foundPicture = CRYPTOCOMPARE_BASE_PICTURE_URL + tickerSymbol.get("ImageUrl");
                                baseArray.add(new Ticker(foundName, foundTicker, foundPicture));
                            }
                        }
                    }
                    return baseArray;
                } else if (jsonType == 2) {
                    JSONArray dateDataArray = baseJsonResponse.getJSONArray("Data");
                    ArrayList<CandleEntry> entries = new ArrayList<>();
                    for (int i = 0; i < dateDataArray.length(); i++) {
                        JSONObject hourData = dateDataArray.getJSONObject(i);
                        String timeString = hourData.getString("time");
                        Log.d(TAG, "extractFeatureFromJson: JSON INFORMATION OF DATE " + timeString);
                        baseArray.add(new Ticker(timeString));

                    }
                    return baseArray;
                }
                JSONObject currencyArray = baseJsonResponse.getJSONObject("RAW");
                if (currencyArray.length() > 0) {
                    for (int i = 0; i < currencyArray.length(); i++) {
                        JSONObject itemCurrencies = currencyArray.getJSONObject(currency[i]);
                        JSONObject itemFiat = itemCurrencies.getJSONObject(fiat);

                        String ticker = "";
                        String price = "";

                        if (itemFiat.has("PRICE")) {
                            price = itemFiat.getString("PRICE");
                        }
                        if (itemFiat.has("FROMSYMBOL")) {
                            ticker = itemFiat.getString("FROMSYMBOL");
                        }

                        baseArray.add(new Ticker(ticker, price));
                    }
                    Log.d(TAG, "extractFeatureFromJson: BASE ARRAY" + baseArray.size());
                    return baseArray;
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }


    public static ArrayList<CandleEntry> extractFeatureFromJson(String jsonResponse, String[] currency, String fiat) {
        try {
            JSONObject baseJsonResponse = new JSONObject(jsonResponse);
            ArrayList<CandleEntry> baseArray = new ArrayList<CandleEntry>();
            if (baseJsonResponse.length() == 0) {
                return null;
            } else {
                JSONArray dateDataArray = baseJsonResponse.getJSONArray("Data");
                ArrayList<CandleEntry> entries = new ArrayList<>();
                for (int i = 0; i < dateDataArray.length(); i++) {
                    JSONObject hourData = dateDataArray.getJSONObject(i);
                    float highString = Float.parseFloat(hourData.getString("high"));
                    float lowString = Float.parseFloat(hourData.getString("low"));
                    float openString = Float.parseFloat(hourData.getString("open"));
                    float closeString = Float.parseFloat(hourData.getString("close"));
                    baseArray.add(new CandleEntry(i, highString, lowString, openString, closeString));

                }
                return baseArray;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}

