package monoliths;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import monoliths.commons.model.DeliveryLocation;
import monoliths.commons.model.DeliveryMethod;
import monoliths.commons.model.OrderSheet;
import monoliths.commons.model.OrderSheet.OrderSheetItem;
import monoliths.orders.OrderContextConfiguration;
import monoliths.orders.domain.entity.Order;
import monoliths.orders.domain.entity.OrderStatus;
import monoliths.orders.domain.usecase.OrderProcessing;
import monoliths.orders.domain.usecase.Orders;
import monoliths.shipments.ShipmentContextConfiguration;
import monoliths.shipments.domain.entity.Delivery;
import monoliths.shipments.domain.entity.DeliveryStatus;
import monoliths.shipments.domain.usecase.Deliveries;

@SpringBootTest(classes = {
        CustomerBuyingFlowIntegrationTests.CustomerBuyingFlowIntegrationTestConfiguration.class
})
class CustomerBuyingFlowIntegrationTests {

    @Autowired Orders orders;
    @Autowired OrderProcessing orderProcessing;
    @Autowired Deliveries deliveries;
    @Autowired RestTemplate restTemplate;

    MockRestServiceServer mockCatalogService;

    @BeforeEach
    void init() {
        mockCatalogService = MockRestServiceServer.createServer(restTemplate);
    }

    @Test
    void simple() {
        UUID productId = UUID.randomUUID();

        OrderSheet orderSheet = OrderSheet.builder()
                .customerId(UUID.randomUUID())
                .items(Arrays.asList(OrderSheetItem.builder().productId(productId).quantity(2).build()))
                .deliveryMethod(DeliveryMethod.INSTANTLY)
                .deliveryLocation(DeliveryLocation.builder()
                        .postCode("63364")
                        .baseAddress("제주 제주시 구좌읍 종달논길 32")
                        .detailAddress("종달어촌계")
                        .roadNameAddressType(true).build())
                .deliveryNote("부재시 현관문 앞에 놔주세요.")
                .build();

        UUID orderId = orderProcessing.placeOrder(orderSheet);
        orderProcessing.payOrder(orderId);

        Order order = orders.getOrder(orderId);
        Delivery delivery = deliveries.getDelivery(orderId);

        assertNotNull(order);
        assertEquals(order.getOrderProducts().size(), 1);
        assertEquals(order.getOrderProducts().get(0).getProductItems().size(), 4);
        assertEquals(order.calculateTotalPrice(), BigDecimal.valueOf(5400 * 2));
        assertEquals(order.getOrderStatus(), OrderStatus.PAYED);

        assertNotNull(delivery);
        assertEquals(delivery.getProducts().size(), order.getOrderProducts().size());
        assertEquals(delivery.getProducts().get(0).getProductItems().size(), order.getOrderProducts().get(0).getProductItems().size());
        assertEquals(delivery.getStatus(), DeliveryStatus.DISPATCHED);
    }

    @Configuration
    @Import({ OrderContextConfiguration.class, ShipmentContextConfiguration.class })
    static class CustomerBuyingFlowIntegrationTestConfiguration {

        @Bean
        public RestTemplate restTemplate() {
            return new RestTemplateBuilder().build();
        }

    }

}
