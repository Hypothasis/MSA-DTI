package br.com.dti.msa.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/host")
public class HostController {

    @GetMapping("/app")
    public String app() {
        return "host/app";
    }

    @GetMapping("/server")
    public String server() {
        return "host/server";
    }

    @GetMapping("/db")
    public String db() {
        return "host/db";
    }

}
