package com.staynest.propertyservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.staynest.propertyservice.entity.Property;
import com.staynest.propertyservice.repository.PropertyRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.SecretKey;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PropertyControllerIntegrationTest {

    private static final String JWT_SECRET = "test-secret-key-1234567890abcdefghijklmnopqrstuvwxyz";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PropertyRepository propertyRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void clearProperties() {
        propertyRepository.deleteAll();
    }

    @Test
    void hostCanCreateAndUpdateOwnPropertyAndResponseDoesNotExposeEntityFields() throws Exception {
        String token = token(21L, "HOST");
        String request = propertyJson("Hill house", "Pune", "Villa", "950.00", 4, "WiFi");

        String response = mockMvc.perform(post("/api/properties")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.hostId").value(21))
                .andExpect(jsonPath("$.amenities[0]").value("WiFi"))
                .andReturn().getResponse().getContentAsString();

        org.assertj.core.api.Assertions.assertThat(response).doesNotContain("password");
        Long propertyId = propertyRepository.findAll().get(0).getId();

        mockMvc.perform(get("/api/properties"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].hostId").value(21));

        mockMvc.perform(put("/api/properties/{id}", propertyId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(propertyJson("Updated hill house", "Pune", "Villa", "1100.00", 5, "Pool")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated hill house"))
                .andExpect(jsonPath("$.hostId").value(21))
                .andExpect(jsonPath("$.amenities[0]").value("Pool"));
    }

    @Test
    void hostCannotUpdateOrDeleteAnotherHostsProperty() throws Exception {
        Property property = saveProperty(21L, "Owner's place", "Mumbai", "Apartment", "750.00", 2,
                Set.of("Kitchen"));
        String otherHostToken = token(22L, "HOST");
        String request = propertyJson("Intrusion", "Mumbai", "Apartment", "750.00", 2, "Kitchen");

        mockMvc.perform(put("/api/properties/{id}", property.getId())
                        .header("Authorization", "Bearer " + otherHostToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/properties/{id}", property.getId())
                        .header("Authorization", "Bearer " + otherHostToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanManagePropertiesOwnedByOtherHosts() throws Exception {
        Property property = saveProperty(21L, "Admin managed", "Delhi", "Apartment", "500.00", 3, Set.of());

        mockMvc.perform(delete("/api/properties/{id}", property.getId())
                        .header("Authorization", "Bearer " + token(1L, "ADMIN")))
                .andExpect(status().isNoContent());

        org.assertj.core.api.Assertions.assertThat(propertyRepository.existsById(property.getId())).isFalse();
    }

    @Test
    void searchAppliesLocationTypePriceGuestAmenityPaginationAndSortFilters() throws Exception {
        saveProperty(21L, "Central home", "Pune", "Villa", "900.00", 5, Set.of("WiFi", "Pool"));
        saveProperty(22L, "Budget room", "Pune", "Apartment", "300.00", 2, Set.of("WiFi"));
        saveProperty(23L, "Beach home", "Goa", "Villa", "1200.00", 6, Set.of("Pool"));

        mockMvc.perform(get("/api/properties/search")
                        .param("location", "pUnE")
                        .param("propertyType", "villa")
                        .param("minPrice", "800")
                        .param("maxPrice", "1000")
                        .param("guests", "4")
                        .param("amenity", "wifi")
                        .param("page", "0")
                        .param("size", "1")
                        .param("sortBy", "pricePerNight")
                        .param("direction", "desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.size").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Central home"));
    }

    @Test
    void invalidInputAndInvalidSearchRangesReturnBadRequest() throws Exception {
        mockMvc.perform(post("/api/properties")
                        .header("Authorization", "Bearer " + token(21L, "HOST"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"\",\"propertyType\":\"Villa\",\"pricePerNight\":0,\"maxGuests\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors").exists());

        mockMvc.perform(get("/api/properties/search")
                        .param("minPrice", "100")
                        .param("maxPrice", "50"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void writesRequireHostOrAdminTokenAndMissingPropertyReturnsNotFound() throws Exception {
        mockMvc.perform(post("/api/properties")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(propertyJson("Home", "Pune", "Villa", "100.00", 2, "WiFi")))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/properties")
                        .header("Authorization", "Bearer " + token(31L, "CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(propertyJson("Home", "Pune", "Villa", "100.00", 2, "WiFi")))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/properties")
                        .header("Authorization", "Bearer not-a-valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(propertyJson("Home", "Pune", "Villa", "100.00", 2, "WiFi")))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/properties/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));

        mockMvc.perform(get("/api/properties/search").param("size", "101"))
                .andExpect(status().isBadRequest());
    }

    private Property saveProperty(Long hostId, String title, String city, String propertyType,
                                  String price, int guests, Set<String> amenities) {
        return propertyRepository.save(Property.builder()
                .hostId(hostId)
                .title(title)
                .propertyType(propertyType)
                .city(city)
                .country("India")
                .pricePerNight(new BigDecimal(price))
                .maxGuests(guests)
                .amenities(amenities)
                .status("ACTIVE")
                .build());
    }

    private String propertyJson(String title, String city, String propertyType,
                                String price, int guests, String amenity) throws Exception {
        return objectMapper.writeValueAsString(new PropertyInput(
                title, "Comfortable stay", propertyType, city, "Maharashtra", "India",
                new BigDecimal(price), guests, 2, 1, List.of(amenity), "ACTIVE", 999L));
    }

    private String token(Long userId, String role) {
        SecretKey key = Keys.hmacShaKeyFor(JWT_SECRET.getBytes(StandardCharsets.UTF_8));
        Instant now = Instant.now();
        return Jwts.builder()
                .subject("user" + userId + "@example.com")
                .claim("userId", userId)
                .claim("roles", List.of(role))
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(3600)))
                .signWith(key)
                .compact();
    }

    private record PropertyInput(String title, String description, String propertyType,
                                 String city, String state, String country, BigDecimal pricePerNight,
                                 Integer maxGuests, Integer bedrooms, Integer bathrooms,
                                 List<String> amenities, String status, Long hostId) {
    }
}
