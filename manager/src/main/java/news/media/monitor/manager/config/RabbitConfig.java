package news.media.monitor.manager.config;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    private static final String PROP_RABBITMQ_URL                    = "${rabbitmq.url}";
    private static final String PROP_ARTICLE_NOTIFICATIONS_QUEUE     = "${rabbitmq.article-notifications-queue}";

    private final String rabbitmqUrl;
    private final String articleNotificationsQueueName;

    public RabbitConfig(@Value(PROP_RABBITMQ_URL) String rabbitmqUrl,
                        @Value(PROP_ARTICLE_NOTIFICATIONS_QUEUE) String articleNotificationsQueueName) {
        this.rabbitmqUrl                   = rabbitmqUrl;
        this.articleNotificationsQueueName = articleNotificationsQueueName;
    }

    @Bean
    public ConnectionFactory connectionFactory() {
        var factory = new CachingConnectionFactory();
        factory.setUri(rabbitmqUrl);
        return factory;
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter jsonMessageConverter) {
        var template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter);
        return template;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory, MessageConverter jsonMessageConverter) {
        var factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter);
        return factory;
    }

    @Bean
    public Queue articleNotificationsQueue() {
        return new Queue(articleNotificationsQueueName, true);
    }
}
