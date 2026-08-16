package com.mpa.service.impl;

import com.mpa.service.EmailService.EmailSendResult;
import com.mpa.service.SystemSettingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withUnauthorizedRequest;

class SendifyEmailServiceImplTest {

    private static final String BASE_URL = "https://sendify.vn/api/emails";

    private SystemSettingService settingService;
    private MockRestServiceServer mockServer;
    private SendifyEmailServiceImpl service;

    @BeforeEach
    void setUp() {
        settingService = mock(SystemSettingService.class);
        RestTemplate restTemplate = new RestTemplate();
        mockServer = MockRestServiceServer.createServer(restTemplate);

        service = new SendifyEmailServiceImpl(restTemplate, settingService);
        ReflectionTestUtils.setField(service, "fromAddress", "no-reply@bidv.com.vn");
        ReflectionTestUtils.setField(service, "fromName", "BIDV MPA");
        ReflectionTestUtils.setField(service, "apiKey", "sfy_test_key");
        ReflectionTestUtils.setField(service, "baseUrl", BASE_URL);
    }

    @Test
    void send_khiEmailDangTat_traVeSkippedDisabled_khongGoiApi() {
        when(settingService.getBoolean("CARD_EMAIL_ENABLED", false)).thenReturn(false);

        EmailSendResult result = service.send("kh@example.com", "Subject", "<p>Hi</p>");

        assertThat(result).isEqualTo(EmailSendResult.SKIPPED_DISABLED);
        mockServer.verify();
    }

    @Test
    void send_khiNguoiNhanRong_traVeSkippedDisabled() {
        when(settingService.getBoolean("CARD_EMAIL_ENABLED", false)).thenReturn(true);

        EmailSendResult result = service.send("   ", "Subject", "<p>Hi</p>");

        assertThat(result).isEqualTo(EmailSendResult.SKIPPED_DISABLED);
        mockServer.verify();
    }

    @Test
    void send_khiThieuApiKey_traVeFailed_khongGoiApi() {
        when(settingService.getBoolean("CARD_EMAIL_ENABLED", false)).thenReturn(true);
        ReflectionTestUtils.setField(service, "apiKey", "");

        EmailSendResult result = service.send("kh@example.com", "Subject", "<p>Hi</p>");

        assertThat(result).isEqualTo(EmailSendResult.FAILED);
        mockServer.verify();
    }

    @Test
    void send_khi202Accepted_traVeSuccess_voiRequestDungDinhDang() {
        when(settingService.getBoolean("CARD_EMAIL_ENABLED", false)).thenReturn(true);

        mockServer.expect(requestTo(BASE_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer sfy_test_key"))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.to").value("kh@example.com"))
                .andExpect(jsonPath("$.from").value("BIDV MPA <no-reply@bidv.com.vn>"))
                .andExpect(jsonPath("$.subject").value("Subject"))
                .andExpect(jsonPath("$.html").value("<p>Hi</p>"))
                .andRespond(withStatus(HttpStatus.ACCEPTED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"id\":\"abc-123\",\"status\":\"queued\"}"));

        EmailSendResult result = service.send("kh@example.com", "Subject", "<p>Hi</p>");

        assertThat(result).isEqualTo(EmailSendResult.SUCCESS);
        mockServer.verify();
    }

    @Test
    void send_khi401Unauthorized_traVeFailed() {
        when(settingService.getBoolean("CARD_EMAIL_ENABLED", false)).thenReturn(true);

        mockServer.expect(requestTo(BASE_URL)).andRespond(withUnauthorizedRequest());

        EmailSendResult result = service.send("kh@example.com", "Subject", "<p>Hi</p>");

        assertThat(result).isEqualTo(EmailSendResult.FAILED);
        mockServer.verify();
    }

    @Test
    void send_khi429RateLimit_traVeFailed() {
        when(settingService.getBoolean("CARD_EMAIL_ENABLED", false)).thenReturn(true);

        mockServer.expect(requestTo(BASE_URL))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));

        EmailSendResult result = service.send("kh@example.com", "Subject", "<p>Hi</p>");

        assertThat(result).isEqualTo(EmailSendResult.FAILED);
        mockServer.verify();
    }
}
