package com.scryer.endpoint.service.imageupload;

public record ImageUploadRequest(String userId, String id, String size, byte[] image, String[] type) {
}
