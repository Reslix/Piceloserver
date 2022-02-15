package com.scryer.model.ddb;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class Elo {
    private int rating;
    private long createDate;
    private List<Comparison> comparisons;
}
