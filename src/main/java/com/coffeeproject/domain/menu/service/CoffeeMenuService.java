package com.coffeeproject.domain.menu.service;

import com.coffeeproject.domain.menu.dto.CoffeeMenuResponse;
import com.coffeeproject.domain.menu.entity.MenuStatus;
import com.coffeeproject.domain.menu.repository.CoffeeMenuRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CoffeeMenuService {

    private final CoffeeMenuRepository coffeeMenuRepository;

    @Transactional(readOnly = true)
    public List<CoffeeMenuResponse> getMenus() {
        return CoffeeMenuResponse.fromList(
                coffeeMenuRepository.findAllByStatusOrderByIdAsc(MenuStatus.ACTIVE)
        );
    }
}
