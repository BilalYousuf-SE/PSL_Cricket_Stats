package com.cricket.stats.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AdminAuthController {
    // AdminAuthFilter validates the same credential used for every mutation.
    @PostMapping("/admin/auth")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void authenticate() {}
}
