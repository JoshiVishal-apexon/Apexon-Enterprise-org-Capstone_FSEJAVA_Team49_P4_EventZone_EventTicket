package com.eventzone.controller;

import com.eventzone.dto.event.EventCategoryRequest;
import com.eventzone.dto.event.EventCategoryResponse;
import com.eventzone.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * GET is public because the organiser's event form needs the category list;
 * every write is ADMIN-only, per the spec's admin category management.
 */
@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    public List<EventCategoryResponse> list() {
        return categoryService.list();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public EventCategoryResponse create(@Valid @RequestBody EventCategoryRequest request) {
        return categoryService.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public EventCategoryResponse rename(@PathVariable UUID id,
                                         @Valid @RequestBody EventCategoryRequest request) {
        return categoryService.rename(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        categoryService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
