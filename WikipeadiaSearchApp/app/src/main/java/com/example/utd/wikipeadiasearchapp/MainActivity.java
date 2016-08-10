package com.example.utd.wikipeadiasearchapp;

import android.content.pm.ActivityInfo;
import android.os.AsyncTask;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import redis.clients.jedis.Jedis;

public class MainActivity extends AppCompatActivity {
    EditText input;
    TextView output;
    Jedis jedis;
    JedisIndex index;
    AsyncTask prev;
    String validationRegex = "\\w+";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        input = (EditText)findViewById(R.id.Input);
        output = (TextView)findViewById(R.id.Output);
        new CreateJedis().execute();
        input.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            public boolean onEditorAction(TextView exampleView, int actionId, KeyEvent event) {

                return false;
            }
        });



    }
    public void search(View view){
        if(jedis==null || index == null) {
            return;
        }
        if(prev!=null)
            prev.cancel(true);
        output.setText("Loading...");
        prev = new SearchTerm().execute(input.getText().toString().toLowerCase().trim());
    }
    private void invalidInput(){
        output.setText("Invalid input");
    }
    private class SearchTerm extends AsyncTask<String,StringBuilder,String>{
        StringBuilder result = new StringBuilder("Invalid Input");
        @Override
        protected String doInBackground(String[] params) {
            System.out.println("****THREAD 2 started");
            String line = params[0];
            String[] stringSearches = line.split("\\s+");
            if(stringSearches.length==0){
                return null;
            }
            WikiSearch[] searches = new WikiSearch[stringSearches.length];
            for(int i = 0 ; i < stringSearches.length ; i++) {
                if(!stringSearches[i].matches(validationRegex)) { // input validation
                    return null;
                }
                searches[i] = WikiSearch.search(stringSearches[i], index);
            }
            WikiSearch search = WikiSearch.orAll(searches);
            List<Map.Entry<String,Integer>> list = search.sort();
            result = new StringBuilder("");
            int count = 0;
            int textBlurbSize = 200;
            for(Map.Entry<String,Integer> entry : list){
                String textBlurb = "";
                WikiFetcher fetcher = new WikiFetcher();
                try {
                    Elements elements = fetcher.fetchWikipedia(entry.getKey());
                    for(int i = 0;i<3;i++) {
                        Element element = elements.get(i);
                        Iterable<Node> iter = new WikiNodeIterable(element);
                        for (Node n : iter)
                            if (n instanceof TextNode) {
                                textBlurb += n.toString();
                                if (textBlurb.length() > textBlurbSize)
                                    break;
                            }
                        textBlurb = textBlurb.trim();
                        if(textBlurb.length()>textBlurbSize*.9)
                            break;
                    }
                }catch (Exception e){

                }
                result.append(entry.getKey());
                result.append("\n");
                if(textBlurb.length()>=textBlurbSize)
                    result.append(textBlurb.substring(0,textBlurbSize).trim());
                else
                    result.append(textBlurb.trim());
                result.append("...");
                result.append("\n\n");
                if(++count>4) {
                    if (!isCancelled())
                        publishProgress(result);
                    else
                        return null;
                }
            }
            System.out.println("****Thread2 eneded");
            return null;
        }
        @Override
        protected void onProgressUpdate(StringBuilder[] progress){
            output.setText(progress[0]);
        }
        @Override
        protected void onPostExecute(String result) {
                output.setText(this.result);
        }
    }
    private class CreateJedis extends AsyncTask{

        @Override
        protected Object doInBackground(Object[] params) {
            System.out.println("THREAD started");
            try {
                jedis = JedisMaker.make();
                index = new JedisIndex(jedis);
            } catch (Exception e) {
                e.printStackTrace();
            }
            System.out.println("Thread eneded");
            return null;
        }


        @Override
        protected void onPostExecute(Object result) {
            output.setText("");
        }
    }
}
