package fr.insa.TPKubernetes.controller;

import fr.insa.TPKubernetes.model.HelloRequest;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/monservice")
public class HelloController {

    @GetMapping("/echo/{nom}")
    public String echo(@PathVariable String nom) {
        return "Bonjour " + nom;
    }

    @PostMapping("/hello")
    public String hello(@RequestBody HelloRequest request) {
        return "Bonjour " + request.getNom();
    }
} 