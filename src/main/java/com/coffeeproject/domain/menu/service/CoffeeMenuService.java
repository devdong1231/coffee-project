package com.coffeeproject.domain.menu.service;

import com.coffeeproject.domain.menu.dto.CoffeeMenuResponse;
import com.coffeeproject.domain.menu.dto.PopularCoffeeMenuResponse;
import com.coffeeproject.domain.menu.entity.CoffeeMenu;
import com.coffeeproject.domain.menu.entity.MenuStatus;
import com.coffeeproject.domain.menu.repository.CoffeeMenuRepository;
import com.coffeeproject.domain.menu.service.PopularMenuRankingService.PopularMenuRank;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CoffeeMenuService {

    private final CoffeeMenuRepository coffeeMenuRepository;
    private final PopularMenuRankingService popularMenuRankingService;

    @Transactional(readOnly = true)
    public List<CoffeeMenuResponse> getMenus() {
        return CoffeeMenuResponse.fromList(
                coffeeMenuRepository.findAllByStatusOrderByIdAsc(MenuStatus.ACTIVE)
        );
    }

    @Transactional(readOnly = true)
    public List<PopularCoffeeMenuResponse> getPopularMenus() {
        List<PopularMenuRank> popularRanks = popularMenuRankingService.getPopularRanks();
        List<Long> menuIds = popularRanks.stream()
                .map(PopularMenuRank::menuId)
                .toList();

        Map<Long, PopularMenuRank> rankMap = popularRanks.stream()
                .collect(Collectors.toMap(
                        PopularMenuRank::menuId,
                        Function.identity(),
                        (first, second) -> first,
                        LinkedHashMap::new
                ));

        Map<Long, CoffeeMenu> menuMap = coffeeMenuRepository.findAllById(menuIds)
                .stream()
                .collect(Collectors.toMap(CoffeeMenu::getId, Function.identity()));

        return rankMap.values()
                .stream()
                .filter(rank -> menuMap.containsKey(rank.menuId()))
                .map(rank -> PopularCoffeeMenuResponse.of(menuMap.get(rank.menuId()), rank.orderCount()))
                .toList();
    }
}
