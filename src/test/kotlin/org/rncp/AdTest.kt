package org.rncp

import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured
import io.restassured.RestAssured.given
import io.restassured.http.ContentType
import jakarta.inject.Inject
import jakarta.transaction.Transactional
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test
import org.rncp.ad.infra.api.AdDto
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.rncp.feedback.infra.api.FeedbackDTO
import org.rncp.reservation.infra.api.ReservationDTO
import org.rncp.reservation.infra.db.ReservationPostGreRepository
import java.time.LocalDateTime
import java.time.Month

@QuarkusTest
class AdTest {

    @Inject
    lateinit var reservationPostGreRepository: ReservationPostGreRepository

    @Transactional
    fun clearReservations() {
        reservationPostGreRepository.deleteAll()
    }

    private fun clearAds() {
        clearReservations()
        val ads = given().get("/api/ads")
                .then()
                .statusCode(200)
                .extract()
                .body()
                .jsonPath()
                .getList(".", AdDto::class.java)

        ads.forEach { ad ->
            given().delete("/api/ads/${ad.id}")
                    .then()
                    .statusCode(204)
        }
    }

    private fun createAd(requestAd: AdDto): AdDto {
        return given().contentType(ContentType.JSON)
                .body(Json.encodeToString(requestAd))
                .post("/api/ads")
                .then()
                .statusCode(201)
                .extract()
                .`as`(AdDto::class.java)
    }

    private fun createReservation(requestReservation: ReservationDTO): ReservationDTO {
        return RestAssured.given().contentType(ContentType.JSON)
                .body(Json.encodeToString(requestReservation))
                .post("/api/reservation")
                .then()
                .statusCode(201)
                .extract()
                .`as`(ReservationDTO::class.java)
    }

    private fun createFeedback(requestFeedback: FeedbackDTO): FeedbackDTO {
        return RestAssured.given().contentType(ContentType.JSON)
                .body(Json.encodeToString(requestFeedback))
                .post("/api/feedback")
                .then()
                .statusCode(201)
                .extract()
                .`as`(FeedbackDTO::class.java)
    }

    private fun getAdById(adId: Int?): AdDto {
        return given().get("/api/ads/$adId")
                .then()
                .statusCode(200)
                .extract()
                .`as`(AdDto::class.java)
    }

    private fun deleteAd(adId: Int?) {
        given().delete("/api/ads/$adId")
                .then()
                .statusCode(204)
    }

    @Test
    fun testCreateAndGetById() {
        clearAds()
        val requestAd = AdDto(null, "Testeur", "Gauthier Ad", "Description de test", 56.3, -0.2562456, 30.295626, true, "")

        val adGiven = createAd(requestAd)
        val adEntity = getAdById(adGiven.id)

        val expectedAd = AdDto(adGiven.id, "Testeur", "Gauthier Ad", "Description de test", 56.3, -0.2562456, 30.295626, true, "")

        assertEquals(expectedAd, adEntity)
    }

    @Test
    fun testDelete() {
        clearAds()

        val requestAd = AdDto(null, "Testeur", "Gauthier Ad", "Description de test", 56.3, -0.2562456, 30.295626, true, "")

        val adGiven = createAd(requestAd)
        deleteAd(adGiven.id)

        val adsEntity = given().get("/api/ads")
                .then()
                .statusCode(200)
                .extract()
                .body()
                .jsonPath()
                .getList(".", AdDto::class.java)

        assertEquals(0, adsEntity.size)
    }

    @Test
    fun testGetAll() {
        clearAds()

        val requestAd = AdDto(null, "Testeur", "Gauthier Ad", "Description de test", 56.3, -0.2562456, 30.295626, true, "")

        createAd(requestAd)
        createAd(requestAd)

        val adsEntity = given().get("/api/ads")
                .then()
                .statusCode(200)
                .extract()
                .body()
                .jsonPath()
                .getList(".", AdDto::class.java)

        assertEquals(2, adsEntity.size)
    }

    @Test
    fun testGetAllWithLocationFilter() {
        clearAds()

        val requestAd = AdDto(null, "Testeur", "Gauthier Ad", "Description de test", 56.3, 48.8666, 2.3722, true, "")
        val requestAd2 = AdDto(null, "Testeur", "Gauthier Ad", "Description de test", 56.3, 48.8466, 2.3322, true, "")
        val requestAd3 = AdDto(null, "Testeur", "Gauthier Ad", "Description de test", 56.3, 48.8566, 2.4889, true, "")
        val requestAd4 = AdDto(null, "Testeur", "Gauthier Ad", "Description de test", 56.3, 48.8566, 2.4888, true, "")

        createAd(requestAd)
        createAd(requestAd2)
        createAd(requestAd3)
        createAd(requestAd4)

        val adsEntity = given().get("/api/ads?latitude=48.8566&longitude=2.3522&distance=10.0")
                .then()
                .statusCode(200)
                .extract()
                .body()
                .jsonPath()
                .getList(".", AdDto::class.java)

        assertEquals(3, adsEntity.size)
    }

    @Test
    fun testGetAllWithDateFilterTrue() {
        clearAds()

        val requestAd = AdDto(null, "Testeur", "Gauthier Ad", "Description de test", 56.3, 48.8666, 2.3722, true, "")
        val adGiven = createAd(requestAd)

        val requestReservation = ReservationDTO(null, adGiven.id!!, adGiven.userId, LocalDateTime.of(2024, Month.SEPTEMBER, 19, 19, 42, 13), LocalDateTime.of(2024, Month.SEPTEMBER, 20, 19, 42, 13), 2)
        createReservation(requestReservation)

        val adsEntity = given().get("/api/ads?beginDate=2024-09-19T09:00:00&endDate=2024-09-19T10:00:00")
                .then()
                .statusCode(200)
                .extract()
                .body()
                .jsonPath()
                .getList(".", AdDto::class.java)

        assertEquals(1, adsEntity.size)
    }

    @Test
    fun testGetAllWithDateFilterFalse() {
        clearAds()

        val requestAd = AdDto(null, "Testeur", "Gauthier Ad", "Description de test", 56.3, 48.8666, 2.3722, true, "")
        val adGiven = createAd(requestAd)

        val requestReservation = ReservationDTO(null, adGiven.id!!, adGiven.userId, LocalDateTime.of(2024, Month.SEPTEMBER, 19, 19, 42, 13), LocalDateTime.of(2024, Month.SEPTEMBER, 20, 19, 42, 13), 2)
        createReservation(requestReservation)

        val adsEntity = given().get("/api/ads?beginDate=2024-09-20T09:00:00&endDate=2024-09-21T10:00:00")
                .then()
                .statusCode(200)
                .extract()
                .body()
                .jsonPath()
                .getList(".", AdDto::class.java)

        assertEquals(0, adsEntity.size)
    }

    @Test
    fun testGetAllWithRatingFilterTrue() {
        clearAds()

        val requestAd = AdDto(null, "Testeur", "Gauthier Ad", "Description de test", 56.3, 48.8666, 2.3722, true, "")
        val adGiven = createAd(requestAd)

        val requestFeedbackRating5 = FeedbackDTO(null, adGiven.id!!, adGiven.userId, 5, "Super", LocalDateTime.of(2023, Month.SEPTEMBER, 19, 19, 42, 13))
        val requestFeedbackRating4 = FeedbackDTO(null, adGiven.id!!, adGiven.userId, 4, "Super", LocalDateTime.of(2023, Month.SEPTEMBER, 19, 19, 42, 13))

        createFeedback(requestFeedbackRating5)
        createFeedback(requestFeedbackRating5)
        createFeedback(requestFeedbackRating5)
        createFeedback(requestFeedbackRating4)

        val adsEntity = given().get("/api/ads?minRate=4.7")
                .then()
                .statusCode(200)
                .extract()
                .body()
                .jsonPath()
                .getList(".", AdDto::class.java)

        assertEquals(1, adsEntity.size)
    }

    @Test
    fun testGetAllWithRatingFilterFalse() {
        clearAds()

        val requestAd = AdDto(null, "Testeur", "Gauthier Ad", "Description de test", 56.3, 48.8666, 2.3722, true, "")
        val adGiven = createAd(requestAd)

        val requestFeedbackRating5 = FeedbackDTO(null, adGiven.id!!, adGiven.userId, 5, "Super", LocalDateTime.of(2023, Month.SEPTEMBER, 19, 19, 42, 13))
        val requestFeedbackRating4 = FeedbackDTO(null, adGiven.id!!, adGiven.userId, 4, "Super", LocalDateTime.of(2023, Month.SEPTEMBER, 19, 19, 42, 13))

        createFeedback(requestFeedbackRating5)
        createFeedback(requestFeedbackRating5)
        createFeedback(requestFeedbackRating5)
        createFeedback(requestFeedbackRating4)

        val adsEntity = given().get("/api/ads?minRate=4.8")
                .then()
                .statusCode(200)
                .extract()
                .body()
                .jsonPath()
                .getList(".", AdDto::class.java)

        assertEquals(0, adsEntity.size)
    }

    @Test
    fun testPublish() {
        val requestAd = AdDto(null, "Testeur", "Gauthier Ad", "Description de test", 56.3, -0.2562456, 30.295626, false, "")

        val adGiven = createAd(requestAd)

        given().contentType(ContentType.JSON)
                .body(requestAd)
                .post("/api/ads/${adGiven.id}/publish")
                .then()
                .statusCode(204)

        val adPublish = getAdById(adGiven.id)

        assertEquals(adGiven.id, adPublish.id)
        assertEquals(adGiven.userId, adPublish.userId)
        assertEquals(adGiven.name, adPublish.name)
        assertEquals(adGiven.description, adPublish.description)
        assertEquals(adGiven.hourPrice, adPublish.hourPrice)
        assertEquals(adGiven.latitude, adPublish.latitude)
        assertEquals(adGiven.longitude, adPublish.longitude)
        assertNotEquals(adGiven.state, adPublish.state)
    }

    @Test
    fun testUnpublish() {
        val requestAd = AdDto(null, "Testeur", "Gauthier Ad", "Description de test", 56.3, -0.2562456, 30.295626, true, "")


        val adGiven = createAd(requestAd)

        given().contentType(ContentType.JSON)
                .body(requestAd)
                .post("/api/ads/${adGiven.id}/unpublish")
                .then()
                .statusCode(204)

        val adUnpublish = getAdById(adGiven.id)

        assertEquals(adGiven.id, adUnpublish.id)
        assertEquals(adGiven.userId, adUnpublish.userId)
        assertEquals(adGiven.name, adUnpublish.name)
        assertEquals(adGiven.description, adUnpublish.description)
        assertEquals(adGiven.hourPrice, adUnpublish.hourPrice)
        assertEquals(adGiven.latitude, adUnpublish.latitude)
        assertEquals(adGiven.longitude, adUnpublish.longitude)
        assertNotEquals(adGiven.state, adUnpublish.state)

    }

    @Test
    fun testUpdate() {
        val requestAd = AdDto(null, "Testeur", "Gauthier Ad", "Description de test", 56.3, -0.2562456, 30.295626, true, "")
        val requestAdUpdate = AdDto(null, "Testeur", "Gauthier Ad Update", "Description de test Update", 25.8, -0.2569191, 30.281220, false, "")

        val adGiven = createAd(requestAd)

        given().contentType(ContentType.JSON)
                .body(Json.encodeToString(requestAdUpdate))
                .put("/api/ads/${adGiven.id}")
                .then()
                .statusCode(204)

        val adUpdate = getAdById(adGiven.id)

        val expectedAd = AdDto(adGiven.id, "Testeur", "Gauthier Ad Update", "Description de test Update", 25.8, -0.2569191, 30.281220, false, "")

        assertEquals(expectedAd, adUpdate)
    }

    @Test
    fun testDeleteAdWithActiveReservation() {
        val requestAd = AdDto(null, "Testeur", "Gauthier Ad", "Description de test", 56.3, -0.2562456, 30.295626, true, "")
        val adGiven = createAd(requestAd)

        val requestReservation = ReservationDTO(null, adGiven.id!!, adGiven.userId, LocalDateTime.of(2024, Month.SEPTEMBER, 19, 19, 42, 13), LocalDateTime.of(2024, Month.SEPTEMBER, 20, 19, 42, 13), 1)
        given().contentType(ContentType.JSON)
                .body(Json.encodeToString(requestReservation))
                .post("/api/reservation")
                .then()
                .statusCode(201)
                .extract()
                .`as`(ReservationDTO::class.java)

        given().delete("/api/ads/${adGiven.id!!}")
                .then()
                .statusCode(409)
    }

    @Test
    fun testGetAdDoesNotExist() {
        given().get("/api/ads/0")
                .then()
                .statusCode(404)
    }

    @Test
    fun testUpdateAdDoesNotExist() {
        val requestAdUpdate = AdDto(null, "Testeur", "Gauthier Ad Update", "Description de test Update", 25.8, -0.2569191, 30.281220, false, "")

        given().contentType(ContentType.JSON)
                .body(Json.encodeToString(requestAdUpdate))
                .put("/api/ads/0")
                .then()
                .statusCode(404)
    }

    @Test
    fun testCreateWithNegativeHourPrice() {
        val badRequestAd = AdDto(null, "Testeur", "Gauthier Ad", "Description de test", -56.3, -0.2562456, 30.295626, true, "")
        given().contentType(ContentType.JSON)
                .body(Json.encodeToString(badRequestAd))
                .post("/api/ads")
                .then()
                .statusCode(400)
    }

    @Test
    fun testCreateWithInvalidLatitudeNegative() {
        val badRequestAd = AdDto(null, "Testeur", "Gauthier Ad", "Description de test", 56.3, -100.25624, 30.295626, true, "")
        given().contentType(ContentType.JSON)
                .body(Json.encodeToString(badRequestAd))
                .post("/api/ads")
                .then()
                .statusCode(400)
    }

    @Test
    fun testCreateWithInvalidLatitudePositive() {
        val badRequestAd = AdDto(null, "Testeur", "Gauthier Ad", "Description de test", 56.3, 100.25624, 30.295626, true, "")
        given().contentType(ContentType.JSON)
                .body(Json.encodeToString(badRequestAd))
                .post("/api/ads")
                .then()
                .statusCode(400)
    }

    @Test
    fun testCreateWithInvalidLongitudeNegative() {
        val badRequestAd = AdDto(null, "Testeur", "Gauthier Ad", "Description de test", 56.3, -0.2562456, -300.9562, true, "")
        given().contentType(ContentType.JSON)
                .body(Json.encodeToString(badRequestAd))
                .post("/api/ads")
                .then()
                .statusCode(400)
    }

    @Test
    fun testCreateWithInvalidLongitudePositive() {
        val badRequestAd = AdDto(null, "Testeur", "Gauthier Ad", "Description de test", 56.3, -0.2562456, 300.9562, true, "")
        given().contentType(ContentType.JSON)
                .body(Json.encodeToString(badRequestAd))
                .post("/api/ads")
                .then()
                .statusCode(400)
    }

    @Test
    fun testUpdateWithNegativeHourPrice() {
        val requestAd = AdDto(null, "Testeur", "Gauthier Ad", "Description de test", 56.3, -0.2562456, 30.295626, true, "")
        val adGiven = createAd(requestAd)

        val badRequestHourPrice = AdDto(null, "Testeur", "Gauthier Ad", "Description de test", -56.3, -0.2562456, 30.295626, true, "")
        given().contentType(ContentType.JSON)
                .body(Json.encodeToString(badRequestHourPrice))
                .put("/api/ads/${adGiven.id}")
                .then()
                .statusCode(400)
    }

    @Test
    fun testUpdateWithInvalidLatitudeNegative() {
        val requestAd = AdDto(null, "Testeur", "Gauthier Ad", "Description de test", 56.3, -0.2562456, 30.295626, true, "")
        val adGiven = createAd(requestAd)

        val badRequestLatitudeNegative = AdDto(null, "Testeur", "Gauthier Ad", "Description de test", 56.3, -100.25624, 30.295626, true, "")
        given().contentType(ContentType.JSON)
                .body(Json.encodeToString(badRequestLatitudeNegative))
                .put("/api/ads/${adGiven.id}")
                .then()
                .statusCode(400)
    }

    @Test
    fun testUpdateWithInvalidLatitudePositive() {
        val requestAd = AdDto(null, "Testeur", "Gauthier Ad", "Description de test", 56.3, -0.2562456, 30.295626, true, "")
        val adGiven = createAd(requestAd)

        val badRequestLatitudePositive = AdDto(null, "Testeur", "Gauthier Ad", "Description de test", 56.3, 100.25624, 30.295626, true, "")
        given().contentType(ContentType.JSON)
                .body(Json.encodeToString(badRequestLatitudePositive))
                .put("/api/ads/${adGiven.id}")
                .then()
                .statusCode(400)
    }

    @Test
    fun testUpdateWithInvalidLongitudeNegative() {
        val requestAd = AdDto(null, "Testeur", "Gauthier Ad", "Description de test", 56.3, -0.2562456, 30.295626, true, "")
        val adGiven = createAd(requestAd)

        val badRequestLongitudeNegative = AdDto(null, "Testeur", "Gauthier Ad", "Description de test", 56.3, -0.2562456, -300.9562, true, "")
        given().contentType(ContentType.JSON)
                .body(Json.encodeToString(badRequestLongitudeNegative))
                .put("/api/ads/${adGiven.id}")
                .then()
                .statusCode(400)
    }

    @Test
    fun testUpdateWithInvalidLongitudePositive() {
        val requestAd = AdDto(null, "Testeur", "Gauthier Ad", "Description de test", 56.3, -0.2562456, 30.295626, true, "")
        val adGiven = createAd(requestAd)

        val badRequestLongitudePositive = AdDto(null, "Testeur", "Gauthier Ad", "Description de test", 56.3, -0.2562456, 300.9562, true, "")
        given().contentType(ContentType.JSON)
                .body(Json.encodeToString(badRequestLongitudePositive))
                .put("/api/ads/${adGiven.id}")
                .then()
                .statusCode(400)
    }

    @Test
    fun testPublishAdDoesNotExist() {
        val requestAd = AdDto(null, "Testeur", "Gauthier Ad", "Description de test", 56.3, -0.2562456, 30.295626, false, "")

        given().contentType(ContentType.JSON)
                .body(requestAd)
                .post("/api/ads/0/publish")
                .then()
                .statusCode(404)
    }

    @Test
    fun testUnpublishAdDoesNotExist() {
        val requestAd = AdDto(null, "Testeur", "Gauthier Ad", "Description de test", 56.3, -0.2562456, 30.295626, false, "")

        given().contentType(ContentType.JSON)
                .body(requestAd)
                .post("/api/ads/0/unpublish")
                .then()
                .statusCode(404)
    }



}
