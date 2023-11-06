package simple.cafekiosk.spring.api.service.order;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import simple.cafekiosk.spring.api.service.mail.MailService;
import simple.cafekiosk.spring.domain.order.Order;
import simple.cafekiosk.spring.domain.order.OrderRepository;
import simple.cafekiosk.spring.domain.order.OrderStatus;

import java.time.LocalDate;
import java.util.List;

@RequiredArgsConstructor
@Service
public class OrderStatisticsService {

    private final OrderRepository orderRepository;
    private final MailService mailService;

    public boolean sendOrderStatisticsMail(LocalDate orderDate, String email) {
        // 해당 일자에 결제 완료된 주문들을 가져와서
        List<Order> orders = orderRepository.findOrdersBy(
                orderDate.atStartOfDay(),
                orderDate.plusDays(1).atStartOfDay(),
                OrderStatus.PAYMENT_COMPLETED
        );

        // 총 매출 합계를 계산하고
        int totalAmount = orders.stream()
                .mapToInt(Order::getTotalPrice)
                .sum();

        // 메일 전송
        boolean result = mailService.sendMail(
                "no-reply@cafekiosk.com",
                email,
                "[매출통계] " + orderDate,
                "총 매출 합계는 " + totalAmount + "원 입니다.");

        if (!result) {
            throw new IllegalArgumentException("매출 통계 메일 전송에 실패했습니다.");
        }

        return true;
    }
}
