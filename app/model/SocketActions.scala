package model

/**
 */
object SocketActions extends Enumeration {
  type SocketActions = String

  val moveTicket = "moveTicket"
  val newTicket = "newTicket"
  val addCollaborator = "addCollaborator"
  val moveKolumn = "moveKolumn"
  val newKolumn = "newKolumn"
  val addUserToBoard = "addUserToBoard"
  val newBoard = "newBoard"
  val newProject = "newProject"
}