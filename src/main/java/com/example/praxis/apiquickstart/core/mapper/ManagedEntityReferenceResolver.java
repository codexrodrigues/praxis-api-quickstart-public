package com.example.praxis.apiquickstart.core.mapper;

import com.example.praxis.apiquickstart.hr.entity.Funcionario;
import jakarta.persistence.EntityManager;
import org.mapstruct.Named;
import org.springframework.stereotype.Component;

/**
 * Resolves relationship identifiers to references owned by the active JPA context.
 *
 * <p>Creating an entity stub with an assigned id is not equivalent to a managed reference,
 * especially when the target uses {@code @Version}. Mappers delegate here so Hibernate can
 * preserve relationship identity without treating the referenced row as a new or detached
 * aggregate.</p>
 */
@Component
public class ManagedEntityReferenceResolver {

    private final EntityManager entityManager;

    public ManagedEntityReferenceResolver(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Named("funcionarioReference")
    public Funcionario funcionarioReference(Integer id) {
        return id == null ? null : entityManager.getReference(Funcionario.class, id);
    }
}
