package com.example.praxis.apiquickstart.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
        name = "LoginRequest",
        description = "Corpo de login (demo). A password nunca e devolvida em respostas; use apenas no POST de autenticacao.")
public record LoginRequest(
        @Schema(
                description = "Identificador de sessao (username ou atributo de login suportado pelo servico de auth).",
                example = "heroi.demo")
        String username,
        @Schema(description = "Segredo de autenticacao. Campo de escrita unica; nao aparece no OpenAPI de respostas.")
        String password) {}

