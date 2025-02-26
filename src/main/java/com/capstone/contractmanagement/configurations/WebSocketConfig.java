package com.capstone.contractmanagement.configurations;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {


    //đăng ký một websocket endpoint mà các máy khách sẽ sử dụng để kết nối với máy chủ websocket
    //SockJS được sử dụng để bật tùy chọn dự phòng cho các trình duyệt không hỗ trợ websocket
    //STOMP là viết tắt của Simple Text Oriented Messaging Protocol.
    // Nó là một giao thức nhắn tin xác định định dạng và quy tắc trao đổi dữ liệu.
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws").withSockJS();
    }

    //tin nhắn sẽ được sử dụng để định tuyến thư từ một khách hàng này đến ứng dụng khách khác.
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        //thư có đích bắt đầu bằng “/app” sẽ được định tuyến đến các phương thức xử lý tin nhắn
        registry.setApplicationDestinationPrefixes("/app");

        //các thông điệp có đích bắt đầu bằng “/topic” được định tuyến tới nhà môi giới tin nhắn.
        //Nhà môi giới tin nhắn sẽ phát các tin nhắn đến tất cả các khách hàng được kết nối đã đăng ký một chủ đề cụ thể.
        registry.enableSimpleBroker("/topic");
    }
}
