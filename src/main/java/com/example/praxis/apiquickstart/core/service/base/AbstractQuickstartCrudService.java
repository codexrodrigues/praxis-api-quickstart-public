package com.example.praxis.apiquickstart.core.service.base;

import org.praxisplatform.uischema.filter.dto.GenericFilterDTO;
import org.praxisplatform.uischema.dto.CursorPage;
import org.praxisplatform.uischema.mapper.base.ResourceMapper;
import org.praxisplatform.uischema.repository.base.BaseCrudRepository;
import org.praxisplatform.uischema.service.base.AbstractBaseResourceService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import java.util.Collection;
import java.util.Optional;
import java.util.function.Function;

/**
 * Base de service mutavel usada pelos recursos canonicos do quickstart.
 *
 * <p>Ela adapta o modelo mais generico do {@code praxis-metadata-starter} a um formato simples e
 * repetivel para o projeto de exemplo: repository JPA + mapper funcional + merge de update. Isso
 * facilita a leitura do monorepo e ajuda quem estuda o quickstart a entender qual parte e regra da
 * plataforma e qual parte e implementacao do dominio.</p>
 */
public abstract class AbstractQuickstartCrudService<E, ResponseDTO, ID, FD extends GenericFilterDTO, CreateDTO, UpdateDTO>
        extends AbstractBaseResourceService<E, ResponseDTO, ID, FD, CreateDTO, UpdateDTO> {

    private final ResourceMapper<E, ResponseDTO, CreateDTO, UpdateDTO, ID> resourceMapper;

    protected AbstractQuickstartCrudService(
            BaseCrudRepository<E, ID> repository,
            Class<E> entityClass,
            Function<E, ResponseDTO> toResponse,
            Function<CreateDTO, E> newEntity,
            Function<UpdateDTO, E> toUpdateEntity,
            Function<E, ID> extractId
    ) {
        super(repository, entityClass);
        this.resourceMapper = new ResourceMapper<>() {
            @Override
            public ResponseDTO toResponse(E entity) {
                return toResponse.apply(entity);
            }

            @Override
            public E newEntity(CreateDTO dto) {
                return newEntity.apply(dto);
            }

            @Override
            public void applyUpdate(E entity, UpdateDTO dto) {
                E fromPayload = toUpdateEntity.apply(dto);
                mergeUpdate(entity, fromPayload);
            }

            @Override
            public ID extractId(E entity) {
                return extractId.apply(entity);
            }
        };
    }

    /**
     * Mescla no agregado existente apenas os campos aceitos pela semantica de update do recurso.
     *
     * <p>O quickstart mantem esse passo explicito para fins pedagogicos: o payload de update nao e
     * automaticamente confiado a JPA nem a reflexao generica.</p>
     */
    public abstract E mergeUpdate(E existing, E fromPayload);

    @Override
    public Optional<String> getDatasetVersion() {
        long count = getRepository().count();
        return Optional.of(getEntityClass().getSimpleName() + ":" + count);
    }

    public <R> Page<R> filterMappedWithIncludeIds(
            FD filter,
            Pageable pageable,
            Collection<ID> includeIds,
            Function<ResponseDTO, R> mapper
    ) {
        return filter(filter, pageable, includeIds).map(mapper);
    }

    public <R> CursorPage<R> filterByCursorMapped(
            FD filter,
            Sort sort,
            String after,
            String before,
            int size,
            Function<ResponseDTO, R> mapper
    ) {
        // Mantem a semantica de cursor do starter e transforma apenas o payload final.
        CursorPage<ResponseDTO> page = filterByCursor(filter, sort, after, before, size);
        return new CursorPage<>(
                page.content().stream().map(mapper).toList(),
                page.next(),
                page.prev(),
                page.size()
        );
    }

    @Override
    protected ResourceMapper<E, ResponseDTO, CreateDTO, UpdateDTO, ID> getResourceMapper() {
        return resourceMapper;
    }
}
