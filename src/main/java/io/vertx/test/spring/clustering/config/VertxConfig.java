package io.vertx.test.spring.clustering.config;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.spi.cluster.ClusterManager;
//import io.vertx.spi.cluster.ignite.IgniteClusterManager;
//import io.vertx.spi.cluster.jgroups.JGroupsClusterManager;
import io.vertx.ext.cluster.infinispan.InfinispanClusterManager;


@Configuration
public class VertxConfig {

  /*@Primary
  @Bean
  public ClusterManager getJGroupsClusterManager(){
	  return new JGroupsClusterManager();
  }
  
  @Bean
  public ClusterManager IgniteClusterManager(){
	  return new IgniteClusterManager();
  }*/
  
  @Primary
  @Bean
  public ClusterManager InfinispanClusterManager(){
	  return new InfinispanClusterManager();
  }

  @Bean
  public Vertx getVertx(ClusterManager clusterManager) throws InterruptedException, ExecutionException{
  VertxOptions options = new VertxOptions().setClusterManager(clusterManager);
  CompletableFuture<Vertx> future = new CompletableFuture<>();
  Vertx.clusteredVertx(options, ar -> {
    if (ar.succeeded()) {
      future.complete(ar.result());
    } else {
      future.completeExceptionally(ar.cause());
    }
  });
  return future.get();
  }
  

}