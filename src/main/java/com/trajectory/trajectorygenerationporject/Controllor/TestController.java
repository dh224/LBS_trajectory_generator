package com.trajectory.trajectorygenerationporject.Controllor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
public class TestController {
    @CrossOrigin(originPatterns = "http://localhost:3000")
    @GetMapping("/list")
    public String list(){
        return "hello world";
    }
}
