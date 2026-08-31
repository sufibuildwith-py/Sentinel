package com.sentinel.revenue.api;
import com.sentinel.revenue.learning.ModelRegistryService;
import com.sentinel.revenue.model.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController @RequestMapping("/api/v1/revenue/models")
public class ModelRegistryController {
    private final ModelRegistryService registry;
    public ModelRegistryController(ModelRegistryService registry){this.registry=registry;}
    @GetMapping public ResponseEntity<List<RegisteredModel>> models(){return ResponseEntity.ok(registry.all());}
    @PostMapping public ResponseEntity<RegisteredModel> register(@RequestBody RegisterRequest r){return ResponseEntity.ok(registry.register(r.name(),r.version(),r.featureSchemaVersion()));}
    @PostMapping("/{id}/promote") public ResponseEntity<RegisteredModel> promote(@PathVariable UUID id,@RequestBody PromotionRequest r){return ResponseEntity.ok(registry.promote(id,r.target(),r.actor(),r.reason()));}
    public record RegisterRequest(String name,String version,String featureSchemaVersion){}
    public record PromotionRequest(ModelLifecycle target,String actor,String reason){}
}
