package com.lexho.catalog.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class CatalogEntityTest {

    @Test
    void shouldSetAndGetCategoryFields() {
        ServiceCategory category = new ServiceCategory();
        category.setId(11L);
        category.setName("Cleaning");
        category.setIcon("icon-url");

        assertEquals(11L, category.getId());
        assertEquals("Cleaning", category.getName());
        assertEquals("icon-url", category.getIcon());
    }

    @Test
    void shouldLinkSubCategoryWithParentCategory() {
        ServiceCategory category = new ServiceCategory();
        category.setId(7L);
        category.setName("Home Services");

        SubCategory subCategory = new SubCategory();
        subCategory.setId(70L);
        subCategory.setName("Bathroom Cleaning");
        subCategory.setCategory(category);

        assertEquals(70L, subCategory.getId());
        assertEquals("Bathroom Cleaning", subCategory.getName());
        assertSame(category, subCategory.getCategory());
    }
}
