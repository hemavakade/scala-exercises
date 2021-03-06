package org.scalaexercises.exercises.persistence.repositories

/*
 * scala-exercises-server
 * Copyright (C) 2015-2016 47 Degrees, LLC. <http://www.47deg.com>
 */

import org.scalaexercises.types.user.UserCreation
import org.scalaexercises.types.user.UserCreation.Request

import org.scalaexercises.exercises.support.{ ArbitraryInstances, DatabaseInstance }
import doobie.imports._
import org.scalacheck.Arbitrary
import org.scalacheck.Shapeless._
import org.scalatest._
import org.scalatest.prop.GeneratorDrivenPropertyChecks
import scalaz.concurrent.Task
import scalaz.syntax.applicative._
import cats.syntax.either._

class UserRepositorySpec
    extends PropSpec
    with GeneratorDrivenPropertyChecks
    with Matchers
    with ArbitraryInstances
    with DatabaseInstance
    with BeforeAndAfterAll {

  implicit val transactor: Transactor[Task] = databaseTransactor

  val repository = implicitly[UserRepository]
  override def beforeAll() =
    repository.deleteAll.transact(transactor).unsafePerformSync

  // Generators
  implicitly[Arbitrary[UserCreation.Request]]

  def assertConnectionIO(cio: ConnectionIO[Boolean]): Unit =
    assert(cio.transact(transactor).unsafePerformSync)

  // Properties
  property("new users can be created") {
    forAll { newUser: Request ⇒
      val tx: ConnectionIO[Boolean] =
        repository.create(newUser).map { storedUser ⇒
          storedUser.toOption.forall(u ⇒ u == newUser.asUser(u.id))
        }
      assertConnectionIO(tx)
    }
  }

  property("users can be queried by their login") {
    forAll { newUser: Request ⇒
      val create = repository.create(newUser)
      val get = repository.getByLogin(newUser.login)

      val tx: ConnectionIO[Boolean] =
        (create *> get).map { storedUser ⇒
          storedUser.forall(u ⇒ u == newUser.asUser(u.id))
        }

      assertConnectionIO(tx)
    }
  }

  property("users can be queried by their ID") {
    forAll { newUser: Request ⇒
      val tx: ConnectionIO[Boolean] =
        repository.create(newUser).flatMap { storedUser ⇒
          storedUser.toOption.fold(
            false.pure[ConnectionIO]
          )(
            u ⇒ repository.getById(u.id).map(_.contains(u))
          )
        }

      assertConnectionIO(tx)
    }
  }

  property("users can be deleted") {
    forAll { newUser: Request ⇒
      val tx: ConnectionIO[Boolean] =
        repository.create(newUser).flatMap { storedUser ⇒
          storedUser.toOption.fold(false.pure[ConnectionIO]) { u ⇒
            val delete = repository.delete(u.id)
            val get = repository.getByLogin(newUser.login)
            (delete *> get).map(_.isEmpty)
          }
        }

      assertConnectionIO(tx)
    }
  }

  property("users can be updated") {
    forAll { newUser: Request ⇒
      val create = repository.create(newUser)
      val get = repository.getByLogin(newUser.login)

      val tx: ConnectionIO[Boolean] =
        (create *> get).flatMap { storedUser ⇒
          storedUser.fold(false.pure[ConnectionIO]) { u ⇒
            val modifiedUser = u.copy(email = Some("alice+spam@example.com"))
            val update = repository.update(modifiedUser)
            val get = repository.getByLogin(u.login)
            (update *> get).map(_.contains(modifiedUser))
          }
        }

      assertConnectionIO(tx)
    }
  }
}
