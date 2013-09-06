package jp.gr.kmc.starlight.simplehttp

import scala.concurrent.{Future,future}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.parsing.json.JSON

import java.io.File
import java.net.URI

import org.apache.http
import http.{HttpEntity,HttpEntityEnclosingRequest,HttpResponse,HttpStatus}
import http.client.{HttpClient,ResponseHandler}
import http.client.methods.{HttpGet,HttpPost,HttpDelete,HttpPut,HttpUriRequest}
import http.entity.mime.{MultipartEntity,FormBodyPart}
import http.entity.mime.content.{StringBody,FileBody}
import http.impl.client.DefaultHttpClient
import http.impl.conn.PoolingClientConnectionManager
import http.util.EntityUtils


class Http(client: HttpClient) {

  def execute[T](request: HttpUriRequest, handler: HttpResponse => T): Future[T] =
    future {
      client.execute(request, new ResponseHandler[T] {
        override def handleResponse(response: HttpResponse): T = handler(response)
      })
    }

  def execute[T](request: HttpUriRequest, headers: Iterable[(String, String)], handler: HttpResponse => T): Future[T] = {
    headers.foreach { case (name, value) => request.addHeader(name, value) }
    execute(request, handler)
  }

  def execute[T](request: HttpUriRequest with HttpEntityEnclosingRequest, headers: Iterable[(String, String)],
                 entity: HttpEntity, handler: HttpResponse => T): Future[T] = {
    request.setEntity(entity)
    execute(request, headers, handler)
  }

  def get[T](uri: URI, headers: (String, String)*)(implicit handler: HttpResponse => T): Future[T] =
    execute(new HttpGet(uri), headers, handler)

  def post[T](uri: URI, headers: (String, String)*)(entity: HttpEntity)(implicit handler: HttpResponse => T): Future[T] =
    execute(new HttpPost(uri), headers, entity, handler)

  def delete[T](uri: URI, headers: (String, String)*)(implicit handler: HttpResponse => T): Future[T] =
    execute(new HttpDelete(uri), headers, handler)

  def put[T](uri: URI, headers: (String, String)*)(entity: HttpEntity)(implicit handler: HttpResponse => T): Future[T] =
    execute(new HttpPut(uri), headers, entity, handler)
}

object Http extends Http(new DefaultHttpClient(new PoolingClientConnectionManager))

object Handlers {
  def json[T] = (response: HttpResponse) =>
    response.getStatusLine.getStatusCode match {
      case HttpStatus.SC_OK =>
        JSON.parseFull(EntityUtils.toString(response.getEntity, "ASCII")).get.asInstanceOf[T]
    }

  val text = (response: HttpResponse) =>
    response.getStatusLine.getStatusCode match {
      case HttpStatus.SC_OK =>
        EntityUtils.toString(response.getEntity, "ASCII")
    }

  val raw = (response: HttpResponse) =>
    response.getStatusLine.getStatusCode match {
      case HttpStatus.SC_OK =>
        EntityUtils.toByteArray(response.getEntity)
    }

  val ignore = (response: HttpResponse) =>
    response.getStatusLine.getStatusCode match {
      case HttpStatus.SC_OK => ()
    }
}

object Multipart {
  def apply(params: (String, String)*)(files: (String, File)*) = {
    val multipart = new MultipartEntity
    params.foreach {
      case (name, value) =>
        multipart.addPart(new FormBodyPart(name, new StringBody(value)))
    }
    files.foreach {
      case (name, file) =>
        multipart.addPart(new FormBodyPart(name, new FileBody(file)))
    }
    multipart
  }
}
