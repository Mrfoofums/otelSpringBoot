package com.mycompany.otelexample;

import io.grpc.ManagedChannelBuilder;
import io.opentelemetry.OpenTelemetry;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.HttpTextFormat;
import io.opentelemetry.exporters.jaeger.JaegerGrpcSpanExporter;
import io.opentelemetry.exporters.logging.LoggingExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.sdk.trace.export.SimpleSpansProcessor;
import io.opentelemetry.trace.Span;
import io.opentelemetry.trace.SpanContext;
import io.opentelemetry.trace.Tracer;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class App 
{
    public static void main( String[] args ) throws Exception
    {
      Tracer tracer = initTracer();

      Span span = tracer.spanBuilder("client Request Span").startSpan();
      try(Scope scope = tracer.withSpan(span)){
          doRequest(tracer);
      }
      catch(Exception e){
          span.addEvent("error");
      }
      finally{
          span.end();
      }

    }
    
    static void doRequest(Tracer tracer) throws Exception {
      Request.Builder reqBuilder = new Request.Builder();
    
      // Inject the current Span into the Request.
      HttpTextFormat<SpanContext> textFormat = tracer.getHttpTextFormat();
      Span currentSpan = tracer.getCurrentSpan();
      textFormat.inject(currentSpan.getContext(), reqBuilder, new
      HttpTextFormat.Setter<Request.Builder>() {
        @Override
        public void put(Request.Builder reqBuilder, String key, String value)
        {
          reqBuilder.addHeader(key, value);
        }
      });
    
      // Perform the actual request with the propagated Span.
      Request req1 = reqBuilder.url("localhost:8080/api/1").build();
      Request req2 = reqBuilder.url("localhost:8080/api/2").build();
      OkHttpClient client = new OkHttpClient();
      try{
        Response res1 = client.newCall(req1).execute();
        Response res2 = client.newCall(req2).execute();
        System.out.println(res1.body().string());
        System.out.println(res2.body().string());
      }
      catch(Exception e){
        currentSpan.addEvent(e.toString());
        currentSpan.setAttribute("error", true);
      }
    }

    static Tracer initTracer(){
        final Tracer tracer = OpenTelemetry.getTracerFactory().get("com.mycompany.otelexample");
        SpanProcessor jaegerProcessor =
            SimpleSpansProcessor.newBuilder(JaegerGrpcSpanExporter.newBuilder()
            .setServiceName("Client Service")
            .setChannel(ManagedChannelBuilder.forAddress(
            "localhost", 14250).usePlaintext().build())
            .build()).build();    

        SpanProcessor logProcessor = SimpleSpansProcessor.newBuilder(new LoggingExporter()).build();

        OpenTelemetrySdk.getTracerFactory().addSpanProcessor(logProcessor);
        OpenTelemetrySdk.getTracerFactory().addSpanProcessor(jaegerProcessor);
        return tracer;
    }
}
