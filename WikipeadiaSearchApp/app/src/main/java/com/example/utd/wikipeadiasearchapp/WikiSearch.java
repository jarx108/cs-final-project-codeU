package com.example.utd.wikipeadiasearchapp;

import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;
import java.util.regex.Matcher;

import redis.clients.jedis.Jedis;


/**
 * Represents the results of a search query.
 *
 */
public class WikiSearch {
	
	// map from URLs that contain the term(s) to relevance score
	private Map<String, Integer> map;

    // map for caching relevancy scores
    private Map<String, Double> relevanceMap = new HashMap<String, Double>();
    private String searchterm;
    int urlsWithTerm = -1;
    double idfNum = -1;




    /**
	 * Constructor.
	 * 
	 * @param map
	 */
	public WikiSearch(Map<String, Integer> map) {
        Map<String, Integer>freshMap = new TreeMap<>(map);
		this.map = freshMap;
	}

	/**
	 * returns  map in WikiSearch
	 *
	 *
	 */
	public Map<String,Integer> getMap() {
		return map;
	}
	
	/**
	 * Looks up the relevance of a given URL.
	 * 
	 * @param url
	 * @return
	 */
	public Integer getRelevance(String url) {
		Integer relevance = map.get(url);
		return relevance==null ? 0: relevance;
	}
	
	/**
	 * Prints the contents in order of term frequency.
	 * 
	 *
	 */
	public void print() {
		List<Entry<String, Integer>> entries = sort();

        for (Entry<String, Integer> entry: entries) {
			System.out.println(entry);
		}
	}
	
	/**
	 * Computes the union of two search results.
	 * Iterates through WikiSearch that and either updates existing key with new relevance score or adds a new entry
	 * 
	 * @param that
	 * @return New WikiSearch object.
	 */
	public WikiSearch or(WikiSearch that) {
        // FILL THIS IN!
        Map<String,Integer> results = map;
        for (Map.Entry<String,Integer> entry: that.getMap().entrySet()) {
			String url = entry.getKey();
			// url exists in both this WikiSearch map and that WikiSearch map
			if (map.containsKey(url)) {
        		//update relevance score
        		int newRelevance = totalRelevance(getRelevance(url), that.getRelevance(url));
        		results.put(url,newRelevance);
        	}
        	// url doesn't exist in this WikiSearch map yet
        	else {
        		results.put(url, entry.getValue());
        	}
		}
		WikiSearch resultsWikiSearch = new WikiSearch(results);
        
		return resultsWikiSearch;
	}
	public static WikiSearch orAll(WikiSearch[] searches){
		if(searches.length==0)
			return null;
		if(searches.length==1)
			return searches[0];
        Map<String,Integer> result = new HashMap<String,Integer>();
        for(WikiSearch search : searches){
            Map<String,Integer> tmpMap = search.getMap();
            for(Entry<String,Integer> entry : tmpMap.entrySet()) {
                if (entry.getValue() == 0)
                    continue;
                if (result.get(entry.getKey()) == null)
                    result.put(entry.getKey(), entry.getValue());
                else
                    result.put(entry.getKey(), entry.getValue() * result.get(entry.getKey()));
            }
        }
		return new WikiSearch(result);
	}

	
	/**
	 * Computes the intersection of two search results.
	 * 
	 * @param that
	 * @return New WikiSearch object.
	 */
	public WikiSearch and(WikiSearch that) {
        // FILL THIS IN!
        Map<String,Integer> results = new HashMap<String,Integer>();
        for (Map.Entry<String,Integer> entry: that.getMap().entrySet()) {
			String url = entry.getKey();
			//url exists in both this WikiSearch map and that WikiSearch map
			if (map.containsKey(url)) {
        		//update relevance score
        		int newRelevance = totalRelevance(getRelevance(url), that.getRelevance(url));
        		results.put(url,newRelevance);
        	}
		}
		WikiSearch resultsWikiSearch = new WikiSearch(results);
		return resultsWikiSearch;
	}
	
	/**
	 * Computes the difference of two search results.
	 * 
	 * @param that
	 * @return New WikiSearch object.
	 */
	public WikiSearch minus(WikiSearch that) {
        // FILL THIS IN!
        Map<String,Integer> results = map;
        for (Map.Entry<String, Integer> entry: that.getMap().entrySet()) {
			String url = entry.getKey();
			//url exists in both this WikiSearch map and that WikiSearch map
			if (map.containsKey(url)) {
        		//delete this url from WikiSearch map
        		results.remove(url); 
        	}
		}
		WikiSearch resultsWikiSearch = new WikiSearch(results);
		return resultsWikiSearch;
	}
	
	/**
	 * Computes the relevance of a search with multiple terms.
	 * 
	 * @param rel1: relevance score for the first search
	 * @param rel2: relevance score for the second search
	 * @return
	 */
	protected int totalRelevance(Integer rel1, Integer rel2) {
		// simple starting place: relevance is the sum of the term frequencies.
		return rel1 + rel2;
	}

	/**
	 * Sort the results by relevance.
	 * 
	 * @return List of entries with URL and relevance.
	 */
	public List<Entry<String, Integer>> sort() {
        // FILL THIS IN!
        List<Entry<String, Integer>> entryList = new LinkedList<Entry<String, Integer>>(map.entrySet());
        Comparator<Entry<String,Integer>> entryComparator = new Comparator<Entry<String,Integer>>() {

		    public int compare(Entry<String,Integer> entry1, Entry<String,Integer> entry2) {
		    	//descending sort
		       	if (entry1.getValue() > entry2.getValue()) {
		            return -1;
		        }
		        if (entry1.getValue() < entry2.getValue()) {
		            return 1;
		        }
		        
		        return 0;
	   		}
		};
        Collections.sort(entryList, entryComparator);
		return entryList;
	}

    /**
     * Sort the result
     * @return
     */
    public double tf_idf_relevance(String url, JedisIndex index) {
        int totalURLCount = index.termCounterKeys().size();
        if (urlsWithTerm == -1) urlsWithTerm = index.getCountsFaster(searchterm).size();
        if (idfNum == -1) idfNum = idfSmoothHelper(searchterm, totalURLCount, index);


        if (relevanceMap.containsKey(url)) {
            return relevanceMap.get(url);
        }
        else {

            double tfNum = tfNormalizedHelper(url, index);

            double tfIdf = tfNum * idfNum;

            relevanceMap.put(url, tfIdf);

            return tfIdf;
        }

    }

    private double tfNormalizedHelper(String url, JedisIndex index) {

        //Map<String, Integer> map = index.getCounts(url);

        int rawCount = this.map.get(url);

        double normalizedTF = 1 + Math.log(rawCount);

        return normalizedTF;
    }

    private double idfSmoothHelper(String term, int urlSetSize, JedisIndex index) {

        // idf smooth
        idfNum = Math.log( 1 + (urlSetSize / Math.abs(1 + urlsWithTerm)));
        return idfNum;
    }

    public void tfIdfPrint(JedisIndex index) {
        List<Entry<String, Integer>> entries = tfIdfSort(index);

        for (Entry<String, Integer> entry: entries) {
            System.out.println(entry);
        }
    }

	/**
	 * Performs a search and makes a WikiSearch object.
	 * 
	 * @param term
	 * @param index
	 * @return
	 */
	public static WikiSearch search(String term, JedisIndex index) {
		Map<String, Integer> map = index.getCountsFaster(term);
        WikiSearch ws =  new WikiSearch(map);
        ws.searchterm = term;
        return ws;

	}


    public  List<Entry<String, Integer>> tfIdfSort(final JedisIndex index) {
        // FILL THIS IN!
        List<Entry<String, Integer>> entryList = new LinkedList<Entry<String, Integer>>(map.entrySet());

        for (Entry<String, Integer> entry : entryList) {
            tf_idf_relevance(entry.getKey(), index);
        }

        Comparator<Entry<String,Integer>> entryComparator = new Comparator<Entry<String,Integer>>() {

            public int compare(Entry<String,Integer> entry1, Entry<String,Integer> entry2) {
                //descending sort

                double entry1Score = relevanceMap.get(entry1.getKey());
                double entry2Score = relevanceMap.get(entry2.getKey());

                if (entry1Score > entry2Score) {
                    return -1;
                }
                else if (entry1Score < entry2Score) {
                    return 1;
                }

                return 0;
            }
        };
        Collections.sort(entryList, entryComparator);
        return entryList;
    }





	public static void main(String[] args) throws IOException {
		
		// make a JedisIndex
		Jedis jedis = JedisMaker.make();
		JedisIndex index = new JedisIndex(jedis); 
		
		// search for the first term
		String term1 = "java";
		System.out.println("Query: " + term1);
		WikiSearch search1 = search(term1, index);
		System.out.println("TF-IDF search:");
        search1.tfIdfPrint(index);
		System.out.println("\n\nNormal search results:");
        search1.print();


        // search for the second term
		String term2 = "programming";
		System.out.println("Query: " + term2);
		WikiSearch search2 = search(term2, index);
		search2.print();
		
		// compute the intersection of the searches
		System.out.println("Query: " + term1 + " AND " + term2);
		WikiSearch intersection = search1.and(search2);
		intersection.print();
	}
}
