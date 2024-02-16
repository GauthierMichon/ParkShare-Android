package org.rncp.user.domain.ports.`in`

import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.ws.rs.core.Response
import org.rncp.user.domain.model.User
import org.rncp.user.domain.ports.out.UserRepository

@ApplicationScoped
class UpdateUseCase {
    @Inject
    private lateinit var userRepository: UserRepository

    fun execute(userData: User): Response {
        val user = userRepository.getByUid(userData.uid!!)
        return if (user != null) {
            Response.status(Response.Status.NO_CONTENT).build()
        } else {
            Response.status(Response.Status.NOT_FOUND).build()
        }
    }
}