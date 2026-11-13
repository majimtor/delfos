package com.manuelalejandro.delfos.results

import com.manuelalejandro.delfos.api.ApiTest
import com.manuelalejandro.delfos.auth.SessionAuthenticationFilter
import com.manuelalejandro.delfos.persistence.AccountEntity
import com.manuelalejandro.delfos.persistence.AccountRepository
import java.time.Instant
import java.util.UUID
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.crypto.password.PasswordEncoder
import tools.jackson.databind.JsonNode
import tools.jackson.databind.json.JsonMapper

class ResultsApiTest : ApiTest() {

    @Autowired lateinit var accounts: AccountRepository
    @Autowired lateinit var passwordEncoder: PasswordEncoder

    private val json = JsonMapper.builder().build()
    private lateinit var token: String
    private lateinit var id: String
    private lateinit var publicId: String

    @BeforeEach
    fun publishedSurveyWithThreeResponses() {
        token = tokenOf(newAccount())
        val created = json.readTree(manager(HttpMethod.POST, "/api/v1/surveys", """{"title":"Five types","language":"en"}""").body)
        id = created.get("id").asString()
        publicId = created.get("publicId").asString()
        assertThat(manager(HttpMethod.PUT, "/api/v1/surveys/$id/draft", DEFINITION).statusCode).isEqualTo(HttpStatus.OK)
        assertThat(manager(HttpMethod.POST, "/api/v1/surveys/$id/publish").statusCode).isEqualTo(HttpStatus.OK)
        submit(1, """[{"questionKey":"colour","optionKey":"red"},{"questionKey":"toppings","optionKeys":["a","b"]},{"questionKey":"age","number":30},{"questionKey":"mood","number":4},{"questionKey":"note","text":"first"}]""")
        submit(1, """[{"questionKey":"colour","optionKey":"red"},{"questionKey":"toppings","optionKeys":["b"]},{"questionKey":"age","number":40},{"questionKey":"mood","number":5}]""")
        submit(1, """[{"questionKey":"colour","optionKey":"blue"},{"questionKey":"age","number":20},{"questionKey":"mood","number":4},{"questionKey":"note","text":"third"}]""")
    }

    @Test
    fun `results of a version aggregate every type and change their ETag with new responses`() {
        val first = manager(HttpMethod.GET, "/api/v1/surveys/$id/versions/1/results")
        assertThat(first.statusCode).isEqualTo(HttpStatus.OK)
        val etag = first.headers.eTag!!
        val body = json.readTree(first.body)
        assertThat(body.get("responseCount").asInt()).isEqualTo(3)
        val byKey = body.get("questions").values().associateBy { it.get("key").asString() }

        assertThat(byKey.getValue("colour").get("answerCount").asInt()).isEqualTo(3)
        assertThat(byKey.getValue("colour").get("options").values().map { it.get("key").asString() to it.get("count").asInt() }).containsExactly("red" to 2, "green" to 0, "blue" to 1)
        assertThat(byKey.getValue("toppings").get("answerCount").asInt()).isEqualTo(2)
        assertThat(byKey.getValue("toppings").get("options").values().map { it.get("count").asInt() }).containsExactly(1, 2, 0)
        val age = byKey.getValue("age").get("number")
        assertThat(age.get("count").asInt()).isEqualTo(3)
        assertThat(age.get("mean").asDouble()).isCloseTo(30.0, within(0.001))
        assertThat(age.get("min").asDouble()).isEqualTo(20.0)
        assertThat(age.get("max").asDouble()).isEqualTo(40.0)
        assertThat(byKey.getValue("mood").get("distribution").values().map { it.get("value").asInt() to it.get("count").asInt() }).containsExactly(1 to 0, 2 to 0, 3 to 0, 4 to 2, 5 to 1)
        assertThat(byKey.getValue("note").get("answerCount").asInt()).isEqualTo(2)
        assertThat(byKey.getValue("note").has("options")).isFalse()

        val unchanged = manager(HttpMethod.GET, "/api/v1/surveys/$id/versions/1/results", ifNoneMatch = etag)
        assertThat(unchanged.statusCode).isEqualTo(HttpStatus.NOT_MODIFIED)
        assertThat(unchanged.body).isNull()

        submit(1, """[{"questionKey":"colour","optionKey":"green"},{"questionKey":"age","number":50},{"questionKey":"mood","number":1}]""")
        val changed = manager(HttpMethod.GET, "/api/v1/surveys/$id/versions/1/results", ifNoneMatch = etag)
        assertThat(changed.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(changed.headers.eTag).isNotEqualTo(etag)
        assertThat(json.readTree(changed.body).get("responseCount").asInt()).isEqualTo(4)
    }

    @Test
    fun `comparable results aggregate across versions and mark questions that changed shape`() {
        manager(HttpMethod.POST, "/api/v1/surveys/$id/draft")
        assertThat(manager(HttpMethod.PUT, "/api/v1/surveys/$id/draft", DEFINITION_V2).statusCode).isEqualTo(HttpStatus.OK)
        assertThat(manager(HttpMethod.POST, "/api/v1/surveys/$id/publish").statusCode).isEqualTo(HttpStatus.OK)
        submit(2, """[{"questionKey":"colour","optionKey":"yellow"},{"questionKey":"age","number":60},{"questionKey":"mood","number":2},{"questionKey":"note","text":"fourth"}]""")

        val response = manager(HttpMethod.GET, "/api/v1/surveys/$id/results?scope=comparable")
        assertThat(response.statusCode).describedAs(response.body).isEqualTo(HttpStatus.OK)
        val body = json.readTree(response.body)
        assertThat(body.get("versionNumbers").values().map { it.asInt() }).containsExactly(1, 2)
        assertThat(body.get("responseCount").asInt()).isEqualTo(4)
        val byKey = body.get("questions").values().associateBy { it.get("key").asString() }
        assertThat(byKey.getValue("colour").get("comparable").asBoolean()).isFalse()
        assertThat(byKey.getValue("colour").has("options")).isFalse()
        assertThat(byKey.getValue("colour").get("versionNumbers").values().map { it.asInt() }).containsExactly(1, 2)
        assertThat(byKey.getValue("age").get("comparable").asBoolean()).isTrue()
        assertThat(byKey.getValue("age").get("number").get("count").asInt()).isEqualTo(4)
        assertThat(byKey.getValue("age").get("number").get("max").asDouble()).isEqualTo(60.0)
        assertThat(byKey.getValue("toppings").get("versionNumbers").values().map { it.asInt() }).containsExactly(1)
        assertThat(byKey.getValue("note").get("answerCount").asInt()).isEqualTo(3)

        val wrongScope = manager(HttpMethod.GET, "/api/v1/surveys/$id/results?scope=everything")
        assertThat(wrongScope.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        assertThat(json.readTree(wrongScope.body).get("errors").get(0).get("path").asString()).isEqualTo("scope")
    }

    @Test
    fun `responses and answers are listed newest first with cursors`() {
        val page1 = json.readTree(manager(HttpMethod.GET, "/api/v1/surveys/$id/versions/1/responses?limit=2").body)
        assertThat(page1.get("items").size()).isEqualTo(2)
        val newest = page1.get("items").get(0)
        assertThat(newest.get("versionNumber").asInt()).isEqualTo(1)
        assertThat(newest.get("answers").values().map { it.get("questionKey").asString() }).containsExactlyInAnyOrder("colour", "age", "mood", "note")
        assertThat(newest.get("answers").values().first { it.get("questionKey").asString() == "colour" }.get("optionKey").asString()).isEqualTo("blue")
        val cursor = page1.get("nextCursor").asString()
        val page2 = json.readTree(manager(HttpMethod.GET, "/api/v1/surveys/$id/versions/1/responses?limit=2&cursor=$cursor").body)
        assertThat(page2.get("items").size()).isEqualTo(1)
        assertThat(page2.get("nextCursor")).isNull()
        assertThat(page2.get("items").get(0).get("answers").values().first { it.get("questionKey").asString() == "toppings" }.get("optionKeys").values().map { it.asString() }).containsExactly("a", "b")

        val notes = json.readTree(manager(HttpMethod.GET, "/api/v1/surveys/$id/versions/1/answers?questionKey=note").body)
        assertThat(notes.get("items").values().map { it.get("answer").get("text").asString() }).containsExactly("third", "first")
        assertThat(notes.get("items").get(0).get("responseId").asString()).isNotBlank()

        val unknown = manager(HttpMethod.GET, "/api/v1/surveys/$id/versions/1/answers?questionKey=nope")
        assertThat(unknown.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
    }

    @Test
    fun `the responses of a version export as CSV, oldest first, with a column per question and per option`() {
        submit(1, """[{"questionKey":"colour","optionKey":"green"},{"questionKey":"age","number":25},{"questionKey":"mood","number":3},{"questionKey":"note","text":"=SUM(A1:A2)"}]""")
        submit(1, """[{"questionKey":"colour","optionKey":"green"},{"questionKey":"age","number":35},{"questionKey":"mood","number":2},{"questionKey":"note","text":"He said \"hi\", twice"}]""")

        val response = manager(HttpMethod.GET, "/api/v1/surveys/$id/versions/1/export.csv")
        assertThat(response.statusCode).describedAs(response.body).isEqualTo(HttpStatus.OK)
        assertThat(response.headers.contentType.toString()).isEqualTo("text/csv;charset=UTF-8")
        assertThat(response.headers.contentDisposition.type).isEqualTo("attachment")
        assertThat(response.headers.contentDisposition.filename).isEqualTo("$publicId-v1.csv")
        val body = response.body!!
        assertThat(body).startsWith("\uFEFF")
        val lines = body.removePrefix("\uFEFF").split("\r\n")
        assertThat(lines.first()).isEqualTo(
            "Response id,Submitted at,Colour (colour),Toppings (toppings) / A (a),Toppings (toppings) / B (b),Toppings (toppings) / C (c),Age (age),Mood (mood),Note (note)",
        )
        val rows = lines.drop(1).filter { it.isNotEmpty() }
        assertThat(rows).hasSize(5)
        val submittedAt = rows.map { it.split(",")[1] }
        assertThat(submittedAt).allMatch { Regex("""\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}Z""").matches(it) }
        assertThat(submittedAt).isSorted()
        assertThat(rows.map { it.substringAfter(',').substringAfter(',') }).containsExactly(
            "Red,1,1,0,30,4,first",
            "Red,0,1,0,40,5,",
            "Blue,,,,20,4,third",
            "Green,,,,25,3,'=SUM(A1:A2)",
            "Green,,,,35,2,\"He said \"\"hi\"\", twice\"",
        )
        assertThat(manager(HttpMethod.GET, "/api/v1/surveys/$id/versions/9/export.csv").statusCode).isEqualTo(HttpStatus.NOT_FOUND)
    }

    @Test
    fun `the responses of a version export as a workbook with the same layout as the CSV`() {
        val response = client.get().uri("/api/v1/surveys/$id/versions/1/export.xlsx").headers {
            it.setBearerAuth(token)
            it.set(SessionAuthenticationFilter.CLIENT_HEADER, "test")
        }.retrieve().toEntity(ByteArray::class.java)
        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.headers.contentType.toString()).isEqualTo("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
        assertThat(response.headers.contentDisposition.filename).isEqualTo("$publicId-v1.xlsx")
        val bytes = response.body!!
        assertThat(bytes.take(2)).containsExactly('P'.code.toByte(), 'K'.code.toByte())
        val sheet = java.util.zip.ZipInputStream(java.io.ByteArrayInputStream(bytes)).use { zip ->
            generateSequence { zip.nextEntry }.first { it.name == "xl/worksheets/sheet1.xml" }
            String(zip.readBytes(), Charsets.UTF_8)
        }
        assertThat(sheet).contains("<t xml:space=\"preserve\">Colour (colour)</t>")
        assertThat(sheet).contains("<t xml:space=\"preserve\">Toppings (toppings) / C (c)</t>")
        assertThat(Regex("<row r=\"\\d+\">").findAll(sheet).count()).isEqualTo(4)
        assertThat(sheet).contains("<c r=\"C2\" t=\"inlineStr\"><is><t xml:space=\"preserve\">Red</t></is></c>")
        assertThat(sheet).contains("<c r=\"G2\"><v>30</v></c>")
        assertThat(sheet).contains("<c r=\"I2\" t=\"inlineStr\"><is><t xml:space=\"preserve\">first</t></is></c>")
        assertThat(manager(HttpMethod.GET, "/api/v1/surveys/$id/versions/9/export.xlsx").statusCode).isEqualTo(HttpStatus.NOT_FOUND)
    }

    @Test
    fun `another manager cannot read results`() {
        val other = tokenOf(newAccount())
        for (path in listOf("/api/v1/surveys/$id/versions/1/results", "/api/v1/surveys/$id/results?scope=comparable", "/api/v1/surveys/$id/versions/1/responses", "/api/v1/surveys/$id/versions/1/answers?questionKey=note", "/api/v1/surveys/$id/versions/1/export.csv", "/api/v1/surveys/$id/versions/1/export.xlsx")) {
            assertThat(manager(HttpMethod.GET, path, bearer = other).statusCode).describedAs(path).isEqualTo(HttpStatus.NOT_FOUND)
        }
    }

    private fun submit(version: Int, answers: String) {
        val response = client.post().uri("/api/v1/public/surveys/$publicId/responses").contentType(MediaType.APPLICATION_JSON)
            .body("""{"versionNumber":$version,"answers":$answers}""").retrieve().toEntity(String::class.java)
        assertThat(response.statusCode).describedAs(response.body).isEqualTo(HttpStatus.CREATED)
    }

    private fun manager(method: HttpMethod, path: String, body: String? = null, bearer: String = token, ifNoneMatch: String? = null): ResponseEntity<String> {
        val request = client.method(method).uri(path).headers {
            it.setBearerAuth(bearer)
            it.set(SessionAuthenticationFilter.CLIENT_HEADER, "test")
            if (ifNoneMatch != null) it.set(HttpHeaders.IF_NONE_MATCH, ifNoneMatch)
        }
        val withBody = if (body != null) request.contentType(MediaType.APPLICATION_JSON).body(body) else request
        return withBody.retrieve().toEntity(String::class.java)
    }

    private fun newAccount(): Pair<String, String> {
        val username = "manager-${UUID.randomUUID().toString().take(8)}"
        val now = Instant.now()
        accounts.save(AccountEntity(UUID.randomUUID(), username, requireNotNull(passwordEncoder.encode("pw")), createdAt = now, updatedAt = now))
        return username to "pw"
    }

    private fun tokenOf(credentials: Pair<String, String>): String {
        val response = client.post().uri("/api/v1/auth/sessions").contentType(MediaType.APPLICATION_JSON)
            .body("""{"username":"${credentials.first}","password":"${credentials.second}","transport":"BEARER"}""")
            .retrieve().toEntity(String::class.java)
        assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
        return json.readTree(response.body).get("token").asString()
    }

    @Suppress("unused")
    private fun JsonNode.text(field: String): String = get(field).asString()

    private companion object {
        val DEFINITION = """
            {"questions":[
              {"key":"colour","type":"SINGLE_CHOICE","text":"Colour","required":true,"options":[{"key":"red","label":"Red"},{"key":"green","label":"Green"},{"key":"blue","label":"Blue"}]},
              {"key":"toppings","type":"MULTIPLE_CHOICE","text":"Toppings","options":[{"key":"a","label":"A"},{"key":"b","label":"B"},{"key":"c","label":"C"}]},
              {"key":"age","type":"NUMBER","text":"Age","required":true,"min":0,"max":120,"integerOnly":true},
              {"key":"mood","type":"SCALE","text":"Mood","required":true,"max":5},
              {"key":"note","type":"TEXT","text":"Note","maxLength":100}
            ],"rules":[]}
        """.trimIndent()
        val DEFINITION_V2 = DEFINITION
            .replace("""{"key":"blue","label":"Blue"}""", """{"key":"blue","label":"Blue"},{"key":"yellow","label":"Yellow"}""")
            .replace(Regex("""\s*\{"key":"toppings".*\n"""), "\n")
    }
}
