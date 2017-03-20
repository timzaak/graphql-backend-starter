import java.util.Date

import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import com.timzaak.di.DI
import com.timzaak.entity.UserAuth
import org.json4s._
import sangria.execution.{ErrorWithResolver, Executor, QueryAnalysisError, QueryReducer}
import sangria.parser.QueryParser
import ws.very.util.json.JsonHelperWithDoubleMode

import scala.util.{Failure, Success}

object Server extends App with JsonHelperWithDoubleMode with DI {

  val rejectComplexQueries = QueryReducer.rejectComplexQueries[Any](100, (c, ctx) ⇒
    new IllegalArgumentException(s"Too complex query"))

  val route: Route =
    (post & path("graphql")) {
      implicit val serialization = native.Serialization
      import JsonExtractors._
      import de.heikoseeberger.akkahttpjson4s.Json4sSupport._
      import sangria.marshalling.json4s.native._

      entity(as[JValue]) { requestJson ⇒
        val JString(query: String) = requestJson \ "query"

        val strOption(operation: Option[String]) = requestJson \ "operationName"

        val variables = requestJson \ "variables" match {
          case JNull | JNothing => JObject()
          case other => other
        }
        val time = new Date().getTime
        QueryParser.parse(query) match {
          case Success(queryAst) ⇒
            complete(Executor.execute(
              graphQLSchema,
              queryAst,
              UserAuth(None),
              variables = variables,
              queryReducers = rejectComplexQueries::Nil,
              operationName = operation
            )
              .map { result =>
                println("cost" + (new Date().getTime - time))
                OK → result
              }
              .recover {
                case error: QueryAnalysisError ⇒
                  BadRequest → error.resolveError
                case error: ErrorWithResolver ⇒
                  InternalServerError → error.resolveError
              })
          case Failure(error) ⇒
            complete(BadRequest, "error" -> error.getMessage)
        }
      }
    } ~
      get {
        getFromResource("graphiql.html")
      }

  val port = sys.props.get("http.port").fold(8080)(_.toInt)
  val bindingFuture = Http().bindAndHandle(route, "0.0.0.0", port)
  bindingFuture.onComplete {
    case Success(binding) ⇒
      println(s"Server is listening on localhost:$port")
    case Failure(e) ⇒
      println(s"Binding failed with ${e.getMessage}")
      system.terminate()
  }
}