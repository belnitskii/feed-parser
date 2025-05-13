package com.belnitskii.feedparser;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.List;

@AllArgsConstructor
@Getter
@Setter
public class PostData {
    private String title;
    private String url;
    private Date publishedDate;
    private List<String> keywords;
    private double score;
}


