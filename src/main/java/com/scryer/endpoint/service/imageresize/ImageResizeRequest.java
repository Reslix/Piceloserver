package com.scryer.endpoint.service.imageresize;

public record ImageResizeRequest(byte[] imageArray, String[] type, Integer maxDimension) {
}
