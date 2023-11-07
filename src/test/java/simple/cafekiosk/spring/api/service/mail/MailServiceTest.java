package simple.cafekiosk.spring.api.service.mail;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import simple.cafekiosk.spring.client.MailSendClient;
import simple.cafekiosk.spring.domain.history.MailSendHistory;
import simple.cafekiosk.spring.domain.history.MailSendHistoryRepository;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MailServiceTest {

    // @Mock, @InjectMocks
    // @Mock 의 Mock 객체를 생성해주고 @InjectMocks 의 해당하는 Mock객체를 주입해 준다.
    // @Mock
    @Spy
    private MailSendClient mailSendClient;

    @Mock
    private MailSendHistoryRepository mailSendHistoryRepository;

    @InjectMocks
    private MailService mailService;

    @DisplayName("메일 전송 테스트")
    @Test
    void sendMail() {
        // given
        // MailSendClient mailSendClient = mock(MailSendClient.class);
        // MailSendHistoryRepository mailSendHistoryRepository = mock(MailSendHistoryRepository.class);

        // MailService mailService = new MailService(mailSendClient, mailSendHistoryRepository);

        // Mock -> stubbing
//        when(mailSendClient.sendMail(anyString(), anyString(), anyString(), anyString()))
//                .thenReturn(true);

        // Spy - 실제 객체를 활용. 일부는 실제 기능을 이용, 일부만 stubbing 하고 싶을 때 사용
        doReturn(true)
                .when(mailSendClient)
                .sendMail(anyString(), anyString(), anyString(), anyString());

        // when
        boolean result = mailService.sendMail("", "", "", "");

        // then
        assertThat(result).isTrue();
        verify(mailSendHistoryRepository, times(1)).save(any(MailSendHistory.class));
    }

}