# Kanban
![alt tag](https://raw.githubusercontent.com/jsflax/kanban/master/kanban_map.png?token=AEJ7QLsMvXh45YAyxm6EJnlWNRUJYk79ks5VFaz9wA%3D%3D)

## Notes

For those unfamiliar with the Play Framework, we are given a fair amount of features that act implicitly, but are actually quite explicit once
you understand the basic protocol we follow for an object.

Most of the code is quite explicit. Controllers control the flow of information between a service and a client. Models are the data structures
that contain our information into tightly coupled case classes. Services interact with the database to read and write info on command of a client.
With the exception of a few syntactical sugars, all of this should be quite legible, even to those untrained in Scala.

As with any higher generation language though, and especially in a language with it's on DSLs, there are several implicit features that I would
like to go over:

## Reads
Most models have implicit reads, writes, and parsers for serialization and de-serialization.

A read would look as follows:
```scala
implicit val reads : Reads[UserBase] = (
    (JsPath \ "email").read(email) and
    (JsPath \ "firstName").read[String] and
    (JsPath \ "lastName").readNullable[String].orElse(Reads.pure(null)) and
    (JsPath \ "username").read[String] and
    (JsPath \ "password").read(minLength[String](8)) and
    (JsPath \ "avatarUrl").readNullable[String] and
    (JsPath \ "authorizedBoards").readNullable[Set[Long]] and
    (JsPath \ "id").readNullable[Long]
  )(UserBase.apply _)
```
JsPath references the the current object being parsed. The '\' operator can be considered as a sort of ".field(fieldName: String)" method.
What we see here is called "JSON combinator syntax", a DSL of the play framework. Reading left to right, it actually reads quite like english:

At the json path, read the field named "email" into the @email parameter of the UserBase, and then move on to the next field.

This implicit value allows us to essentially convert JSON directly into one of our data models.

Reads can also be written as:
```scala
implicit val reads = Json.reads[Project]
```
This will use reflection to "auto-create" the implicit reader using a 1:1 relationship with the names of the classes parameters. Default
read protocol is really only appropriate for single level objects (non-nested).

## Writes
An implicit write would look quite similar:
```scala
implicit val writes = new Writes[FullProject] {
    def writes(fullProject: FullProject): JsValue = {
      Json.obj(
        "project" -> Json.toJson(fullProject.project),
        "kolumns" -> Json.arr(
          Json.toJson(fullProject.columns)
        ),
        "tickets" -> Json.arr(
          Json.toJson(fullProject.tickets)
        )
      )
    }
  }
```
In this case, we have our @FullProject data model, which contains the project model, an array of our kolumn models (yes, it is spelled with a
'k' on purpose), and an array of tickets associated with the project. All we are doing is using the *play.api.libs.Json* singleton to create
a json object. This will now be the implicitly created object when we serialize our data model.

Writes can also be written as:
```scala
implicit val writes = Json.writes[Project]
```
This will use reflection to "auto-create" the implicit writer using a 1:1 relationship with the names of the classes parameters. Default
write protocol is really only appropriate for single level objects (non-nested).

## Anorm RowParsers

A row parser parses a result set from a SQL query (using anorm) into one of our data models. Take the following piece of code:
```scala
protected def getProjectsForBoards(ids: Seq[Long]): Seq[Project] = {
   DB.withConnection { implicit c =>
      SQL(
        s"""
           |SELECT * FROM project
           |WHERE board_id
           |IN (${ids.mkString(",")})
         """.stripMargin
      ).as(Project.parser.*)
   }
}
```
This function takes in a sequence of board IDs, and returns us a sequence of projects. Anorm maintains a connection to the database
(or any database in our config -- if we had more than one, we would have to pass in a parameter to determine which to use), so we
use that context to make a SQL call. In this case, we are selecting each column from the project table where we have a board ID
in the sequence that was passed in. The key here is the final part of the method chain: ```.as(Project.parser.*)```. This is
auto-converting that into our Project data model (those familiar with ORMs should have a clear idea of what is happening now).
This is what a simple parser looks like:
```scala
implicit val parser: RowParser[Project] = {
      get[Long]("board_id") ~
      get[String]("name") ~
      get[String]("prefix") ~
      get[Long]("created_by_user") ~
      get[Long]("id") map {
      case boardId~name~prefix~created~id =>
        Project(boardId, name, prefix, created, Option(id))
    }
  }
```
This should look similar to the combinator "read" syntax from the read section of this ReadMe. We declare the type of object, the
name of the column, and then map it to the appropriate paramater for the class (preferably a case class).

Parsers can also have nested parsers for JOINs. This is where things get tricky:
```scala
implicit val collaboratorParser: RowParser[Ticket] = {
    get[Long]("ticket.project_id") ~
    get[String]("ticket.name") ~
    get[Option[String]]("ticket.description") ~
    UserBase.userParser.? ~
    CommentItem.parser.? ~
    get[Option[Boolean]]("ticket.ready_for_next_stage") ~
    get[Option[Boolean]]("ticket.blocked") ~
    get[Long]("ticket.current_kolumn_id") ~
    get[Option[DateTime]]("ticket.due_date") ~
    get[Option[Boolean]]("ticket.archived") ~
    get[Option[Int]]("ticket.priority") ~
    get[Option[Int]]("ticket.difficulty") ~
    get[Long]("ticket.assigner_id") ~
    get[Long]("ticket.id") map {
    case projectId~name~description~userId~commentItems~readyForNextStage~blocked~kolumnId
      ~dueDate~archived~priority~difficulty~assignerId~id =>
      Ticket(projectId, name, description, Option(Seq(userId.get)), Option(Seq(commentItems.getOrElse(CommentItem(None, None, None, None)))), readyForNextStage, blocked,
        kolumnId, dueDate, archived, priority, difficulty, assignerId, Option(id))
  }
}
```
Kill it with fire! No no, it's not that bad. It should appear familiar to the simple parser: we are doing reads based on type and column name.
The main differences? We are using the table name as a prefix, and the inclusion of the ```UserBase.userParser.?``` line and the
```CommentItem.parser.?``` line. All these are are nested row parsers for the JOINed tables. The '?' at the end just means that they can
be null. Then they are mapped to the object in the same fashion as before.