package com.patson.stream

object TestEventStream extends App {
  RemoteSubscribe.subscribe((event, any) => {
    Some(println("hi")) 
  }, "testing")
}