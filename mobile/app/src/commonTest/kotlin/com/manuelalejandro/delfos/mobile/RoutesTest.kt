package com.manuelalejandro.delfos.mobile

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RoutesTest {
    @Test
    fun aCodeOpensTheSurvey() {
        assertEquals(AnswerRoute("demo-mobility"), parseSurveyLink("  demo-mobility "))
    }

    @Test
    fun aLinkOfTheWebClientOpensTheSurveyWithItsInvitation() {
        assertEquals(
            AnswerRoute("demo-workshop", "demo-workshop-3"),
            parseSurveyLink("https://delfos.manuelalejandro.com/s/demo-workshop?invitation=demo-workshop-3"),
        )
        assertEquals(AnswerRoute("demo-mobility"), parseSurveyLink("https://delfos.manuelalejandro.com/s/demo-mobility/"))
        assertEquals(AnswerRoute("demo-mobility"), parseSurveyLink("http://localhost:5173/s/demo-mobility?invitation="))
    }

    @Test
    fun anythingElseIsRefused() {
        assertNull(parseSurveyLink(""))
        assertNull(parseSurveyLink("short"))
        assertNull(parseSurveyLink("https://delfos.manuelalejandro.com/surveys"))
        assertNull(parseSurveyLink("https://delfos.manuelalejandro.com/s/not valid"))
    }
}
