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





public class Recursive extends RecursiveAction{
    private final Instant deadline;
    private int maxDepth;
    String url;
    private final Clock clock = Clock.systemUTC();
    private final PageParserFactory parserFactory;
    private final List<Pattern> ignoredUrls;
    public static Map<String,Integer> countsCollector = new ConcurrentHashMap<>();
    public static Set<String> visitedUrlsCollector = new ConcurrentSkipListSet<>();



    public Recursive(PageParserFactory parserFactory, List<Pattern> ignoredUrls,
                             Instant deadline, int maxDepth, String url){
        this.deadline = deadline;
        this.maxDepth = maxDepth;
        this.url = url;
        this.parserFactory = parserFactory;
        this.ignoredUrls = ignoredUrls;
    }


    public Set<String> getVisitedUrls(){return visitedUrlsCollector;}

    private Recursive reBuild(String url){
        return new Recursive(parserFactory,ignoredUrls,deadline,maxDepth - 1,url);
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
            return;
        }
        try {
            visitedUrlsCollector.add(url);
        } catch (Exception e) {
            e.getLocalizedMessage();
        }

        PageParser.Result result = parserFactory.get(url).parse();
        for (Map.Entry<String, Integer> e : result.getWordCounts().entrySet()) {
            String ekey = e.getKey();
            int evalue = e.getValue();
            //countsCollector.put(e.getKey(), e.getValue() + countsCollector.get(e.getKey()));
            synchronized (countsCollector) {
                countsCollector.computeIfAbsent(ekey, k -> evalue);
            }
            countsCollector.computeIfPresent(ekey, (k, v) -> v + evalue);

        }


        List<Recursive> subtasks = result.getLinks().stream()
                .map(this::reBuild).collect(Collectors.toList());

        invokeAll(subtasks);
        return;
    }
}
