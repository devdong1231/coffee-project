package com.coffeeproject.domain.menu.repository;

import com.coffeeproject.domain.menu.entity.CoffeeMenu;
import com.coffeeproject.domain.menu.entity.MenuStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CoffeeMenuRepository extends JpaRepository<CoffeeMenu, Long> {

    List<CoffeeMenu> findAllByStatusOrderByIdAsc(MenuStatus status);
}
