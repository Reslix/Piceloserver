package com.scryer.endpoint;

import lombok.Getter;
import lombok.Setter;

public class Greeting {
    @Setter
    @Getter
    private String message;

    public Greeting() {}

    public Greeting(String message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return "Greeting{message=\"" + message + "\"}";
    }


}
