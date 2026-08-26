package br.com.dti.msa.controller;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HomeControllerTest {

    private final HomeController homeController =
            new HomeController();

    @Test
    void shouldReturnHomeIndexView() {
        String result = homeController.index();

        assertEquals("home/index", result);
    }
}