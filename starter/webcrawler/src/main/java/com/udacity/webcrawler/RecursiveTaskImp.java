package com.udacity.webcrawler;

import com.udacity.webcrawler.parser.PageParser;
import com.udacity.webcrawler.parser.PageParserFactory;

import java.time.Clock;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.RecursiveTask;
import java.util.regex.Pattern;
import java.util.stream.Collectors;



public class RecursiveTaskImp extends RecursiveAction {
    private final Instant deadline;
    private int maxDepth;
    private String url;
    private final Clock clock= Clock.systemUTC();
    private final PageParserFactory parserFactory;
    private final List<Pattern> ignoredUrls;
    private final List<Pattern> ignoredwords;
    public static Map<String,Integer> countsCollector = new ConcurrentHashMap<>();
    public static Set<String> visitedUrlsCollector = new ConcurrentSkipListSet<>();
    private static final Object lock1 = new Object();
    private static final Object lock2 = new Object();



    private RecursiveTaskImp(PageParserFactory parserFactory,List<Pattern> ignoredwords, List<Pattern> ignoredUrls,
                             Instant deadline, int maxDepth, String url){
        this.deadline = deadline;
        this.maxDepth = maxDepth;
        this.url = url;
        this.parserFactory = parserFactory;
        this.ignoredUrls = ignoredUrls;
        this.ignoredwords = ignoredwords;
    }


    public Set<String> getVisitedUrls(){return visitedUrlsCollector;}

    private RecursiveTaskImp reBuild(String url){
        return new RecursiveTaskImp.Builder().setDeadline(deadline)
                .setMaxDepth(maxDepth - 1).setUrl(url).setParserFactory(parserFactory)
                .setIgnoredUrls(ignoredUrls).setIgnoredWords(ignoredwords).Build();
    }

    public String getUrl(){return this.url;}


    @Override
    protected void compute() {

        if ((maxDepth == 0) || clock.instant().isAfter(deadline)) {
            return;
        }
        for (Pattern pattern : ignoredUrls) {
            if (pattern.matcher(url).matches()) {
                return;
            }
        }
        synchronized (lock1) {
            if (visitedUrlsCollector.contains(url)) {
                System.out.println("visited Url not added content" + url);
                return;
            }
            try {
                visitedUrlsCollector.add(url);
                System.out.println("visited Url added content:  " + url);
                System.out.println("the max depth from visitedUrl:  " + maxDepth);
            } catch (Exception e) {
                System.out.println("error message writing to set: " + e.getLocalizedMessage());
            }
        }

        PageParser.Result result = parserFactory.get(url).parse();
        if (result.getWordCounts().isEmpty()) {
            return;
        }

        for (Map.Entry<String, Integer> e : result.getWordCounts().entrySet()) {
            String ekey = e.getKey();
            int evalue = e.getValue();
            int matches = 0;
            if (!ignoredwords.isEmpty()) {
                for (Pattern pattern : ignoredwords) {
                    if (pattern.matcher(ekey).matches()) {
                        matches = matches + 1;
                    }
                }
            }

            if (matches == 0) {
                synchronized (lock2) {
                    countsCollector.computeIfAbsent(ekey, k -> evalue);

                    countsCollector.computeIfPresent(ekey, (k, v) -> v + evalue);
                    //System.out.println("the max depth from countCollectors:  " + maxDepth);
                    //System.out.println("visited Url added countCollectors:  " + url);
                }
            }
        }


        List<RecursiveTaskImp> subtasks = result.getLinks().stream()
                .map((l) -> reBuild(l)).collect(Collectors.toList());


        invokeAll(subtasks);
        return;
    }



    public static final class Builder{
        private  Instant deadline ;
        private int maxDepth;
        private String url;
        private PageParserFactory parserFactory;
        private List<Pattern> ignoredUrls;
        private List<Pattern> ignoredwords;

        public Builder setDeadline(Instant deadline){
            this.deadline = deadline;
            return this;
        }
        public Builder setParserFactory(PageParserFactory parserFactory){
            this.parserFactory = parserFactory;
            return this;

        }
        public Builder setIgnoredUrls(List<Pattern> ignoredUrls){
            this.ignoredUrls = Objects.requireNonNull(ignoredUrls);
            return this;
        }

        public Builder setIgnoredWords(List<Pattern> ignoredwords){
            this.ignoredwords = ignoredwords;
            return this;
        }

        public Builder setMaxDepth(int maxDepth){
            this.maxDepth = maxDepth;
            return this;
        }


        public Builder setUrl(String url){
            this.url = Objects.requireNonNull(url);
            return this;

        }
        public RecursiveTaskImp Build(){
            return new RecursiveTaskImp(this.parserFactory,this.ignoredUrls,this.ignoredwords,this.deadline,
                    this.maxDepth,this.url);
        }
    }
}
