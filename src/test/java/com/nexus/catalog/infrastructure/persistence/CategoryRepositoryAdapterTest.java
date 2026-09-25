package com.nexus.catalog.infrastructure.persistence;

import com.nexus.catalog.domain.model.Category;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(CategoryRepositoryAdapter.class)
class CategoryRepositoryAdapterTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("catalog_db").withUsername("nexus").withPassword("nexus");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private CategoryRepositoryAdapter adapter;

    @Test
    void saveThenFindById_roundTripsTheCategory() {
        Category root = Category.create("Electronics", null);
        adapter.save(root);

        Optional<Category> found = adapter.findById(root.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Electronics");
        assertThat(found.get().getParentId()).isNull();
    }

    @Test
    void findByParentId_returnsOnlyDirectChildren() {
        Category root = Category.create("Electronics", null);
        adapter.save(root);
        Category child = Category.create("Laptops", root.getId());
        adapter.save(child);
        Category grandchild = Category.create("Gaming Laptops", child.getId());
        adapter.save(grandchild);

        List<Category> children = adapter.findByParentId(root.getId());

        assertThat(children).hasSize(1);
        assertThat(children.get(0).getName()).isEqualTo("Laptops");
    }

    @Test
    void findRoots_returnsOnlyTopLevelCategories() {
        Category root = Category.create("Electronics", null);
        adapter.save(root);
        Category child = Category.create("Laptops", root.getId());
        adapter.save(child);

        List<Category> roots = adapter.findRoots();

        assertThat(roots).extracting(Category::getName).containsExactly("Electronics");
    }

    // A malformed id is simply an id that matches nothing: it must map to "not found" (-> 404 via
    // CategoryNotFoundException), not leak an IllegalArgumentException from UUID.fromString (-> 500).
    @Test
    void findById_returnsEmptyForMalformedId() {
        assertThat(adapter.findById("not-a-uuid")).isEmpty();
    }

    @Test
    void findByParentId_returnsEmptyForMalformedId() {
        assertThat(adapter.findByParentId("not-a-uuid")).isEmpty();
    }
}