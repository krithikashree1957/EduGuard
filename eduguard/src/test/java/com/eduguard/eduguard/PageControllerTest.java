package com.eduguard.eduguard;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

class PageControllerTest {
    private final MockMvc mockMvc = MockMvcBuilders
            .standaloneSetup(new PageController("http://localhost:8081"))
            .build();

    @Test
    void dashboardReceivesConfiguredRiskServiceUrl() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"))
                .andExpect(model().attribute("riskServiceBaseUrl", "http://localhost:8081"));
    }

    @Test
    void studentPageRemainsAvailable() throws Exception {
        mockMvc.perform(get("/students"))
                .andExpect(status().isOk())
                .andExpect(view().name("student"))
                .andExpect(model().attribute("riskServiceBaseUrl", "http://localhost:8081"));
    }
}
