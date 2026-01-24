package com.mesh;

import com.mesh.auth.dto.AuthResponse;
import com.mesh.auth.dto.RegisterRequest;
import com.mesh.message.dto.ChatMessageResponse;
import com.mesh.room.dto.CreateRoomRequest;
import com.mesh.room.dto.RoomResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
class AuthenticatedChatFlowIntegrationTest {

    @Autowired
    TestRestTemplate restTemplate;

    @Test
    void registerLoginCreateRoomAndFetchMessageHistory() {
        String username = "flowuser" + UUID.randomUUID().toString().substring(0, 8);

        var registerRequest = new RegisterRequest(username, username + "@example.com", "password123");
        ResponseEntity<AuthResponse> registerResponse =
                restTemplate.postForEntity("/api/auth/register", registerRequest, AuthResponse.class);

        assertThat(registerResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        String token = registerResponse.getBody().token();
        assertThat(token).isNotBlank();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);

        var createRoomRequest = new CreateRoomRequest("flow-room-" + UUID.randomUUID().toString().substring(0, 8));
        ResponseEntity<RoomResponse> roomResponse = restTemplate.exchange(
                "/api/rooms", HttpMethod.POST, new HttpEntity<>(createRoomRequest, headers), RoomResponse.class);

        assertThat(roomResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        UUID roomId = roomResponse.getBody().id();
        assertThat(roomId).isNotNull();

        ResponseEntity<List<RoomResponse>> listResponse = restTemplate.exchange(
                "/api/rooms", HttpMethod.GET, new HttpEntity<>(headers),
                new org.springframework.core.ParameterizedTypeReference<List<RoomResponse>>() {
                });

        assertThat(listResponse.getBody()).extracting(RoomResponse::name).contains(createRoomRequest.name());

        ResponseEntity<ChatMessageResponse[]> historyResponse = restTemplate.exchange(
                "/api/rooms/" + roomId + "/messages", HttpMethod.GET, new HttpEntity<>(headers),
                ChatMessageResponse[].class);

        assertThat(historyResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(historyResponse.getBody()).isEmpty();
    }

    @Test
    void requestWithoutTokenIsRejected() {
        ResponseEntity<String> response = restTemplate.getForEntity("/api/rooms", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void duplicateUsernameIsRejected() {
        String username = "dupuser" + UUID.randomUUID().toString().substring(0, 8);
        var request = new RegisterRequest(username, username + "@example.com", "password123");

        ResponseEntity<AuthResponse> first = restTemplate.postForEntity("/api/auth/register", request, AuthResponse.class);
        assertThat(first.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        ResponseEntity<String> second = restTemplate.postForEntity("/api/auth/register", request, String.class);
        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }
}
