package br.com.dti.msa.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequestMapping("/admin")
public class AdminController {
    
    @GetMapping({"","/"})
    public String index() {
        return "admin/index";
    }

    @GetMapping({"search"})
    public String search() {
        return "admin/search";
    }

    @GetMapping({"create"})
    public String create() {
        return "admin/create";
    }
    
}
