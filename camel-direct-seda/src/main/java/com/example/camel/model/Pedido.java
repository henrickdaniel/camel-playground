package com.example.camel.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.List;

@Data
@AllArgsConstructor
public class Pedido {
    private String pedidoId;
    private List<Produto> produtos;
}