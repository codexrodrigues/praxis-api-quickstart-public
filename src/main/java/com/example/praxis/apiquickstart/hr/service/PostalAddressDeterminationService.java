package com.example.praxis.apiquickstart.hr.service;

import com.example.praxis.apiquickstart.hr.dto.EnderecoDTO;
import com.example.praxis.apiquickstart.hr.dto.determination.PostalAddressDeterminationResponse;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

/**
 * Fonte de fatos deterministica do piloto postal.
 *
 * <p>O diretorio e deliberadamente pequeno e ficticio: ele prova a fronteira de execucao do host
 * sem introduzir dependencia de rede ou transformar o Quickstart em dono de um cadastro postal
 * corporativo. Aplicacoes reais substituem esta fonte por um provider governado do seu dominio.</p>
 */
@Service
public class PostalAddressDeterminationService {

    public static final String DECISION_VERSION = "quickstart-postal-directory-v1";

    private static final Map<String, PostalAddressDeterminationResponse> DIRECTORY = directory();

    public PostalAddressDeterminationResponse determine(String cep) {
        String normalizedCep = normalizeCep(cep);
        return Optional.ofNullable(DIRECTORY.get(normalizedCep))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNPROCESSABLE_ENTITY,
                        "CEP is not covered by the Quickstart postal determination directory."));
    }

    /** Reexecuta a mesma autoridade deterministica no comando final. */
    public void validateFinalAddress(EnderecoDTO address) {
        if (address == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Address is required.");
        }
        PostalAddressDeterminationResponse expected = determine(address.getCep());
        if (!same(expected.logradouro(), address.getLogradouro())
                || !same(expected.bairro(), address.getBairro())
                || !same(expected.cidade(), address.getCidade())
                || !same(expected.estado(), address.getEstado())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Address fields do not match the current governed postal determination.");
        }
    }

    private static boolean same(String expected, String actual) {
        return expected.equalsIgnoreCase(StringUtils.trimWhitespace(actual == null ? "" : actual));
    }

    private static String normalizeCep(String value) {
        if (!StringUtils.hasText(value)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "CEP is required.");
        }
        String digits = value.replaceAll("\\D", "");
        if (digits.length() != 8) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "CEP must contain eight digits.");
        }
        return digits;
    }

    private static Map<String, PostalAddressDeterminationResponse> directory() {
        Map<String, PostalAddressDeterminationResponse> values = new LinkedHashMap<>();
        values.put("01310100", response("Avenida Paulista", "Bela Vista", "Sao Paulo", "SP"));
        values.put("20040002", response("Rua da Assembleia", "Centro", "Rio de Janeiro", "RJ"));
        values.put("30140071", response("Avenida Afonso Pena", "Funcionarios", "Belo Horizonte", "MG"));
        return Map.copyOf(values);
    }

    private static PostalAddressDeterminationResponse response(
            String logradouro,
            String bairro,
            String cidade,
            String estado
    ) {
        return new PostalAddressDeterminationResponse(
                logradouro,
                bairro,
                cidade,
                estado.toUpperCase(Locale.ROOT),
                DECISION_VERSION);
    }
}
