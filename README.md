# Kanban
![alt tag](https://raw.githubusercontent.com/jsflax/kanban/master/kanban_map.png?token=AEJ7QLsMvXh45YAyxm6EJnlWNRUJYk79ks5VFaz9wA%3D%3D)

## Migrating to Scala

So you've decided to finally grow up from Java. You don't want to leave the JVM, but you don't want the verbosity of Java, nor the half-cocked languages that followed.
I present: Scala. Started in 2003 in the Swiss Alps, Scala sounded like an idea that shouldn't work: with so many people believing so strongly in so many different paradigms, let's create
a language that does it all. Like OOP? Like functional programming? Have both. Hate singletons and operator overloading? Let's come to a compromise. Inheritance got you down? Have traits instead.
I'm happy to say after having worked with this in my personal time (see: at my own leisure), that this language, does, surprisingly work. And it works well. I mean really well. I don't
want to code in anything else. And not only that -- it's fun. It hasn't felt this good to code since my first real program got released into the public.

Simply put, I cannot teach you the entirety of Scala here. The best place to start would, of course, be the Scala home page: [Scala: Object Oriented Meets Functional](http://www.scala-lang.org/).
My goal here is to introduce some of the idioms of Scala, as well as some of the syntactical differences.

As a note: Scala is a strongly typed language. It uses structural typing, as opposed to full on duck typing. You do have a concept of full polymorphism just like
Java. However, the Scala ```Any``` class replaces the Java ```Object``` class:
![alt tag](http://www.scala-lang.org/old/sites/default/files/images/classhierarchy.png)
It is certainly looser than Java, but not as loose as a true scripting language. As an example, functions do not need to explcitly declare their return types,
but it is still implicitly in existence. Scala does offer you the ability to write your own implicit conversions, which is quite nice, though I have never
had the opportunity to use this myself.

### Classes, Case Classes, and Objects
#### class
A class in scala is your classic class structure, near identical to Java's.
```scala
class A extends B {
     def this() {
        this()
     }
}
```
With the exception of the constructor, that should look quite familiar.
#### case class
So what is a case class? Case classes can be seen as plain
and immutable data-holding objects that should exclusively depend on their constructor arguments. This is, in fact, a functional concept.
This allows us to:
- use a compact initialisation syntax (Node(1, Leaf(2), None)))
- decompose them using pattern matching
- have equality comparisons implicitly defined
A general rule of them is that if an object performs stateful computations on the inside or exhibits other kinds of
complex behaviour, it should be an ordinary class.
```scala
case class UserBase(email : String,
                    firstName : String,
					lastName : Option[String],
				    username : String,
				    password : String,
					avatarUrl : Option[String],
					var authorizedBoards : Option[Set[Long]],
					var id : Option[Long])
```
As a note, the same constructor syntax here can be used for regular classes.
#### object
Objects are, aside from being confusingly named when speaking classically about OOP, are essentially singletons. These can be standalone
singletons, or static instances of a companion class.
```scala
object UserBase {
     lazy val DefaultAvatarUrl: String = "http://s3-us-west-1.amazonaws.com/witty-avatars/default-avatar-2-l.jpg"
}
```
So we have a singleton companion class to the previous ```UserBase``` case class. The constant ```DefaultAvatarUrl``` can be accessed as you
would any static member: ```UserBase.DefaultAvatarUrl```. As a note, constants in Scala are styled as upper camel case. No more Java all caps
snake case. A full style guide can be found here: http://docs.scala-lang.org/style/naming-conventions.html .
### Var, val, lazy, optional arguments, and def
These are tough concepts to teach individually. It is better to understand them together since they are all so tightly coupled:
```scala
case class Rect(left: Int,
                top: Int,
				right: Int,
				bottom: Int,
				color: Color = null)
var newRectangle: Rect = null
lazy val bigBlueRect = Rect(0,0,500,500,Color.Blue)
def transferRectangleAsRedRect(rect: Rect) {
    newRectangle = rect
	if(newRectangle.color != Color.Red) {
	    newRectangle = Rect(rect.left, rect.top, rect.right, rect.bottom, color=Color.Red)
	}
}
```
What we have to begin is a case class, with a single optional argument called color. Note that all of these construction args are ```val```.
This means that these are **immutable** variables. They cannot be changed after a new Rect has been constructed.

From there we have declared a field in this file called "newRectangle":
```scala
var newRectangle: Rect = null
```
```var``` means that this variable is **mutable**. It can, and will be changed later. The difference between var and val is not just for
the compiler to optimize your code: it is expressive to the reader of what you intend to do with this data.

From there we have our first ```lazy val```:
```scala
lazy val bigBlueRect = Rect(0,0,500,500,Color.Blue)
```
So what is this sorcery? ```lazy``` in Scala just means that this variable will not be initialized until it's first use. The Java equivalent
that should be recognizable:
```java
Rect bigBlueRect = null;
public void getBigBlueRect() {
     if (bigBlueRect == null) {
	      bigBlueRect = new Rect(0,0,500,500,Color.Blue);
     }
	 return bigBlueRect;
}
```
It is a very nice optimization feature. The return value can also be a high order function if you would like.

Moving on from there, we have our first ```def```, or, function definition. This function takes a single parameter, rect. Note that all function
parameters are immutable vals.
```scala
def transferRectangleAsRedRect(rect: Rect) {
    newRectangle = rect
	if(newRectangle.color != Color.Red) {
	     newRectangle = Rect(rect.left, rect.top, rect.right, rect.bottom, color=Color.Red)
	}
}
```
Inside our function, we immediately set our ```var newRectangle``` to the passed in value. And then, just to prove
a point, if it is not of a certain color, we then create a new rect to change it to that color. Why? Because the color field of Rect is an
immutable value, so we have to create a new object. Finally, at the end of this new Rect (also, notice the lack of the keyword ```new```: yay
case classes) we see that I specifically initialize the optional color argument, by using it's name. This can be done for any amount of optional
arguments.

## Play Framework
For those unfamiliar with the Play Framework, we are given a fair amount of features that act implicitly, but are actually quite explicit once
you understand the basic protocol we follow for an object.

Most of the code is quite explicit. Controllers control the flow of information between a service and a client. Models are the data structures
that contain our information into tightly coupled case classes. Services interact with the database to read and write info on command of a client.
With the exception of a few syntactical sugars, all of this should be quite legible, even to those untrained in Scala.

As with any higher generation language though, and especially in a language with it's on DSLs, there are several implicit features that I would
like to go over:

### Reads
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

### Writes
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

### Anorm RowParsers

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
