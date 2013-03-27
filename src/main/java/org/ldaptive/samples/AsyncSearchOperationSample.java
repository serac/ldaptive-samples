/*
  $Id: $

  Copyright (C) 2013 Virginia Tech.
  All rights reserved.

  SEE LICENSE FOR MORE INFORMATION

  Author:  Middleware Services
  Email:   middleware@vt.edu
  Version: $Revision: $
  Updated: $Date: $
*/
package org.ldaptive.samples;

import org.ldaptive.Connection;
import org.ldaptive.DefaultConnectionFactory;
import org.ldaptive.LdapException;
import org.ldaptive.Request;
import org.ldaptive.Response;
import org.ldaptive.SearchEntry;
import org.ldaptive.SearchRequest;
import org.ldaptive.SearchResult;
import org.ldaptive.async.AsyncRequest;
import org.ldaptive.async.AsyncSearchOperation;
import org.ldaptive.async.FutureResponse;
import org.ldaptive.async.handler.AsyncRequestHandler;
import org.ldaptive.async.handler.ExceptionHandler;
import org.ldaptive.extended.CancelOperation;
import org.ldaptive.extended.CancelRequest;
import org.ldaptive.handler.HandlerResult;
import org.ldaptive.handler.OperationResponseHandler;
import org.ldaptive.handler.SearchEntryHandler;

/**
 * Demonstrates use of the {@link AsyncSearchOperation} component.
 *
 * @author Middleware Services
 * @version $Revision: $
 */
public class AsyncSearchOperationSample implements Sample
{
  public void execute() throws Exception
  {
    class Holder<T> {
      private T data;

      public T getData()
      {
        return data;
      }

      public void setData(T data)
      {
        this.data = data;
      }
    }

    Connection conn = DefaultConnectionFactory.getConnection("ldap://directory.ldaptive.org");
    try {
      conn.open();
      final AsyncSearchOperation search = new AsyncSearchOperation(conn);
      final Holder<AsyncRequest> asyncRequestHolder = new Holder<AsyncRequest>();

      // do something when a response is received
      search.setOperationResponseHandlers(
        new OperationResponseHandler<SearchRequest, SearchResult>() {
          @Override
          public HandlerResult<Response<SearchResult>> handle(
            Connection conn, SearchRequest request, Response<SearchResult> response)
            throws LdapException
          {
            System.err.println("received: " + response);
            return new HandlerResult<Response<SearchResult>>(response);
          }
        });

      // if you plan to cancel or abandon this operation you need a reference to the AsyncRequest
      search.setAsyncRequestHandlers(
        new AsyncRequestHandler() {
          @Override
          public HandlerResult<AsyncRequest> handle(
            Connection conn, Request request, AsyncRequest asyncRequest)
            throws LdapException
          {
            // store the async request handle and notify waiting threads we have it
            asyncRequestHolder.setData(asyncRequest);
            asyncRequestHolder.notify();
            return new HandlerResult<AsyncRequest>(asyncRequest);
          }
        });

      // long running searches may experience problems, do something when an exception occurs
      search.setExceptionHandler(
        new ExceptionHandler() {
          @Override
          public HandlerResult<Exception> handle(
            Connection conn, Request request, Exception exception)
          {
            System.err.println("received exception: " + exception);
            return new HandlerResult<Exception>(null);
          }
        });

      SearchRequest request = new SearchRequest("ou=people,dc=ldaptive,dc=org", "(cn=*fisher)");
      // do something when an entry is received
      request.setSearchEntryHandlers(
        new SearchEntryHandler() {
          @Override
          public HandlerResult<SearchEntry> handle(
            Connection conn, SearchRequest request, SearchEntry entry)
            throws LdapException
          {
            System.err.println("received: " + entry);
            // return null value in the handler result to prevent the entry from being stored in the SearchResult
            return new HandlerResult<SearchEntry>(entry);
          }
          @Override
          public void initializeRequest(final SearchRequest request) {}
        });

      FutureResponse<SearchResult> response = search.execute(request);

      // wait for the async handle
      asyncRequestHolder.wait();

      // now we have the async request handle
      final AsyncRequest asyncRequest = asyncRequestHolder.getData();

      // now you can abandon the operation
      asyncRequest.abandon();

      // or cancel the operation
      CancelOperation cancel = new CancelOperation(conn);
      cancel.execute(new CancelRequest(asyncRequest.getMessageId()));

      // or just block until the response arrives
      SearchResult result = response.getResult();

      // cleanup the underlying executor service
      search.shutdown();

    } finally {
      conn.close();
    }

  }
}
