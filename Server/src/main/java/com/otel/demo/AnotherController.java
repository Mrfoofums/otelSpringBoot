package com.otel.demo;

import java.util.concurrent.atomic.AtomicLong;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AnotherController {
    private final AtomicLong counter = new AtomicLong();

    @RequestMapping("/api/2")
    public Greeting greeting(@RequestParam(value="name", defaultValue="level1Default") String name) {
     
                return new Greeting(counter.incrementAndGet(), getContent());
    }

    public String getContent(){
   
        return "Some content";
    }

}