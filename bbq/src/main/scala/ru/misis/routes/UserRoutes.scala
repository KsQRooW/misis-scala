package ru.misis.routes

import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import ru.misis.model.{User}
import ru.misis.registry.UserRegistry
import ru.misis.registry.UserRegistry._

import scala.concurrent.Future

//#import-json-formats
//#user-routes-class
class UserRoutes(userRegistry: ActorRef[UserRegistry.Command])(implicit val system: ActorSystem[_]) {

    //#user-routes-class
    //#import-json-formats
    import ru.misis.JsonFormats._
    import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._

    // If ask takes more time than this to complete the request is failed
    private implicit val timeout = Timeout.create(system.settings.config.getDuration("my-app.routes.ask-timeout"))

    /*
    Idea Ultimate
    Future
    Actor
     */

    def getUsers(): Future[Users] =
        userRegistry.ask(GetUsers)
    def getUser(name: String): Future[GetUserResponse] =
        userRegistry.ask(GetUser(name, _))
    def createUser(user: User): Future[ActionPerformed] =
        userRegistry.ask(CreateUser(user, _))
    def deleteUser(name: String): Future[ActionPerformed] =
        userRegistry.ask(DeleteUser(name, _))
    def updateUser(name: String, user: User): Future[ActionPerformed] =
        userRegistry.ask(UpdateUser(name, user, _))

    //#all-routes
    //#users-get-post
    //#users-get-delete
    val routes: Route =
    path("users") {
        get {
            complete(getUsers())
        }
    } ~
    path("users") {
        (post & entity(as[User])) { user =>
            onSuccess(createUser(user)) { performed =>
                complete((StatusCodes.Created, performed))
            }
        }
    } ~
    path("user" / Segment) { name =>
        get {
            rejectEmptyResponse {
                onSuccess(getUser(name)) { response =>
                    complete(response.maybeUser)
                }
            }
        }
    } ~
    path("user" / Segment) { name =>
        (put & entity(as[User])) { user =>
            onSuccess(updateUser(name, user)) { performed =>
                complete((StatusCodes.OK, performed))
            }
        }
    } ~
    path("user" / Segment) { name =>
        delete {
            onSuccess(deleteUser(name)) { performed =>
                complete((StatusCodes.OK, performed))
            }
        }
    }

}
