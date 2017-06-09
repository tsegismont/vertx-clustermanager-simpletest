package io.vertx.test.spring.clustering;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.event.EventListener;
import org.springframework.util.StopWatch;

import java.util.concurrent.atomic.AtomicInteger;


@SpringBootApplication
@ComponentScan("io.vertx.test.spring.clustering.config")
public class MainTest {

  @Autowired
  Vertx vertx;

  static final int MSGNUM = 500000;

  public static void main(String[] args) {
    SpringApplication.run(MainTest.class, args);
  }

  /**
   * Deploy verticles when the Spring application is ready.
   */

  @EventListener
  void deployVerticles(ApplicationReadyEvent event) {
    vertx.deployVerticle(new ReceiveVerticle(), new DeploymentOptions().setInstances(1), e -> {
      // when ReceiveVerticle is deployed, deploy SendVerticle
      vertx.deployVerticle(new SendVerticle(), new DeploymentOptions().setInstances(1));
    });
  }

  public static class SendVerticle extends AbstractVerticle {

    static AtomicInteger i = new AtomicInteger();
    static final StopWatch watch = new StopWatch();

    @Override
    public void start() throws Exception {
      System.out.println("testing with : " + MSGNUM + " messages.");
      vertx.eventBus().send("receive_address", "hello", ar -> {
        if (ar.succeeded()) {
          doTest();
        } else {
          ar.cause().printStackTrace();
          System.exit(1);
        }
      });
    }

    private void doTest() {
      vertx.eventBus().send("initialize", "init", reply -> {
        if (reply.succeeded()) {
          watch.start();
          //I realise this is a tight loop, but it seems to make no appreciable difference when
          //messages are sent not in a loop but via a scheduler or other thread.
          for (int ii = 0; ii < MSGNUM; ii++) {
            String message = String.valueOf(i.incrementAndGet());
            vertx.eventBus().send("receive_address", message);
            if (i.get() >= MSGNUM && watch.isRunning()) {
              watch.stop();
              System.out.println("Sender StopWatch in millis: " + watch.getTotalTimeMillis());
            }
          }
        }
      });
    }
  }

  public static class ReceiveVerticle extends AbstractVerticle {
    static final StopWatch watch = new StopWatch();
    AtomicInteger i = new AtomicInteger();
    boolean initialized;

    @Override
    public void start(Future<Void> startFuture) throws Exception {
      vertx.eventBus().<String>consumer("initialize", message -> {
        message.reply("reply");
        initialized = true;
      });

      vertx.eventBus().<String>consumer("receive_address", message -> {
        if (!initialized) {
          message.reply(message.body());
          return;
        }
        if (i.get() == 0) {
          watch.start();
        }
        if (i.incrementAndGet() >= MSGNUM && watch.isRunning()) {
          watch.stop();
          System.out.println("Receiver StopWatch in millis: " + watch.getTotalTimeMillis());
          System.out.println("done.");
          System.exit(0);
        }

      }).completionHandler(startFuture.completer());
    }

  }

}