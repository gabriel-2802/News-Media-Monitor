package rssfetcher.demo.entities;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "topics")
@Data
public class Topic {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "topic_id")
    private Long id;
    @Column(unique = true, nullable = false)
    private String name;
}
