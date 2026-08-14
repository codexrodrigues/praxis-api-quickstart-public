package com.example.praxis.apiquickstart.hr.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.praxis.apiquickstart.hr.dto.CreateEnderecoDTO;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class PostalAddressDeterminationServiceTest {

    private final PostalAddressDeterminationService service = new PostalAddressDeterminationService();

    @Test
    void determinesTheSameAddressForFormattedAndUnformattedCep() {
        assertThat(service.determine("01310-100"))
                .isEqualTo(service.determine("01310100"));
        assertThat(service.determine("01310-100").decisionVersion())
                .isEqualTo(PostalAddressDeterminationService.DECISION_VERSION);
    }

    @Test
    void rejectsUnknownCepWithoutInventingAFallback() {
        assertThatThrownBy(() -> service.determine("99999-999"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("not covered");
    }

    @Test
    void commandValidationRejectsManipulatedDerivedFieldsForKnownCep() {
        CreateEnderecoDTO address = address("01310-100", "Rua adulterada", "Bela Vista", "Sao Paulo", "SP");

        assertThatThrownBy(() -> service.validateFinalAddress(address))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("do not match");
    }

    @Test
    void commandValidationRejectsUnknownCepInsteadOfBypassingTheAuthority() {
        CreateEnderecoDTO address = address("99999-999", "Rua inventada", "Centro", "Sao Paulo", "SP");

        assertThatThrownBy(() -> service.validateFinalAddress(address))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("not covered");
    }

    @Test
    void commandValidationAcceptsTheCurrentDetermination() {
        CreateEnderecoDTO address = address("01310-100", "Avenida Paulista", "Bela Vista", "Sao Paulo", "SP");

        service.validateFinalAddress(address);
    }

    private static CreateEnderecoDTO address(
            String cep,
            String logradouro,
            String bairro,
            String cidade,
            String estado
    ) {
        CreateEnderecoDTO dto = new CreateEnderecoDTO();
        dto.setCep(cep);
        dto.setLogradouro(logradouro);
        dto.setBairro(bairro);
        dto.setCidade(cidade);
        dto.setEstado(estado);
        return dto;
    }
}
