package com.udacity.webcrawler;

import com.udacity.webcrawler.parser.PageParser;
import com.udacity.webcrawler.parser.PageParserFactory;

import java.time.Clock;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.RecursiveAction;
import java.util.regex.Pattern;
import java.util.stream.Collectors;



public class RecursiveTaskImp extends RecursiveAction {
    private final Instant deadline;
    private int maxDepth;
    private String url;
    private final Clock clock= Clock.systemUTC();
    private final PageParserFactory parserFactory;
    private final List<Pattern> ignoredUrls;
    private final List<Pattern> ignoredWords;
    public static ConcurrentHashMap<String,Integer> countsCollector = new ConcurrentHashMap<>();
    public static ConcurrentSkipListSet<String> visitedUrlsCollector = new ConcurrentSkipListSet<>();
    private static final Object lock1 = new Object();
    private static final Object lock2 = new Object();



    private RecursiveTaskImp(PageParserFactory parserFactory,List<Pattern> ignoredUrls,List<Pattern> ignoredWords,
                             Instant deadline, int maxDepth, String url,ConcurrentHashMap<String,Integer> countsCollect,
                             ConcurrentSkipListSet<String> visitedUrlsCollect){

        this.parserFactory = parserFactory;
        this.ignoredWords = ignoredWords;
        this.ignoredUrls = ignoredUrls;
        this.deadline = deadline;
        this.maxDepth = maxDepth;
        this.url = url;
        countsCollector = countsCollect;
        visitedUrlsCollector = visitedUrlsCollect;

    }


    public Set<String> getVisitedUrls(){return visitedUrlsCollector;}

    private RecursiveTaskImp reBuild(String url){
        return new RecursiveTaskImp.Builder().setDeadline(deadline)
                .setMaxDepth(maxDepth - 1).setUrl(url).setParserFactory(parserFactory)
                .setIgnoredUrls(ignoredUrls).setIgnoredWords(ignoredWords).setCounts(countsCollector)
                .setVisitedUrls(visitedUrlsCollector).Build();
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
        if (visitedUrlsCollector.contains(url)) {
            System.out.println("visited Url not added content" + url);

            return;
        }
        visitedUrlsCollector.add(url);
        System.out.println("visited Url added content:  " + url);
        System.out.println("the max depth from visitedUrl:  " + maxDepth);


        PageParser.Result result = parserFactory.get(url).parse();

        for (ConcurrentMap.Entry<String, Integer> e : result.getWordCounts().entrySet()) {
            countsCollector.compute(e.getKey(), (k,v) -> (v==null) ? e.getValue(): e.getValue() + v);

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
        private List<Pattern> ignoredWords;
        ConcurrentHashMap<String,Integer> Counts = new ConcurrentHashMap<>();
        ConcurrentSkipListSet<String> visitedUrls = new ConcurrentSkipListSet<>();

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

        public Builder setIgnoredWords(List<Pattern> ignoredWords){
            this.ignoredWords = ignoredWords;
            return this;
        }

        public Builder setMaxDepth(int maxDepth){
            this.maxDepth = maxDepth;
            return this;
        }
        public Builder setCounts(ConcurrentHashMap<String,Integer> counts){
            this.Counts = counts;
            return this;
        }
        public Builder setVisitedUrls(ConcurrentSkipListSet<String> visitedUrls){
            this.visitedUrls = visitedUrls;
            return this;
        }


        public Builder setUrl(String url){
            this.url = Objects.requireNonNull(url);
            return this;

        }
        public RecursiveTaskImp Build(){
            return new RecursiveTaskImp(this.parserFactory,this.ignoredUrls,this.ignoredWords,this.deadline,
                    this.maxDepth,this.url,this.Counts,this.visitedUrls);
        }
    }
}
