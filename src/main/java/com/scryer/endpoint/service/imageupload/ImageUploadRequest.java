package com.scryer.endpoint.service.imageupload;

public record ImageUploadRequest(String id, String size, byte[] image, String[] type) {
}
