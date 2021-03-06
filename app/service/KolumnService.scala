package service

import anorm.SqlParser._
import anorm._
import controllers.KanbanSocketController
import model.{UserBase, StatusCode, ServiceResponse, Kolumn}
import play.api.db.DB
import play.api.Play.current

/**
 * Trait service in domain of Kolumn.
 */
protected trait KolumnService {
  /**
   * Return a list of columns from a list of projects
   * @param ids ids of projects to be queried
   * @return sequence of kolumns from the projects queried
   */
  protected def getKolumnsForProjects(ids: Seq[Long]): Seq[Kolumn] = {
    DB.withConnection{ implicit c =>
      SQL(
        s"""
           |SELECT * FROM kolumn
           |WHERE project_id
           |IN (${ids.mkString(",")})
         """.stripMargin
      ).as(model.Kolumn.parser.*)
    }
  }

  /**
   * Insert a new kolumn for an existing project.
   * @param kolumn case class kolumn with appropriate project id inside
   * @return id of newly inserted kolumn
   */
  def insertNewKolumn(kolumn : Kolumn): ServiceResponse[Long] = {
    DB.withConnection { implicit c =>
      SQL(
        s"""
           |SELECT
           |COUNT(*)
           |FROM project
           |WHERE id=${kolumn.projectId}
         """.stripMargin
      ).apply().head[Int]("COUNT(*)") match {
        case 0 =>
          implicit val error = -1L
          ServiceResponse(StatusCode.IdentifierNotFound,
            message=s"projectId ${kolumn.projectId} not found")
        case _ =>
          implicit val insertedKolumn : Long = SQL(
            s"""
               |INSERT INTO kolumn(project_id,name,position,threshold,created_by_user,is_archive_kolumn)
               |VALUES(
               |${kolumn.projectId},
               |"${kolumn.name}",
               |${kolumn.position},
               |${kolumn.threshold},
               |${kolumn.createdByUserId},
               |${kolumn.isArchiveKolumn})
            """.stripMargin
          ).executeInsert(scalar[Long].single)
          kolumn.id = Option(insertedKolumn)
          KanbanSocketController.newKolumn(
            kolumn,
            SQL(s"SELECT * FROM user WHERE id=${kolumn.createdByUserId}").as(UserBase.userParser.*).head,
            SQL(s"SELECT board_id FROM project WHERE id=${kolumn.projectId}").as(scalar[Long].single))
          ServiceResponse(StatusCode.OK)
      }
    }
  }
}