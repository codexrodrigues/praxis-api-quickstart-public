package com.example.praxis.apiquickstart.core.service.base;

import org.praxisplatform.uischema.filter.dto.GenericFilterDTO;
import org.praxisplatform.uischema.dto.CursorPage;
import org.praxisplatform.uischema.mapper.base.ResourceMapper;
import org.praxisplatform.uischema.repository.base.BaseCrudRepository;
import org.praxisplatform.uischema.service.base.AbstractReadOnlyResourceService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import java.util.Collection;
import java.util.Optional;
import java.util.function.Function;

/**
 * Base de service para recursos read-only do quickstart.
 *
 * <p>Ela existe para mostrar que recursos consultivos continuam usando o mesmo pipeline de filtro,
 * cursor, schema e hypermedia do restante da plataforma, mas sem capacidade de create/update. Em
 * vez de esconder isso em herancas menos claras, o quickstart deixa a restricao explicitamente
 * documentada aqui.</p>
 */
public abstract class AbstractQuickstartReadOnlyService<E, ResponseDTO, ID, FD extends GenericFilterDTO>
        extends AbstractReadOnlyResourceService<E, ResponseDTO, ID, FD> {

    private final ResourceMapper<E, ResponseDTO, Void, Void, ID> resourceMapper;

    protected AbstractQuickstartReadOnlyService(
            BaseCrudRepository<E, ID> repository,
            Class<E> entityClass,
            Function<E, ResponseDTO> toResponse,
            Function<E, ID> extractId
    ) {
        super(repository, entityClass);
        this.resourceMapper = new ResourceMapper<>() {
            @Override
            public ResponseDTO toResponse(E entity) {
                return toResponse.apply(entity);
            }

            @Override
            public E newEntity(Void dto) {
                throw new UnsupportedOperationException("Read-only resource does not support create");
            }

            @Override
            public void applyUpdate(E entity, Void dto) {
                throw new UnsupportedOperationException("Read-only resource does not support update");
            }

            @Override
            public ID extractId(E entity) {
                return extractId.apply(entity);
            }
        };
    }

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
    protected ResourceMapper<E, ResponseDTO, ?, ?, ID> getResourceMapper() {
        return resourceMapper;
    }
}
