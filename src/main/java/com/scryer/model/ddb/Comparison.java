package com.scryer.model.ddb;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Comparison {
    private long imageId1;
    private long imageId2;
    private int magnitude;
    private long createDate;
}
