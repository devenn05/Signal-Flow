package com.sw.signalFlowBackend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "coins")
@Entity
public class CoinEntity{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long coinId;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false, unique = true)
    private String symbol;

}
