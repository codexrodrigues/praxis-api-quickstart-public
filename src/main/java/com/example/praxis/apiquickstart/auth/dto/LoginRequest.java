package com.example.praxis.apiquickstart.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
        name = "LoginRequest",
        description = "Credenciais usadas para abrir uma sessao administrativa no host de referencia. "
                + "Quando aceitas, o servidor emite a sessao em cookie HttpOnly; o segredo nunca e devolvido em respostas.")
public record LoginRequest(
        @Schema(
                description = "Identificador configurado para autenticar a sessao local do quickstart, normalmente o usuario administrativo do host.",
                example = "admin")
        String username,
        @Schema(
                description = "Segredo correspondente ao identificador informado. Campo exclusivo da requisicao de login e nunca publicado em payloads de resposta.",
                accessMode = Schema.AccessMode.WRITE_ONLY)
        String password) {}
