package com.sw.signalFlowBackend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@Entity
@Table(name = "portfolio")
@AllArgsConstructor
@NoArgsConstructor
public class PortfolioEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long portfolioId;

    private List<CoinEntity> coinList;


}
