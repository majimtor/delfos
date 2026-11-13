package com.manuelalejandro.delfos.accounts

import com.manuelalejandro.delfos.api.generated.AccountsApi
import com.manuelalejandro.delfos.api.generated.model.Account
import com.manuelalejandro.delfos.api.generated.model.AccountPage
import com.manuelalejandro.delfos.api.generated.model.AccountStatus
import com.manuelalejandro.delfos.api.generated.model.CreateAccountRequest
import com.manuelalejandro.delfos.auth.CurrentSession
import com.manuelalejandro.delfos.persistence.AccountEntity
import com.manuelalejandro.delfos.survey.SurveyMapping.utc
import java.net.URI
import java.util.UUID
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController

@RestController
class AccountController(private val service: AccountService) : AccountsApi {

    override fun listAccounts(cursor: String?, limit: Int): ResponseEntity<AccountPage> {
        val page = service.list(CurrentSession.require(), cursor, limit)
        return ResponseEntity.ok(AccountPage(page.items.map { toDto(it) }, page.nextCursor))
    }

    override fun createAccount(createAccountRequest: CreateAccountRequest, xDelfosClient: String?): ResponseEntity<Account> {
        val account = service.create(CurrentSession.require(), createAccountRequest.username, createAccountRequest.password, createAccountRequest.isAdmin ?: false)
        return ResponseEntity.created(URI.create("/api/v1/accounts/${account.id}")).body(toDto(account))
    }

    override fun disableAccount(accountId: UUID, xDelfosClient: String?): ResponseEntity<Account> =
        ResponseEntity.ok(toDto(service.disable(CurrentSession.require(), accountId)))

    private fun toDto(entity: AccountEntity) = Account(
        id = entity.id,
        username = entity.username,
        isAdmin = entity.isAdmin,
        status = AccountStatus.valueOf(entity.status.name),
        createdAt = entity.createdAt.utc(),
    )
}
