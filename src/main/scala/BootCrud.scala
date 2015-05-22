import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.{IncomingConnection, ServerBinding}
import akka.http.scaladsl.model._
import akka.stream.{UniformFanInShape, ActorFlowMaterializer}
import akka.stream.scaladsl._
import play.api.libs.json.Json
import play.modules.reactivemongo.json.BSONFormats
import reactivemongo.bson.BSONDocument
import scala.concurrent.duration._

import scala.concurrent.Future
import scala.util.Random

object BootCrud extends App {
  implicit val actorSystem = ActorSystem("Crud-with-Streams")
  implicit val flowMaterializer = ActorFlowMaterializer()
  implicit val executionContext = actorSystem.dispatcher


  /**
   * CRUD
   */
  //bind is a source of connections
  private val bind: Source[IncomingConnection, Future[ServerBinding]] = Http().bind("localhost", 4231)

  bind.to(Sink.foreach {
    connection =>
      println(s"Accepted new connection from ${connection.remoteAddress}")
      connection.handleWithAsyncHandler(asyncHandler)
  }).run()

  /**
   * Define a function that handles request  to future of something (response)
   */
  def asyncHandler(request: HttpRequest): Future[HttpResponse] = {
    println("handling any request. ")
    request match {
      case HttpRequest(HttpMethods.GET, Uri.Path("/getAllTickers"), _, _, _) => {
        println("handling /getAllTickers")
        //map future of lists to future of HttpResponse
        Database.findAllTickers().map(list => HttpResponse(entity = bsonListToString(list)))
      }

      case HttpRequest(HttpMethods.GET, Uri.Path("/get"), _, _, _) => {
        println("handling /get")
        request.uri.query.get("ticker") match {
          case Some(ticker) => {
            println("getting ticker " + ticker)
            val tickerValue: Future[Option[BSONDocument]] = Database.findTicker(ticker)

            for{
              extractedTicker <- tickerValue
            } yield {
              extractedTicker match {
                case Some(bson) => HttpResponse(entity = bsonToString(bson))
                case None => HttpResponse(StatusCodes.NotFound)
              }
            }
          }

          case None => Future.successful(HttpResponse(StatusCodes.BadRequest))
        }
      }

      case req: HttpRequest => {
        println(s"Any other request: ${req.uri}")
        Future.successful(HttpResponse(StatusCodes.BadRequest))
      }
    }
  }


  def bsonToString(bsonDocument: BSONDocument): String = {
    Json.stringify(BSONFormats.toJSON(bsonDocument))
  }

  def bsonListToString(list: List[BSONDocument]): String = {
    list.map(b => bsonToString(b)).toString()
  }


  /**
   * Another server
   */

  val bind2 = Http().bind("localhost", 4232)

  import akka.pattern.after
  //this handler simulates a randomly (uniform within 1 sec) delayed response
  def getTickerHandler(ticker: String)(request: HttpRequest): Future[String] = {
    Database.findTicker(ticker).flatMap{
      case Some(foundDocument) => after(Random.nextInt(1000) seconds, actorSystem.scheduler)(Future.successful(bsonToString(foundDocument)))
      case None => after(Random.nextInt(1000) seconds, actorSystem.scheduler)(Future.successful("Nothing"))
    }
  }

  //define the flow

  //components: a broadcast

  def broadcastAndMergeWithFastest = Flow() { implicit builder =>

    val bcast = builder.add(Broadcast[HttpRequest](3))
    val merge = builder.add(Merge[String](3))

    val flow1: Flow[HttpRequest, String, Unit] = Flow[HttpRequest].mapAsync(1)(getTickerHandler("GOOG"))
    val flow2: Flow[HttpRequest, String, Unit] = Flow[HttpRequest].mapAsync(1)(getTickerHandler("AAPL"))
    val flow3: Flow[HttpRequest, String, Unit] = Flow[HttpRequest].mapAsync(1)(getTickerHandler("MSFT"))

    import FlowGraph.Implicits._

    bcast.out(0) ~> flow1 ~> merge.in(0)
    bcast.out(1) ~> flow2 ~> merge.in(1)
    bcast.out(2) ~> flow3 ~> merge.in(2)

    (bcast.in, merge.out)
  }


  bind2.to(Sink.foreach(
  connection => connection.handleWith(broadcastAndMergeWithFastest.map((s: String) => HttpResponse(entity = s)))
  )).run()
}


