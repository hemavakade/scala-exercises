package org.scalaexercises.algebra.exercises

import org.scalaexercises.types.exercises._
import cats.free._
import org.scalaexercises.types.exercises.ExerciseEvaluation.EvaluationRequest

/** Exercise Ops GADT
  */
sealed trait ExerciseOp[A]
final case class GetLibraries() extends ExerciseOp[List[Library]]
final case class GetSection(libraryName: String, sectionName: String) extends ExerciseOp[Option[Section]]
final case class BuildRuntimeInfo(exerciseEvaluation: ExerciseEvaluation) extends ExerciseOp[EvaluationRequest]

/** Exposes Exercise operations as a Free monadic algebra that may be combined with other Algebras via
  * Coproduct
  */
class ExerciseOps[F[_]](implicit I: Inject[ExerciseOp, F]) {

  def getLibraries: Free[F, List[Library]] =
    Free.inject[ExerciseOp, F](GetLibraries())

  def getLibrary(libraryName: String): Free[F, Option[Library]] =
    getLibraries map (_.find(_.name == libraryName))

  def getSection(libraryName: String, sectionName: String): Free[F, Option[Section]] =
    Free.inject[ExerciseOp, F](GetSection(libraryName, sectionName))

  def buildRuntimeInfo(evaluation: ExerciseEvaluation): Free[F, EvaluationRequest] =
    Free.inject[ExerciseOp, F](BuildRuntimeInfo(evaluation))

}

/** Default implicit based DI factory from which instances of the ExerciseOps may be obtained
  */
object ExerciseOps {

  implicit def instance[F[_]](
    implicit
    I: Inject[ExerciseOp, F]
  ): ExerciseOps[F] = new ExerciseOps[F]

}
