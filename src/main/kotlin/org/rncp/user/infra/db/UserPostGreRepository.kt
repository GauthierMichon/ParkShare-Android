package org.rncp.user.infra.db

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase
import jakarta.enterprise.context.ApplicationScoped
import org.rncp.user.domain.model.User
import org.rncp.user.domain.ports.out.UserRepository

@ApplicationScoped
class UserPostGreRepository: PanacheRepositoryBase<UserDAO, Int>, UserRepository {
    override fun getByUid(uid: String): User? {
        val results = find("uid", uid)
        return if (results.count() > 0) {
            results.firstResult<UserDAO>().toUser()
        } else {
            null
        }
    }

    override fun create(user: User): User {
        val userDAO = UserDAO(user.uid!!, user.firstname, user.lastname, user.email, user.roleId)
        persist(userDAO)
        return userDAO.toUser()
    }

    override fun update(userData: User) {
        val user = find("uid", userData.uid).firstResult<UserDAO>()
        user.apply {
            firstname = userData.firstname
            lastname = userData.lastname
            email = userData.email
            roleId = userData.roleId
        }
    }

    override fun delete(userUid: String) {
        val user = find("uid", userUid).firstResult<UserDAO>()
        delete(user)
    }
}