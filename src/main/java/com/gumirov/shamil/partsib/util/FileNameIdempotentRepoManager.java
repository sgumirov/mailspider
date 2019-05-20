package com.gumirov.shamil.partsib.util;

import com.gumirov.shamil.partsib.MainRouteBuilder;
import org.apache.camel.Expression;
import org.apache.camel.builder.SimpleBuilder;

import java.io.File;
import java.io.IOException;

import static org.apache.camel.builder.ExpressionBuilder.append;

/**
 * (c) 2017 by Shamil Gumirov (shamil@gumirov.com).<br/>
 * Date: 17/2/2017 Time: 02:36<br/>
 */
public class FileNameIdempotentRepoManager {
  private Expression expression;

  private String filename = "idempotent_repo.dat";

  public FileNameIdempotentRepoManager() throws IOException {
    startup();
  }

  public FileNameIdempotentRepoManager(String filename) throws IOException {
    this.filename = filename;
    startup();
  }

  public Expression createExpression(){
    if (expression == null) {
      expression = append(
          append(
              SimpleBuilder.simple("header."+ MainRouteBuilder.HeaderKeys.ENDPOINT_ID_HEADER),
              SimpleBuilder.simple("-")),
          append(
              append(
                  SimpleBuilder.simple("header.CamelFileName"),
                  SimpleBuilder.simple("-")),
              SimpleBuilder.simple("header.CamelFileLength")));
    }
    return expression;
  }

  public File getRepoFile(){
    return new File(filename);
  }

  public void startup() throws IOException {
    File idempotentRepo = new File(filename);
    if (!idempotentRepo.exists()) idempotentRepo.createNewFile();
  }
}
