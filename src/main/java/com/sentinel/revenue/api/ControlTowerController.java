package com.sentinel.revenue.api;

import com.sentinel.revenue.controltower.ControlTowerService;
import com.sentinel.revenue.controltower.ControlTowerView;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/revenue/control-tower")
public class ControlTowerController {
    private final ControlTowerService controlTower;

    public ControlTowerController(ControlTowerService controlTower) {
        this.controlTower = controlTower;
    }

    @GetMapping
    public ResponseEntity<ControlTowerView> view() {
        return ResponseEntity.ok(controlTower.view());
    }
}
