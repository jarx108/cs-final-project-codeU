package com.example.utd.wikipeadiasearchapp;

import redis.clients.jedis.Jedis;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;

/**
 * Created by aliceli on 8/9/16.
 */
public class WikiHome {
    public static void main(String[] args) throws IOException {
        Jedis jedis = JedisMaker.make();
        JedisIndex index = new JedisIndex(jedis);

        String validationRegex = "\\w+";
        Scanner input = new Scanner(System.in);
        main:while(true) {
            System.out.print("Input search request: ");
            String line = input.nextLine();
            if(line.equals("EXIT"))
                break;
            line = line.toLowerCase();
            String[] stringSearches = line.split("\\s+");
            if(stringSearches.length==0){
                System.out.println("Invalid input");
                continue;
            }
            WikiSearch[] searches = new WikiSearch[stringSearches.length];
            for(int i = 0 ; i < stringSearches.length ; i++) {
                if(!stringSearches[i].matches(validationRegex)) { // input validation
                    System.out.println("Invalid input");
                    continue main;
                }
                searches[i] = WikiSearch.search(stringSearches[i], index);
            }
            WikiSearch search = WikiSearch.orAll(searches);
            List<Map.Entry<String,Integer>> list = search.sort();
            int current = 0;
            while(current<list.size()){
                for(int i = 0;i<10 && current<list.size();i++,current++ )
                    printEntry(list.get(current));
                if(current>=list.size())break;
                String response = "";
                while(!response.equals("yes")&&!response.equals("no")) {
                    System.out.print("show more (yes/no)? ");
                    response = input.nextLine();
                }
                if(response.equals("no"))
                    break;

            }
        }
    }
    public static void printEntry(Entry<String,Integer> entry){
        System.out.println(entry.getKey()+" "+entry.getValue());
    }

}
