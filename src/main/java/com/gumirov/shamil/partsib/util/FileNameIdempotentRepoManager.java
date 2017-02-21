package com.gumirov.shamil.partsib.util;

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
  private static Expression expression;
  private String filename = "target/idempotent_repo.dat";

  public static Expression createExpression(){
    if (expression == null) {
      expression = append(
          append(SimpleBuilder.simple("header.CamelFileName"),
          SimpleBuilder.simple("-")),
          SimpleBuilder.simple("header.CamelFileLength"));
    }
    return expression;
  }

  public void startup() throws IOException {
    File idempotentRepo = new File(filename);
    if (!idempotentRepo.exists()) idempotentRepo.createNewFile();
  }
}
