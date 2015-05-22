import reactivemongo.api.{MongoConnection, MongoDriver}
import reactivemongo.api.collections.default.BSONCollection
import reactivemongo.bson.BSONDocument

import scala.concurrent.Future

/**
 * Object responsible for Mongo connection and non blocking interaction
 */
object Database {

  val collection = connect()
  import scala.concurrent.ExecutionContext.Implicits.global

  def connect(): BSONCollection = {
    val driver = new MongoDriver

    //connect to a cluster of one node, localhost
    val connection = driver.connection(List("localhost"))
    //connect to akka db
    val db = connection("akka")
    //return stocks colleciton
    db.collection("stocks")
  }

  def findAllTickers(): Future[List[BSONDocument]] = {
    val query = BSONDocument()
    // equivalent to query:
    // db.stocks.find({}, {"Company": 1, "Country": 1, "Ticker": 1})
    val filter = BSONDocument("Company" -> 1, "Country" -> 1, "Ticker" -> 1)
    //collect returns a future
    collection.find(query, filter).cursor[BSONDocument].collect[List]()
  }

  def findTicker(ticker: String): Future[Option[BSONDocument]] = {
    val query = BSONDocument("Ticker" -> ticker)
    collection.find(query).one
  }



}
