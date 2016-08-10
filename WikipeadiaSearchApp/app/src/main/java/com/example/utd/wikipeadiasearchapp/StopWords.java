package com.example.utd.wikipeadiasearchapp;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;

/**
 * Created by aliceli on 8/9/16.
 */
public class StopWords {

    private static final String STOPWORDFILE = "src/resources/stopwords" ;
    public HashSet<String> stopWordList = new HashSet<String>();

    /**
     * Constructor
     *
     */
    public StopWords() {
        try {
            FileReader input = new FileReader(STOPWORDFILE);
            BufferedReader bufRead = new BufferedReader(input);
            String myLine = null;

            while ((myLine = bufRead.readLine()) != null) {
                //System.out.println(myLine);
                stopWordList.add(myLine);
            }
        }
        catch(FileNotFoundException e) {
            e.printStackTrace();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * checks if term is in hashset
     * @param args
     * @throws IOException
     */
    public boolean contains(String term) {
        return(stopWordList.contains(term));
    }


    public static void main(String[] args) throws IOException {
        StopWords list = new StopWords();
    }
}
