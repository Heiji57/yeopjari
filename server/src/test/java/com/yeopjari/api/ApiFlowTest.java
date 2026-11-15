package com.yeopjari.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 실제 Spring 컨텍스트로 API 흐름 확인. 로컬에서 `mvn test` 로 돌린다. */
@SpringBootTest
@AutoConfigureMockMvc
class ApiFlowTest {

    @Autowired
    MockMvc mvc;

    @Test
    void parkingAdviceForTheMall() throws Exception {
        mvc.perform(get("/api/parking/mall_b2/difficulty"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.target.stars").value(4))
                .andExpect(jsonPath("$.alternative.stars").value(1))
                .andExpect(jsonPath("$.walkMinutes").value(3));
    }

    @Test
    void familyFeedHasSeniorWeeklyCard() throws Exception {
        mvc.perform(get("/api/family/kim/feed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.members.length()").value(2));
    }

    @Test
    void unknownTripIs404() throws Exception {
        mvc.perform(post("/api/trips/nope/finish").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }
}
