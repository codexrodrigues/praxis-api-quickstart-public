package com.example.praxis.apiquickstart.operationalassets.service;

import com.example.praxis.apiquickstart.operationalassets.dto.VeiculoDTO;
import com.example.praxis.apiquickstart.operationalassets.dto.CreateVeiculoDTO;
import com.example.praxis.apiquickstart.operationalassets.dto.UpdateVeiculoDTO;
import com.example.praxis.apiquickstart.operationalassets.dto.filter.VeiculoFilterDTO;
import com.example.praxis.apiquickstart.operationalassets.entity.Veiculo;
import com.example.praxis.apiquickstart.operationalassets.mapper.VeiculoMapper;
import com.example.praxis.apiquickstart.operationalassets.repository.VeiculoRepository;
import com.example.praxis.apiquickstart.core.service.base.AbstractQuickstartCrudService;
import org.springframework.stereotype.Service;

/**
 * Service basico de veiculos usado para mostrar o caminho mais simples de um ativo CRUD.
 *
 * <p>Neste caso, o valor pedagogico esta justamente na simplicidade: quando nao ha workflow ou
 * surface especial, o quickstart ainda deixa explicito o merge controlado do agregado e o encaixe
 * no pipeline canonico da plataforma.</p>
 */
@Service
public class VeiculoService extends AbstractQuickstartCrudService<Veiculo, VeiculoDTO, Integer, VeiculoFilterDTO, CreateVeiculoDTO, UpdateVeiculoDTO> {

    private final VeiculoMapper mapper;

    public VeiculoService(VeiculoRepository repository, VeiculoMapper mapper) {
        super(repository, Veiculo.class, mapper::toDto, mapper::toEntity, mapper::toEntity, Veiculo::getId);
        this.mapper = mapper;
    }

    @Override
    public Veiculo mergeUpdate(Veiculo existing, Veiculo fromPayload) {
        mapper.updateEntity(fromPayload, existing);
        return existing;
    }
}







