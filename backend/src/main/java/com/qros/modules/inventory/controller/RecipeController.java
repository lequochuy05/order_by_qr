package com.qros.modules.inventory.controller;

import com.qros.modules.inventory.dto.request.RecipeUpdateRequest;
import com.qros.modules.inventory.dto.response.RecipeItemResponse;
import com.qros.modules.inventory.service.RecipeService;
import com.qros.shared.constants.ApiRoutes;
import com.qros.shared.response.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping(ApiRoutes.INVENTORY_RECIPES)
@RequiredArgsConstructor
public class RecipeController {

    private final RecipeService recipeService;

    @GetMapping("/{menuItemId}")
    public ApiResponse<List<RecipeItemResponse>> getRecipe(
            @PathVariable @Min(value = 1, message = "Menu item id must be positive") Long menuItemId) {
        return ApiResponse.success(recipeService.getRecipeByMenuItemId(menuItemId));
    }

    @PutMapping("/{menuItemId}")
    public ApiResponse<List<RecipeItemResponse>> updateRecipe(
            @PathVariable @Min(value = 1, message = "Menu item id must be positive") Long menuItemId,
            @Valid @RequestBody RecipeUpdateRequest request) {
        return ApiResponse.success("Recipe updated successfully", recipeService.updateRecipe(menuItemId, request));
    }

    @DeleteMapping("/{menuItemId}")
    public ApiResponse<Void> deleteRecipe(
            @PathVariable @Min(value = 1, message = "Menu item id must be positive") Long menuItemId) {
        recipeService.deleteRecipe(menuItemId);

        return ApiResponse.success("Recipe deleted successfully", null);
    }
}
