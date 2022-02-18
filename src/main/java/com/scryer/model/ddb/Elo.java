package com.scryer.model.ddb;

import lombok.Builder;

import java.util.List;

public record Elo(Float rating, Long createDate, List<Comparison> comparisons) {
}
