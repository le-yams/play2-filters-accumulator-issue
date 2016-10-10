
import akka.stream.Materializer
import akka.stream.scaladsl.Flow
import akka.util.ByteString
import javax.inject._
import play.api._
import play.api.http.HttpFilters
import play.api.libs.streams.Accumulator
import play.api.mvc._
import scala.concurrent.ExecutionContext
import scala.concurrent.Promise

/**
 * This class configures filters that run on every request. This
 * class is queried by Play to get a list of filters.
 *
 * Play will automatically use filters from any class called
 * `Filters` that is placed the root package. You can load filters
 * from a different class by adding a `play.http.filters` setting to
 * the `application.conf` configuration file.
 *
 * @param env Basic environment settings for the current application.
 * @param exampleFilter A demonstration filter that adds a header to
 * each response.
 */
@Singleton
class Filters @Inject() (
  testFilter: TestFilter
) extends HttpFilters {

  override val filters = Seq(testFilter)

}

class TestFilter @Inject() (
  implicit val mat: Materializer,
  implicit val ec: ExecutionContext
) extends EssentialFilter with BodyParsers {
  
  override def apply(nextFilter: EssentialAction) = new EssentialAction {

    lazy val log = play.api.Logger
    
    override def apply(requestHeader: RequestHeader): Accumulator[ByteString, Result] = {
      log.info(">>>> 0. applying TestFilter essential action")
      
      val flowEntered = Promise[Boolean]()
      
      val flow = Flow[ByteString].map { bs =>
        log.info(">>>> 1. passing through flow")
        flowEntered.success(true)
        bs
      }
      
      val accumulator = nextFilter(requestHeader)
      accumulator.through(flow).map { result =>
        val msg = if(flowEntered.isCompleted == false) {
          s"!!!! flow has NOT been feeded !!!!"
        } else {
          "flow has been feeded :)"
        }
        log.info(s"${requestHeader.method} ${requestHeader.path} => $msg (accumulator=$accumulator)")
        result
      }
      
    }
  }

}
