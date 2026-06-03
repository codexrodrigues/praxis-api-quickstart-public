package com.example.praxis.apiquickstart.core.service.base;

import jakarta.persistence.Id;
import org.junit.jupiter.api.Test;
import org.praxisplatform.uischema.filter.dto.GenericFilterDTO;
import org.praxisplatform.uischema.mapper.base.ResourceMapper;
import org.praxisplatform.uischema.repository.base.BaseCrudRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

class QuickstartResourceMapperFoundationTest {

    @Test
    void shouldBridgeSeparatedCrudMapperMethods() {
        TestCrudService service = new TestCrudService();
        ResourceMapper<TestEntity, TestResponseDto, TestCreateDto, TestUpdateDto, Integer> mapper = service.resourceMapper();

        TestEntity entity = new TestEntity();
        entity.setId(7);
        entity.setNome("Alice");

        assertEquals(7, mapper.extractId(entity));
        assertEquals("Alice", mapper.toResponse(entity).getNome());

        TestEntity created = mapper.newEntity(new TestCreateDto("Bruno"));
        assertEquals("Bruno", created.getNome());

        mapper.applyUpdate(entity, new TestUpdateDto("Carla"));
        assertEquals("Carla", entity.getNome());
    }

    @Test
    void shouldRejectCreateAndUpdateForReadOnlyResources() {
        TestReadOnlyService service = new TestReadOnlyService();
        ResourceMapper<TestEntity, TestResponseDto, ?, ?, Integer> mapper = service.resourceMapper();

        TestEntity entity = new TestEntity();
        entity.setId(11);
        entity.setNome("Diana");

        assertEquals(11, mapper.extractId(entity));
        assertEquals("Diana", mapper.toResponse(entity).getNome());
        assertThrows(UnsupportedOperationException.class, () -> mapper.newEntity(null));
        assertThrows(UnsupportedOperationException.class, () -> mapper.applyUpdate(entity, null));
    }

    static class TestCrudService extends AbstractQuickstartCrudService<TestEntity, TestResponseDto, Integer, TestFilterDto, TestCreateDto, TestUpdateDto> {

        TestCrudService() {
            super(
                    mock(BaseCrudRepository.class),
                    TestEntity.class,
                    entity -> new TestResponseDto(entity.getId(), entity.getNome()),
                    create -> {
                        TestEntity entity = new TestEntity();
                        entity.setNome(create.getNome());
                        return entity;
                    },
                    update -> {
                        TestEntity entity = new TestEntity();
                        entity.setNome(update.getNome());
                        return entity;
                    },
                    TestEntity::getId
            );
        }

        @Override
        public TestEntity mergeUpdate(TestEntity existing, TestEntity fromPayload) {
            existing.setNome(fromPayload.getNome());
            return existing;
        }

        ResourceMapper<TestEntity, TestResponseDto, TestCreateDto, TestUpdateDto, Integer> resourceMapper() {
            return getResourceMapper();
        }
    }

    static class TestReadOnlyService extends AbstractQuickstartReadOnlyService<TestEntity, TestResponseDto, Integer, TestFilterDto> {

        TestReadOnlyService() {
            super(
                    mock(BaseCrudRepository.class),
                    TestEntity.class,
                    entity -> new TestResponseDto(entity.getId(), entity.getNome()),
                    TestEntity::getId
            );
        }

        ResourceMapper<TestEntity, TestResponseDto, ?, ?, Integer> resourceMapper() {
            return getResourceMapper();
        }
    }

    static class TestFilterDto implements GenericFilterDTO {
    }

    static class TestEntity {
        @Id
        private Integer id;
        private String nome;

        Integer getId() {
            return id;
        }

        void setId(Integer id) {
            this.id = id;
        }

        String getNome() {
            return nome;
        }

        void setNome(String nome) {
            this.nome = nome;
        }
    }

    static class TestResponseDto {
        private final Integer id;
        private final String nome;

        TestResponseDto(Integer id, String nome) {
            this.id = id;
            this.nome = nome;
        }

        Integer getId() {
            return id;
        }

        String getNome() {
            return nome;
        }
    }

    static class TestCreateDto {
        private final String nome;

        TestCreateDto(String nome) {
            this.nome = nome;
        }

        String getNome() {
            return nome;
        }
    }

    static class TestUpdateDto {
        private final String nome;

        TestUpdateDto(String nome) {
            this.nome = nome;
        }

        String getNome() {
            return nome;
        }
    }
}
