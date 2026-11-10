package com.manuelalejandro.delfos.api

import com.manuelalejandro.delfos.api.generated.HealthApi
import com.manuelalejandro.delfos.api.generated.model.Health
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController

@RestController
class HealthController : HealthApi {

    override fun getHealth(): ResponseEntity<Health> = ResponseEntity.ok(Health(Health.Status.UP))
}
